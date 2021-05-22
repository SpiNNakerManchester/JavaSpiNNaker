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

import static java.util.Collections.unmodifiableList;
import static uk.ac.manchester.spinnaker.alloc.DatabaseEngine.query;
import static uk.ac.manchester.spinnaker.alloc.DatabaseEngine.transaction;
import static uk.ac.manchester.spinnaker.alloc.DatabaseEngine.update;
import static uk.ac.manchester.spinnaker.alloc.allocator.JobState.DESTROYED;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.annotation.JsonIgnore;

import uk.ac.manchester.spinnaker.alloc.DatabaseEngine;
import uk.ac.manchester.spinnaker.alloc.DatabaseEngine.Query;
import uk.ac.manchester.spinnaker.alloc.DatabaseEngine.Update;
import uk.ac.manchester.spinnaker.alloc.allocator.Epochs.JobsEpoch;
import uk.ac.manchester.spinnaker.alloc.allocator.Epochs.MachinesEpoch;
import uk.ac.manchester.spinnaker.machine.ChipLocation;
import uk.ac.manchester.spinnaker.spalloc.messages.BoardCoordinates;
import uk.ac.manchester.spinnaker.spalloc.messages.BoardPhysicalCoordinates;
import uk.ac.manchester.spinnaker.storage.GeneratesID;
import uk.ac.manchester.spinnaker.storage.Parameter;
import uk.ac.manchester.spinnaker.storage.ResultColumn;

@Component
public class Spalloc implements SpallocInterface {
	@ResultColumn("machine_id")
	@ResultColumn("machine_name")
	@ResultColumn("width")
	@ResultColumn("height")
	private static final String GET_ALL_MACHINES =
			"SELECT machine_id, machine_name, width, height FROM machines";

	@Parameter("machine_name")
	@ResultColumn("machine_id")
	@ResultColumn("machine_name")
	@ResultColumn("width")
	@ResultColumn("height")
	private static final String GET_NAMED_MACHINE =
			"SELECT machine_id, machine_name, width, height FROM machines "
					+ "WHERE machine_name = ? LIMIT 1";

	@Parameter("limit")
	@Parameter("offset")
	@ResultColumn("job_id")
	@ResultColumn("machine_id")
	@ResultColumn("job_state")
	@ResultColumn("keepalive_timestamp")
	private static final String GET_JOB_IDS =
			"SELECT job_id, machine_id, job_state, keepalive_timestamp "
					+ "FROM jobs ORDER BY job_id DESC LIMIT ? OFFSET ?";

	@Parameter("job_state:DESTROYED")
	@Parameter("limit")
	@Parameter("offset")
	@ResultColumn("job_id")
	@ResultColumn("machine_id")
	@ResultColumn("job_state")
	@ResultColumn("keepalive_timestamp")
	private static final String GET_LIVE_JOB_IDS =
			"SELECT job_id, machine_id, job_state, keepalive_timestamp "
					+ "FROM jobs WHERE job_state != ? "
					+ "ORDER BY job_id DESC LIMIT ? OFFSET ?";

	@Parameter("job_id")
	@ResultColumn("machine_id")
	@ResultColumn("width")
	@ResultColumn("height")
	@ResultColumn("root_id")
	@ResultColumn("job_state")
	@ResultColumn("keepalive_timestamp")
	@ResultColumn("keepalive_host")
	private static final String GET_JOB =
			"SELECT machine_id, width, height, root_id, job_state, "
					+ "keepalive_timestamp, keepalive_host FROM jobs "
					+ "WHERE job_id = ? LIMIT 1";

	@Parameter("machine_id")
	@Parameter("owner")
	@Parameter("keepalive_interval")
	@Parameter("keepalive_timestamp")
	@Parameter("create_timestamp")
	@GeneratesID
	private static final String INSERT_JOB = "INSERT INTO jobs("
			+ "machine_id, owner, keepalive_interval, keepalive_timestamp, "
			+ "create_timestamp) " //
			+ "VALUES (?, ?, ?, ?, ?)";

