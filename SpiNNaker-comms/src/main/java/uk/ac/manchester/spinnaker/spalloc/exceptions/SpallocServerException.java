/*
 * Copyright (c) 2018 The University of Manchester
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package uk.ac.manchester.spinnaker.spalloc.exceptions;

import uk.ac.manchester.spinnaker.spalloc.messages.ExceptionResponse;

/**
 * Thrown when something went wrong on the server side that caused us to be sent
 * a message.
 */
public class SpallocServerException extends Exception {
	private static final long serialVersionUID = 3865188016221866202L;

	/** @param msg The message of the exception. */
	public SpallocServerException(String msg) {
		super(msg);
	}

	/** @param r The deserialised message from the server. */
	public SpallocServerException(ExceptionResponse r) {
		super(r.getException());
	}
}
