/*
 * Copyright (c) 2019 The University of Manchester
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

import uk.ac.manchester.spinnaker.messages.model.UnexpectedResponseCodeException;

/**
 * An SCP response that should never be received because its request is
 * guaranteed to be a one-way request.
 */
public final class NoResponse extends SCPResponse {
	/**
	 * Create an instance.
	 *
	 * @param operation
	 *            The overall operation that we are doing.
	 * @param command
	 *            The particular command that had the problem.
	 * @param buffer
	 *            The buffer holding the response data.
	 * @throws UnexpectedResponseCodeException
	 *             Always.
	 */
	public NoResponse(String operation, Enum<?> command, ByteBuffer buffer)
			throws UnexpectedResponseCodeException {
		super(buffer);
		throw new UnexpectedResponseCodeException(operation, command,
				result, null);
	}
}
