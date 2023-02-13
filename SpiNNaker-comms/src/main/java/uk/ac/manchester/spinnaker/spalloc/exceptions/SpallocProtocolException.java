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
package uk.ac.manchester.spinnaker.spalloc.exceptions;

import java.io.IOException;

/** Thrown when a network-level problem occurs during protocol handling. */
public class SpallocProtocolException extends IOException {
	private static final long serialVersionUID = -1591596793445886688L;

	/** @param cause The cause of this exception. */
	public SpallocProtocolException(Throwable cause) {
		super(cause);
	}

	/** @param msg The message of this exception. */
	public SpallocProtocolException(String msg) {
		super(msg);
	}
}
