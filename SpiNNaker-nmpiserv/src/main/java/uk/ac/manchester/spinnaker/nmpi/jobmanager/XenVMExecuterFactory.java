/*
 * Copyright (c) 2014 The University of Manchester
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package uk.ac.manchester.spinnaker.nmpi.jobmanager;

import static com.xensource.xenapi.Session.loginWithPassword;
import static com.xensource.xenapi.Session.logout;
import static com.xensource.xenapi.Types.VbdMode.RW;
import static com.xensource.xenapi.Types.VbdType.DISK;
import static com.xensource.xenapi.Types.VdiType.USER;
import static com.xensource.xenapi.Types.VmPowerState.HALTED;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNull;
import static java.util.UUID.randomUUID;
import static org.slf4j.LoggerFactory.getLogger;
import static uk.ac.manchester.spinnaker.nmpi.ThreadUtils.sleep;
import static uk.ac.manchester.spinnaker.nmpi.ThreadUtils.waitfor;
import static uk.ac.manchester.spinnaker.nmpi.model.job.JobManagerInterface.JOB_PROCESS_MANAGER;

import java.io.IOException;
import java.net.URL;

import org.apache.xmlrpc.XmlRpcException;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Value;

import com.google.errorprone.annotations.concurrent.GuardedBy;
import com.xensource.xenapi.Connection;
import com.xensource.xenapi.Types.VmPowerState;
import com.xensource.xenapi.Types.XenAPIException;
import com.xensource.xenapi.VBD;
import com.xensource.xenapi.VDI;
import com.xensource.xenapi.VM;

/**
 * Executer factory that uses Xen VMs.
 */
public final class XenVMExecuterFactory implements JobExecuterFactory {
	/** Bytes in a gigabyte. Well, a gibibyte, but that's a nasty word. */
	private static final long GB = 1024L * 1024L * 1024L;

	/** Time (in ms) between checks to see if a VM is running. */
	private static final int VM_POLL_INTERVAL = 10000;

	/** Lock object used for synchronisation. */
	private final Object lock = new Object();

	/** The thread group of any threads. */
	private final ThreadGroup threadGroup;

	/** Logging. */
	private static final Logger logger = getLogger(Executer.class);

	/** The URL of the Xen Server. */
	@Value("${xen.server.url}")
	private URL xenServerUrl;

	/** The username under which to control Xen. */
	@Value("${xen.server.username}")
	private String username;

	/** The password for the username. */
	@Value("${xen.server.password}")
	private String password;

	/** The name of the Xen Template VM. */
	@Value("${xen.server.templateVm}")
	private String templateLabel;

	/** True if the VMs should be deleted on shutdown of the VM. */
	@Value("${deleteJobsOnExit}")
	private boolean deleteOnExit;

	/** True if the Xen VM should shutdown on exit of the job. */
	@Value("${xen.server.shutdownOnExit}")
	private boolean shutdownOnExit;

	/** True if the log of the job should upload as it is output. */
	@Value("${liveUploadOutput}")
	private boolean liveUploadOutput;

	/** True if a SpiNNaker machine should be requested. */
	@Value("${requestSpiNNakerMachine}")
	private boolean requestSpiNNakerMachine;

	/** The default size of the disk to attach to the Xen VM. */
	@Value("${xen.server.diskspaceInGbs}")
	private long defaultDiskSizeInGbs;

	/** The maximum number of VMs to create. */
	@Value("${xen.server.maxVms}")
	private int maxNVirtualMachines;

	/** The current number of VMs. */
	@GuardedBy("lock")
	private int nVirtualMachines = 0;

	/**
	 * Create a new Xen Executor Factory.
	 */
	public XenVMExecuterFactory() {
		this.threadGroup = new ThreadGroup("XenVM");
	}

	@Override
	public JobExecuter createJobExecuter(JobManager manager, URL baseUrl)
			throws IOException {
		requireNonNull(manager);
		requireNonNull(baseUrl);
		waitToClaimVM();

		try {
			return new Executer(manager, baseUrl);
		} catch (Exception e) {
			throw new IOException("Error creating VM", e);
		}
	}

