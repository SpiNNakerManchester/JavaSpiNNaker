/*
 * Copyright (c) 2019 The University of Manchester
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
package uk.ac.manchester.spinnaker.front_end.iobuf;

import static com.fasterxml.jackson.annotation.JsonFormat.Shape.OBJECT;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * The result of the IOBUF retrieval operations. Contains the notable errors and
 * warnings that were found.
 *
 * @author Donal Fellows
 */
@JsonFormat(shape = OBJECT)
public class NotableMessages {
	private List<String> errors;

	private List<String> warnings;

	/**
	 * @param errors
	 *            The notable errors in the IOBUFs.
	 * @param warnings
	 *            The notable warnings in the IOBUFs.
	 */
	public NotableMessages(
			@JsonProperty(value = "errors", required = true) List<
					String> errors,
			@JsonProperty(value = "warnings", required = true) List<
					String> warnings) {
		this.errors = errors;
		this.warnings = warnings;
	}

	/**
	 * @return The notable errors in the IOBUFs.
	 */
	public List<String> getErrors() {
		return errors;
	}

	/**
	 * @return The notable warnings in the IOBUFs.
	 */
	public List<String> getWarnings() {
		return warnings;
	}
}
