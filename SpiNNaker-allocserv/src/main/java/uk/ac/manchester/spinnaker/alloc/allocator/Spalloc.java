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
import static org.slf4j.LoggerFactory.getLogger;
import static uk.ac.manchester.spinnaker.alloc.DatabaseEngine.query;
import static uk.ac.manchester.spinnaker.alloc.DatabaseEngine.transaction;
import static uk.ac.manchester.spinnaker.alloc.DatabaseEngine.update;
import static uk.ac.manchester.spinnaker.alloc.SecurityConfig.MAY_SEE_JOB_DETAILS;
import static uk.ac.manchester.spinnaker.alloc.model.JobState.READY;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.ws.rs.WebApplicationException;

import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PostFilter;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.annotation.JsonIgnore;

import uk.ac.manchester.spinnaker.alloc.DatabaseEngine;
import uk.ac.manchester.spinnaker.alloc.DatabaseEngine.Query;
import uk.ac.manchester.spinnaker.alloc.DatabaseEngine.Row;
import uk.ac.manchester.spinnaker.alloc.DatabaseEngine.Update;
import uk.ac.manchester.spinnaker.alloc.SQLProblem;
import uk.ac.manchester.spinnaker.alloc.SQLQueries;
import uk.ac.manchester.spinnaker.alloc.allocator.Epochs.Epoch;
import uk.ac.manchester.spinnaker.alloc.model.BoardCoords;
import uk.ac.manchester.spinnaker.alloc.model.ConnectionInfo;
import uk.ac.manchester.spinnaker.alloc.model.Direction;
import uk.ac.manchester.spinnaker.alloc.model.DownLink;
import uk.ac.manchester.spinnaker.alloc.model.JobDescription;
import uk.ac.manchester.spinnaker.alloc.model.JobListEntryRecord;
import uk.ac.manchester.spinnaker.alloc.model.JobState;
import uk.ac.manchester.spinnaker.alloc.model.MachineDescription;
import uk.ac.manchester.spinnaker.alloc.model.MachineDescription.JobInfo;
import uk.ac.manchester.spinnaker.alloc.model.MachineListEntryRecord;
import uk.ac.manchester.spinnaker.alloc.model.PowerState;
import uk.ac.manchester.spinnaker.machine.ChipLocation;
import uk.ac.manchester.spinnaker.spalloc.messages.BoardCoordinates;
import uk.ac.manchester.spinnaker.spalloc.messages.BoardPhysicalCoordinates;

/**
 * The core implementation of the Spalloc service.
 *
 * @author Donal Fellows
 */
@Service
public class Spalloc extends SQLQueries implements SpallocAPI {
	private static final Logger log = getLogger(Spalloc.class);

	private static final int N_COORDS_COUNT = 1;

	private static final int N_COORDS_RECTANGLE = 2;

	private static final int N_COORDS_LOCATION = 3;

	@Autowired
	private DatabaseEngine db;

	@Autowired
	private PowerController powerController;

	@Autowired
	private Epochs epochs;

	@Autowired
	private QuotaManager quotaManager;

	@Override
	public Map<String, Machine> getMachines() throws SQLException {
		return db.execute(this::getMachines);
	}

	private Map<String, Machine> getMachines(Connection conn)
			throws SQLException {
		Epoch me = epochs.getMachineEpoch();
		Map<String, Machine> map = new HashMap<>();
		try (Query listMachines = query(conn, GET_ALL_MACHINES)) {
			listMachines.call().forEach(row -> {
				MachineImpl m = new MachineImpl(conn, row, me);
				map.put(m.name, m);
			});
		}
		return map;
	}

	@Override
	public List<MachineListEntryRecord> listMachines() throws SQLException {
		return db.execute(this::listMachines);
	}

	private List<MachineListEntryRecord> listMachines(Connection conn)
			throws SQLException {
		try (Query listMachines = query(conn, GET_ALL_MACHINES);
				Query countBoards = query(conn, COUNT_BOARDS);
				Query countBoardsInUse = query(conn, COUNT_BOARDS_IN_USE);
				Query countJobsOnMachine = query(conn, COUNT_JOBS_ON_MACHINE);
				Query getTags = query(conn, GET_TAGS)) {
			// TODO can we merge these queries?
			return rowsAsList(listMachines.call(),
					row -> makeMachineListEntryRecord(countBoards,
							countBoardsInUse, countJobsOnMachine, getTags,
							row));
		}
	}

