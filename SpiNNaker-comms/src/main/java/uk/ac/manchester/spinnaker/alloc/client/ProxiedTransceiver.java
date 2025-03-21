/*
 * Copyright (c) 2022 The University of Manchester
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
package uk.ac.manchester.spinnaker.alloc.client;

import static uk.ac.manchester.spinnaker.messages.Constants.UDP_MESSAGE_MAX_SIZE;
import static uk.ac.manchester.spinnaker.machine.ChipLocation.ZERO_ZERO;
import static uk.ac.manchester.spinnaker.utils.InetFactory.getByNameQuietly;

import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.errorprone.annotations.CheckReturnValue;
import com.google.errorprone.annotations.MustBeClosed;

import uk.ac.manchester.spinnaker.alloc.client.SpallocClient.Job;
import uk.ac.manchester.spinnaker.connections.EIEIOConnection;
import uk.ac.manchester.spinnaker.connections.SCPConnection;
import uk.ac.manchester.spinnaker.connections.model.Connection;
import uk.ac.manchester.spinnaker.machine.ChipLocation;
import uk.ac.manchester.spinnaker.machine.HasCoreLocation;
import uk.ac.manchester.spinnaker.machine.MachineVersion;
import uk.ac.manchester.spinnaker.machine.MemoryLocation;
import uk.ac.manchester.spinnaker.storage.BufferManagerStorage;
import uk.ac.manchester.spinnaker.storage.StorageException;
import uk.ac.manchester.spinnaker.storage.BufferManagerStorage.Region;
import uk.ac.manchester.spinnaker.transceiver.ParallelSafe;
import uk.ac.manchester.spinnaker.transceiver.ProcessException;
import uk.ac.manchester.spinnaker.transceiver.SpinnmanException;
import uk.ac.manchester.spinnaker.transceiver.Transceiver;

/** A transceiver that routes messages across the proxy. */
final class ProxiedTransceiver extends Transceiver {

	/** Size of the buffer for moving bytes around. */
	private static final int BUFFER_SIZE = 1024;

	private final Job job;

	private final ProxyProtocolClient websocket;

	private final Map<InetAddress, ChipLocation> hostToChip = new HashMap<>();

	/**
	 * @param version
	 *            The version of the machine connected to.
	 * @param connections
	 *            The proxied connections we will use.
	 * @param hostToChip
	 *            The mapping from addresses to chip locations, to enable
	 *            manufacturing of proxied {@link EIEIOConnection}s.
	 * @param websocket
	 *            The proxy handle.
	 * @throws SpinnmanException
	 * @throws IOException
	 *             If we couldn't finish setting up our networking.
	 * @throws InterruptedException
	 *             If communications are interrupted.
	 * @throws SpinnmanExcception
	 *             If SpiNNaker rejects a message.
	 */
	@MustBeClosed
	ProxiedTransceiver(Job job,
			ProxyProtocolClient websocket)
			throws IOException, SpinnmanException, InterruptedException {
		super(getVersion(job), getConnections(job, websocket));
		this.job = job;
		this.websocket = websocket;

		for (Connection conn : getConnections()) {
			if (conn instanceof ProxiedSCPConnection) {
				ProxiedSCPConnection pConn = (ProxiedSCPConnection) conn;
				hostToChip.put(pConn.getRemoteIPAddress(), pConn.getChip());
			}
		}
	}

	private static MachineVersion getVersion(Job job) throws IOException {
		var machine = job.machine();
		return MachineVersion.bySize(machine.getWidth(), machine.getHeight());
	}

