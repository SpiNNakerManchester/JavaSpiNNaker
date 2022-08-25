/*
 * Copyright (c) 2018 The University of Manchester
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package uk.ac.manchester.spinnaker.spalloc.messages;

import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.NON_PRIVATE;

import com.fasterxml.jackson.annotation.JsonAutoDetect;

/**
 * A response to a request that indicates a failure.
 */
@JsonAutoDetect(setterVisibility = NON_PRIVATE)
public class ExceptionResponse implements Response {
	private String exception;

	/** @return The exception message. Should not include a stack trace. */
	public String getException() {
		return exception;
	}

	void setException(String exception) {
		this.exception = exception == null ? "" : exception.toString();
	}
}
