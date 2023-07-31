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
package uk.ac.manchester.spinnaker.nmpi.model.job;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a stack trace provided remotely.
 */
public class RemoteStackTrace {

	/**
	 * The elements of the stack trace.
	 */
	private List<RemoteStackTraceElement> elements = new ArrayList<>();

	/**
	 * Create a basic remote stack trace without stack elements.
	 */
	public RemoteStackTrace() {
		// Does Nothing
	}

	/**
	 * Create a basic remote stack trace from an exception.
	 *
	 * @param throwable
	 *            The exception to make the stack trace from.
	 */
	public RemoteStackTrace(final Throwable throwable) {
		for (final var element : throwable.getStackTrace()) {
			elements.add(new RemoteStackTraceElement(element));
		}
	}

	/**
	 * Create a basic remote stack trace from a list of elements.
	 *
	 * @param elementsParam
	 *            The elements to make the stack trace from.
	 */
	public RemoteStackTrace(final List<RemoteStackTraceElement> elementsParam) {
		this.elements = elementsParam;
	}

	/**
	 * Get the elements.
	 *
	 * @return the elements
	 */
	public List<RemoteStackTraceElement> getElements() {
		return elements;
	}

	/**
	 * Set the elements.
	 *
	 * @param elementsParam The elements to set
	 */
	public void setElements(final List<RemoteStackTraceElement> elementsParam) {
		this.elements = elementsParam;
	}
}