	/**
	 * Wait for the VM to come up.
	 */
	private void waitToClaimVM() {
		synchronized (lock) {
			logger.info("{} of {} in use", nVirtualMachines,
					maxNVirtualMachines);
			while (nVirtualMachines >= maxNVirtualMachines) {
				logger.debug("Waiting for a VM to become available "
						+ "({} of {} in use)", nVirtualMachines,
						maxNVirtualMachines);
				waitfor(lock);
			}
			nVirtualMachines++;
		}
	}

	/** Callback when the executor is finished. */
	protected void executorFinished() {
		synchronized (lock) {
			nVirtualMachines--;
			logger.info("{} of {} now in use", nVirtualMachines,
					maxNVirtualMachines);
			lock.notifyAll();
		}
	}

	/** Taming the Xen API a bit. */
	class XenConnection implements AutoCloseable {
		/** The Xen connection. */
		private Connection conn;

		/** The ID of the executor on the VM. */
		private final String id;

		/**
		 * Make a connection to the Xen server.
		 *
		 * @param idParam
		 *            The ID of the connection.
		 * @throws XenAPIException
		 *             something went wrong
		 * @throws XmlRpcException
		 *             something went wrong
		 */
		XenConnection(String idParam) throws XenAPIException, XmlRpcException {
			this.id = idParam;
			conn = new Connection(xenServerUrl);
			loginWithPassword(conn, username, password);
		}

		/**
		 * Get a virtual machine.
		 *
		 * @return a VM handle
		 * @throws XmlRpcException
		 *             something went wrong
		 * @throws IOException
		 *             something went wrong
		 */
		VM getVirtualMachine() throws XmlRpcException, IOException {
			var vmsWithLabel = VM.getByNameLabel(conn, templateLabel);
			if (vmsWithLabel.isEmpty()) {
				throw new IOException("No template with name " + templateLabel
						+ " was found");
			}
			var template = vmsWithLabel.iterator().next();
			return template.createClone(conn, templateLabel + "_" + id);
		}

		/**
		 * Get the first block device attached to a VM.
		 *
		 * @param vm
		 *            the VM that the block device is attached to.
		 * @return the block device
		 * @throws XmlRpcException
		 *             something went wrong
		 * @throws IOException
		 *             something went wrong
		 */
		VBD getVirtualBlockDevice(VM vm)
				throws XmlRpcException, IOException {
			var disks = vm.getVBDs(conn);
			if (disks.isEmpty()) {
				throw new IOException("No disks found on " + templateLabel);
			}
			return disks.iterator().next();
		}

		/**
		 * Get the label for a disk image.
		 *
		 * @param vdi
		 *            The disk image
		 * @param suffix
		 *            The operational suffix
		 * @return The label
		 * @throws XmlRpcException
		 *             something went wrong
		 * @throws XenAPIException
		 *             something went wrong
		 */
		String getLabel(VDI vdi, String suffix)
				throws XmlRpcException, XenAPIException {
			return vdi.getNameLabel(conn) + "_" + id + "_" + suffix;
		}

		/**
		 * Get a base disk image.
		 *
		 * @param disk
		 *            The block device hosting the image.
		 * @return the disk image.
		 * @throws XmlRpcException
		 *             something went wrong
		 * @throws XenAPIException
		 *             something went wrong
		 */
		VDI getBaseVDI(VBD disk) throws XmlRpcException, XenAPIException {
			var vdi = disk.getVDI(conn);
			vdi.setNameLabel(conn, getLabel(vdi, "base"));
			return vdi;
		}

		/**
		 * Create a derived disk image.
		 *
		 * @param baseVDI
		 *            The base disk image.
		 * @return the disk image.
		 * @throws XenAPIException
		 *             something went wrong
		 * @throws XmlRpcException
		 *             something went wrong
		 */
		VDI createVDI(VDI baseVDI)
				throws XenAPIException, XmlRpcException {
			var descriptor = new VDI.Record();
			descriptor.nameLabel = getLabel(baseVDI, "storage");
			descriptor.type = USER;
			descriptor.SR = baseVDI.getSR(conn);
			descriptor.virtualSize = defaultDiskSizeInGbs * GB;

			return VDI.create(conn, descriptor);
		}

		/**
		 * Create a block device.
		 *
		 * @param vm
		 *            The VM to attach the device to.
		 * @param vdi
		 *            The disk image to initialise the device with.
		 * @return the block device
		 * @throws XenAPIException
		 *             something went wrong
		 * @throws XmlRpcException
		 *             something went wrong
		 */
		VBD createVBD(VM vm, VDI vdi)
				throws XenAPIException, XmlRpcException {
			var descriptor = new VBD.Record();
			descriptor.VM = vm;
			descriptor.VDI = vdi;
			descriptor.userdevice = "1";
			descriptor.mode = RW;
			descriptor.type = DISK;

			return VBD.create(conn, descriptor);
		}

