/*
 * Copyright (c) 2021 The University of Manchester
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
