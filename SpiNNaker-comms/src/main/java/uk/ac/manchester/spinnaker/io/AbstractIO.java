package uk.ac.manchester.spinnaker.io;

import java.io.IOException;

public interface AbstractIO extends AutoCloseable {
	/** The size of the entire region of memory */
	int size();

	// FIXME What does this become in Java?
	// def __getitem__(self, new_slice):
	// /** Get a sub-region of this memory object. The index or slice must
	// be in range of the current region to be valid.
	//
	// :param new_slice:
	// A single index for a single byte of memory, or a contiguous slice
	// :rtype: :py:class:`~MemoryIO`
	// :raise ValueError:
	// If the index or slice is outside of the current region
	// */

	/** Indicates if the object has been closed */
	boolean isClosed();

	/** Flush any outstanding written data */
	void flush() throws IOException;

	/** Seek to a position within the region.
	 * @param numBytes Where to seek to relative to whence.
	 * @param whence Where the numBytes should be measured relative to. */
	default void seek(int numBytes, Seek whence) throws IOException {
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
	void seek(int numBytes) throws IOException;

	/** Return the current position within the region relative to the start */
	int tell() throws IOException;

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
	default byte[] read() throws IOException {
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
	byte[] read(Integer numBytes) throws IOException;

	/**
	 * Write some data to the region.
	 *
	 * @param data
	 *            The data to write
	 * @return The number of bytes written
	 * @throws IOException
	 *             If the write will go over the end of the region
	 */
	int write(byte[] data) throws IOException;

	/**
	 * Fill the rest of the region with repeated data.
	 *
	 * @param repeat_value
	 *            The value to repeat
	 * @throws IOException
	 *             If the amount of data to fill is more than the region
	 */
	default void fill(int repeat_value) throws IOException {
		fill(repeat_value, null, FillDataType.WORD);
	}

	/**
	 * Fill the next part of the region with repeated data words.
	 *
	 * @param repeat_value
	 *            The value to repeat
	 * @param bytes_to_fill
	 *            Number of bytes to fill from current position
	 * @throws IOException
	 *             If the amount of data to fill is more than the region
	 */
	default void fill(int repeat_value, int bytes_to_fill) throws IOException {
		fill(repeat_value, bytes_to_fill, FillDataType.WORD);
	}

	/**
	 * Fill the rest of the region with repeated data.
	 *
	 * @param repeat_value
	 *            The value to repeat
	 * @param data_type
	 *            The type of the repeat value
	 * @throws IOException
	 *             If the amount of data to fill is more than the region
	 */
	default void fill(int repeat_value, FillDataType data_type)
			throws IOException {
		fill(repeat_value, null, data_type);
	}

	/**
	 * Fill the next part of the region with repeated data.
	 *
	 * @param repeat_value
	 *            The value to repeat
	 * @param bytes_to_fill
	 *            Number of bytes to fill from current position, or
	 *            <tt>null</tt> to fill to the end
	 * @param data_type
	 *            The type of the repeat value
	 * @throws IOException
	 *             If the amount of data to fill is more than the region
	 */
	void fill(int repeat_value, Integer bytes_to_fill, FillDataType data_type)
			throws IOException;

	enum Seek {
		/** Seek relative to the start of the region. */
		SET,
		/** Seek relative to the current location in the region. */
		CUR,
		/** Seek relative to the end of the region. */
		END
	}
	enum FillDataType {
		WORD
		// FIXME define contents and move to right location
	}
}
