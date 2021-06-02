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
import static java.util.stream.Collectors.toList;
import static uk.ac.manchester.spinnaker.alloc.DatabaseEngine.query;
import static uk.ac.manchester.spinnaker.alloc.DatabaseEngine.transaction;
import static uk.ac.manchester.spinnaker.alloc.DatabaseEngine.update;
import static uk.ac.manchester.spinnaker.alloc.allocator.JobState.READY;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.annotation.JsonIgnore;

import uk.ac.manchester.spinnaker.alloc.DatabaseEngine;
import uk.ac.manchester.spinnaker.alloc.DatabaseEngine.Query;
import uk.ac.manchester.spinnaker.alloc.DatabaseEngine.Row;
import uk.ac.manchester.spinnaker.alloc.DatabaseEngine.Update;
import uk.ac.manchester.spinnaker.alloc.allocator.Epochs.JobsEpoch;
import uk.ac.manchester.spinnaker.alloc.allocator.Epochs.MachinesEpoch;
import uk.ac.manchester.spinnaker.machine.ChipLocation;
import uk.ac.manchester.spinnaker.spalloc.messages.BoardCoordinates;
import uk.ac.manchester.spinnaker.spalloc.messages.BoardPhysicalCoordinates;

@Component
public class Spalloc extends SQLQueries implements SpallocAPI {
	private static final int N_COORDS_COUNT = 1;

	private static final int N_COORDS_RECTANGLE = 2;

	private static final int N_COORDS_LOCATION = 3;

	@Autowired
	private DatabaseEngine db;