	private MachineListEntryRecord makeMachineListEntryRecord(Query countBoards,
			Query countBoardsInUse, Query countJobsOnMachine, Query getTags,
			Row row) throws SQLException {
		int id = row.getInt("machine_id");
		MachineListEntryRecord rec = new MachineListEntryRecord();
		rec.setName(row.getString("machine_name"));
		rec.setNumBoards(countBoards.call1(id).get().getInt("c"));
		rec.setNumInUse(countBoardsInUse.call1(id).get().getInt("c"));
		rec.setNumJobs(countJobsOnMachine.call1(id).get().getInt("c"));
		rec.setTags(rowsAsList(getTags.call(id),
				tagRow -> tagRow.getString("tag")));
		return rec;
	}

	private static <T> List<T> rowsAsList(Iterable<Row> rows,
			RowMapper<T> mapper) throws SQLException {
		List<T> result = new ArrayList<>();
		for (Row row : rows) {
			result.add(mapper.mapRow(row));
		}
		return result;
	}

	private interface RowMapper<T> {
		T mapRow(Row row) throws SQLException;
	}

	@Override
	public Optional<Machine> getMachine(String name) throws SQLException {
		return db.execute(conn -> getMachine(name, conn).map(m -> m));
	}

	private Optional<MachineImpl> getMachine(int id, Connection conn)
			throws SQLException {
		Epoch me = epochs.getMachineEpoch();
		try (Query idMachine = query(conn, GET_MACHINE_BY_ID)) {
			return idMachine.call1(id)
					.map(row -> new MachineImpl(conn, row, me));
		}
	}

	private Optional<MachineImpl> getMachine(String name, Connection conn)
			throws SQLException {
		Epoch me = epochs.getMachineEpoch();
		try (Query namedMachine = query(conn, GET_NAMED_MACHINE)) {
			return namedMachine.call1(name)
					.map(row -> new MachineImpl(conn, row, me));
		}
	}

	@Override
	public Optional<MachineDescription> getMachineInfo(String machine,
			String currentUser, boolean isAdmin) throws SQLException {
		return db.execute(conn -> {
			try (Query namedMachine = query(conn, GET_NAMED_MACHINE);
					Query countBoardsInUse = query(conn, COUNT_BOARDS_IN_USE);
					Query getTags = query(conn, GET_TAGS);
					Query getJobs = query(conn, GET_MACHINE_JOBS);
					Query getCoords = query(conn, GET_JOB_BOARD_COORDS);
					Query getLive = query(conn, GET_LIVE_BOARDS);
					Query getDead = query(conn, GET_DEAD_BOARDS)) {
				MachineDescription md = null;
				for (Row row : namedMachine.call(machine)) {
					md = new MachineDescription();
					md.setId(row.getInt("machine_id"));
					md.setName(row.getString("machine_name"));
					md.setWidth(row.getInt("width"));
					md.setHeight(row.getInt("height"));
				}
				if (md == null) {
					return Optional.empty();
				}
				md.setNumInUse(
						countBoardsInUse.call1(md.getId()).get().getInt("c"));
				md.setTags(rowsAsList(getTags.call(md.getId()),
						tagRow -> tagRow.getString("tag")));
				md.setJobs(rowsAsList(getJobs.call(md.getId()), row -> {
					JobInfo ji = new JobInfo();
					int jobId = row.getInt("job_id");
					String owner =
							(currentUser.equals(row.getString("owner_name"))
									|| isAdmin) ? row.getString("owner_name")
											: null;
					ji.setId(jobId);
					ji.setOwner(owner);
					ji.setBoards(rowsAsList(getCoords.call(jobId),
							r -> new BoardCoords(r, owner == null)));
					return ji;
				}));
				md.setLive(rowsAsList(getLive.call(md.getId()),
						r -> new BoardCoords(r, !isAdmin)));
				md.setDead(rowsAsList(getDead.call(md.getId()),
						r -> new BoardCoords(r, !isAdmin)));
				return Optional.of(md);
			}
		});
	}

