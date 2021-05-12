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

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;

import uk.ac.manchester.spinnaker.machine.ChipLocation;
import uk.ac.manchester.spinnaker.spalloc.messages.BoardCoordinates;
import uk.ac.manchester.spinnaker.spalloc.messages.BoardPhysicalCoordinates;

public class BoardLocation {

	public Job job;
	public String machine;
	public ChipLocation chip;
	public BoardCoordinates logical;
	public BoardPhysicalCoordinates physical;

	public static BoardLocation buildFromBoardQuery(Connection conn,
			ResultSet rs) throws SQLException {
		if (rs.next()) {
			BoardLocation l = new BoardLocation();
			l.machine = (String) rs.getObject("machine_name");
			l.logical = new BoardCoordinates(rs.getInt("x"), rs.getInt("y"), 0);
			l.physical = new BoardPhysicalCoordinates();
			l.chip = new ChipLocation(rs.getInt("chip_x"), rs.getInt("chip_y"));
			Integer job = (Integer) rs.getObject("job_id");
			if (job != null) {
				l.job = new Job(conn);
				l.job.id = job;
				// FIXME
			}
			return l;
		}
		return null;
	}

	public ChipLocation getBoardChip() {
		return null;
	}

	public ChipLocation getChipRelativeTo(ChipLocation rootChip) {
		return new ChipLocation(chip.getX() - rootChip.getX(),
				chip.getY() - rootChip.getY());
	}

}
