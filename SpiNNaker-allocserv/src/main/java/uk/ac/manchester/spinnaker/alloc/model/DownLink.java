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
package uk.ac.manchester.spinnaker.alloc.model;

import static com.fasterxml.jackson.annotation.JsonFormat.Shape.ARRAY;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.google.errorprone.annotations.Immutable;

/**
 * Describes a link that is disabled.
 *
 * @author Donal Fellows
 */
@Immutable
@JsonFormat(shape = ARRAY)
public final class DownLink {
	/**
	 * Describes one end of a link that is disabled.
	 *
	 * @author Donal Fellows
	 */
	@Immutable
	public static final class End {
		private End(BoardCoords board, Direction direction) {
			this.board = board;
			this.direction = direction;
		}

		/**
		 * On what board is this end of the link.
		 */
		@Valid
		public final BoardCoords board;

		/**
		 * In which direction does this end of the link go?
		 */
		@NotNull
		public final Direction direction;
	}

	/**
	 * Create a down link description.
	 *
	 * @param board1
	 *            Which board is one end of the link.
	 * @param dir1
	 *            In which direction off of {@code board1} is the link.
	 * @param board2
	 *            Which board is one end of the link.
	 * @param dir2
	 *            In which direction off of {@code board2} is the link.
	 */
	public DownLink(BoardCoords board1, Direction dir1, BoardCoords board2,
			Direction dir2) {
		end1 = new End(board1, dir1);
		end2 = new End(board2, dir2);
	}

	/** One end of the down link. */
	@Valid
	public final DownLink.End end1;

	/** The other end of the down link. */
	@Valid
	public final DownLink.End end2;
}
