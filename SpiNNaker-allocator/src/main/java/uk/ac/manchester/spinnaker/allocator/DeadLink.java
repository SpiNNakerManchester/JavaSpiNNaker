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
package uk.ac.manchester.spinnaker.allocator;

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
