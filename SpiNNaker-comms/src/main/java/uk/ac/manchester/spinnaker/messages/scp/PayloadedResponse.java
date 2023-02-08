/*
 * Copyright (c) 2023 The University of Manchester
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
