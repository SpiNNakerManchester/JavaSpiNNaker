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
package uk.ac.manchester.spinnaker.alloc.admin;

import static uk.ac.manchester.spinnaker.alloc.db.Row.bool;
import static uk.ac.manchester.spinnaker.alloc.db.Row.instant;
import static uk.ac.manchester.spinnaker.alloc.db.Row.integer;
import static uk.ac.manchester.spinnaker.alloc.db.Row.string;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;

import org.springframework.stereotype.Service;

import uk.ac.manchester.spinnaker.alloc.db.DatabaseAwareBean;
import uk.ac.manchester.spinnaker.alloc.db.DatabaseEngine.Query;
import uk.ac.manchester.spinnaker.alloc.db.DatabaseEngine.Update;
import uk.ac.manchester.spinnaker.alloc.db.Row;
import uk.ac.manchester.spinnaker.alloc.model.BoardIssueReport;

/**
 * How to manage the state of a machine and boards in it.
 *
 * @author Donal Fellows
 */
@Service
public class MachineStateControl extends DatabaseAwareBean {
	/**
	 * Access to the enablement-state of a board.
	 */
	public final class BoardState {
		/** The name of the containing SpiNNaker machine. */
		public final String machineName;

		/** The board ID. Unique. */
		public final int id;

		/** The X triad coordinate. */
		public final int x;

		/** The Y triad coordinate. */
		public final int y;

		/** The Z triad coordinate. */
		public final int z;

		/** The cabinet number. */
		public final int cabinet;

		/** The frame number. */
		public final int frame;

		/** The board number. */
		public final Integer board;

		/** The IP address managed by the board's root chip. */
		public final String address;

		private BoardState(Row row) {
			this.id = row.getInt("board_id");
			this.x = row.getInt("x");
			this.y = row.getInt("y");
			this.z = row.getInt("z");
			this.cabinet = row.getInt("cabinet");
			this.frame = row.getInt("frame");
			this.board = row.getInteger("board_num");
			this.address = row.getString("address");
			this.machineName = row.getString("machine_name");
		}

		/**
		 * @return The state of the board.
		 */
		public boolean getState() {
			return execute(false, conn -> {
				try (Query q = conn.query(GET_FUNCTIONING_FIELD)) {
					return q.call1(id).map(bool("functioning")).orElse(false);
				}
			});
		}

		public void setState(boolean newValue) {
			execute(conn -> {
				try (Update u = conn.update(SET_FUNCTIONING_FIELD)) {
					return u.call(newValue, id);
				}
			});
		}

		/** @return What job has been allocated to the board? */
		public Optional<Integer> getAllocatedJob() {
			return execute(false, conn -> {
				try (Query q = conn.query(GET_BOARD_JOB)) {
					return q.call1(id).map(integer("allocated_job"));
				}
			});
		}

		/** @return Is the board switched on? */
		public boolean getPower() {
			return execute(false, conn -> {
				try (Query q = conn.query(GET_BOARD_POWER_INFO)) {
					return q.call1(id).map(bool("board_power")).orElse(false);
				}
			});
		}

		/** @return When was the board last switched on? */
		public Optional<Instant> getPowerOnTime() {
			return execute(false, conn -> {
				try (Query q = conn.query(GET_BOARD_POWER_INFO)) {
					return q.call1(id).map(instant("power_on_timestamp"));
				}
			});
		}

		/** @return When was the board last switched off? */
		public Optional<Instant> getPowerOffTime() {
			return execute(false, conn -> {
				try (Query q = conn.query(GET_BOARD_POWER_INFO)) {
					return q.call1(id).map(instant("power_off_timestamp"));
				}
			});
		}

		/** @return What issues have been logged against the board? */
		public List<BoardIssueReport> getReports() {
			return execute(false, conn -> {
				try (Query q = conn.query(GET_BOARD_REPORTS)) {
					return q.call(id).map(BoardIssueReport::new).toList();
				}
			});
		}
	}

