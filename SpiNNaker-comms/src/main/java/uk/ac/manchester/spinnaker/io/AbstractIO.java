package uk.ac.manchester.spinnaker.io;

import static java.lang.Math.max;
import static java.lang.System.arraycopy;
import static uk.ac.manchester.spinnaker.io.Constants.BYTE_MASK;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import uk.ac.manchester.spinnaker.processes.FillProcess;
import uk.ac.manchester.spinnaker.processes.Process.Exception;
import uk.ac.manchester.spinnaker.utils.Slice;

/**
 * Presents a view of an entity (SpiNNaker memory or a file on disk) as
 * something like an IO-capable device.
 *
 * @author Donal Fellows
 */
public interface AbstractIO extends AutoCloseable {
	/** @return The size of the entire region of memory. */
	int size();

	/**
	 * Get a sub-region of this memory object. The index must be in range of the
	 * current region to be valid.
	 *
	 * @param slice
	 *            A single index for a single byte of memory.
	 * @return The slice view of the byte.
	 * @throws Exception
	 *             If the communications with SpiNNaker fails
	 * @throws IOException
	 *             If something goes wrong
	 */
	AbstractIO get(int slice) throws IOException, Exception;

	/**
	 * Get a sub-region of this memory object. The slice must be in range of the
	 * current region to be valid.
	 *
	 * @param slice
	 *            A description of a contiguous slice of memory.
	 * @return The slice view of the chunk.
	 * @throws Exception
	 *             If the communications with SpiNNaker fails
	 * @throws IOException
	 *             If something goes wrong
	 */
	AbstractIO get(Slice slice) throws IOException, Exception;

	/** @return whether the object has been closed. */
	boolean isClosed();

	/**
	 * Flush any outstanding written data.
	 *
	 * @throws Exception
	 *             If the communications with SpiNNaker fails
	 * @throws IOException
	 *             If something goes wrong
	 */
	void flush() throws IOException, Exception;

	/**
	 * Seek to a position within the region.
	 *
	 * @param numBytes
	 *            Where to seek to relative to whence. (Note that this isn't the
	 *            same concept as the address property; this still allows for
	 *            the file region to be restricted by slice.)
	 * @param whence
	 *            Where the numBytes should be measured relative to.
	 * @throws Exception
	 *             If the communications with SpiNNaker fails
	 * @throws IOException
	 *             If something goes wrong
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
		default:
			throw new IllegalArgumentException();
		}
	}

	/**
	 * Seek to a position within the region.
	 *
	 * @param numBytes
	 *            The absolute location within the region. (Note that this isn't
	 *            the same concept as the address property; this still allows
	 *            for the file region to be restricted by slice.)
	 * @throws Exception
	 *             If the communications with SpiNNaker fails
	 * @throws IOException
	 *             If something goes wrong
	 */
	void seek(int numBytes) throws IOException, Exception;

	/**
	 * Return the current position within the region relative to the start.
	 *
	 * @return the current offset
	 * @throws Exception
	 *             If the communications with SpiNNaker fails
	 * @throws IOException
	 *             If something goes wrong
	 */
	int tell() throws IOException, Exception;

	/** @return the current absolute address within the region. */
	int getAddress();

	/**
	 * Read the rest of the data.
	 *
	 * @return The bytes that have been read.
	 * @throws Exception
	 *             If the communications with SpiNNaker fails
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
	 * @return The bytes that have been read.
	 * @throws Exception
	 *             If the communications with SpiNNaker fails
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
	 * @throws Exception
	 *             If the communications with SpiNNaker fails
	 * @throws IOException
	 *             If the write will go over the end of the region
	 */
	int write(byte[] data) throws IOException, Exception;

	/**
	 * Fill the rest of the region with repeated data words.
	 *
	 * @param value
	 *            The value to repeat
	 * @throws Exception
	 *             If the communications with SpiNNaker fails
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
	 * @throws Exception
	 *             If the communications with SpiNNaker fails
	 * @throws IOException
	 *             If the amount of data to fill is more than the region
	 */
	default void fill(int value, int size) throws IOException, Exception {
		fill(value, size, FillProcess.DataType.WORD);
	}

	/**
	 * Fill the rest of the region with repeated data.
	 *
	 * @param value
	 *            The value to repeat
	 * @param type
	 *            The type of the repeat value
	 * @throws Exception
	 *             If the communications with SpiNNaker fails
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
	 * @throws Exception
	 *             If the communications with SpiNNaker fails
	 * @throws IOException
	 *             If the amount of data to fill is more than the region
	 */
	void fill(int value, Integer size, FillProcess.DataType type)
			throws IOException, Exception;

	/**
	 * The various positions that a {@link #seek(int,Seek)} may be relative to.
	 */
	enum Seek {
		/** Seek relative to the start of the region. */
		SET,
		/** Seek relative to the current location in the region. */
		CUR,
		/** Seek relative to the end of the region. */
		END
	}

	/**
	 * Get a view of this IO object as a Java input stream.
	 *
	 * @return The input stream.
	 */
	default InputStream asInputStream() {
		return new InputStream() {
			@Override
			public int read() throws IOException {
				try {
					byte[] b = AbstractIO.this.read(1);
					return b[0];
				} catch (EOFException e) {
					return -1;
				} catch (Exception e) {
					throw new IOException(e);
				}
			}

			@Override
			public int read(byte[] buffer) throws IOException {
				try {
					byte[] b = AbstractIO.this.read(buffer.length);
					arraycopy(b, 0, buffer, 0, b.length);
					return b.length;
				} catch (EOFException e) {
					return -1;
				} catch (Exception e) {
					throw new IOException(e);
				}
			}

			@Override
			public int read(byte[] buffer, int offset, int length)
					throws IOException {
				try {
					byte[] b = AbstractIO.this.read(length);
					arraycopy(b, 0, buffer, offset, b.length);
					return b.length;
				} catch (EOFException e) {
					return -1;
				} catch (Exception e) {
					throw new IOException(e);
				}
			}

			@Override
			public long skip(long n) throws IOException {
				try {
					int before = tell();
					seek(max((int) n, 0), Seek.CUR);
					int after = tell();
					return after - before;
				} catch (Exception e) {
					throw new IOException(e);
				}
			}
		};
	}

	/**
	 * Get a view of this IO object as a Java input stream.
	 *
	 * @return The input stream.
	 */
	default OutputStream asOutputStream() {
		return new OutputStream() {
			@Override
			public void write(int b) throws IOException {
				byte[] buffer = new byte[1];
				buffer[0] = (byte) (b & BYTE_MASK);
				try {
					AbstractIO.this.write(buffer);
				} catch (Exception e) {
					throw new IOException(e);
				}
			}

			@Override
			public void write(byte[] bytes) throws IOException {
				try {
					AbstractIO.this.write(bytes);
				} catch (Exception e) {
					throw new IOException(e);
				}
			}

			@Override
			public void write(byte[] bytes, int offset, int length)
					throws IOException {
				byte[] buffer = new byte[length];
				arraycopy(bytes, offset, buffer, 0, length);
				try {
					AbstractIO.this.write(buffer);
				} catch (Exception e) {
					throw new IOException(e);
				}
			}

			@Override
			public void flush() throws IOException {
				try {
					AbstractIO.this.flush();
				} catch (Exception e) {
					throw new IOException(e);
				}
			}
		};
	}
}
