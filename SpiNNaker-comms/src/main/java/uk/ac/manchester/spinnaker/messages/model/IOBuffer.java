/*
 * Copyright (c) 2018-2023 The University of Manchester
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package uk.ac.manchester.spinnaker.messages.model;

import static java.nio.ByteBuffer.wrap;
import static java.nio.ByteOrder.LITTLE_ENDIAN;
import static java.nio.charset.StandardCharsets.US_ASCII;

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
			if (b.hasArray()) {
				baos.write(b.array(), b.arrayOffset() + b.position(),
						b.remaining());
			} else {
				// Must copy
				var temp = new byte[b.remaining()];
				b.get(temp);
				baos.write(temp, 0, temp.length);
			}
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
		return getContentsString(US_ASCII);
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
