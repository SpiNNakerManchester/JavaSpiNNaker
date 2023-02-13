/*
 * Copyright (c) 2021-2023 The University of Manchester
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
package uk.ac.manchester.spinnaker.alloc.client;

import static com.fasterxml.jackson.annotation.JsonFormat.Shape.ARRAY;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

/** Describes a dead or administratively disabled link. */
@JsonFormat(shape = ARRAY)
@JsonPropertyOrder({
	"end1", "end2"
})
public final class DeadLink {
	/** One end of a dead link. */
	public static final class End {
		/** The board at the end of a dead link. */
		public final BoardCoords board;

		/** The direction that the dead link goes in. */
		public final Direction direction;

		/**
		 * @param board
		 *            The board at the end of a dead link.
		 * @param direction
		 *            The direction that the dead link goes in.
		 */
		public End(@JsonProperty("board") BoardCoords board,
				@JsonProperty("direction") Direction direction) {
			this.board = board;
			this.direction = direction;
		}
	}

	@JsonProperty
	private DeadLink.End end1;

	@JsonProperty
	private DeadLink.End end2;

	/**
	 * Get the two ends of the dead link.
	 *
	 * @return The ends of the link.
	 */
	public List<DeadLink.End> getEnds() {
		return List.of(end1, end2);
	}
}