	@Parameter("job_id")
	@Parameter("num_boards")
	@Parameter("max_dead_boards")
	@GeneratesID
	private static final String INSERT_REQ_N_BOARDS =
			"INSERT INTO job_request(job_id, num_boards, max_dead_boards) "
					+ "VALUES (?, ?, ?)";

	@Parameter("job_id")
	@Parameter("width")
	@Parameter("height")
	@Parameter("max_dead_boards")
	@GeneratesID
	private static final String INSERT_REQ_SIZE =
			"INSERT INTO job_request(job_id, width, height, max_dead_boards) "
					+ "VALUES (?, ?, ?, ?)";

	@Parameter("job_id")
	@Parameter("cabinet")
	@Parameter("frame")
	@Parameter("board")
	@GeneratesID
	private static final String INSERT_REQ_LOCATION =
			"INSERT INTO job_request(job_id, cabinet, frame, board) "
					+ "VALUES (?, ?, ?, ?)";

	@Parameter("machine_id")
	@Parameter("chip_x")
	@Parameter("chip_y")
	@ResultColumn("board_id")
	@ResultColumn("address")
	@ResultColumn("bmp_id")
	@ResultColumn("board_nnum")
	@ResultColumn("x")
	@ResultColumn("y")
	@ResultColumn("job_id")
	@ResultColumn("machine_name")
	@ResultColumn("cabinet")
	@ResultColumn("frame")
	@ResultColumn("board_num")
	@ResultColumn("chip_x")
	@ResultColumn("chip_y")
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

	@Parameter("machine_id")
	@Parameter("cabinet")
	@Parameter("frame")
	@Parameter("board")
	@ResultColumn("board_id")
	@ResultColumn("address")
	@ResultColumn("bmp_id")
	@ResultColumn("board_nnum")
	@ResultColumn("x")
	@ResultColumn("y")
	@ResultColumn("job_id")
	@ResultColumn("machine_name")
	@ResultColumn("cabinet")
	@ResultColumn("frame")
	@ResultColumn("board_num")
	@ResultColumn("chip_x")
	@ResultColumn("chip_y")
	private static final String FIND_BOARD_BY_CFB =
			"SELECT boards.board_id, address, bmp_id, board_num, x, y, "
					+ "job_id, m.machine_name, bmp.cabinet, bmp.frame, "
					+ "boards.board_num, root_x AS chip_x, root_y AS chip_y "
					+ "FROM boards JOIN bmp ON boards.bmp_id = bmp.bmp_id "
					+ "JOIN machines AS m ON boards.machine_id = m.machine_id "
					+ "WHERE boards.machine_id = ? AND bmp.cabinet = ? "
					+ "AND bmp.frame = ? AND boards.board_num = ? LIMIT 1";

	@Parameter("machine_id")
	@Parameter("x")
	@Parameter("y")
	@Parameter("z")
	@ResultColumn("board_id")
	@ResultColumn("address")
	@ResultColumn("bmp_id")
	@ResultColumn("board_nnum")
	@ResultColumn("x")
	@ResultColumn("y")
	@ResultColumn("job_id")
	@ResultColumn("machine_name")
	@ResultColumn("cabinet")
	@ResultColumn("frame")
	@ResultColumn("board_num")
	@ResultColumn("chip_x")
	@ResultColumn("chip_y")
	private static final String FIND_BOARD_BY_XYZ =
			"SELECT boards.board_id, address, bmp_id, board_num, x, y, "
					+ "job_id, m.machine_name, bmp.cabinet, bmp.frame, "
					+ "boards.board_num, root_x AS chip_x, root_y AS chip_y "
					+ "FROM boards JOIN bmp ON boards.bmp_id = bmp.bmp_id "
					+ "JOIN machines AS m ON boards.machine_id = m.machine_id "
					+ "WHERE boards.machine_id = ? AND boards.x = ? "
					+ "AND boards.y = ? AND 0 = ? LIMIT 1";

	@Parameter("machine_id")
	@ResultColumn("tag")
	private static final String GET_TAGS =
			"SELECT tag FROM tags WHERE machine_id = ?";

