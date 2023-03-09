/*
 * Copyright (c) 2018 The University of Manchester
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
package uk.ac.manchester.spinnaker.transceiver;

import static java.lang.Math.min;
import static java.nio.ByteBuffer.allocate;
import static java.nio.ByteOrder.LITTLE_ENDIAN;
import static java.util.Objects.requireNonNull;
import static uk.ac.manchester.spinnaker.messages.Constants.CPU_IOBUF_ADDRESS_OFFSET;
import static uk.ac.manchester.spinnaker.messages.Constants.UDP_MESSAGE_MAX_SIZE;
import static uk.ac.manchester.spinnaker.transceiver.Utils.getVcpuAddress;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayDeque;
import java.util.Iterator;
import java.util.Map;
import java.util.Queue;
import java.util.TreeMap;

import uk.ac.manchester.spinnaker.connections.ConnectionSelector;
import uk.ac.manchester.spinnaker.connections.SCPConnection;
import uk.ac.manchester.spinnaker.machine.CoreLocation;
import uk.ac.manchester.spinnaker.machine.CoreSubsets;
import uk.ac.manchester.spinnaker.machine.MemoryLocation;
import uk.ac.manchester.spinnaker.messages.model.IOBuffer;
import uk.ac.manchester.spinnaker.messages.scp.ClearIOBUF;
import uk.ac.manchester.spinnaker.messages.scp.ReadMemory;
import uk.ac.manchester.spinnaker.messages.scp.UpdateProvenanceAndExit;
import uk.ac.manchester.spinnaker.messages.scp.UpdateRuntime;
import uk.ac.manchester.spinnaker.utils.DefaultMap;
import uk.ac.manchester.spinnaker.utils.MappableIterable;

/**
 * A process for controlling an application running on a SpiNNaker core. The
 * operations on this process <em>do not make sense when applied to a SCAMP
 * core;</em> they only apply to application cores.
 *
 * @author Donal Fellows
 */
final class RuntimeControlProcess extends TxrxProcess {
	private static final int BUF_HEADER_BYTES = 16;

	private static final int BLOCK_HEADER_BYTES = 16;

	private static final int WORD = 4;

	private final Queue<NextRead> nextReads = new ArrayDeque<>();

	private final Queue<ExtraRead> extraReads = new ArrayDeque<>();

	private final Map<CoreLocation, Map<Integer, ByteBuffer>> iobuf =
			new DefaultMap<>(TreeMap::new);

	/**
	 * @param connectionSelector
	 *            How to select how to communicate.
	 * @param retryTracker
	 *            Object used to track how many retries were used in an
	 *            operation. May be {@code null} if no suck tracking is
	 *            required.
	 */
	RuntimeControlProcess(
			ConnectionSelector<? extends SCPConnection> connectionSelector,
			RetryTracker retryTracker) {
		super(connectionSelector, retryTracker);
	}

	/**
	 * Clear the IOBUF buffers of a core.
	 *
	 * @param core
	 *            the core where the IOBUF is.
	 * @throws IOException
	 *             If anything goes wrong with networking.
	 * @throws ProcessException
	 *             If SpiNNaker rejects the message.
	 * @throws InterruptedException
	 *             If the communications were interrupted.
	 */
	void clearIOBUF(CoreLocation core)
			throws IOException, ProcessException, InterruptedException {
		call(new ClearIOBUF(core));
	}

	/**
	 * Clear the IOBUF buffers of some cores.
	 *
	 * @param coreSubsets
	 *            the cores where the IOBUFs are.
	 * @throws IOException
	 *             If anything goes wrong with networking.
	 * @throws ProcessException
	 *             If SpiNNaker rejects the message.
	 * @throws InterruptedException
	 *             If the communications were interrupted.
	 */
	void clearIOBUF(CoreSubsets coreSubsets)
			throws IOException, ProcessException, InterruptedException {
		for (var core : requireNonNull(coreSubsets,
				"must have actual core subset to iterate over")) {
			sendRequest(new ClearIOBUF(core));
		}
		finishBatch();
	}

	/**
	 * Update the running time configuration of some cores.
	 *
	 * @param runTimesteps
	 *            The number of machine timesteps to run for. {@code null}
	 *            indicates an infinite run.
	 * @param currentTime
	 *            The current simulation time.
	 * @param syncTimesteps
	 *            The number of timesteps before we pause to synchronise.
	 * @param coreSubsets
	 *            the cores to update the information of.
	 * @throws IOException
	 *             If anything goes wrong with networking.
	 * @throws ProcessException
	 *             If SpiNNaker rejects the message.
	 * @throws InterruptedException
	 *             If the communications were interrupted.
	 */
	void updateRuntime(Integer runTimesteps, int currentTime, int syncTimesteps,
			CoreSubsets coreSubsets)
			throws IOException, ProcessException, InterruptedException {
		int runTime = (runTimesteps == null ? 0 : runTimesteps);
		boolean infiniteRun = runTimesteps == null;
		for (var core : requireNonNull(coreSubsets,
				"must have actual core subset to iterate over")) {
			sendRequest(new UpdateRuntime(core, runTime, infiniteRun,
					currentTime, syncTimesteps));
		}
		finishBatch();
	}

	/**
	 * Ask some cores to update their provenance data and exit. It is up to the
	 * caller to check for the cores' response, which is by changing state to
	 * the exited state.
	 *
	 * @param coreSubsets
	 *            the cores to update the provenance of.
	 * @throws IOException
	 *             If anything goes wrong with networking.
	 * @throws InterruptedException
	 *             If communications are interrupted.
	 */
	void updateProvenanceAndExit(CoreSubsets coreSubsets)
			throws IOException, InterruptedException {
		for (var core : requireNonNull(coreSubsets,
				"must have actual core subset to iterate over")) {
			sendOneWayRequest(new UpdateProvenanceAndExit(core));
		}
	}

