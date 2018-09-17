package uk.ac.manchester.spinnaker.data_spec;

import static java.nio.ByteBuffer.wrap;
import static java.nio.ByteOrder.LITTLE_ENDIAN;
import static java.util.Arrays.stream;
import static java.util.Collections.unmodifiableCollection;
import static org.apache.commons.io.FileUtils.openInputStream;
import static org.apache.commons.io.IOUtils.toByteArray;
import static org.slf4j.LoggerFactory.getLogger;
import static uk.ac.manchester.spinnaker.data_spec.Constants.APPDATA_MAGIC_NUM;
import static uk.ac.manchester.spinnaker.data_spec.Constants.APP_PTR_TABLE_BYTE_SIZE;
import static uk.ac.manchester.spinnaker.data_spec.Constants.DSE_VERSION;
import static uk.ac.manchester.spinnaker.data_spec.Constants.END_SPEC_EXECUTOR;
import static uk.ac.manchester.spinnaker.data_spec.Constants.MAX_MEM_REGIONS;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.Collection;
import java.util.stream.Collectors;

import org.slf4j.Logger;

import uk.ac.manchester.spinnaker.data_spec.exceptions.DataSpecificationException;

/**
 * Used to execute a SpiNNaker data specification language file to produce a
 * memory image.
 *
 * @author Donal Fellows
 */
public class Executor implements Closeable {
	private static final Logger log = getLogger(Executor.class);
	private InputStream inputStream;
	private final ByteBuffer input;
	private final Functions funcs;
	/** The collection of memory regions that can be written to. */
	private final MemoryRegionCollection memRegions;

	/**
	 * Create an executor.
	 *
	 * @param inputFile
	 *            The file to read the specification from
	 * @param memorySpace
	 *            memory available on the destination architecture
	 * @throws IOException
	 *             If a problem happens when reading the file
	 */
	public Executor(File inputFile, int memorySpace) throws IOException {
		this(openInputStream(inputFile), memorySpace);
	}

	/**
	 * Create an executor.
	 *
	 * @param inputStream
	 *            The object to read the specification language file from
	 * @param memorySpace
	 *            memory available on the destination architecture
	 * @throws IOException
	 *             If a problem happens when reading the input stream
	 */
	public Executor(InputStream inputStream, int memorySpace)
			throws IOException {
		this(wrap(toByteArray(inputStream)), memorySpace);
		this.inputStream = inputStream;
	}

	/**
	 * Create an executor.
	 *
	 * @param input
	 *            The object to read the specification language file from
	 * @param memorySpace
	 *            memory available on the destination architecture
	 */
	public Executor(ByteBuffer input, int memorySpace) {
		this.input = input.asReadOnlyBuffer().order(LITTLE_ENDIAN);
		this.input.rewind(); // Ensure we start from the beginning
		memRegions = new MemoryRegionCollection(MAX_MEM_REGIONS);
		funcs = new Functions(this.input, memorySpace, memRegions);
		if (log.isDebugEnabled()) {
			logInput();
		}
	}

	private void logInput() {
		IntBuffer b = input.asIntBuffer();
		int[] a = new int[b.limit()];
		b.get(a);
		log.debug("processing input: {}", stream(a)
				.mapToObj(Integer::toHexString).collect(Collectors.toList()));
	}

	/**
	 * @throws IOException
	 *             if the spec is being read from a stream and a close of the
	 *             stream fails.
	 */
	@Override
	public void close() throws IOException {
		if (inputStream != null) {
			inputStream.close();
			inputStream = null;
		}
	}

	/**
	 * Executes the specification.
	 *
	 * @throws DataSpecificationException
	 *             if anything goes wrong
	 */
	public void execute() throws DataSpecificationException {
		while (true) {
			int index = input.position();
			int cmd = input.getInt();
			Callable instruction = funcs.getOperation(cmd, index);
			if (END_SPEC_EXECUTOR == instruction.execute(cmd)) {
				break;
			}
		}
	}

	/**
	 * Get how much space was allocated overall. Only useful after a
	 * specification has been executed.
	 *
	 * @return The total space allocated, not including any header or region
	 *         address table.
	 */
	public int getTotalSpaceAllocated() {
		return funcs.spaceAllocated;
	}

	/**
	 * Get the memory region with a particular ID.
	 *
	 * @param regionID
	 *            The ID to look up.
	 * @return The memory region with that ID.
	 */
	public MemoryRegion getRegion(int regionID) {
		return memRegions.get(regionID);
	}

	/**
	 * Get the header of the data added to a buffer.
	 *
	 * @param buffer
	 *            The buffer to write into.
	 */
	public void addHeader(ByteBuffer buffer) {
		buffer.putInt(APPDATA_MAGIC_NUM);
		buffer.putInt(DSE_VERSION);
	}

	/**
	 * Get the pointer table stored in a buffer.
	 *
	 * @param buffer
	 *            The buffer to store it in
	 * @param startAddress
	 *            Where in memory the memory block is being written.
	 */
	public void addPointerTable(ByteBuffer buffer, int startAddress) {
		int nextOffset = APP_PTR_TABLE_BYTE_SIZE;
		for (MemoryRegion r : memRegions) {
			if (r != null) {
				r.setRegionBase(nextOffset + startAddress);
				buffer.putInt(r.getRegionBase());
				nextOffset += r.getAllocatedSize();
			} else {
				buffer.putInt(0);
			}
		}
	}

	/** @return the size of the data that will be written to memory. */
	public int getConstructedDataSize() {
		return APP_PTR_TABLE_BYTE_SIZE
				+ memRegions.stream().filter(r -> r != null)
						.mapToInt(r -> r.getAllocatedSize()).sum();
	}

	/**
	 * Get the regions of the executor as an unmodifiable iterable.
	 *
	 * @return The regions.
	 */
	public Collection<MemoryRegion> regions() {
		return unmodifiableCollection(memRegions);
	}
}
