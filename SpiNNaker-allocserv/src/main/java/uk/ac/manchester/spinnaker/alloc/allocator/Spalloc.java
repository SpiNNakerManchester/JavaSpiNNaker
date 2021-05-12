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

import static java.sql.Statement.RETURN_GENERATED_KEYS;
import static uk.ac.manchester.spinnaker.alloc.DatabaseEngine.runQuery;
import static uk.ac.manchester.spinnaker.alloc.DatabaseEngine.runUpdate;
import static uk.ac.manchester.spinnaker.alloc.DatabaseEngine.transaction;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import uk.ac.manchester.spinnaker.alloc.DatabaseEngine;

@Component
public class Spalloc implements SpallocInterface {
	private static final String GET_ALL_MACHINES =
			"SELECT machine_id, machine_name, width, height FROM machines";

	private static final String GET_NAMED_MACHINE =
			"SELECT machine_id, machine_name, width, height FROM machines "
					+ "WHERE machine_name = ? LIMIT 1";

	private static final String GET_TAGS =
			"SELECT tag FROM tags WHERE machine_id = ?";

	private static final String GET_JOB_IDS =
			"SELECT machine_id, job_state, keepalive_timestamp FROM jobs";

	private static final String GET_JOB =
			"SELECT machine_id, width, height, root_id, job_state, "
					+ "keepalive_timestamp, keepalive_host FROM jobs "
					+ "WHERE job_id = ? LIMIT 1";

	private static final String INSERT_JOB =
			"INSERT INTO jobs(machine_id, keepalive_timestamp) VALUES (?, ?)";

	private static final String INSERT_REQ_N_BOARDS =
			"INSERT INTO job_request(job_id, num_boards, max_dead_boards) "
					+ "VALUES (?, ?, ?)";

	private static final String INSERT_REQ_SIZE =
			"INSERT INTO job_request(job_id, width, height, max_dead_boards) "
					+ "VALUES (?, ?, ?, ?)";

	private static final String INSERT_REQ_LOCATION =
			"INSERT INTO job_request(job_id, cabinet, frame, board) "
					+ "VALUES (?, ?, ?, ?)";

	@Autowired
	DatabaseEngine db;

	private List<String> getMachineTags(PreparedStatement ts, int id)
			throws SQLException {
		// Takes a prepared statement because that allows reuse
		List<String> tags = new ArrayList<>();
		try (ResultSet rs = runQuery(ts, id)) {
			while (rs.next()) {
				tags.add(rs.getString("tag"));
			}
		}
		return tags;
	}

	@Override
	public Map<String, Machine> getMachines() throws SQLException {
		try (Connection conn = db.getConnection()) {
			return getMachines(conn);
		}
	}

	private Map<String, Machine> getMachines(Connection conn)
			throws SQLException {
		Map<String, Machine> map = new HashMap<>();
		try (PreparedStatement ms = conn.prepareStatement(GET_ALL_MACHINES);
				PreparedStatement ts = conn.prepareStatement(GET_TAGS);
				ResultSet rs = ms.executeQuery()) {
			while (rs.next()) {
				Machine m = new Machine(conn);
				m.id = rs.getInt("machine_id");
				m.name = rs.getString("machine_name");
				m.width = rs.getInt("width");
				m.height = rs.getInt("height");
				m.tags = getMachineTags(ts, m.id);
				map.put(m.name, m);
			}
		}
		return map;
	}

	@Override
	public Machine getMachine(String name) throws SQLException {
		try (Connection conn = db.getConnection()) {
			return getMachine(name, conn);
		}
	}

	private Machine getMachine(String name, Connection conn)
			throws SQLException {
		try (PreparedStatement ms = conn.prepareStatement(GET_NAMED_MACHINE);
				PreparedStatement ts = conn.prepareStatement(GET_TAGS)) {
			try (ResultSet rs = runQuery(ms, name)) {
				while (rs.next()) {
					Machine m = new Machine(conn);
					m.id = rs.getInt("machine_id");
					m.name = rs.getString("machine_name");
					m.width = rs.getInt("width");
					m.height = rs.getInt("height");
					m.tags = getMachineTags(ts, m.id);
					return m;
				}
			}
		}
		return null;
	}

