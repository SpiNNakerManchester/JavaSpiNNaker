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

import static uk.ac.manchester.spinnaker.alloc.DatabaseEngine.query;
import static uk.ac.manchester.spinnaker.alloc.allocator.BoardLocation.buildFromBoardQuery;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;

import uk.ac.manchester.spinnaker.alloc.DatabaseEngine;
import uk.ac.manchester.spinnaker.alloc.DatabaseEngine.Query;
import uk.ac.manchester.spinnaker.alloc.allocator.Epochs.JobsEpoch;
import uk.ac.manchester.spinnaker.alloc.allocator.Epochs.MachinesEpoch;

public class Machine {
	private static final String FIND_BOARD_BY_CHIP =
			"SELECT boards.board_id, address, bmp_id, board_num, x, y, "
					+ "job_id, m.machine_name, bmp.cabinet, bmp.frame, "
					+ "boards.board_num, root_x + bmc.chip_x AS chip_x,"
					+ "root_y + bmc.chip_y AS chip_y FROM boards "
					+ "JOIN bmp ON boards.bmp_id = bmp.bmp_id "
					+ "JOIN machines AS m ON boards.machine_id = m.machine_id "
					+ "JOIN board_model_coords AS bmc "
					+ "ON m.board_model = bmc.model "
					+ "WHERE boards.machine_id = ? "
					+ "AND chip_x = ? AND chip_y = ? LIMIT 1";

	private static final String FIND_BOARD_BY_CFB =
			"SELECT boards.board_id, address, bmp_id, board_num, x, y, "
					+ "job_id, m.machine_name, bmp.cabinet, bmp.frame, "
					+ "boards.board_num, root_x AS chip_x, root_y AS chip_y "
					+ "FROM boards JOIN bmp ON boards.bmp_id = bmp.bmp_id "
					+ "JOIN machines AS m ON boards.machine_id = m.machine_id "
					+ "WHERE boards.machine_id = ? AND bmp.cabinet = ? "
					+ "AND bmp.frame = ? AND boards.board_num = ? LIMIT 1";

	private static final String FIND_BOARD_BY_XYZ =
			"SELECT boards.board_id, address, bmp_id, board_num, x, y, "
					+ "job_id, m.machine_name, bmp.cabinet, bmp.frame, "
					+ "boards.board_num, root_x AS chip_x, root_y AS chip_y "
					+ "FROM boards JOIN bmp ON boards.bmp_id = bmp.bmp_id "
					+ "JOIN machines AS m ON boards.machine_id = m.machine_id "
					+ "WHERE boards.machine_id = ? AND boards.x = ? "
					+ "AND boards.y = ? AND 0 = ? LIMIT 1";

	private static final String GET_TAGS =
			"SELECT tag FROM tags WHERE machine_id = ?";

	/** The ID of the machine. */
	public int id;

	/** The name of the machine. */
	public String name;

	/** The tags associated with the machine. */
	public List<String> tags = new ArrayList<>();

	/** The width of the machine. */
	public int width;

	/** The height of the machine. */
	public int height;

	// TODO: dead boards, dead links

	@JsonIgnore
	private DatabaseEngine db;

	@JsonIgnore
	private MachinesEpoch epoch;

	/** Don't use this constructor; just there for serialization engine. */
	public Machine() {
		throw new UnsupportedOperationException();
	}

	Machine(DatabaseEngine db, Connection conn, ResultSet rs,
			MachinesEpoch epoch) throws SQLException {
		this.db = db;
		this.epoch = epoch;
		id = rs.getInt("machine_id");
		name = rs.getString("machine_name");
		width = rs.getInt("width");
		height = rs.getInt("height");
		try (Query getTags = DatabaseEngine.query(conn, GET_TAGS)) {
			for (ResultSet tagSet : getTags.call(id)) {
				tags.add(tagSet.getString("tag"));
			}
		}
	}

	public void waitForChange(long timeout) {
		try {
			epoch.waitForChange(timeout);
		} catch (InterruptedException ignored) {
		}
	}

	public BoardLocation getBoardByChip(int x, int y, JobsEpoch je)
			throws SQLException {
		try (Connection conn = db.getConnection();
				Query q = query(conn, FIND_BOARD_BY_CHIP)) {
			for (ResultSet rs : q.call(id, x, y)) {
				return buildFromBoardQuery(conn, rs, je);
			}
		}
		return null; // Query failed
	}

	public BoardLocation getBoardByPhysicalCoords(int cabinet, int frame,
			int board, JobsEpoch je) throws SQLException {
		try (Connection conn = db.getConnection();
				Query q = query(conn, FIND_BOARD_BY_CFB)) {
			for (ResultSet rs : q.call(id, cabinet, frame, board)) {
				return buildFromBoardQuery(conn, rs, je);
			}
		}
		return null; // Query failed
	}

	public BoardLocation getBoardByLogicalCoords(int x, int y, int z,
			JobsEpoch je) throws SQLException {
		try (Connection conn = db.getConnection();
				Query q = query(conn, FIND_BOARD_BY_XYZ)) {
			for (ResultSet rs : q.call(id, x, y, z)) {
				return buildFromBoardQuery(conn, rs, je);
			}
		}
		return null; // Query failed
	}

	public String getRootBoardBMPAddress() throws SQLException {
		// FIXME Auto-generated method stub
		return null;
	}

	public List<Integer> getBoardNumbers() throws SQLException {
		// FIXME Auto-generated method stub
		return null;
	}

}
