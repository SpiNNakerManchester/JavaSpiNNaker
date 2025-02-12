/*
 * Copyright (c) 2025 The University of Manchester
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
package uk.ac.manchester.spinnaker.spalloc;

import static java.net.InetAddress.getByName;
import static uk.ac.manchester.spinnaker.machine.ChipLocation.ZERO_ZERO;

import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import com.google.errorprone.annotations.CheckReturnValue;

import uk.ac.manchester.spinnaker.connections.BootConnection;
import uk.ac.manchester.spinnaker.connections.SCPConnection;
import uk.ac.manchester.spinnaker.connections.model.Connection;
import uk.ac.manchester.spinnaker.machine.HasCoreLocation;
import uk.ac.manchester.spinnaker.machine.MachineVersion;
import uk.ac.manchester.spinnaker.machine.MemoryLocation;
import uk.ac.manchester.spinnaker.spalloc.exceptions.SpallocServerException;
import uk.ac.manchester.spinnaker.transceiver.ParallelSafe;
import uk.ac.manchester.spinnaker.transceiver.ProcessException;
import uk.ac.manchester.spinnaker.transceiver.SpinnmanException;
import uk.ac.manchester.spinnaker.transceiver.Transceiver;

public class SpallocTransceiver extends Transceiver {

	private final SpallocJobAPI job;

	protected SpallocTransceiver(SpallocJobAPI job)
			throws IOException, SpinnmanException, InterruptedException,
			IllegalStateException, SpallocServerException {
		super(MachineVersion.bySize(job.getDimensions()),
				getConnectionsFromJob(job));
		this.job = job;
	}

	private static final List<Connection> getConnectionsFromJob(
			SpallocJobAPI job) throws IllegalStateException, IOException,
				SpallocServerException, InterruptedException {
		var connInfo = job.getConnections();
		if (connInfo == null) {
			return null;
		}
		String bootHost = null;
		for (var c : connInfo) {
			if (c.getChip().equals(ZERO_ZERO)) {
				bootHost = c.getHostname();
			}
		}
		if (bootHost == null) {
			return null;
		}
		var connections = new ArrayList<
				uk.ac.manchester.spinnaker.connections.model.Connection>();
		connections.add(new BootConnection(getByName(bootHost), null));
		for (var c : connInfo) {
			connections.add(
					new SCPConnection(c.getChip(), getByName(c.getHostname())));
		}
		return connections;
	}

	@Override
	@ParallelSafe
	public void writeMemory(HasCoreLocation core, MemoryLocation baseAddress,
			InputStream dataStream, int numBytes)
			throws IOException, ProcessException, InterruptedException {
		ByteBuffer data = ByteBuffer.allocate(numBytes);
		byte[] buffer = new byte[1024];
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
		try {
			this.job.writeMemory(core, baseAddress, data);
		} catch (SpallocServerException e) {
			throw new IOException(e);
		}
	}

	@Override
	@ParallelSafe
	public void writeMemory(HasCoreLocation core, MemoryLocation baseAddress,
			File dataFile)
			throws IOException, ProcessException, InterruptedException {
		try (var stream = new FileInputStream(dataFile)) {
			writeMemory(core, baseAddress, stream, (int) dataFile.length());
		}
	}

	@Override
	@ParallelSafe
	public void writeMemory(HasCoreLocation core, MemoryLocation baseAddress,
			ByteBuffer data)
			throws IOException, ProcessException, InterruptedException {
		try {
			this.job.writeMemory(core, baseAddress, data);
		} catch (SpallocServerException e) {
			throw new IOException(e);
		}
	}

	@Override
	@CheckReturnValue
	@ParallelSafe
	public ByteBuffer readMemory(HasCoreLocation core,
			MemoryLocation baseAddress, int length)
			throws IOException, ProcessException, InterruptedException {
		try {
			return job.readMemory(core, baseAddress, length);
		} catch (SpallocServerException e) {
			throw new IOException(e);
		}
	}

}
