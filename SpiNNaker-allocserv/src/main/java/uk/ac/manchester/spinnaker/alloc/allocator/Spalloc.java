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
import static uk.ac.manchester.spinnaker.alloc.DatabaseEngine.transaction;
import static uk.ac.manchester.spinnaker.alloc.DatabaseEngine.update;
import static uk.ac.manchester.spinnaker.alloc.allocator.JobState.DESTROYED;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import uk.ac.manchester.spinnaker.alloc.DatabaseEngine;
import uk.ac.manchester.spinnaker.alloc.DatabaseEngine.Query;
import uk.ac.manchester.spinnaker.alloc.DatabaseEngine.Update;
import uk.ac.manchester.spinnaker.alloc.allocator.Epochs.JobsEpoch;
import uk.ac.manchester.spinnaker.alloc.allocator.Epochs.MachinesEpoch;

@Component
public class Spalloc implements SpallocInterface {
	private static final String GET_ALL_MACHINES =
			"SELECT machine_id, machine_name, width, height FROM machines";

	private static final String GET_NAMED_MACHINE =
			"SELECT machine_id, machine_name, width, height FROM machines "
					+ "WHERE machine_name = ? LIMIT 1";

	private static final String GET_JOB_IDS =
			"SELECT machine_id, job_state, keepalive_timestamp FROM jobs "
					+ "ORDER BY job_id DESC LIMIT ? OFFSET ?";

	private static final String GET_LIVE_JOB_IDS =
			"SELECT machine_id, job_state, keepalive_timestamp FROM jobs "
					+ "WHERE job_state != ? "
					+ "ORDER BY job_id DESC LIMIT ? OFFSET ?";

	private static final String GET_JOB =
			"SELECT machine_id, width, height, root_id, job_state, "
					+ "keepalive_timestamp, keepalive_host FROM jobs "
					+ "WHERE job_id = ? LIMIT 1";

	private static final String INSERT_JOB = "INSERT INTO jobs("
			+ "machine_id, owner, keepalive_timestamp, create_timestamp) "
			+ "VALUES (?, ?, ?, ?)";

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

	@Autowired
	Epochs epochs;

	@Override
	public Map<String, Machine> getMachines() throws SQLException {
		try (Connection conn = db.getConnection()) {
			return getMachines(conn);
		}
	}

	private Map<String, Machine> getMachines(Connection conn)
			throws SQLException {
		MachinesEpoch me = epochs.getMachineEpoch();
		Map<String, Machine> map = new HashMap<>();
		try (Query listMachines = query(conn, GET_ALL_MACHINES)) {
			for (ResultSet rs : listMachines.call()) {
				Machine m = new Machine(this, db, conn, rs, me);
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
		MachinesEpoch me = epochs.getMachineEpoch();
		Machine m = null;
		try (Query namedMachine = query(conn, GET_NAMED_MACHINE)) {
			for (ResultSet rs : namedMachine.call(name)) {
				m = new Machine(this, db, conn, rs, me);
				break;
			}
		}
		return m;
	}

	@Override
	public JobCollection getJobs(boolean deleted, int limit, int start)
			throws SQLException {
		JobsEpoch je = epochs.getJobsEpoch();
		try (Connection conn = db.getConnection()) {
			JobCollection jc = new JobCollection(conn, je);
			if (deleted) {
				try (Query jobs = query(conn, GET_JOB_IDS)) {
					for (ResultSet rs : jobs.call(limit, start)) {
						jc.addJob(rs.getInt("job_id"), rs.getInt("job_state"),
								rs.getLong("keepalive_timestamp"));
					}
				}
			} else {
				try (Query jobs = query(conn, GET_LIVE_JOB_IDS)) {
					for (ResultSet rs : jobs.call(DESTROYED, limit, start)) {
						jc.addJob(rs.getInt("job_id"), rs.getInt("job_state"),
								rs.getLong("keepalive_timestamp"));
					}
				}
			}
			return jc;
		}
	}

	@Override
	public Job getJob(int id) throws SQLException {
		try (Connection conn = db.getConnection()) {
			return getJob(id, conn);
		}
	}

	private Job getJob(int id, Connection conn) throws SQLException {
		JobsEpoch epoch = epochs.getJobsEpoch();
		try (Query s = query(conn, GET_JOB)) {
			for (ResultSet rs : s.call(id)) {
				Job j = new Job(this, epoch, rs);
				return j;
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

				int id = insertJob(conn, m, owner);
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

	private static final int N_COORDS_COUNT = 1;

	private static final int N_COORDS_RECTANGLE = 2;

	private static final int N_COORDS_LOCATION = 3;

	private void insertRequest(Connection conn, int id, List<Integer> dims,
			Integer numDeadBoards) throws SQLException {
		switch (dims.size()) {
		case N_COORDS_COUNT:
			// Request by number of boards
			try (Update ps = update(conn, INSERT_REQ_N_BOARDS)) {
				ps.call(id, dims.get(0), numDeadBoards);
			}
			break;
		case N_COORDS_RECTANGLE:
			// Request by specific size
			try (Update ps = update(conn, INSERT_REQ_SIZE)) {
				ps.call(id, dims.get(0), dims.get(1), numDeadBoards);
			}
			break;
		case N_COORDS_LOCATION:
			// Request by specific (physical) location
			try (Update ps = update(conn, INSERT_REQ_LOCATION)) {
				ps.call(id, dims.get(0), dims.get(1), dims.get(2));
			}
			break;
		default:
			throw new Error("should be unreachable");
		}
	}

	private int insertJob(Connection conn, Machine m, String owner)
			throws SQLException {
		// TODO add in additional info
		Date now = new Date();
		int pk = -1;
		try (Update makeJob = update(conn, INSERT_JOB)) {
			for (int key : makeJob.keys(m.id, owner, now, now)) {
				pk = key;
				break;
			}
		}
		return pk;
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

	private static final String UPDATE_KEEPALIVE =
			"UPDATE jobs SET keepalive_timestamp = ?, keepalive_host = ? "
					+ "WHERE job_id = ? AND job_state != ?";

	void jobAccess(Job job, Date now, String keepaliveAddress)
			throws SQLException {
		try (Connection conn = db.getConnection();
				Update keepAlive = update(conn, UPDATE_KEEPALIVE)) {
			keepAlive.call(now, keepaliveAddress, job.id, DESTROYED);
		}
	}

	private static final String DESTROY_JOB = "UPDATE jobs SET "
			+ "job_state = ?, death_reason = ?, death_timestamp = ? "
			+ "WHERE job_id = ? AND job_state != ?";

	void jobDestroy(Job job, Date now, String reason) throws SQLException {
		try (Connection conn = db.getConnection();
				Update destroyJob = update(conn, DESTROY_JOB)) {
			destroyJob.call(DESTROYED, reason, now, job.id, DESTROYED);
		}
	}
}
