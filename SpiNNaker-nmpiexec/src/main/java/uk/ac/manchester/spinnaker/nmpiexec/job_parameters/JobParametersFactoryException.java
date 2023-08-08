/*
 * Copyright (c) 2014 The University of Manchester
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
package uk.ac.manchester.spinnaker.nmpiexec.job_parameters;

/**
 * Indicates that whilst the job type was supported, there was an error
 * converting the job to parameters.
 */
public class JobParametersFactoryException extends Exception {
	private static final long serialVersionUID = 1L;

	/**
	 * Create an exception with a message.
	 *
	 * @param message
	 *            The message
	 */
	public JobParametersFactoryException(String message) {
		super(message);
	}

	/**
	 * Create an exception with a message and cause.
	 *
	 * @param message
	 *            The message
	 * @param cause
	 *            The cause
	 */
	public JobParametersFactoryException(String message, Throwable cause) {
		super(message, cause);
	}
}
