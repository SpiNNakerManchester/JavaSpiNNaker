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
package uk.ac.manchester.spinnaker.front_end.download.request;

import static com.fasterxml.jackson.annotation.JsonFormat.Shape.OBJECT;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;

import uk.ac.manchester.spinnaker.machine.HasCoreLocation;
import uk.ac.manchester.spinnaker.machine.ValidP;
import uk.ac.manchester.spinnaker.machine.ValidX;
import uk.ac.manchester.spinnaker.machine.ValidY;

/**
 * Vertex placement information.
 *
 * @author Christian-B
 * @param x
 *            The X coordinate of the core this vertex is placed on.
 * @param y
 *            The Y coordinate of the core this vertex is placed on.
 * @param p
 *            The processor ID of the core this vertex is placed on.
 * @param vertex
 *            Vertex recording region information.
 */
@JsonFormat(shape = OBJECT)
public record Placement(
		@JsonProperty(value = "x", required = true) @ValidX int x,
		@JsonProperty(value = "y", required = true) @ValidY int y,
		@ValidP @JsonProperty(value = "p", required = true) int p,
		@JsonProperty(value = "vertex", required = true) Vertex vertex)
		implements HasCoreLocation {
	/**
	 * Type reference for deserializing a list of placements.
	 */
	public static final TypeReference<List<Placement>> LIST = new TR();

	private static final class TR extends TypeReference<List<Placement>> {
	}

	@Override
	public int getX() {
		return x;
	}

	@Override
	public int getY() {
		return y;
	}

	@Override
	public int getP() {
		return p;
	}
}
