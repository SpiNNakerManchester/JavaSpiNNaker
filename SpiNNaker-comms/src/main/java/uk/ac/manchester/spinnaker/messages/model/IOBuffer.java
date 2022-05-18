/*
 * Copyright (c) 2018 The University of Manchester
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package uk.ac.manchester.spinnaker.messages.model;

import static java.nio.ByteBuffer.wrap;
import static java.nio.ByteOrder.LITTLE_ENDIAN;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;

import uk.ac.manchester.spinnaker.machine.HasCoreLocation;

/**
 * The contents of IOBUF for a core.
 *
 * @author Donal Fellows
 */
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
		iobuf = contents.clone();
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
		var baos = new ByteArrayOutputStream();
		for (var b : contents) {
			baos.write(b.array(), 0, b.limit()); // FIXME
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

	/** @return The raw contents of the buffer as an input stream. */
	public InputStream getContentsStream() {
		return new ByteArrayInputStream(iobuf);
	}

	/**
	 * @return The raw contents of the buffer as a read-only little-endian byte
	 *         buffer.
	 */
	public ByteBuffer getContentsBuffer() {
		return wrap(iobuf).asReadOnlyBuffer().order(LITTLE_ENDIAN);
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