	@Override
	public JobCollection getJobs() throws SQLException {
		try (Connection conn = db.getConnection()) {
			JobCollection jc = new JobCollection(conn);
			try (PreparedStatement s = conn.prepareStatement(GET_JOB_IDS);
					ResultSet rs = s.executeQuery()) {
				while (rs.next()) {
					jc.addJob(rs.getInt("machine_id"), rs.getInt("job_state"),
							rs.getLong("keepalive_timestamp"));
				}
			}
			return jc;
		}
	}

	private static Integer getInteger(ResultSet rs, String column)
			throws SQLException {
		// This is nuts!
		int value = rs.getInt(column);
		return rs.wasNull() ? null : value;
	}

	@Override
	public Job getJob(int id) throws SQLException {
		try (Connection conn = db.getConnection()) {
			return getJob(id, conn);
		}
	}

	private Job getJob(int id, Connection conn) throws SQLException {
		try (PreparedStatement s = conn.prepareStatement(GET_JOB)) {
			try (ResultSet rs = runQuery(s, id)) {
				while (rs.next()) {
					Job j = new Job(conn);
					j.id = rs.getInt("machine_id");
					j.width = getInteger(rs, "width");
					j.height = getInteger(rs, "height");
					j.root = getInteger(rs, "root_id");
					j.state = rs.getInt("job_state");
					j.keepaliveTime = rs.getLong("keepalive_timestamp");
					j.keepaliveHost = rs.getString("keepalive_host");
					// TODO fill this out
					return j;
				}
			}
		}
		return null;
	}

	@Override
	public Job createJob(String owner, List<Integer> dimensions,
			String machineName, List<String> tags, Integer maxDeadBoards)
			throws SQLException {
		try (Connection conn = db.getConnection()) {
			return transaction(conn, () -> {
				Machine m = selectMachine(conn, machineName, tags);
				if (m == null) {
					// Cannot find machine!
					return null;
				}

				int id = insertJob(conn, m);
				if (id < 0) {
					// Insert failed
					return null;
				}

				// Ask the allocator engine to do the allocation
				insertRequest(conn, id, dimensions, maxDeadBoards);
				return getJob(id);
			});
		}
	}

	private void insertRequest(Connection conn, int id,
			List<Integer> dimensions, Integer numDeadBoards)
			throws SQLException {
		switch (dimensions.size()) {
		case 1:
			// Request by number of boards
			try (PreparedStatement ps =
					conn.prepareStatement(INSERT_REQ_N_BOARDS)) {
				runUpdate(ps, id, dimensions.get(0), numDeadBoards);
			}
			break;
		case 2:
			// Request by specific size
			try (PreparedStatement ps =
					conn.prepareStatement(INSERT_REQ_SIZE)) {
				runUpdate(ps, id, dimensions.get(0), dimensions.get(1),
						numDeadBoards);
			}
			break;
		case 3:
			// Request by specific (logical) location
			try (PreparedStatement ps =
					conn.prepareStatement(INSERT_REQ_LOCATION)) {
				runUpdate(ps, id, dimensions.get(0), dimensions.get(1),
						dimensions.get(2));
			}
			break;
		default:
			throw new Error("should be unreachable");
		}
	}

	private int insertJob(Connection conn, Machine m) throws SQLException {
		// TODO add in additional info
		Date timestamp = new Date();
		try (PreparedStatement ps =
				conn.prepareStatement(INSERT_JOB, RETURN_GENERATED_KEYS)) {
			runUpdate(ps, m.id, timestamp);
			try (ResultSet rs = ps.getGeneratedKeys()) {
				while (rs.next()) {
					return rs.getInt(1);
				}
			}
		}
		return -1;
	}

	private Machine selectMachine(Connection conn, String machineName,
			List<String> tags) throws SQLException {
		if (machineName != null) {
			return getMachine(machineName, conn);
		} else if (!tags.isEmpty()) {
			for (Machine m : getMachines(conn).values()) {
				if (m.tags.containsAll(tags)) {
					/*
					 * Originally, spalloc checked if allocation was possible;
					 * we just assume that it is because there really isn't ever
					 * going to be that many different machines on one service.
					 */
					return m;
				}
			}
		}
		return null;
	}

}