	@Parameter("keepalive_timestamp")
	@Parameter("keepalive_host")
	@Parameter("job_id")
	@Parameter("job_state:DESTROYED")
	private static final String UPDATE_KEEPALIVE =
			"UPDATE jobs SET keepalive_timestamp = ?, keepalive_host = ? "
					+ "WHERE job_id = ? AND job_state != ?";

	@Parameter("job_state:DESTROYED")
	@Parameter("death_reason")
	@Parameter("death_timestamp")
	@Parameter("job_id")
	@Parameter("job_state:DESTROYED")
	private static final String DESTROY_JOB = "UPDATE jobs SET "
			+ "job_state = ?, death_reason = ?, death_timestamp = ? "
			+ "WHERE job_id = ? AND job_state != ?";

	private static final int N_COORDS_COUNT = 1;

	private static final int N_COORDS_RECTANGLE = 2;

	private static final int N_COORDS_LOCATION = 3;

	@Autowired
	private DatabaseEngine db;

	@Autowired
	private Epochs epochs;

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
				MachineImpl m = new MachineImpl(conn, rs, me);
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

	private MachineImpl getMachine(String name, Connection conn)
			throws SQLException {
		MachinesEpoch me = epochs.getMachineEpoch();
		MachineImpl m = null;
		try (Query namedMachine = query(conn, GET_NAMED_MACHINE)) {
			for (ResultSet rs : namedMachine.call(name)) {
				m = new MachineImpl(conn, rs, me);
				break;
			}
		}
		return m;
	}

