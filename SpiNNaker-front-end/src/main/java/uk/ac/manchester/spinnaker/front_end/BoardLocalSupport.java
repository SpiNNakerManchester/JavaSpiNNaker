/*
 * Copyright (c) 2019 The University of Manchester
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
package uk.ac.manchester.spinnaker.front_end;

import static java.util.stream.Collectors.toList;
import static uk.ac.manchester.spinnaker.alloc.client.SpallocClientFactory.getJobFromProxyInfo;
import static uk.ac.manchester.spinnaker.messages.Constants.SCP_SCAMP_PORT;

import java.io.IOException;
import java.util.List;

import org.slf4j.MDC;
import org.slf4j.MDC.MDCCloseable;

import com.google.errorprone.annotations.MustBeClosed;

import uk.ac.manchester.spinnaker.alloc.client.SpallocClient;
import uk.ac.manchester.spinnaker.connections.MachineAware;
import uk.ac.manchester.spinnaker.machine.HasChipLocation;
import uk.ac.manchester.spinnaker.machine.Machine;
import uk.ac.manchester.spinnaker.storage.ProxyAwareStorage;
import uk.ac.manchester.spinnaker.storage.StorageException;
import uk.ac.manchester.spinnaker.transceiver.SpinnmanException;
import uk.ac.manchester.spinnaker.transceiver.Transceiver;
import uk.ac.manchester.spinnaker.transceiver.TransceiverInterface;
import uk.ac.manchester.spinnaker.transceiver.Transceiver.ConnectionDescriptor;

/**
 * A class for making things easier to do on a per-board basis.
 *
 * @author Donal Fellows
 */
public abstract class BoardLocalSupport {
	private static final String BOARD_ROOT = "boardRoot";

	/** A storage object that is aware of proxies. */
	protected final ProxyAwareStorage storage;

	/** The description of the SpiNNaker machine. */
	protected final Machine machine;

	/**
	 * @param storage
	 *            Database containing information about how to speak to the
	 *            proxy.
	 * @param machine
	 *            Which machine is this on? Used for address mapping and
	 *            provided as a general service to subclasses.
	 */
	protected BoardLocalSupport(ProxyAwareStorage storage, Machine machine) {
		this.storage = storage;
		this.machine = machine;
	}

	private String root(HasChipLocation chipLoc) {
		var root = machine.getChipAt(chipLoc).nearestEthernet;
		return "(board:" + root.getX() + "," + root.getY() + ")";
	}

	@MustBeClosed
	@SuppressWarnings("MustBeClosed")
	protected TransceiverInterface getTransceiver()
			throws IOException, StorageException, SpinnmanException,
			InterruptedException {
		final TransceiverInterface txrx;
		var job = getJob();
		if (job == null) {
			// No job; must be a direct connection
			txrx = Transceiver.makeWithDescriptors(
					machine.version, generateScampConnections());
		} else {
			txrx = job.getTransceiver();
		}
		var scpSelector = txrx.getScampConnectionSelector();
		if (scpSelector instanceof MachineAware) {
			((MachineAware) scpSelector).setMachine(machine);
		}
		return txrx;
	}

	private List<ConnectionDescriptor> generateScampConnections() {
		return machine.ethernetConnectedChips().stream()
				.map(chip -> new ConnectionDescriptor(chip.ipAddress,
						SCP_SCAMP_PORT, chip.asChipLocation()))
				.collect(toList());
	}

	protected SpallocClient.Job getJob() throws IOException, StorageException {
		if (storage == null) {
			return null;
		}
		return getJobFromProxyInfo(storage.getProxyInformation());
	}

	/**
	 * A context that can be pushed to state in logging what board is the root.
	 *
	 * @author Donal Fellows
	 */
	protected final class BoardLocal implements AutoCloseable {
		private MDCCloseable context;

		/**
		 * @param chipLocation
		 *            The location of a chip on its board.
		 */
		public BoardLocal(HasChipLocation chipLocation) {
			context = MDC.putCloseable(BOARD_ROOT, root(chipLocation));
		}

		@Override
		public void close() {
			context.close();
		}
	}
}