		/**
		 * Adds boot data to a VM.
		 *
		 * @param vm
		 *            The VM to add the data to.
		 * @param key
		 *            The key for the data.
		 * @param value
		 *            The value for the data. Will be converted to string.
		 * @throws XenAPIException
		 *             something went wrong
		 * @throws XmlRpcException
		 *             something went wrong
		 */
		void addData(VM vm, String key, Object value)
				throws XenAPIException, XmlRpcException {
			vm.addToXenstoreData(conn, key, value.toString());
		}

		/**
		 * Boot up a VM.
		 *
		 * @param vm
		 *            The VM to boot.
		 * @throws XenAPIException
		 *             something went wrong
		 * @throws XmlRpcException
		 *             something went wrong
		 */
		void start(VM vm) throws XenAPIException, XmlRpcException {
			vm.start(conn, false, true);
		}

		/**
		 * Destroy a VM. Shuts the VM down as well if required.
		 *
		 * @param vm
		 *            The VM to destroy.
		 * @throws XenAPIException
		 *             something went wrong
		 * @throws XmlRpcException
		 *             something went wrong
		 */
		void destroy(VM vm) throws XenAPIException, XmlRpcException {
			vm.destroy(conn);
		}

		/**
		 * Destroy a block device.
		 *
		 * @param vbd
		 *            The block device to destroy.
		 * @throws XenAPIException
		 *             something went wrong
		 * @throws XmlRpcException
		 *             something went wrong
		 */
		void destroy(VBD vbd) throws XenAPIException, XmlRpcException {
			vbd.destroy(conn);
		}

		/**
		 * Destroy a disk image.
		 *
		 * @param vdi
		 *            The disk image to destroy.
		 * @throws XenAPIException
		 *             something went wrong
		 * @throws XmlRpcException
		 *             something went wrong
		 */
		void destroy(VDI vdi) throws XenAPIException, XmlRpcException {
			vdi.destroy(conn);
		}

		/**
		 * Get the state of a VM.
		 *
		 * @param vm
		 *            The VM to get the state of.
		 * @return The state.
		 * @throws XenAPIException
		 *             something went wrong
		 * @throws XmlRpcException
		 *             something went wrong
		 */
		VmPowerState getState(VM vm)
				throws XenAPIException, XmlRpcException {
			return vm.getPowerState(conn);
		}

		@Override
		public void close() {
			try {
				logout(conn);
			} catch (XenAPIException | XmlRpcException e) {
				logger.error("problem when closing connection; "
						+ "resource potentially leaked", e);
			} finally {
				conn = null;
			}
		}
	}

	/**
	 * The executer core connector.
	 */
	protected final class Executer implements JobExecuter {
		// Parameters from constructor
		/** The Job Manager to report to. */
		private final JobManager jobManager;

		/** The UUID to assign to this executor. */
		private final String uuid;

		/**
		 * The URL to give to the job process manager for it to speak to the
		 * job manager.
		 */
		private final URL jobProcessManagerUrl;

		/** The arguments for the job process manager. */
		private final String args;

		// Internal entities
		/** The VM of the job. */
		private VM clonedVm;

		/** The disk of the cloned VM. */
		private VBD disk;

		/** The Virtual Disk Image of the cloned VM. */
		private VDI vdi;

		/** The Virtual Disk Image added to the VM. */
		private VDI extraVdi;

		/** The disk added to the VM. */
		private VBD extraDisk;

		/**
		 * Instantiate a connector.
		 *
		 * @param jobManagerParam
		 *            The job manager.
		 * @param baseUrl
		 *            the service root URL.
		 * @throws XmlRpcException
		 *             something went wrong
		 * @throws IOException
		 *             something went wrong
		 */
		Executer(JobManager jobManagerParam, URL baseUrl)
				throws XmlRpcException, IOException {
			this.jobManager = jobManagerParam;
			uuid = randomUUID().toString();
			jobProcessManagerUrl =
					new URL(baseUrl, "job/" + JOB_PROCESS_MANAGER);

			var execArgs = new StringBuilder("-jar ");
			execArgs.append(JOB_PROCESS_MANAGER);
			execArgs.append(" --serverUrl ");
			execArgs.append(baseUrl);
			execArgs.append(" --executerId ");
			execArgs.append(uuid);
			if (deleteOnExit) {
				execArgs.append(" --deleteOnExit");
			}
			if (liveUploadOutput) {
				execArgs.append(" --liveUploadOutput");
			}
			if (requestSpiNNakerMachine) {
				execArgs.append(" --requestMachine");
			}
			this.args = execArgs.toString();
		}

