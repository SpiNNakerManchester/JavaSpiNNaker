package uk.ac.manchester.spinnaker.messages.model;

import static java.nio.ByteBuffer.wrap;
import static java.nio.ByteOrder.LITTLE_ENDIAN;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;

import uk.ac.manchester.spinnaker.machine.HasCoreLocation;

/** The contents of IOBUF for a core. */
public class IOBuffer implements HasCoreLocation {
	private static final Charset ASCII = Charset.forName("ascii");
	private final HasCoreLocation core;
	private final byte[] iobuf;

	/**
	 * @param core
	 *            The coordinates of a core
	 * @param contents
	 *            The contents of the buffer for the chip
	 */
	public IOBuffer(HasCoreLocation core, byte[] contents) {
		this.core = core;
		iobuf = contents;
	}

	/**
	 * @param core
	 *            The coordinates of a core
	 * @param contents
	 *            The contents of the buffer for the chip
	 */
	public IOBuffer(HasCoreLocation core, ByteBuffer contents) {
		this.core = core;
		iobuf = new byte[contents.remaining()];
		contents.asReadOnlyBuffer().order(LITTLE_ENDIAN).get(iobuf);
	}

	/**
	 * @param core
	 *            The coordinates of a core
	 * @param contents
	 *            The contents of the buffer for the chip
	 */
	public IOBuffer(HasCoreLocation core, Iterable<ByteBuffer> contents) {
		this.core = core;
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		for (ByteBuffer b : contents) {
			baos.write(b.array(), 0, b.limit());
		}
		iobuf = baos.toByteArray();
	}

	@Override
	public int getX() {
		return core.getX();
	}

	@Override
	public int getY() {
		return core.getY();
	}

	@Override
	public int getP() {
		return core.getP();
	}

	/** @return The contents of the buffer. */
	public byte[] getContents() {
		return iobuf;
	}

	/** @return The contents of the buffer as an input stream. */
	public InputStream getContentsStream() {
		return new ByteArrayInputStream(iobuf);
	}

	/** @return The contents of the buffer as a little-endian byte buffer. */
	public ByteBuffer getContentsBuffer() {
		return wrap(iobuf).order(LITTLE_ENDIAN);
	}

	/**
	 * @return The contents of the buffer as a string, interpreting it as ASCII.
	 */
	public String getContentsString() {
		return getContentsString(ASCII);
	}

	/**
	 * Get the contents of the buffer as a string.
	 *
	 * @param charset
	 *            How to decode bytes into characters.
	 * @return The contents of the buffer as a string in the specified encoding.
	 */
	public String getContentsString(Charset charset) {
		return new String(iobuf, charset);
	}
}
