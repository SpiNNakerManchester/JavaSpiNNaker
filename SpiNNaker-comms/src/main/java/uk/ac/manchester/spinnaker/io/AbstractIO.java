package uk.ac.manchester.spinnaker.io;

import java.io.IOException;

import uk.ac.manchester.spinnaker.processes.FillProcess;
import uk.ac.manchester.spinnaker.processes.Process.Exception;

public interface AbstractIO extends AutoCloseable {
	/** The size of the entire region of memory */
	int size();

	/**
	 * Get a sub-region of this memory object. The index must be in range of the
	 * current region to be valid.
	 *
	 * @param new_slice
	 *            A single index for a single byte of memory.
	 */
	AbstractIO get(int slice) throws IOException, Exception;

	/**
	 * Get a sub-region of this memory object. The slice must be in range of the
	 * current region to be valid.
	 *
	 * @param new_slice
	 *            A contiguous slice of memory.
	 */
	AbstractIO get(Slice slice) throws IOException, Exception;

	/** Indicates if the object has been closed */
	boolean isClosed();

	/** Flush any outstanding written data */
	void flush() throws IOException, Exception;

	/**
	 * Seek to a position within the region.
	 *
	 * @param numBytes
	 *            Where to seek to relative to whence.
	 * @param whence
	 *            Where the numBytes should be measured relative to.
	 */
	default void seek(int numBytes, Seek whence) throws IOException, Exception {
		switch (whence) {
		case SET:
			seek(numBytes);
			break;
		case CUR:
			seek(numBytes + tell());
			break;
		case END:
			seek(numBytes + size());
			break;
		}
	}

	/** Seek to a position within the region */
	void seek(int numBytes) throws IOException, Exception;

	/** Return the current position within the region relative to the start */
	int tell() throws IOException, Exception;

	/** Return the current absolute address within the region */
	int getAddress();

	/**
	 * Read the rest of the data.
	 *
	 * @param numBytes
	 *            The number of bytes to read
	 * @throws IOException
	 *             If the read will be beyond the end of the region
	 */
	default byte[] read() throws IOException, Exception {
		return read(null);
	}

	/**
	 * Read a number of bytes, or the rest of the data if numBytes is null or
	 * negative.
	 *
	 * @param numBytes
	 *            The number of bytes to read
	 * @throws IOException
	 *             If the read will be beyond the end of the region
	 */
	byte[] read(Integer numBytes) throws IOException, Exception;

	/**
	 * Write some data to the region.
	 *
	 * @param data
	 *            The data to write
	 * @return The number of bytes written
	 * @throws IOException
	 *             If the write will go over the end of the region
	 */
	int write(byte[] data) throws IOException, Exception;

	/**
	 * Fill the rest of the region with repeated data words.
	 *
	 * @param value
	 *            The value to repeat
	 * @throws IOException
	 *             If the amount of data to fill is more than the region
	 */
	default void fill(int value) throws IOException, Exception {
		fill(value, null, FillProcess.DataType.WORD);
	}

	/**
	 * Fill the next part of the region with repeated data words.
	 *
	 * @param value
	 *            The value to repeat
	 * @param size
	 *            Number of bytes to fill from current position
	 * @throws IOException
	 *             If the amount of data to fill is more than the region
	 */
	default void fill(int value, int size)
			throws IOException, Exception {
		fill(value, size, FillProcess.DataType.WORD);
	}

	/**
	 * Fill the rest of the region with repeated data.
	 *
	 * @param value
	 *            The value to repeat
	 * @param type
	 *            The type of the repeat value
	 * @throws IOException
	 *             If the amount of data to fill is more than the region
	 */
	default void fill(int value, FillProcess.DataType type)
			throws IOException, Exception {
		fill(value, null, type);
	}

	/**
	 * Fill the next part of the region with repeated data.
	 *
	 * @param value
	 *            The value to repeat
	 * @param size
	 *            Number of bytes to fill from current position, or
	 *            <tt>null</tt> to fill to the end
	 * @param type
	 *            The type of the repeat value
	 * @throws IOException
	 *             If the amount of data to fill is more than the region
	 */
	void fill(int value, Integer size, FillProcess.DataType type)
			throws IOException, Exception;

	enum Seek {
		/** Seek relative to the start of the region. */
		SET,
		/** Seek relative to the current location in the region. */
		CUR,
		/** Seek relative to the end of the region. */
		END
	}

	static final class Slice {
		public Integer start;
		public Integer stop;

		private Slice() {
		}

		public static Slice from(int start) {
			Slice s = new Slice();
			s.start = start;
			return s;
		}

		public static Slice to(int end) {
			Slice s = new Slice();
			s.stop = end;
			return s;
		}

		public static Slice over(int start, int end) {
			Slice s = new Slice();
			s.start = start;
			s.stop = end;
			return s;
		}
	}
}
