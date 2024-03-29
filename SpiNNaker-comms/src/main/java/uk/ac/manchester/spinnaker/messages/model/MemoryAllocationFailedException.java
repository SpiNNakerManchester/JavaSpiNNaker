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
package uk.ac.manchester.spinnaker.messages.model;

/**
 * Indicate that a memory allocation operation has failed.
 */
public class MemoryAllocationFailedException extends Exception {
	private static final long serialVersionUID = 4463116302552127934L;

	/**
	 * @param message
	 *            The message text of the exception.
	 */
	public MemoryAllocationFailedException(String message) {
		super(message);
	}
}
