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
package uk.ac.manchester.spinnaker.data_spec;

/**
 * An exception that indicates that a memory region has not been selected.
 */
public class NoRegionSelectedException extends DataSpecificationException {
	private static final long serialVersionUID = -3704038507680648327L;

	/**
	 * Create an instance.
	 *
	 * @param msg
	 *            The message in the exception.
	 */
	NoRegionSelectedException(String msg) {
		super(msg);
	}

	/**
	 * Create an instance.
	 *
	 * @param command
	 *            What command was using memory without a region selected.
	 */
	NoRegionSelectedException(Commands command) {
		super("no region has been selected for writing by " + command);
	}
}
