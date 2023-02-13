/*
 * Copyright (c) 2019-2023 The University of Manchester
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