	@Override
	public Jobs getJobs(boolean deleted, int limit, int start)
			throws SQLException {
		return db.execute(conn -> {
			Epoch je = epochs.getJobsEpoch();
			JobCollection jc = new JobCollection(je);
			if (deleted) {
				try (Query jobs = query(conn, GET_JOB_IDS)) {
					jc.addJobs(jobs.call(limit, start));
				}
			} else {
				try (Query jobs = query(conn, GET_LIVE_JOB_IDS)) {
					jc.addJobs(jobs.call(limit, start));
				}
			}
			return jc;
		});
	}

	@Override
	public List<JobListEntryRecord> listJobs(String currentUser,
			boolean isAdmin) throws SQLException {
		return db.execute(conn -> {
			try (Query listLiveJobs = query(conn, LIST_LIVE_JOBS);
					Query countPoweredBoards =
							query(conn, COUNT_POWERED_BOARDS)) {
				return rowsAsList(listLiveJobs.call(),
						row -> makeJobListEntryRecord(currentUser, isAdmin,
								countPoweredBoards, row));
			}
		});
	}

	private JobListEntryRecord makeJobListEntryRecord(String currentUser,
			boolean isAdmin, Query countPoweredBoards, Row row)
			throws SQLException {
		JobListEntryRecord rec = new JobListEntryRecord();
		int id = row.getInt("job_id");
		rec.setId(id);
		rec.setState(row.getEnum("job_state", JobState.class).name());
		Integer numBoards = (Integer) row.getObject("allocation_size");
		rec.setNumBoards(numBoards);
		rec.setPowered((numBoards != null)
				&& numBoards == countPoweredBoards.call1(id).get().getInt("c"));
		rec.setMachineId(row.getInt("machine_id"));
		rec.setMachineName(row.getString("machine_name"));
		rec.setCreationTimestamp(row.getInstant("create_timestamp"));
		rec.setKeepaliveInterval(row.getDuration("keepalive_interval"));
		String owner = row.getString("owner_name");
		if (isAdmin || owner.equals(currentUser)) {
			rec.setOwner(owner);
			rec.setHost(row.getString("keepalive_host"));
		}
		return rec;
	}

	@Override
	@PostFilter(MAY_SEE_JOB_DETAILS)
	public Optional<Job> getJob(int id) throws SQLException {
		return db.execute(conn -> Optional.ofNullable((Job) getJob(id, conn)));
	}

	private JobImpl getJob(int id, Connection conn) throws SQLException {
		Epoch epoch = epochs.getJobsEpoch();
		try (Query s = query(conn, GET_JOB)) {
			return s.call1(id).map(row -> new JobImpl(epoch, conn, row))
					.orElse(null);
		}
	}

	@Override
	@PostFilter(MAY_SEE_JOB_DETAILS)
	public Optional<JobDescription> getJobInfo(int id) throws SQLException {
		return db.execute(conn -> {
			try (Query s = query(conn, GET_JOB);
					Query chipDimensions = query(conn, GET_JOB_CHIP_DIMENSIONS);
					Query countPoweredBoards =
							query(conn, COUNT_POWERED_BOARDS);
					Query getCoords = query(conn, GET_JOB_BOARD_COORDS)) {
				for (Row job : s.call(id)) {
					return Optional.of(jobDescription(id, job, chipDimensions,
							countPoweredBoards, getCoords));
				}
				return Optional.empty();
			}
		});
	}

	private JobDescription jobDescription(int id, Row job, Query chipDimensions,
			Query countPoweredBoards, Query getCoords) throws SQLException {
		/*
		 * We won't deliver this object to the front end unless they are allowed
		 * to see it in its entirety.
		 */
		JobDescription jd = new JobDescription();
		jd.setId(id);
		jd.setMachine(job.getString("machine_name"));
		jd.setState(job.getEnum("job_state", JobState.class));
		jd.setOwner(job.getString("owner"));
		jd.setOwnerHost(job.getString("keepalive_host"));
		jd.setStartTime(job.getInstant("create_timestamp"));
		jd.setKeepAlive(job.getDuration("keepalive_interval"));
		jd.setRequestBytes(job.getBytes("original_request"));
		chipDimensions.call1(id).ifPresent(cd -> {
			try {
				jd.setWidth(cd.getInt("width"));
				jd.setHeight(cd.getInt("height"));
			} catch (SQLException e) {
				log.error("failed to get elements", e);
			}
		});
		int poweredCount = countPoweredBoards.call1(id).get().getInt("c");
		jd.setBoards(
				rowsAsList(getCoords.call(id), r -> new BoardCoords(r, false)));
		jd.setPowered(jd.getBoards().size() == poweredCount);
		return jd;
	}

