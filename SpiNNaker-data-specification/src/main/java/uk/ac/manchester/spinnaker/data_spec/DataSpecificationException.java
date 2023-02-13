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
package uk.ac.manchester.spinnaker.data_spec;

/**
 * Exceptions thrown by the Data Specification code.
 */
public class DataSpecificationException extends Exception {
	private static final long serialVersionUID = 6442679259006679916L;

	/**
	 * Create an exception.
	 *
	 * @param msg
	 *            The message in the exception.
	 */
	public DataSpecificationException(String msg) {
		super(msg);
	}

	/**
	 * Create an exception.
	 */
	DataSpecificationException() {
	}

	/**
	 * Create an exception.
	 *
	 * @param msg
	 *            The message in the exception.
	 * @param cause
	 *            The cause of the exception.
	 */
	public DataSpecificationException(String msg, Throwable cause) {
		super(msg, cause);
	}
}