	/**
	 * Look up a board for management.
	 *
	 * @param id
	 *            The unique ID of the board. Because this is fully unique, the
	 *            machine name is not needed.
	 * @return Board state manager
	 */
	public Optional<BoardState> findId(int id) {
		return execute(false, conn -> {
			try (Query q = conn.query(FIND_BOARD_BY_ID)) {
				return q.call1(id).map(BoardState::new);
			}
		});
	}

	/**
	 * Look up a board for management.
	 *
	 * @param machine
	 *            The name of the machine.
	 * @param x
	 *            X coordinate
	 * @param y
	 *            Y coordinate
	 * @param z
	 *            Z coordinate
	 * @return Board state manager
	 */
	public Optional<BoardState> findTriad(String machine, int x, int y, int z) {
		return execute(false, conn -> {
			try (Query q = conn.query(FIND_BOARD_BY_NAME_AND_XYZ)) {
				return q.call1(machine, x, y, z).map(BoardState::new);
			}
		});
	}

	/**
	 * Look up a board for management.
	 *
	 * @param machine
	 *            The name of the machine.
	 * @param c
	 *            Cabinet number
	 * @param f
	 *            Frame number
	 * @param b
	 *            Board number
	 * @return Board state manager
	 */
	public Optional<BoardState> findPhysical(String machine, int c, int f,
			int b) {
		return execute(false, conn -> {
			try (Query q = conn.query(FIND_BOARD_BY_NAME_AND_CFB)) {
				return q.call1(machine, c, f, b).map(BoardState::new);
			}
		});
	}

	/**
	 * Look up a board for management.
	 *
	 * @param machine
	 *            The name of the machine.
	 * @param address
	 *            Board IP address
	 * @return Board state manager
	 */
	public Optional<BoardState> findIP(String machine, String address) {
		return execute(false, conn -> {
			try (Query q = conn.query(FIND_BOARD_BY_NAME_AND_IP_ADDRESS)) {
				return q.call1(machine, address).map(BoardState::new);
			}
		});
	}

	/**
	 * @return The mapping from machine names+IDs to tags.
	 */
	public List<MachineTagging> getMachineTagging() {
		return execute(false, conn -> {
			try (Query getMachines = conn.query(GET_ALL_MACHINES);
					Query getTags = conn.query(GET_TAGS)) {
				List<MachineTagging> infos = new ArrayList<>();
				getMachines.call(true).map(MachineTagging::new)
						.forEach(infos::add);
				for (MachineTagging t : infos) {
					t.setTags(
							getTags.call(t.getId()).map(string("tag")).toSet());
				}
				return infos;
			}
		});
	}

	/**
	 * @return The unacknowledged reports about boards with potential problems
	 *         in existing machines, categorised by machine.
	 */
	public Map<String, List<BoardIssueReport>> getMachineReports() {
		return execute(false, conn -> {
			try (Query getMachines = conn.query(GET_ALL_MACHINES);
					Query getMachineReports = conn.query(GET_MACHINE_REPORTS)) {
				Map<String, List<BoardIssueReport>> reports = new TreeMap<>();
				getMachines.call(true)
						.forEach(machine -> reports.put(
								machine.getString("machine_name"),
								getMachineReports
										.call(machine.getInt("machine_id"))
										.map(BoardIssueReport::new).toList()));
				return reports;
			}
		});
	}

	/**
	 * Replace the tags on a machine with a given set.
	 *
	 * @param machineId
	 *            The ID of the machine to update the tags of.
	 * @param tags
	 *            The tags to apply. Existing tags will be removed.
	 */
	public void updateTags(int machineId, Set<String> tags) {
		execute(conn -> {
			try (Update delete = conn.update(DELETE_MACHINE_TAGS);
					Update add = conn.update(INSERT_TAG)) {
				delete.call(machineId);
				for (String tag : tags) {
					add.call(machineId, tag);
				}
				return this; // Unimportant value
			}
		});
	}
}