	@Override
	public Job createJob(String owner, List<Integer> dimensions,
			String machineName, List<String> tags, Duration keepaliveInterval,
			Integer maxDeadBoards, byte[] req) throws SQLException {
		/*
		 * TODO convert dimensions into something better
		 *
		 * We should allow allocation by anything that supports the
		 * identification of a unique board (as well as by rectangle sizes and
		 * board counts). Maybe also allow allocation that requires a specific
		 * board to be present?
		 */
		return db.execute(conn -> {
			int user = getUser(conn, owner).orElseThrow(
					() -> new SQLException("no such user: " + owner));
			Optional<MachineImpl> mach = selectMachine(conn, machineName, tags);
			if (!mach.isPresent()) {
				// Cannot find machine!
				return null;
			}
			MachineImpl m = mach.get();
			if (!quotaManager.hasQuotaRemaining(m.id, owner)) {
				// No quota left
				return null;
			}
			int id = insertJob(conn, m, user, keepaliveInterval, req);
			if (id < 0) {
				// Insert failed
				return null;
			}

			// Ask the allocator engine to do the allocation
			insertRequest(conn, m, id, dimensions, maxDeadBoards);
			return getJob(id, conn);
		});
	}

	private Optional<Integer> getUser(Connection conn, String userName)
			throws SQLException {
		try (Query getUser = query(conn, GET_USER_ID)) {
			for (Row row : getUser.call(userName)) {
				return Optional.of(row.getInt("user_id"));
			}
			return Optional.empty();
		}
	}

	private static final int TRIAD_SIZE = 3;

	private void insertRequest(Connection conn, MachineImpl machine, int id,
			List<Integer> dims, Integer numDeadBoards) throws SQLException {
		switch (dims.size()) {
		case N_COORDS_COUNT:
			// Request by number of boards
			if (machine.getArea() < dims.get(0)) {
				throw new IllegalArgumentException(
						"request cannot fit on machine");
			}
			try (Update ps = update(conn, INSERT_REQ_N_BOARDS)) {
				ps.call(id, dims.get(0), numDeadBoards);
			}
			break;
		case N_COORDS_RECTANGLE:
			// Request by specific size IN BOARDS
			if (machine.getArea() < dims.get(0)
					* dims.get(1)) {
				throw new IllegalArgumentException(
						"request cannot fit on machine");
			}
			try (Update ps = update(conn, INSERT_REQ_SIZE)) {
				ps.call(id, dims.get(0), dims.get(1), numDeadBoards);
			}
			break;
		case N_COORDS_LOCATION:
			// Request by specific location
			if (machine.width < dims.get(0) || machine.height < dims.get(1)
					|| TRIAD_SIZE <= dims.get(2)) {
				throw new IllegalArgumentException(
						"request cannot fit on machine");
			}
			try (Update ps = update(conn, INSERT_REQ_LOCATION)) {
				ps.call(id, dims.get(0), dims.get(1), dims.get(2));
			}
			break;
		default:
			throw new Error("should be unreachable");
		}
	}

	private int insertJob(Connection conn, MachineImpl m, int owner,
			Duration keepaliveInterval, byte[] req) throws SQLException {
		int pk = -1;
		try (Update makeJob = update(conn, INSERT_JOB)) {
			for (int key : makeJob.keys(m.id, owner, keepaliveInterval, req)) {
				pk = key;
			}
		}
		return pk;
	}