		@Override
		public String getExecuterId() {
			return uuid;
		}

		@Override
		public void startExecuter() {
			new Thread(threadGroup, this::runInVm,
					"Executer (" + uuid + ")").start();
		}

		/**
		 * Make a VM and launch it.
		 *
		 * @param conn
		 *            The underlying connection to the Xen server.
		 * @throws XmlRpcException
		 *             something went wrong
		 * @throws IOException
		 *             something went wrong
		 */
		synchronized void createVm(XenConnection conn)
				throws XmlRpcException, IOException {
			clonedVm = conn.getVirtualMachine();
			disk = conn.getVirtualBlockDevice(clonedVm);
			vdi = conn.getBaseVDI(disk);
			extraVdi = conn.createVDI(vdi);
			extraDisk = conn.createVBD(clonedVm, extraVdi);
			conn.addData(clonedVm, "vm-data/nmpiurl", jobProcessManagerUrl);
			conn.addData(clonedVm, "vm-data/nmpifile", JOB_PROCESS_MANAGER);
			conn.addData(clonedVm, "vm-data/nmpiargs", args);
			if (shutdownOnExit) {
				conn.addData(clonedVm, "vm-data/shutdown", true);
			}
			conn.start(clonedVm);
		}

		/**
		 * Delete a VM for a job.
		 *
		 * @param conn
		 *            The connection to the Xen server to use.
		 * @throws XenAPIException
		 *             If there is a Xen API issue
		 * @throws XmlRpcException
		 *             If there is an error speaking to Xen
		 */
		private synchronized void deleteVm(XenConnection conn)
				throws XenAPIException, XmlRpcException {
			if (isNull(conn)) {
				return;
			}
			if (nonNull(disk)) {
				conn.destroy(disk);
			}
			if (nonNull(extraDisk)) {
				conn.destroy(extraDisk);
			}
			if (nonNull(vdi)) {
				conn.destroy(vdi);
			}
			if (nonNull(extraVdi)) {
				conn.destroy(extraVdi);
			}
			if (nonNull(clonedVm)) {
				conn.destroy(clonedVm);
			}
		}

		/**
		 * Wait until the running VM shuts down.
		 *
		 * @param conn
		 *            The connection to Xen
		 * @throws XenAPIException
		 *             If there is a Xen API issue
		 * @throws XmlRpcException
		 *             If there is an error speaking to Xen
		 */
		private void waitForHalt(XenConnection conn)
				throws XenAPIException, XmlRpcException {
			VmPowerState powerState;
			do {
				sleep(VM_POLL_INTERVAL);
				powerState = conn.getState(clonedVm);
				logger.debug("VM for {} is in state {}", uuid, powerState);
			} while (powerState != HALTED);
		}

		/**
		 * Run the VM.
		 *
		 * @param conn The connection to Xen
		 */
		private void runInVm(XenConnection conn) {
			String action = null;
			try {
				action = "setting up VM";
				createVm(conn);
				action = "getting VM power state; assuming off";
				waitForHalt(conn);
				jobManager.setExecutorExited(uuid, null);
			} catch (Exception e) {
				logger.error("Error " + action, e);
				jobManager.setExecutorExited(uuid, e.getMessage());
			} finally {
				try {
					if (deleteOnExit) {
						deleteVm(conn);
					}
				} catch (Exception e) {
					logger.error("Error deleting VM", e);
				}
			}
		}

		/**
		 * Connect to Xen and run the VM. Notifies the factory when done.
		 */
		private void runInVm() {
			try (var conn = new XenConnection(uuid)) {
				runInVm(conn);
			} catch (Exception e) {
				logger.error("Error talking to Xen", e);
				jobManager.setExecutorExited(uuid, e.getMessage());
			} finally {
				executorFinished();
			}
		}
	}
}
