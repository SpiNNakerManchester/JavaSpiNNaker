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
package uk.ac.manchester.spinnaker.storage;

/**
 * Exceptions caused by the storage system.
 *
 * @author Donal Fellows
 */
public class StorageException extends Exception {
	private static final long serialVersionUID = 3553555491656536568L;

	/**
	 * Create a storage exception.
	 *
	 * @param message
	 *            What overall operation was being done
	 * @param cause
	 *            What caused the problem
	 */
	public StorageException(String message, Throwable cause) {
		super(message + ": " + cause.getMessage(), cause);
	}
}