	private static int chunk(int overall) {
		return min(overall, UDP_MESSAGE_MAX_SIZE);
	}

	/**
	 * Get the IOBUF buffers from some cores.
	 *
	 * @param size
	 *            How much to read.
	 * @param cores
	 *            Which cores to read from.
	 * @return The buffers. It is the responsibility of the caller to handle
	 *         whatever order they are produced in.
	 * @throws IOException
	 *             If anything goes wrong with networking.
	 * @throws ProcessException
	 *             If SpiNNaker rejects a message.
	 * @throws InterruptedException
	 *             If the communications were interrupted.
	 */
	MappableIterable<IOBuffer> readIOBuf(int size, CoreSubsets cores)
			throws ProcessException, IOException, InterruptedException {
		// Get the IOBuf address for each core
		for (var core : requireNonNull(cores,
				"must have actual core subset to iterate over")) {
			sendGet(new ReadMemory(core.getScampCore(),
					getVcpuAddress(core).add(CPU_IOBUF_ADDRESS_OFFSET), WORD),
					bytes -> issueReadForIOBufHead(core, 0,
							new MemoryLocation(bytes.getInt()),
							chunk(size + BUF_HEADER_BYTES)));
		}
		finishBatch();

		// Run rounds of the process until reading is complete
		while (!nextReads.isEmpty() || !extraReads.isEmpty()) {
			while (!extraReads.isEmpty()) {
				var read = extraReads.remove();
				sendGet(read.message(),
						bytes -> saveIOBufTailSection(read, bytes));
			}

			while (!nextReads.isEmpty()) {
				var read = nextReads.remove();
				sendGet(read.message(), bytes -> {
					// Unpack the IOBuf header
					var nextAddress = new MemoryLocation(bytes.getInt());
					bytes.getLong(); // Ignore 8 bytes
					int bytesToRead = bytes.getInt();

					// Save the rest of the IOBuf
					int packetBytes = saveIOBufHead(read, bytes, bytesToRead);

					// Ask for the rest of the IOBuf buffer to be copied over
					issueReadsForIOBufTail(read, bytesToRead,
							read.base.add(packetBytes + BLOCK_HEADER_BYTES),
							packetBytes);

					// If there is another IOBuf buffer, read this next
					issueReadForIOBufHead(read.core, read.blockID + 1,
							nextAddress, read.size);
				});
			}

			finishBatch();
		}

		return () -> new Iterator<IOBuffer>() {
			private final Iterator<CoreLocation> cores =
					iobuf.keySet().iterator();

			@Override
			public boolean hasNext() {
				return cores.hasNext();
			}

			@Override
			public IOBuffer next() {
				var core = cores.next();
				return new IOBuffer(core, iobuf.get(core).values());
			}
		};
	}

	private void issueReadForIOBufHead(CoreLocation core, int blockID,
			MemoryLocation next, int size) {
		if (!next.isNull()) {
			nextReads.add(new NextRead(core, blockID, next, size));
		}
	}

	private int saveIOBufHead(NextRead read, ByteBuffer bytes,
			int bytesToRead) {
		// Create a buffer for the data
		var buffer = allocate(bytesToRead).order(LITTLE_ENDIAN);
		// Put the data from this packet into the buffer
		int packetBytes = min(bytes.remaining(), bytesToRead);
		if (packetBytes > 0) {
			buffer.put(bytes);
		}
		iobuf.get(read.core).put(read.blockID, buffer);
		return packetBytes;
	}

	private void issueReadsForIOBufTail(NextRead read, int bytesToRead,
			MemoryLocation baseAddress, int readOffset) {
		bytesToRead -= readOffset;
		// While more reads need to be done to read the data
		while (bytesToRead > 0) {
			// Read the next bit of memory making up the buffer
			int next = chunk(bytesToRead);
			extraReads.add(new ExtraRead(read, baseAddress, next, readOffset));
			baseAddress = baseAddress.add(next);
			readOffset += next;
			bytesToRead -= next;
		}
	}

	private void saveIOBufTailSection(ExtraRead read, ByteBuffer bytes) {
		var buffer = iobuf.get(read.core).get(read.blockID);
		synchronized (buffer) {
			buffer.position(read.offset);
			buffer.put(bytes);
		}
	}

	private static class NextRead {
		final CoreLocation core;

		final int blockID;

		final MemoryLocation base;

		final int size;

		NextRead(CoreLocation core, int blockID, MemoryLocation base,
				int size) {
			this.core = core;
			this.blockID = blockID;
			this.base = base;
			this.size = size;
		}

		/**
		 * @return the message implied by this object
		 */
		ReadMemory message() {
			return new ReadMemory(core.getScampCore(), base, size);
		}
	}

	private static class ExtraRead {
		final CoreLocation core;

		final int blockID;

		final MemoryLocation base;

		final int size;

		final int offset;

		ExtraRead(NextRead head, MemoryLocation base, int size, int offset) {
			this.core = head.core;
			this.blockID = head.blockID;
			this.base = base;
			this.size = size;
			this.offset = offset;
		}

		/**
		 * @return the message implied by this object
		 */
		ReadMemory message() {
			return new ReadMemory(core.getScampCore(), base, size);
		}
	}
}