	private Optional<MachineImpl> selectMachine(Connection conn,
			String machineName, List<String> tags) throws SQLException {
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
					return Optional.of(mi);
				}
			}
		}
		return Optional.empty();
	}

	private class MachineImpl implements Machine {
		private final int id;

		private final String name;

		private final List<String> tags;

		private final int width;

		private final int height;

		private transient List<BoardCoords> downBoardsCache;

		private transient List<DownLink> downLinksCache;

		@JsonIgnore
		private final Epoch epoch;

		MachineImpl(Connection conn, Row rs, Epoch epoch) {
			this.epoch = epoch;
			try {
				id = rs.getInt("machine_id");
				name = rs.getString("machine_name");
				width = rs.getInt("width");
				height = rs.getInt("height");
				try (Query getTags = query(conn, GET_TAGS)) {
					tags = rowsAsList(getTags.call(id),
							row -> row.getString("tag"));
				}
			} catch (SQLException e) {
				throw new SQLProblem("creating machine object", e);
			}
		}

		private int getArea() {
			return width * height * TRIAD_SIZE;
		}

		@Override
		public void waitForChange(long timeout) {
			if (epoch == null) {
				return;
			}
			try {
				epoch.waitForChange(timeout);
			} catch (InterruptedException ignored) {
				Thread.currentThread().interrupt();
			}
		}

		@Override
		public Optional<BoardLocation> getBoardByChip(int x, int y)
				throws SQLException {
			try (Connection conn = db.getConnection();
					Query findBoard = query(conn, findBoardByGlobalChip)) {
				return transaction(conn, () -> findBoard.call1(id, x, y)
						.map(row -> new BoardLocationImpl(row, id)));
			}
		}

		@Override
		public Optional<BoardLocation> getBoardByPhysicalCoords(int cabinet,
				int frame, int board) throws SQLException {
			try (Connection conn = db.getConnection();
					Query findBoard = query(conn, findBoardByPhysicalCoords)) {
				return transaction(conn,
						() -> findBoard.call1(id, cabinet, frame, board)
								.map(row -> new BoardLocationImpl(row, id)));
			}
		}

		@Override
		public Optional<BoardLocation> getBoardByLogicalCoords(int x, int y,
				int z) throws SQLException {
			try (Connection conn = db.getConnection();
					Query findBoard = query(conn, findBoardByLogicalCoords)) {
				return transaction(conn, () -> findBoard.call1(id, x, y, z)
						.map(row -> new BoardLocationImpl(row, id)));
			}
		}

		@Override
		public Optional<BoardLocation> getBoardByIPAddress(String address)
				throws SQLException {
			try (Connection conn = db.getConnection();
					Query findBoard = query(conn, findBoardByIPAddress)) {
				return transaction(conn, () -> findBoard.call1(id, address)
						.map(row -> new BoardLocationImpl(row, id)));
			}
		}

		@Override
		public String getRootBoardBMPAddress() throws SQLException {
			try (Connection conn = db.getConnection();
					Query rootBMPaddr = query(conn, GET_ROOT_BMP_ADDRESS)) {
				return transaction(conn, () -> {
					Optional<Row> row = rootBMPaddr.call1(id);
					if (row.isPresent()) {
						return row.get().getString("address");
					}
					return null;
				});
			}
		}

		@Override
		public List<Integer> getBoardNumbers() throws SQLException {
			try (Connection conn = db.getConnection();
					Query boardNumbers = query(conn, GET_BOARD_NUMBERS)) {
				return transaction(conn, () -> {
					return rowsAsList(boardNumbers.call(id),
							row -> (Integer) row.getObject("board_num"));
				});
			}
		}

		@Override
		public List<BoardCoords> getDeadBoards() throws SQLException {
			// Assume that the list doesn't change for the duration of this obj
			synchronized (this) {
				if (downBoardsCache != null) {
					return unmodifiableList(downBoardsCache);
				}
			}
			try (Connection conn = db.getConnection();
					Query boardNumbers = query(conn, GET_DEAD_BOARDS)) {
				List<BoardCoords> downBoards = transaction(conn, () -> {
					return rowsAsList(boardNumbers.call(id),
							row -> new BoardCoords(row.getInt("x"),
									row.getInt("y"), row.getInt("z"),
									row.getInt("cabinet"), row.getInt("frame"),
									row.getInt("boardNum"),
									row.getString("address")));
				});
				synchronized (this) {
					if (downBoardsCache == null) {
						downBoardsCache = downBoards;
					}
				}
				return unmodifiableList(downBoards);
			}
		}

		@Override
		public List<DownLink> getDownLinks() throws SQLException {
			// Assume that the list doesn't change for the duration of this obj
			synchronized (this) {
				if (downLinksCache != null) {
					return unmodifiableList(downLinksCache);
				}
			}
			try (Connection conn = db.getConnection();
					Query boardNumbers = query(conn, getDeadLinks)) {
				List<DownLink> downLinks = transaction(conn, () -> {
					return rowsAsList(boardNumbers.call(id),
							row -> new DownLink(
									new BoardCoords(row.getInt("board_1_x"),
											row.getInt("board_1_y"),
											row.getInt("board_1_z"),
											row.getInt("board_1_c"),
											row.getInt("board_1_f"),
											row.getInt("board_1_b"),
											row.getString("board_1_addr")),
									row.getEnum("dir_1", Direction.class),
									new BoardCoords(row.getInt("board_2_x"),
											row.getInt("board_2_y"),
											row.getInt("board_2_z"),
											row.getInt("board_2_c"),
											row.getInt("board_2_f"),
											row.getInt("board_2_b"),
											row.getString("board_2_addr")),
									row.getEnum("dir_2", Direction.class)));
				});
				synchronized (this) {
					if (downLinksCache == null) {
						downLinksCache = downLinks;
					}
				}
				return unmodifiableList(downLinks);
			}
		}

		@Override
		public List<Integer> getAvailableBoards() throws SQLException {
			try (Connection conn = db.getConnection();
					Query boardNumbers =
							query(conn, GET_AVAILABLE_BOARD_NUMBERS)) {
				return transaction(conn, () -> {
					return rowsAsList(boardNumbers.call(id),
							row -> (Integer) row.getObject("board_num"));
				});
			}
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
		private Epoch epoch;

		private List<Job> jobs = new ArrayList<>();

		JobCollection(Epoch je) {
			epoch = je;
		}

		@Override
		public void waitForChange(long timeout) {
			if (epoch == null) {
				return;
			}
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

		void addJobs(Iterable<Row> rows) throws SQLException {
			jobs = rowsAsList(rows, this::makeJob);
		}

		/**
		 * Makes "partial" jobs; some fields are shrouded, modifications are
		 * disabled.
		 *
		 * @param row
		 *            The row to make the job from.
		 * @throws SQLException
		 *             If DB access fails
		 */
		private Job makeJob(Row row) throws SQLException {
			int jobId = row.getInt("job_id");
			int machineId = row.getInt("machine_id");
			JobState jobState = row.getEnum("job_state", JobState.class);
			Instant keepalive = row.getInstant("keepalive_timestamp");
			return new JobImpl(epoch, jobId, machineId, jobState, keepalive);
		}
	}

	private final class JobImpl implements Job {
		@JsonIgnore
		private Epoch epoch;

		private final int id;

		private final int machineId;

		private Integer width;

		private Integer height;

		private Integer depth;

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

		private byte[] request;

		private boolean partial;

		JobImpl(Epoch epoch, int id, int machineId) {
			this.epoch = epoch;
			this.id = id;
			this.machineId = machineId;
			partial = true;
		}

		JobImpl(Epoch epoch, int jobId, int machineId, JobState jobState,
				Instant keepalive) {
			this(epoch, jobId, machineId);
			state = jobState;
			keepaliveTime = keepalive;
		}

		JobImpl(Epoch epoch, Connection conn, Row row) {
			try {
				this.epoch = epoch;
				this.id = row.getInt("job_id");
				this.machineId = row.getInt("machine_id");
				width = (Integer) row.getObject("width");
				height = (Integer) row.getObject("height");
				depth = (Integer) row.getObject("depth");
				root = (Integer) row.getObject("root_id");
				owner = row.getString("owner");
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
				request = row.getBytes("original_request");
				partial = false;
			} catch (SQLException e) {
				throw new SQLProblem("creating job object", e);
			}
		}

		@Override
		public void access(String keepaliveAddress) throws SQLException {
			if (partial) {
				throw new PartialJobException();
			}
			try (Connection conn = db.getConnection();
					Update keepAlive = update(conn, UPDATE_KEEPALIVE)) {
				transaction(conn, () -> {
					keepAlive.call(keepaliveAddress, id);
				});
			}
		}

		@Override
		public void destroy(String reason) throws SQLException {
			if (partial) {
				throw new PartialJobException();
			}
			powerController.destroyJob(id, reason);
		}

		@Override
		public void waitForChange(long timeout) {
			if (epoch == null) {
				return;
			}
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
		public Optional<Instant> getFinishTime() {
			return Optional.ofNullable(finishTime);
		}

		@Override
		public Optional<String> getReason() {
			return Optional.ofNullable(deathReason);
		}

		@Override
		public Optional<String> getKeepaliveHost() {
			if (partial) {
				return Optional.empty();
			}
			return Optional.ofNullable(keepaliveHost);
		}

		@Override
		public Instant getKeepaliveTimestamp() {
			return keepaliveTime;
		}

		@Override
		public Optional<byte[]> getOriginalRequest() throws SQLException {
			if (partial) {
				return Optional.empty();
			}
			return Optional.ofNullable(request);
		}

		@Override
		public Optional<SubMachine> getMachine() throws SQLException {
			if (root == null) {
				return Optional.empty();
			}
			try (Connection conn = db.getConnection()) {
				return Optional.of(new SubMachineImpl(conn));
			}
		}

		@Override
		public Optional<BoardLocation> whereIs(int x, int y)
				throws SQLException {
			if (root == null) {
				return Optional.empty();
			}
			try (Connection conn = db.getConnection();
					Query findBoard = query(conn, findBoardByJobChip)) {
				return transaction(conn, () -> findBoard.call1(id, root, x, y)
						.map(row -> new BoardLocationImpl(row, machineId)));
			}
		}

		@Override
		public Optional<ChipLocation> getRootChip() {
			return Optional.ofNullable(chipRoot);
		}

		@Override
		public Optional<String> getOwner() {
			if (partial) {
				return Optional.empty();
			}
			return Optional.ofNullable(owner);
		}

		@Override
		public Optional<Integer> getWidth() {
			return Optional.ofNullable(width);
		}

		@Override
		public Optional<Integer> getHeight() {
			return Optional.ofNullable(height);
		}

		@Override
		public Optional<Integer> getDepth() {
			return Optional.ofNullable(depth);
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
				machine = Spalloc.this.getMachine(machineId, conn).get();
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
			public int getDepth() {
				return depth;
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
				try (Connection conn = db.getConnection();
						Query power = query(conn, GET_BOARD_POWER)) {
					return transaction(conn, () -> {
						PowerState result = null;
						for (Row row : power.call(id)) {
							if (row.getInt("total_on") < boardIds.size()) {
								result = PowerState.OFF;
							} else {
								result = PowerState.ON;
							}
						}
						return result;
					});
				}
			}

			@Override
			public void setPower(PowerState ps) throws SQLException {
				if (partial) {
					throw new PartialJobException();
				}
				powerController.setPower(id, ps, READY);
			}
		}
	}

	private final class BoardLocationImpl implements BoardLocation {
		private JobImpl job;

		private final String machine;

		private final ChipLocation chip;

		private final ChipLocation boardChip;

		private final BoardCoordinates logical;

		private final BoardPhysicalCoordinates physical;

		// Transaction is open
		private BoardLocationImpl(Row row, int machineId) {
			try {
				machine = row.getString("machine_name");
				logical = new BoardCoordinates(row.getInt("x"), row.getInt("y"),
						row.getInt("z"));
				physical = new BoardPhysicalCoordinates(row.getInt("cabinet"),
						row.getInt("frame"), row.getInt("board_num"));
				chip = new ChipLocation(row.getInt("chip_x"),
						row.getInt("chip_y"));
				Integer boardX = (Integer) row.getObject("board_chip_x");
				if (boardX != null) {
					boardChip = new ChipLocation(boardX,
							row.getInt("board_chip_y"));
				} else {
					boardChip = chip;
				}

				Integer jobId = (Integer) row.getObject("job_id");
				if (jobId != null) {
					// No epoch; can't wait on this
					job = new JobImpl(null, jobId, machineId);
					job.chipRoot =
							new ChipLocation(row.getInt("job_root_chip_x"),
									row.getInt("job_root_chip_y"));
				}
			} catch (SQLException e) {
				throw new WebApplicationException(
						"failed to construct board location descriptor", e);
			}
		}

		@Override
		public ChipLocation getBoardChip() {
			return boardChip;
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

	static class PartialJobException extends IllegalStateException {
		private static final long serialVersionUID = 2997856394666135483L;

		PartialJobException() {
			super("partial job only");
		}
	}
}
