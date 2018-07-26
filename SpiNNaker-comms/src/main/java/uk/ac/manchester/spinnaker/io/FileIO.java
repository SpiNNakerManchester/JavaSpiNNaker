package uk.ac.manchester.spinnaker.io;

import static java.nio.ByteBuffer.allocate;
import static java.nio.ByteOrder.LITTLE_ENDIAN;

import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;

import uk.ac.manchester.spinnaker.processes.FillProcess.DataType;

/** A file input/output interface to match the MemoryIO interface */
public class FileIO implements AbstractIO {
	/** The file to write to */
	private final RandomAccessFile file;

	/** The current offset in the file */
	private int current_offset;

	/** The start offset in the file */
	private int start_offset;

	/** The end offset in the file */
	private int end_offset;

	/**
	 * @param file
	 *            The file handle or file name to write to
	 * @param startOffset
	 *            The start offset into the file
	 * @param endOffset
	 *            The end offset from the start of the file
	 * @throws IOException
	 *             If anything goes wrong opening the file.
	 */
	public FileIO(File file, int startOffset, int endOffset)
			throws IOException {
		this(new RandomAccessFile(file, "rw"), startOffset, endOffset);
	}

	/**
	 * @param file
	 *            The file handle or file name to write to
	 * @param startOffset
	 *            The start offset into the file
	 * @param endOffset
	 *            The end offset from the start of the file
	 */
	public FileIO(RandomAccessFile file, int startOffset, int endOffset) {
		this.file = file;
		this.current_offset = startOffset;
		this.start_offset = startOffset;
		this.end_offset = endOffset;
	}

	@Override
	public void close() throws Exception {
		file.close();
	}

	@Override
	public int size() {
		return end_offset - start_offset;
	}

	@Override
	public FileIO get(int slice) throws IOException {
		if (slice < 0 || slice >= size()) {
			throw new ArrayIndexOutOfBoundsException(slice);
		}
		return new FileIO(file, start_offset + slice, start_offset + slice + 1);
	}

	@Override
	public FileIO get(Slice slice) throws IOException {
		int from = start_offset;
		int to = end_offset;
		if (slice.start != null) {
			if (slice.start < 0) {
				from = end_offset + slice.start;
			} else {
				from += slice.start;
			}
		}
		if (slice.stop != null) {
			if (slice.stop < 0) {
				to = end_offset + slice.stop;
			} else {
				to += slice.stop;
			}
		}
		if (from < start_offset || from > end_offset) {
			throw new ArrayIndexOutOfBoundsException(slice.start);
		}
		if (to < start_offset || to > end_offset) {
			throw new ArrayIndexOutOfBoundsException(slice.stop);
		}
		if (from == to) {
			throw new ArrayIndexOutOfBoundsException(
					"zero-sized regions are not supported");
		}
		return new FileIO(file, from, to);
	}

	@Override
	public boolean isClosed() {
		return !file.getChannel().isOpen();
	}

	@Override
	public void flush() throws IOException {
		// Do nothing
	}

	@Override
	public void seek(int position) throws IOException {
		position += start_offset;
		if (position < start_offset || position > end_offset) {
			throw new IOException("illegal seek position");
		}
		current_offset = position;
	}

	@Override
	public int tell() throws IOException {
		return current_offset - start_offset;
	}

	@Override
	public int getAddress() {
		return current_offset;
	}

	@Override
	public byte[] read(Integer numBytes) throws IOException {
		if (numBytes != null && numBytes == 0) {
			return new byte[0];
		}
		int n = (numBytes == null || numBytes < 0)
				? (end_offset - current_offset)
				: numBytes;
		if (current_offset + n > end_offset) {
			throw new EOFException();
		}
		synchronized (file) {
			file.seek(current_offset);
			byte[] data = new byte[n];
			file.readFully(data, 0, n);
			current_offset += n;
			return data;
		}
	}

	@Override
	public int write(byte[] data) throws IOException {
		int n = data.length;
		if (current_offset + n > end_offset) {
			throw new EOFException();
		}
		synchronized (file) {
			file.seek(current_offset);
			file.write(data, 0, n);
		}
		current_offset += n;
		return n;
	}

	@Override
	public void fill(int repeat_value, Integer bytes_to_fill,
			DataType data_type) throws IOException {
		int len = (bytes_to_fill == null) ? (end_offset - current_offset)
				: bytes_to_fill;
		if (current_offset + len > end_offset) {
			throw new EOFException();
		}
		if (len < 0 || len % data_type.value != 0) {
			throw new IllegalArgumentException(
					"length to fill must be multiple of fill unit size");
		}
		ByteBuffer b = allocate(len).order(LITTLE_ENDIAN);
		while (b.hasRemaining()) {
			switch (data_type) {
			case WORD:
				b.putInt(repeat_value);
				break;
			case HALF_WORD:
				b.putShort((short) (repeat_value & 0xFFFF));
				break;
			case BYTE:
				b.put((byte) (repeat_value & 0xFF));
				break;
			}
		}
		synchronized (file) {
			file.seek(current_offset);
			file.write(b.array(), b.arrayOffset(), len);
		}
		current_offset += len;
	}

}
