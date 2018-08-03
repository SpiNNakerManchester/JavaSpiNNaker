package uk.ac.manchester.spinnaker.processes;

import static java.lang.Math.min;
import static java.nio.ByteBuffer.allocate;
import static java.nio.ByteOrder.LITTLE_ENDIAN;
import static uk.ac.manchester.spinnaker.messages.Constants.CPU_IOBUF_ADDRESS_OFFSET;
import static uk.ac.manchester.spinnaker.messages.Constants.UDP_MESSAGE_MAX_SIZE;
import static uk.ac.manchester.spinnaker.transceiver.Utils.getVcpuAddress;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.TreeMap;

import uk.ac.manchester.spinnaker.connections.SCPConnection;
import uk.ac.manchester.spinnaker.connections.selectors.ConnectionSelector;
import uk.ac.manchester.spinnaker.machine.CoreLocation;
import uk.ac.manchester.spinnaker.machine.CoreSubsets;
import uk.ac.manchester.spinnaker.messages.model.IOBuffer;
import uk.ac.manchester.spinnaker.messages.scp.ReadMemory;
import uk.ac.manchester.spinnaker.messages.scp.ReadMemory.Response;
import uk.ac.manchester.spinnaker.utils.DefaultMap;

/**
 * A process for reading IOBUF memory (mostly log messages) from a SpiNNaker
 * core.
 */
public class ReadIOBufProcess extends MultiConnectionProcess<SCPConnection> {
	private static final int BUF_HEADER_BYTES = 16;
	private static final int BLOCK_HEADER_BYTES = 16;
	private static final int WORD = 4;
	private final Queue<NextRead> nextReads = new LinkedList<>();
	private final Queue<ExtraRead> extraReads = new LinkedList<>();
	private final Map<CoreLocation, Map<Integer, ByteBuffer>> iobuf =
			new DefaultMap<>(TreeMap::new);

	/**
	 * @param connectionSelector
	 *            How to select how to communicate.
	 */
	public ReadIOBufProcess(
			ConnectionSelector<SCPConnection> connectionSelector) {
		super(connectionSelector);
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
	 * @throws Exception
	 *             If SpiNNaker rejects a message.
	 */
	public Iterable<IOBuffer> readIOBuf(int size, CoreSubsets cores)
			throws Exception, IOException {
		// Get the IOBuf address for each core
		for (CoreLocation core : cores) {
			sendRequest(new ReadMemory(core.getScampCore(),
					CPU_IOBUF_ADDRESS_OFFSET + getVcpuAddress(core), WORD),
					response -> issueReadForIOBufHead(core, 0,
							response.data.getInt(),
							chunk(size + BUF_HEADER_BYTES)));
		}
		finish();
		checkForError();

		// Run rounds of the process until reading is complete
		while (!nextReads.isEmpty() || !extraReads.isEmpty()) {
			while (!extraReads.isEmpty()) {
				ExtraRead read = extraReads.remove();
				sendRequest(read.message(),
						response -> saveIOBufTailSection(read, response));
			}

			while (!nextReads.isEmpty()) {
				NextRead read = nextReads.remove();
				sendRequest(read.message(), response -> {
					// Unpack the IOBuf header
					int nextAddress = response.data.getInt();
					response.data.getLong(); // Ignore 8 bytes
					int bytesToRead = response.data.getInt();

					// Save the rest of the IOBuf
					int packetBytes =
							saveIOBufHead(read, response, bytesToRead);

					// Ask for the rest of the IOBuf buffer to be copied over
					issueReadsForIOBufTail(read, bytesToRead,
							read.base + packetBytes + BLOCK_HEADER_BYTES,
							packetBytes);

					// If there is another IOBuf buffer, read this next
					issueReadForIOBufHead(read.core, read.blockID + 1,
							nextAddress, read.size);
				});
			}

			finish();
			checkForError();
		}

		return new Iterable<IOBuffer>() {
			@Override
			public Iterator<IOBuffer> iterator() {
				return new Iterator<IOBuffer>() {
					private final Iterator<CoreLocation> cores =
							iobuf.keySet().iterator();

					@Override
					public boolean hasNext() {
						return cores.hasNext();
					}

					@Override
					public IOBuffer next() {
						CoreLocation core = cores.next();
						return new IOBuffer(core, iobuf.get(core).values());
					}
				};
			}
		};
	}

	private void issueReadForIOBufHead(CoreLocation core, int blockID, int next,
			int size) {
		if (next != 0) {
			nextReads.add(new NextRead(core, blockID, next, size));
		}
	}

	private int saveIOBufHead(NextRead read, Response response,
			int bytesToRead) {
		// Create a buffer for the data
		ByteBuffer buffer = allocate(bytesToRead).order(LITTLE_ENDIAN);
		iobuf.get(read.core).put(read.blockID, buffer);

		// Put the data from this packet into the buffer
		int packetBytes = min(response.data.remaining(), bytesToRead);
		if (packetBytes > 0) {
			buffer.put(response.data);
		}
		return packetBytes;
	}

	private void issueReadsForIOBufTail(NextRead read, int bytesToRead,
			int baseAddress, int readOffset) {
		bytesToRead -= readOffset;
		// While more reads need to be done to read the data
		while (bytesToRead > 0) {
			// Read the next bit of memory making up the buffer
			int next = chunk(bytesToRead);
			extraReads.add(new ExtraRead(read, baseAddress, next, readOffset));
			baseAddress += next;
			readOffset += next;
			bytesToRead -= next;
		}
	}

	private void saveIOBufTailSection(ExtraRead read, Response response) {
		ByteBuffer buffer = iobuf.get(read.core).get(read.blockID);
		synchronized (buffer) {
			buffer.position(read.offset);
			buffer.put(response.data);
		}
	}

	private static class NextRead {
		final CoreLocation core;
		final int blockID;
		final int base;
		final int size;

		NextRead(CoreLocation core, int blockID, int base, int size) {
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
		final int base;
		final int size;
		final int offset;

		ExtraRead(NextRead head, int base, int size, int offset) {
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