	@Override
	public Jobs getJobs(boolean deleted, int limit, int start)
			throws SQLException {
		JobsEpoch je = epochs.getJobsEpoch();
		try (Connection conn = db.getConnection()) {
			JobCollection jc = new JobCollection(je);
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

	private JobImpl getJob(int id, Connection conn) throws SQLException {
		JobsEpoch epoch = epochs.getJobsEpoch();
		JobImpl j = null;
		try (Query s = query(conn, GET_JOB)) {
			for (ResultSet rs : s.call(id)) {
				j = new JobImpl(epoch, rs);
			}
		}
		return j;
	}

	@Override
	public Job createJob(String owner, List<Integer> dimensions,
			String machineName, List<String> tags, Duration keepaliveInterval,
			Integer maxDeadBoards) throws SQLException {
		try (Connection conn = db.getConnection()) {
			return transaction(conn, () -> {
				MachineImpl m = selectMachine(conn, machineName, tags);
				if (m == null) {
					// Cannot find machine!
					return null;
				}

				int id = insertJob(conn, m, owner, keepaliveInterval);
				if (id < 0) {
					// Insert failed
					return null;
				}

				// Ask the allocator engine to do the allocation
				insertRequest(conn, id, dimensions, maxDeadBoards);
				return getJob(id, conn);
			});
		}
	}

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

	private int insertJob(Connection conn, MachineImpl m, String owner,
			Duration keepaliveInterval) throws SQLException {
		// TODO add in additional info
		Date now = new Date();
		long interval = keepaliveInterval.getSeconds();
		int pk = -1;
		try (Update makeJob = update(conn, INSERT_JOB)) {
			for (int key : makeJob.keys(m.id, owner, interval, now, now)) {
				pk = key;
				break;
			}
		}
		return pk;
	}

	private MachineImpl selectMachine(Connection conn, String machineName,
			List<String> tags) throws SQLException {
		if (machineName != null) {
			return getMachine(machineName, conn);
		} else if (!tags.isEmpty()) {
			for (Machine m : getMachines(conn).values()) {
				MachineImpl mi = (MachineImpl) m;
				if (mi.tags.containsAll(tags)) {
					/*
					 * Originally, spalloc checked if allocation was possible;
					 * we just assume that it is because there really isn't ever
					 * going to be that many different machines on one service.
					 */
					return mi;
				}
			}
		}
		return null;
	}

	void jobAccess(JobImpl job, Date now, String keepaliveAddress)
			throws SQLException {
		try (Connection conn = db.getConnection();
				Update keepAlive = update(conn, UPDATE_KEEPALIVE)) {
			keepAlive.call(now, keepaliveAddress, job.id, DESTROYED);
		}
	}

	void jobDestroy(JobImpl job, Date now, String reason) throws SQLException {
		try (Connection conn = db.getConnection();
				Update destroyJob = update(conn, DESTROY_JOB)) {
			destroyJob.call(DESTROYED, reason, now, job.id, DESTROYED);
		}
	}

	private class MachineImpl implements Machine {
		private final int id;

		private final String name;

		private final List<String> tags = new ArrayList<>();

		private final int width;

		private final int height;

		@JsonIgnore
		private final MachinesEpoch epoch;

		MachineImpl(Connection conn, ResultSet rs, MachinesEpoch epoch)
				throws SQLException {
			this.epoch = epoch;
			id = rs.getInt("machine_id");
			name = rs.getString("machine_name");
			width = rs.getInt("width");
			height = rs.getInt("height");
			try (Query getTags = query(conn, GET_TAGS)) {
				for (ResultSet tagSet : getTags.call(id)) {
					tags.add(tagSet.getString("tag"));
				}
			}
		}

		@Override
		public void waitForChange(long timeout) {
			try {
				epoch.waitForChange(timeout);
			} catch (InterruptedException ignored) {
				Thread.currentThread().interrupt();
			}
		}

		@Override
		public BoardLocation getBoardByChip(int x, int y, JobsEpoch je)
				throws SQLException {
			BoardLocation loc = null; // Default to null for query failed
			try (Connection conn = db.getConnection();
					Query q = query(conn, FIND_BOARD_BY_CHIP)) {
				for (ResultSet rs : q.call(id, x, y)) {
					loc = new BoardLocationImpl(rs, je);
				}
			}
			return loc;
		}

		@Override
		public BoardLocation getBoardByPhysicalCoords(int cabinet, int frame,
				int board, JobsEpoch je) throws SQLException {
			BoardLocation loc = null; // Default to null for query failed
			try (Connection conn = db.getConnection();
					Query q = query(conn, FIND_BOARD_BY_CFB)) {
				for (ResultSet rs : q.call(id, cabinet, frame, board)) {
					loc = new BoardLocationImpl(rs, je);
				}
			}
			return loc;
		}

		@Override
		public BoardLocation getBoardByLogicalCoords(int x, int y, int z,
				JobsEpoch je) throws SQLException {
			BoardLocation loc = null; // Default to null for query failed
			try (Connection conn = db.getConnection();
					Query q = query(conn, FIND_BOARD_BY_XYZ)) {
				for (ResultSet rs : q.call(id, x, y, z)) {
					loc = new BoardLocationImpl(rs, je);
				}
			}
			return loc;
		}

		@Override
		public String getRootBoardBMPAddress() throws SQLException {
			// FIXME Auto-generated method stub
			return null;
		}

		@Override
		public List<Integer> getBoardNumbers() throws SQLException {
			// FIXME Auto-generated method stub
			return null;
		}

		@Override
		public int getId() {
			return id;
		}

		@Override
		public String getName() {
			return name;
		}

		@Override
		public List<String> getTags() {
			return unmodifiableList(tags);
		}

		@Override
		public int getWidth() {
			return width;
		}

		@Override
		public int getHeight() {
			return height;
		}
	}

	private class JobCollection implements Jobs {
		@JsonIgnore
		private JobsEpoch epoch;

		private List<Job> jobs = new ArrayList<>();

		JobCollection(JobsEpoch je) {
			epoch = je;
		}

		@Override
		public void waitForChange(long timeout) {
			try {
				epoch.waitForChange(timeout);
			} catch (InterruptedException ignored) {
				Thread.currentThread().interrupt();
			}
		}

		@Override
		public List<Integer> ids(int start, int limit) {
			// TODO Auto-generated method stub
			return null;
		}

		void addJob(int jobId, int jobState, long keepalive) {
			jobs.add(new JobImpl(epoch, jobId, jobState, keepalive));
		}
	}

	private final class JobImpl implements Job {
		@JsonIgnore
		private JobsEpoch epoch;

		private final int id;

		private Integer width;

		private Integer height;

		private JobState state;

		/** If not {@code null}, the ID of the root board of the job. */
		private Integer root;

		private String owner;

		private String keepaliveHost;

		private Instant startTime;

		private Instant keepaliveTime;

		JobImpl(JobsEpoch epoch, int id) {
			this.epoch = epoch;
			this.id = id;
		}

		JobImpl(JobsEpoch epoch, int jobId, int jobState,
				long keepalive) {
			this(epoch, jobId);
			state = JobState.values()[jobState];
			keepaliveTime = Instant.ofEpochSecond(keepalive);
		}

		JobImpl(JobsEpoch epoch, ResultSet row) throws SQLException {
			this(epoch, row.getInt("machine_id")); // FIXME wtf? job_id surely?
			width = (Integer) row.getObject("width");
			height = (Integer) row.getObject("height");
			root = (Integer) row.getObject("root_id");
			state = JobState.values()[row.getInt("job_state")];
			keepaliveHost = row.getString("keepalive_host");
			// TODO fill this out
		}

		@Override
		public void access(String keepaliveAddress) throws SQLException {
			jobAccess(this, new Date(), keepaliveAddress);
		}

		@Override
		public void destroy(String reason) throws SQLException {
			jobDestroy(this, new Date(), reason);
		}

		@Override
		public void waitForChange(long timeout) {
			try {
				epoch.waitForChange(timeout);
			} catch (InterruptedException ignored) {
				Thread.currentThread().interrupt();
			}
		}

		@Override
		public int getId() {
			return id;
		}

		@Override
		public JobState getState() {
			return state;
		}

		@Override
		public Instant getStartTime() {
			// TODO Auto-generated method stub
			return startTime;
		}

		@Override
		public String getReason() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public String getKeepaliveHost() {
			return keepaliveHost;
		}

		@Override
		public SubMachine getMachine() {
			if (root == null) {
				return null;
			}
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public BoardLocation whereIs(int x, int y) {
			if (root == null) {
				return null;
			}
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public ChipLocation getRootChip() {
			if (root == null) {
				return null;
			}
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public String getOwner() {
			return owner;
		}

		@Override
		public Integer getWidth() {
			return width;
		}

		@Override
		public Integer getHeight() {
			return height;
		}
	}

	private final class BoardLocationImpl implements BoardLocation {
		private Spalloc.Job job;

		private final String machine;

		private final ChipLocation chip;

		private final BoardCoordinates logical;

		private final BoardPhysicalCoordinates physical;

		private BoardLocationImpl(ResultSet row, JobsEpoch epoch)
				throws SQLException {
			machine = row.getString("machine_name");
			logical = new BoardCoordinates(row.getInt("x"), row.getInt("y"), 0);
			physical = new BoardPhysicalCoordinates(0, 0, 0); // FIXME
			chip = new ChipLocation(row.getInt("chip_x"), row.getInt("chip_y"));

			Integer jobId = (Integer) row.getObject("job_id");
			if (jobId != null) {
				job = new JobImpl(epoch, jobId);
				// FIXME also pass other basic values
			}
		}

		@Override
		public ChipLocation getBoardChip() {
			return null;
		}

		@Override
		public ChipLocation getChipRelativeTo(ChipLocation rootChip) {
			return new ChipLocation(chip.getX() - rootChip.getX(),
					chip.getY() - rootChip.getY());
		}

		@Override
		public String getMachine() {
			return machine;
		}

		@Override
		public BoardCoordinates getLogical() {
			return logical;
		}

		@Override
		public BoardPhysicalCoordinates getPhysical() {
			return physical;
		}

		@Override
		public ChipLocation getChip() {
			return chip;
		}

		@Override
		public Job getJob() {
			return job;
		}
	}
}
