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
package uk.ac.manchester.spinnaker.alloc.allocator;

import java.sql.ResultSet;
import java.sql.SQLException;

import uk.ac.manchester.spinnaker.alloc.allocator.Epochs.JobsEpoch;
import uk.ac.manchester.spinnaker.machine.ChipLocation;
import uk.ac.manchester.spinnaker.spalloc.messages.BoardCoordinates;
import uk.ac.manchester.spinnaker.spalloc.messages.BoardPhysicalCoordinates;

/**
 * Describes the locations of boards in a machine.
 *
 * @author Donal Fellows
 */
public final class BoardLocation {
	public Job job;

	/** What machine is the board on? */
	public final String machine;

	/** Where is the chip of interest? Usually the root chip of the board. */
	public final ChipLocation chip;

	/** Where is the board logically within its machine? */
	public final BoardCoordinates logical;

	/** Where is the board physically in its machine? */
	public final BoardPhysicalCoordinates physical;

	private BoardLocation(String machine, BoardCoordinates logical,
			BoardPhysicalCoordinates physical, ChipLocation chip) {
		this.machine = machine;
		this.logical = logical;
		this.physical = physical;
		this.chip = chip;
	}

	static BoardLocation buildFromBoardQuery(Spalloc spallocCore, ResultSet row,
			JobsEpoch epoch) throws SQLException {
		String name = row.getString("machine_name");
		BoardCoordinates logical =
				new BoardCoordinates(row.getInt("x"), row.getInt("y"), 0);
		BoardPhysicalCoordinates physical =
				new BoardPhysicalCoordinates(0, 0, 0); // FIXME
		ChipLocation chip =
				new ChipLocation(row.getInt("chip_x"), row.getInt("chip_y"));

		BoardLocation l = new BoardLocation(name, logical, physical, chip);
		Integer job = (Integer) row.getObject("job_id");
		if (job != null) {
			l.job = new Job(spallocCore, epoch, job);
			// FIXME
		}
		return l;
	}

	public ChipLocation getBoardChip() {
		return null;
	}

	public ChipLocation getChipRelativeTo(ChipLocation rootChip) {
		return new ChipLocation(chip.getX() - rootChip.getX(),
				chip.getY() - rootChip.getY());
	}
}
