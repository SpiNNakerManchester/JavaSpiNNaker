/*
 * Copyright (c) 2018 The University of Manchester
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
 * An SCP response to a request which has an empty payload.
 */
public class EmptyResponse extends CheckOKResponse {
	/**
	 * Create an instance.
	 *
	 * @param operation
	 *            The overall operation that we are doing.
	 * @param command
	 *            The command that we are handling a response to.
	 * @param buffer
	 *            The buffer holding the response data.
	 * @throws UnexpectedResponseCodeException
	 *             If the response wasn't OK.
	 */
	EmptyResponse(String operation, Enum<?> command, ByteBuffer buffer)
			throws UnexpectedResponseCodeException {
		super(operation, command, buffer);
	}
}