	@Autowired
	private PowerController powerController;

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
			for (Row rs : listMachines.call()) {
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

	private MachineImpl getMachine(int id, Connection conn)
			throws SQLException {
		MachinesEpoch me = epochs.getMachineEpoch();
		MachineImpl m = null;
		try (Query idMachine = query(conn, GET_MACHINE_BY_ID)) {
			for (Row rs : idMachine.call(id)) {
				m = new MachineImpl(conn, rs, me);
				break;
			}
		}
		return m;
	}

	private MachineImpl getMachine(String name, Connection conn)
			throws SQLException {
		MachinesEpoch me = epochs.getMachineEpoch();
		MachineImpl m = null;
		try (Query namedMachine = query(conn, GET_NAMED_MACHINE)) {
			for (Row rs : namedMachine.call(name)) {
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
					for (Row row : jobs.call(limit, start)) {
						jc.addJob(row);
					}
				}
			} else {
				try (Query jobs = query(conn, GET_LIVE_JOB_IDS)) {
					for (Row row : jobs.call(limit, start)) {
						jc.addJob(row);
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
			for (Row rs : s.call(id)) {
				j = new JobImpl(epoch, conn, rs);
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
			// Request by specific location
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
		int pk = -1;
		try (Update makeJob = update(conn, INSERT_JOB)) {
			for (int key : makeJob.keys(m.id, owner, keepaliveInterval)) {
				pk = key;
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

	private class MachineImpl implements Machine {
		private final int id;

		private final String name;

		private final List<String> tags = new ArrayList<>();

		private final int width;

		private final int height;

		@JsonIgnore
		private final MachinesEpoch epoch;

		MachineImpl(Connection conn, Row rs, MachinesEpoch epoch)
				throws SQLException {
			this.epoch = epoch;
			id = rs.getInt("machine_id");
			name = rs.getString("machine_name");
			width = rs.getInt("width");
			height = rs.getInt("height");
			try (Query getTags = query(conn, GET_TAGS)) {
				for (Row tagSet : getTags.call(id)) {
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
					Query findBoard = query(conn, FIND_BOARD_BY_CHIP)) {
				for (Row rs : findBoard.call(id, x, y)) {
					loc = new BoardLocationImpl(rs, je, id);
				}
			}
			return loc;
		}

		@Override
		public BoardLocation getBoardByPhysicalCoords(int cabinet, int frame,
				int board, JobsEpoch je) throws SQLException {
			BoardLocation loc = null; // Default to null for query failed
			try (Connection conn = db.getConnection();
					Query findBoard = query(conn, FIND_BOARD_BY_CFB)) {
				for (Row rs : findBoard.call(id, cabinet, frame, board)) {
					loc = new BoardLocationImpl(rs, je, id);
				}
			}
			return loc;
		}

		@Override
		public BoardLocation getBoardByLogicalCoords(int x, int y, int z,
				JobsEpoch je) throws SQLException {
			BoardLocation loc = null; // Default to null for query failed
			try (Connection conn = db.getConnection();
					Query findBoard = query(conn, FIND_BOARD_BY_XYZ)) {
				for (Row rs : findBoard.call(id, x, y, z)) {
					loc = new BoardLocationImpl(rs, je, id);
				}
			}
			return loc;
		}

		@Override
		public String getRootBoardBMPAddress() throws SQLException {
			String address = null;
			try (Connection conn = db.getConnection();
					Query rootBMPaddr = query(conn, GET_ROOT_BMP_ADDRESS)) {
				for (Row rs : rootBMPaddr.call(id)) {
					address = rs.getString("address");
				}
			}
			return address;
		}

		@Override
		public List<Integer> getBoardNumbers() throws SQLException {
			List<Integer> boards = new ArrayList<>();
			try (Connection conn = db.getConnection();
					Query boardNumbers = query(conn, GET_BOARD_NUMBERS)) {
				for (Row row : boardNumbers.call(id)) {
					boards.add((Integer) row.getObject("board_num"));
				}
			}
			return boards;
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
		public List<Job> jobs() {
			return unmodifiableList(jobs);
		}

		@Override
		public List<Integer> ids() {
			return jobs.stream().map(Job::getId).collect(toList());
		}

		void addJob(Row row) throws SQLException {
			int jobId = row.getInt("job_id");
			int machineId = row.getInt("machine_id");
			JobState jobState = row.getEnum("job_state", JobState.class);
			Instant keepalive = row.getInstant("keepalive_timestamp");
			jobs.add(new JobImpl(epoch, jobId, machineId, jobState, keepalive));
		}
	}

	private final class JobImpl implements Job {
		@JsonIgnore
		private JobsEpoch epoch;

		private final int id;

		private final int machineId;

		private Integer width;

		private Integer height;

		private JobState state;

		/** If not {@code null}, the ID of the root board of the job. */
		private Integer root;

		private ChipLocation chipRoot;

		private String owner;

		private String keepaliveHost;

		private Instant startTime;

		private Instant keepaliveTime;

		private Instant finishTime;

		private String deathReason;

		JobImpl(JobsEpoch epoch, int id, int machineId) {
			this.epoch = epoch;
			this.id = id;
			this.machineId = machineId;
		}

		JobImpl(JobsEpoch epoch, int jobId, int machineId, JobState jobState,
				Instant keepalive) {
			this(epoch, jobId, machineId);
			state = jobState;
			keepaliveTime = keepalive;
		}

		JobImpl(JobsEpoch epoch, Connection conn, Row row)
				throws SQLException {
			this(epoch, row.getInt("job_id"), row.getInt("machine_id"));
			width = (Integer) row.getObject("width");
			height = (Integer) row.getObject("height");
			root = (Integer) row.getObject("root_id");
			if (root != null) {
				try (Query boardRoot = query(conn, GET_ROOT_OF_BOARD)) {
					for (Row subrow : boardRoot.call(root)) {
						chipRoot = new ChipLocation(subrow.getInt("root_x"),
								subrow.getInt("root_y"));
					}
				}
			}
			state = row.getEnum("job_state", JobState.class);
			keepaliveHost = row.getString("keepalive_host");
			keepaliveTime = row.getInstant("keepalive_timestamp");
			startTime = row.getInstant("create_timestamp");
			finishTime = row.getInstant("death_timestamp");
			deathReason = row.getString("death_reason");
		}

		@Override
		public void access(String keepaliveAddress) throws SQLException {
			try (Connection conn = db.getConnection();
					Update keepAlive = update(conn, UPDATE_KEEPALIVE)) {
				keepAlive.call(keepaliveAddress, id);
			}
		}

		@Override
		public void destroy(String reason) throws SQLException {
			try (Connection conn = db.getConnection();
					Update destroyJob = update(conn, DESTROY_JOB)) {
				destroyJob.call(reason, id);
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
		public int getId() {
			return id;
		}

		@Override
		public JobState getState() {
			return state;
		}

		@Override
		public Instant getStartTime() {
			return startTime;
		}

		@Override
		public Instant getFinishTime() {
			return finishTime;
		}

		@Override
		public String getReason() {
			return deathReason;
		}

		@Override
		public String getKeepaliveHost() {
			return keepaliveHost;
		}

		@Override
		public Instant getKeepaliveTimestamp() {
			return keepaliveTime;
		}

		@Override
		public SubMachine getMachine() throws SQLException {
			if (root == null) {
				return null;
			}
			try (Connection conn = db.getConnection()) {
				return new SubMachineImpl(conn);
			}
		}

		@Override
		public BoardLocation whereIs(int x, int y) throws SQLException {
			BoardLocation loc = null; // Default to null for query failed
			if (root != null) {
				try (Connection conn = db.getConnection();
						Query findBoard = query(conn, findBoardByJobChip)) {
					for (Row row : findBoard.call(id, root, x, y)) {
						loc = new BoardLocationImpl(row, epoch,
								machineId);
					}
				}
			}
			return loc;
		}

		@Override
		public ChipLocation getRootChip() {
			return chipRoot;
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

		private final class SubMachineImpl implements SubMachine {
			/** The machine that this sub-machine is part of. */
			private final Machine machine;

			/** The root X coordinate of this sub-machine. */
			private int rootX;

			/** The root Y coordinate of this sub-machine. */
			private int rootY;

			/** The root Z coordinate of this sub-machine. */
			private int rootZ;

			/** The connection details of this sub-machine. */
			private List<ConnectionInfo> connections;

			/** The board locations of this sub-machine. */
			private List<BoardCoordinates> boards;

			private List<Integer> boardIds;

			private SubMachineImpl(Connection conn) throws SQLException {
				machine = Spalloc.this.getMachine(machineId, conn);
				try (Query getRootXY = query(conn, GET_ROOT_COORDS);
						Query getBoardInfo =
								query(conn, GET_BOARD_CONNECT_INFO)) {
					for (Row row : getRootXY.call(root)) {
						rootX = row.getInt("x");
						rootY = row.getInt("y");
						rootZ = row.getInt("z");
					}
					int capacityEstimate = width * height;
					connections = new ArrayList<>(capacityEstimate);
					boards = new ArrayList<>(capacityEstimate);
					boardIds = new ArrayList<>(capacityEstimate);
					for (Row row : getBoardInfo.call(id)) {
						boardIds.add(row.getInt("board_id"));
						boards.add(new BoardCoordinates(row.getInt("x"),
								row.getInt("y"), row.getInt("z")));
						connections.add(new ConnectionInfo(
								new ChipLocation(
										row.getInt("root_x") - chipRoot.getX(),
										row.getInt("root_y") - chipRoot.getY()),
								row.getString("address")));
					}
				}
			}

			@Override
			public Machine getMachine() {
				return machine;
			}

			@Override
			public int getRootX() {
				return rootX;
			}

			@Override
			public int getRootY() {
				return rootY;
			}

			@Override
			public int getRootZ() {
				return rootZ;
			}

			@Override
			public int getWidth() {
				return width;
			}

			@Override
			public int getHeight() {
				return height;
			}

			@Override
			public List<ConnectionInfo> getConnections() {
				return connections;
			}

			@Override
			public List<BoardCoordinates> getBoards() {
				return boards;
			}

			@Override
			public PowerState getPower() throws SQLException {
				PowerState result = null;
				try (Connection conn = db.getConnection();
						Query power = query(conn, GET_BOARD_POWER)) {
					for (Row row : power.call(id)) {
						if (row.getInt("total_on") < boardIds.size()) {
							result = PowerState.OFF;
						} else {
							result = PowerState.ON;
						}
					}
				}
				return result;
			}

			@Override
			public void setPower(PowerState ps) throws SQLException {
				powerController.setPower(id, ps, READY);
			}
		}
	}

	private final class BoardLocationImpl implements BoardLocation {
		private Spalloc.Job job;

		private final String machine;

		private final ChipLocation chip;

		private final BoardCoordinates logical;

		private final BoardPhysicalCoordinates physical;

		private BoardLocationImpl(Row row, JobsEpoch epoch, int machineId)
				throws SQLException {
			machine = row.getString("machine_name");
			logical = new BoardCoordinates(row.getInt("x"), row.getInt("y"),
					row.getInt("z"));
			physical = new BoardPhysicalCoordinates(row.getInt("cabinet"),
					row.getInt("frame"), row.getInt("board_num"));
			chip = new ChipLocation(row.getInt("chip_x"), row.getInt("chip_y"));

			Integer jobId = (Integer) row.getObject("job_id");
			if (jobId != null) {
				job = new JobImpl(epoch, jobId, machineId);
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
