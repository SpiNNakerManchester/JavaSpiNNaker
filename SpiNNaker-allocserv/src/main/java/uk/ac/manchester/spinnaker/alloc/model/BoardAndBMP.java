/*
 * Copyright (c) 2023 The University of Manchester
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
package uk.ac.manchester.spinnaker.alloc.model;

import uk.ac.manchester.spinnaker.alloc.admin.MachineStateControl.BoardState;
import uk.ac.manchester.spinnaker.alloc.db.Row;

/**
 * A Board and a BMP.
 *
 */
public class BoardAndBMP {

	/** The board identifier. */
	public final int boardId;

	/** The BMP identifier. */
	public final int bmpId;

	/**
	 * Make a new Board-and-BMP from a database query.
	 *
	 * @param result The database query results to read from.
	 */
	public BoardAndBMP(Row result) {
		this.boardId = result.getInt("board_id");
		this.bmpId = result.getInt("bmp_id");
	}

	public BoardAndBMP(BoardState bs) {
		this.boardId = bs.board;
		this.bmpId = bs.bmpId;
	}

}
