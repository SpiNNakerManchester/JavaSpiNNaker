/*
 * Copyright (c) 2021 The University of Manchester
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
package uk.ac.manchester.spinnaker.alloc.compat;

import static java.util.Objects.isNull;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * A message indicating an operation failed.
 *
 * @author Donal Fellows
 */
final class ExceptionResponse {
	private String exception;

	ExceptionResponse(String message) {
		exception = message;
	}

	@JsonProperty("exception")
	public String getException() {
		return exception;
	}

	public void setException(String exception) {
		this.exception = isNull(exception) ? "" : exception.toString();
	}
}