	private static List<Connection> getConnections(Job job,
			ProxyProtocolClient ws) throws IOException, InterruptedException {
		var conns = new ArrayList<Connection>();
		var machine = job.machine();
		InetAddress bootChipAddress = null;
		for (var bc : machine.getConnections()) {
			var chipAddr = getByNameQuietly(bc.getHostname());
			var chipLoc = bc.getChip().asChipLocation();
			conns.add(new ProxiedSCPConnection(chipLoc, ws, chipAddr));
			if (chipLoc.equals(ZERO_ZERO)) {
				bootChipAddress = chipAddr;
			}
		}
		if (bootChipAddress != null) {
			conns.add(new ProxiedBootConnection(ws, bootChipAddress));
		}
		return conns;
	}

	/** {@inheritDoc} */
	@Override
	public void close() throws IOException {
		super.close();
		websocket.close();
	}

	@Override
	public SCPConnection createScpConnection(ChipLocation chip,
			InetAddress addr) throws IOException {
		try {
			return new ProxiedSCPConnection(chip, websocket, addr);
		} catch (InterruptedException e) {
			throw new IOException("failed to proxy connection", e);
		}
	}

	@Override
	protected EIEIOConnection newEieioConnection(InetAddress localHost,
			Integer localPort) throws IOException {
		try {
			return new ProxiedEIEIOListenerConnection(hostToChip, websocket);
		} catch (InterruptedException e) {
			throw new IOException("failed to proxy connection", e);
		}
	}

	@Override
	@ParallelSafe
	public void writeMemory(HasCoreLocation core, MemoryLocation baseAddress,
			InputStream dataStream, int numBytes)
			throws IOException, ProcessException, InterruptedException {
		// If this will use a single message, just use SCP
		if (numBytes <= UDP_MESSAGE_MAX_SIZE) {
			super.writeMemory(core, baseAddress, dataStream, numBytes);
		} else {
			ByteBuffer data = ByteBuffer.allocate(numBytes);
			byte[] buffer = new byte[BUFFER_SIZE];
			int remaining = numBytes;
			while (remaining > 0) {
				int toRead = Math.min(remaining, buffer.length);
				int read = dataStream.read(buffer, 0, toRead);
				if (read < 0) {
					throw new EOFException();
				}
				data.put(buffer, 0, read);
				remaining -= read;
			}
			this.job.writeMemory(core, baseAddress, data);
		}
	}

	@Override
	@ParallelSafe
	public void writeMemory(HasCoreLocation core, MemoryLocation baseAddress,
			File dataFile)
			throws IOException, ProcessException, InterruptedException {
		// If this will use a single message, just use SCP
		if (dataFile.length() <= UDP_MESSAGE_MAX_SIZE) {
			super.writeMemory(core, baseAddress, dataFile);
		} else {
			try (var stream = new FileInputStream(dataFile)) {
				writeMemory(core, baseAddress, stream, (int) dataFile.length());
			}
		}
	}

	@Override
	@ParallelSafe
	public void writeMemory(HasCoreLocation core, MemoryLocation baseAddress,
			ByteBuffer data)
			throws IOException, ProcessException, InterruptedException {
		// If this will use a single message, just use SCP
		if (data.remaining() <= UDP_MESSAGE_MAX_SIZE) {
			super.writeMemory(core, baseAddress, data);
		} else {
			this.job.writeMemory(core, baseAddress, data);
		}
	}

	@Override
	@CheckReturnValue
	@ParallelSafe
	public ByteBuffer readMemory(HasCoreLocation core,
			MemoryLocation baseAddress, int length)
			throws IOException, ProcessException, InterruptedException {
		// If this will use a single message, just use SCP
		if (length <= UDP_MESSAGE_MAX_SIZE) {
			return super.readMemory(core, baseAddress, length);
		} else {
			return job.readMemory(core, baseAddress, length);
		}
	}

	@Override
	public void readRegion(Region region, BufferManagerStorage storage)
			throws IOException, ProcessException, StorageException,
			InterruptedException {
		if (region.size < UDP_MESSAGE_MAX_SIZE) {
			super.readRegion(region, storage);
		} else {
			var buffer = job.readMemory(
					region.core, region.startAddress, region.size);
			storage.addRecordingContents(region, buffer);
		}
	}
}
