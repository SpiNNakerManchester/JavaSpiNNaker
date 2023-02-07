/*
 * Copyright (c) 2023 The University of Manchester
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
package uk.ac.manchester.spinnaker.messages.scp;

import java.nio.ByteBuffer;
import java.util.function.Supplier;

import com.google.errorprone.annotations.ForOverride;

import uk.ac.manchester.spinnaker.messages.model.UnexpectedResponseCodeException;

/**
 * A response that holds a payload.
 *
 * @author Donal Fellows
 * @param <T>
 *            The type of parsed payload value.
 * @param <E>
 *            The type of exception thrown by the payload parser.
 */
public abstract class PayloadedResponse<T, E extends Exception>
		extends CheckOKResponse implements Supplier<T> {
	private final T value;

	PayloadedResponse(String operation, Enum<?> command, ByteBuffer buffer)
			throws UnexpectedResponseCodeException, E {
		super(operation, command, buffer);
		value = parse(buffer);
	}

	@Override
	public final T get() {
		return value;
	}

	/**
	 * Parse the payload of a response. The buffer will be positioned after the
	 * checked header.
	 *
	 * @param buffer
	 *            The buffer to parse. Little-endian.
	 * @return The parsed value.
	 * @throws E
	 *             If parsing fails.
	 */
	@ForOverride
	protected abstract T parse(ByteBuffer buffer) throws E;
}
