package uk.ac.manchester.spinnaker.data_spec;

import static java.nio.ByteBuffer.wrap;
import static java.nio.ByteOrder.LITTLE_ENDIAN;
import static java.util.Arrays.stream;
import static org.apache.commons.io.IOUtils.toByteArray;
import static uk.ac.manchester.spinnaker.data_spec.Constants.APPDATA_MAGIC_NUM;
import static uk.ac.manchester.spinnaker.data_spec.Constants.APP_PTR_TABLE_BYTE_SIZE;
import static uk.ac.manchester.spinnaker.data_spec.Constants.DSE_VERSION;
import static uk.ac.manchester.spinnaker.data_spec.Constants.END_SPEC_EXECUTOR;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.stream.Collectors;

import uk.ac.manchester.spinnaker.data_spec.exceptions.DataSpecificationException;

/**
 * Used to execute a SpiNNaker data specification language file to produce a
 * memory image.
 *
 * @author Donal Fellows
 */
public class Executor implements AutoCloseable {
	private ByteBuffer input;
	private Functions funcs;

	/**
	 * Create an executor.
	 *
	 * @param inputStream
	 *            The object to read the specification language file from
	 * @param memorySpace
	 *            memory available on the destination architecture
	 */
	public Executor(InputStream inputStream, int memorySpace)
			throws IOException {
		this.input = wrap(toByteArray(inputStream)).order(LITTLE_ENDIAN);
		this.funcs = new Functions(input, memorySpace);
		printInput();
	}

	/**
	 * Create an executor.
	 *
	 * @param input
	 *            The object to read the specification language file from
	 * @param memorySpace
	 *            memory available on the destination architecture
	 */
	public Executor(ByteBuffer input, int memorySpace) throws IOException {
		this.input = input.asReadOnlyBuffer().order(LITTLE_ENDIAN);
		this.input.rewind(); // Ensure we start from the beginning
		this.funcs = new Functions(this.input, memorySpace);
		printInput();
	}

	private void printInput() {
		IntBuffer b = input.asIntBuffer();
		int[] a = new int[b.limit()];
		b.get(a);
		System.out.println("processing input: " + stream(a)
				.mapToObj(Integer::toHexString).collect(Collectors.toList()));
	}

	@Override
	public void close() {
		// Does nothing; the original spec is read eagerly
	}

	/** Executes the specification. */
	public void execute() throws DataSpecificationException {
		while (true) {
			int index = input.position();
			int cmd = input.getInt();
			OperationCallable instruction = funcs.getOperation(cmd, index);
			if (END_SPEC_EXECUTOR == instruction.execute(cmd)) {
				break;
			}
		}
	}

	/** Get a region with a given ID. */
	public MemoryRegion getRegion(int regionID) {
		return funcs.getRegion(regionID);
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
		for (MemoryRegion r : funcs.getRegions()) {
			if (r != null) {
				buffer.putInt(nextOffset + startAddress);
				nextOffset += r.getAllocatedSize();
			} else {
				buffer.putInt(0);
			}
		}
	}

	/** @return the size of the data that will be written to memory. */
	public int getConstructedDataSize() {
		return APP_PTR_TABLE_BYTE_SIZE
				+ funcs.getRegions().stream().filter(r -> r != null)
						.mapToInt(r -> r.getAllocatedSize()).sum();
	}
}
