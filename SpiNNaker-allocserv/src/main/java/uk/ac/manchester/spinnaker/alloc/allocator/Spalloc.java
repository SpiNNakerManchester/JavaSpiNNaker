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
import static java.util.Collections.unmodifiableSet;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.stream.Collectors.toList;
import static org.slf4j.LoggerFactory.getLogger;
import static uk.ac.manchester.spinnaker.alloc.DatabaseEngine.query;
import static uk.ac.manchester.spinnaker.alloc.DatabaseEngine.rowsAsList;
import static uk.ac.manchester.spinnaker.alloc.DatabaseEngine.rowsAsSet;
import static uk.ac.manchester.spinnaker.alloc.DatabaseEngine.transaction;
import static uk.ac.manchester.spinnaker.alloc.DatabaseEngine.update;
import static uk.ac.manchester.spinnaker.alloc.SecurityConfig.MAY_SEE_JOB_DETAILS;
import static uk.ac.manchester.spinnaker.alloc.model.JobState.READY;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.access.prepost.PostFilter;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.annotation.JsonIgnore;

import uk.ac.manchester.spinnaker.alloc.DatabaseEngine;
import uk.ac.manchester.spinnaker.alloc.DatabaseEngine.Connection;
import uk.ac.manchester.spinnaker.alloc.DatabaseEngine.Query;
import uk.ac.manchester.spinnaker.alloc.DatabaseEngine.Row;
import uk.ac.manchester.spinnaker.alloc.DatabaseEngine.Update;
import uk.ac.manchester.spinnaker.alloc.SQLQueries;
import uk.ac.manchester.spinnaker.alloc.SecurityConfig.Permit;
import uk.ac.manchester.spinnaker.alloc.SpallocProperties.AllocatorProperties;
import uk.ac.manchester.spinnaker.alloc.SpallocProperties.PriorityScale;
import uk.ac.manchester.spinnaker.alloc.SpallocProperties.ReportProperties;
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
import uk.ac.manchester.spinnaker.alloc.web.IssueReportRequest;
import uk.ac.manchester.spinnaker.alloc.web.IssueReportRequest.ReportedBoard;
import uk.ac.manchester.spinnaker.machine.ChipLocation;
import uk.ac.manchester.spinnaker.machine.HasChipLocation;
import uk.ac.manchester.spinnaker.spalloc.messages.BoardCoordinates;
import uk.ac.manchester.spinnaker.spalloc.messages.BoardPhysicalCoordinates;

/**
 * The core implementation of the Spalloc service.
 *
 * @author Donal Fellows
 */
@Service
public class Spalloc extends SQLQueries implements SpallocAPI {
	private static final String NO_BOARD_MSG =
			"request does not identify an existing board";

	private static final Logger log = getLogger(Spalloc.class);

	@Autowired
	private DatabaseEngine db;

	@Autowired
	private PowerController powerController;

	@Autowired
	private Epochs epochs;

	@Autowired
	private QuotaManager quotaManager;

	@Autowired(required = false)
	private JavaMailSender emailSender;

	@Autowired
	private AllocatorProperties props;

	@Override
	public Map<String, Machine> getMachines() {
		return db.execute(this::getMachines);
	}

	private Map<String, Machine> getMachines(Connection conn) {
		Epoch me = epochs.getMachineEpoch();
		Map<String, Machine> map = new HashMap<>();
		try (Query listMachines = query(conn, GET_ALL_MACHINES)) {
			listMachines.call()
					.forEach(row -> map.put(row.getString("machine_name"),
							new MachineImpl(conn, row, me)));
		}
		return map;
	}

	@Override
	public List<MachineListEntryRecord> listMachines() {
		return db.execute(this::listMachines);
	}

	private List<MachineListEntryRecord> listMachines(Connection conn) {
		try (Query listMachines = query(conn, GET_ALL_MACHINES);
				Query countMachineThings = query(conn, COUNT_MACHINE_THINGS);
				Query getTags = query(conn, GET_TAGS)) {
			return rowsAsList(listMachines.call(),
					row -> makeMachineListEntryRecord(countMachineThings,
							getTags, row));
		}
	}

	private MachineListEntryRecord makeMachineListEntryRecord(
			Query countMachineThings, Query getTags, Row row) {
		int id = row.getInt("machine_id");
		MachineListEntryRecord rec = new MachineListEntryRecord();
		rec.setName(row.getString("machine_name"));
		Row m = countMachineThings.call1(id).get();
		rec.setNumBoards(m.getInt("board_count"));
		rec.setNumInUse(m.getInt("in_use"));
		rec.setNumJobs(m.getInt("num_jobs"));
		rec.setTags(rowsAsList(getTags.call(id),
				tagRow -> tagRow.getString("tag")));
		return rec;
	}

	@Override
	public Optional<Machine> getMachine(String name) {
		return db.execute(conn -> getMachine(name, conn).map(m -> m));
	}

	private Optional<MachineImpl> getMachine(int id, Connection conn) {
		Epoch me = epochs.getMachineEpoch();
		try (Query idMachine = query(conn, GET_MACHINE_BY_ID)) {
			return idMachine.call1(id)
					.map(row -> new MachineImpl(conn, row, me));
		}
	}

	private Optional<MachineImpl> getMachine(String name, Connection conn) {
		Epoch me = epochs.getMachineEpoch();
		try (Query namedMachine = query(conn, GET_NAMED_MACHINE)) {
			return namedMachine.call1(name)
					.map(row -> new MachineImpl(conn, row, me));
		}
	}

	@Override
	public Optional<MachineDescription> getMachineInfo(String machine,
			Permit permit) {
		return db.execute(conn -> {
			try (Query namedMachine = query(conn, GET_NAMED_MACHINE);
					Query countMachineThings =
							query(conn, COUNT_MACHINE_THINGS);
					Query getTags = query(conn, GET_TAGS);
					Query getJobs = query(conn, GET_MACHINE_JOBS);
					Query getCoords = query(conn, GET_JOB_BOARD_COORDS);
					Query getLive = query(conn, GET_LIVE_BOARDS);
					Query getDead = query(conn, GET_DEAD_BOARDS)) {
				return getBasicMachineInfo(machine, namedMachine).map(md -> {
					md.setNumInUse(countMachineThings.call1(md.getId()).get()
							.getInt("in_use"));
					md.setTags(rowsAsList(getTags.call(md.getId()),
							row -> row.getString("tag")));
					md.setJobs(rowsAsList(getJobs.call(md.getId()),
							row -> getMachineJobInfo(permit, getCoords, row)));
					md.setLive(rowsAsList(getLive.call(md.getId()),
							row -> new BoardCoords(row, !permit.admin)));
					md.setDead(rowsAsList(getDead.call(md.getId()),
							row -> new BoardCoords(row, !permit.admin)));
					return md;
				});
			}
		});
	}

	private Optional<MachineDescription> getBasicMachineInfo(String machine,
			Query namedMachine) {
		return namedMachine.call1(machine).map(row -> {
			MachineDescription md = new MachineDescription();
			md.setId(row.getInt("machine_id"));
			md.setName(row.getString("machine_name"));
			md.setWidth(row.getInt("width"));
			md.setHeight(row.getInt("height"));
			return md;
		});
	}

	private JobInfo getMachineJobInfo(Permit permit, Query getCoords, Row row) {
		int jobId = row.getInt("job_id");
		String owner = permit.unveilFor(row.getString("owner_name"))
				? row.getString("owner_name")
				: null;

		JobInfo ji = new JobInfo();
		ji.setId(jobId);
		ji.setOwner(owner);
		ji.setBoards(rowsAsList(getCoords.call(jobId),
				r -> new BoardCoords(r, isNull(owner))));
		return ji;
	}

	@Override
	public Jobs getJobs(boolean deleted, int limit, int start) {
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
	public List<JobListEntryRecord> listJobs(Permit permit) {
		return db.execute(conn -> {
			try (Query listLiveJobs = query(conn, LIST_LIVE_JOBS);
					Query countPoweredBoards =
							query(conn, COUNT_POWERED_BOARDS)) {
				return rowsAsList(listLiveJobs.call(),
						row -> makeJobListEntryRecord(permit,
								countPoweredBoards, row));
			}
		});
	}

	private JobListEntryRecord makeJobListEntryRecord(Permit permit,
			Query countPoweredBoards, Row row) {
		JobListEntryRecord rec = new JobListEntryRecord();
		int id = row.getInt("job_id");
		rec.setId(id);
		rec.setState(row.getEnum("job_state", JobState.class));
		Integer numBoards = row.getInteger("allocation_size");
		rec.setNumBoards(numBoards);
		rec.setPowered(nonNull(numBoards)
				&& numBoards == countPoweredBoards.call1(id).get().getInt("c"));
		rec.setMachineId(row.getInt("machine_id"));
		rec.setMachineName(row.getString("machine_name"));
		rec.setCreationTimestamp(row.getInstant("create_timestamp"));
		rec.setKeepaliveInterval(row.getDuration("keepalive_interval"));
		String owner = row.getString("owner_name");
		if (permit.unveilFor(owner)) {
			rec.setOwner(owner);
			rec.setHost(row.getString("keepalive_host"));
		}
		return rec;
	}

	@Override
	@PostFilter(MAY_SEE_JOB_DETAILS)
	public Optional<Job> getJob(Permit permit, int id) {
		return db.execute(conn -> getJob(id, conn).map(j -> (Job) j));
	}

	private Optional<JobImpl> getJob(int id, Connection conn) {
		Epoch epoch = epochs.getJobsEpoch();
		try (Query s = query(conn, GET_JOB)) {
			return s.call1(id).map(row -> new JobImpl(epoch, conn, row));
		}
	}

	@Override
	@PostFilter(MAY_SEE_JOB_DETAILS)
	public Optional<JobDescription> getJobInfo(Permit permit, int id) {
		return db.execute(conn -> {
			try (Query s = query(conn, GET_JOB);
					Query chipDimensions = query(conn, GET_JOB_CHIP_DIMENSIONS);
					Query countPoweredBoards =
							query(conn, COUNT_POWERED_BOARDS);
					Query getCoords = query(conn, GET_JOB_BOARD_COORDS)) {
				return s.call1(id).map(job -> jobDescription(id, job,
						chipDimensions, countPoweredBoards, getCoords));
			}
		});
	}

	private JobDescription jobDescription(int id, Row job, Query chipDimensions,
			Query countPoweredBoards, Query getCoords) {
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
			jd.setWidth(cd.getInt("width"));
			jd.setHeight(cd.getInt("height"));
		});
		int poweredCount = countPoweredBoards.call1(id).get().getInt("c");
		jd.setBoards(
				rowsAsList(getCoords.call(id), r -> new BoardCoords(r, false)));
		jd.setPowered(jd.getBoards().size() == poweredCount);
		return jd;
	}

	@Override
	public Job createJob(String owner, CreateDescriptor descriptor,
			String machineName, List<String> tags, Duration keepaliveInterval,
			Integer maxDeadBoards, byte[] req) {
		return db.execute(conn -> {
			int user = getUser(conn, owner).orElseThrow(
					() -> new RuntimeException("no such user: " + owner));
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
			epochs.nextJobsEpoch();

			// Ask the allocator engine to do the allocation
			insertRequest(conn, m, id, descriptor, maxDeadBoards);
			return getJob(id, conn).orElse(null);
		});
	}

	private Optional<Integer> getUser(Connection conn, String userName) {
		try (Query getUser = query(conn, GET_USER_ID)) {
			return getUser.call1(userName).map(row -> row.getInt("user_id"));
		}
	}

	private static final int TRIAD_SIZE = 3;

	private void insertRequest(Connection conn, MachineImpl machine, int id,
			CreateDescriptor descriptor, Integer numDeadBoards) {
		PriorityScale scale = props.getPriorityScale();
		if (descriptor instanceof CreateNumBoards) {
			// Request by number of boards
			CreateNumBoards nb = (CreateNumBoards) descriptor;
			if (machine.getArea() < nb.numBoards) {
				throw new IllegalArgumentException(
						"request cannot fit on machine");
			}
			try (Update ps = update(conn, INSERT_REQ_N_BOARDS)) {
				int priority = (int) (nb.numBoards * scale.getSize());
				ps.call(id, nb.numBoards, numDeadBoards, priority);
			}
		} else if (descriptor instanceof CreateDimensions) {
			// Request by specific size IN BOARDS
			CreateDimensions d = (CreateDimensions) descriptor;
			if (machine.getArea() < d.width * d.height) {
				throw new IllegalArgumentException(
						"request cannot fit on machine");
			}
			try (Update ps = update(conn, INSERT_REQ_SIZE)) {
				int priority =
						(int) (d.width * d.height * scale.getDimensions());
				ps.call(id, d.width, d.height, numDeadBoards, priority);
			}
		} else {
			/*
			 * Request by specific location; resolve to board ID now, as that
			 * doesn't depend on whether the board is currently in use.
			 */
			CreateBoard b = (CreateBoard) descriptor;
			int boardId;
			if (nonNull(b.triad)) {
				try (Query find = query(conn, FIND_BOARD_BY_NAME_AND_XYZ)) {
					boardId = find
							.call1(machine.name, b.triad.x, b.triad.y,
									b.triad.z)
							.map(row -> row.getInt("board_id"))
							.orElseThrow(() -> new IllegalArgumentException(
									NO_BOARD_MSG));
				}
			} else if (nonNull(b.physical)) {
				try (Query find = query(conn, FIND_BOARD_BY_NAME_AND_CFB)) {
					boardId = find
							.call1(machine.name, b.physical.cabinet,
									b.physical.frame, b.physical.board)
							.map(row -> row.getInt("board_id"))
							.orElseThrow(() -> new IllegalArgumentException(
									NO_BOARD_MSG));
				}
			} else {
				try (Query find =
						query(conn, FIND_BOARD_BY_NAME_AND_IP_ADDRESS)) {
					boardId = find.call1(machine.name, b.ip)
							.map(row -> row.getInt("board_id"))
							.orElseThrow(() -> new IllegalArgumentException(
									NO_BOARD_MSG));
				}
			}
			try (Update ps = update(conn, INSERT_REQ_BOARD)) {
				int priority = (int) scale.getSpecificBoard();
				ps.call(id, boardId, priority);
			}
		}
	}

	private int insertJob(Connection conn, MachineImpl m, int owner,
			Duration keepaliveInterval, byte[] req) {
		int pk = -1;
		try (Update makeJob = update(conn, INSERT_JOB)) {
			for (int key : makeJob.keys(m.id, owner, keepaliveInterval, req)) {
				pk = key;
			}
		}
		return pk;
	}

	private Optional<MachineImpl> selectMachine(Connection conn,
			String machineName, List<String> tags) {
		if (nonNull(machineName)) {
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

		private final Set<String> tags;

		private final int width;

		private final int height;

		private transient List<BoardCoords> downBoardsCache;

		private transient List<DownLink> downLinksCache;

		@JsonIgnore
		private final Epoch epoch;

		MachineImpl(Connection conn, Row rs, Epoch epoch) {
			this.epoch = epoch;
			id = rs.getInt("machine_id");
			name = rs.getString("machine_name");
			width = rs.getInt("width");
			height = rs.getInt("height");
			try (Query getTags = query(conn, GET_TAGS)) {
				tags = rowsAsSet(getTags.call(id),
						row -> row.getString("tag"));
			}
		}

		private int getArea() {
			return width * height * TRIAD_SIZE;
		}

		@Override
		public void waitForChange(Duration timeout) {
			if (isNull(epoch)) {
				return;
			}
			try {
				epoch.waitForChange(timeout);
			} catch (InterruptedException ignored) {
				Thread.currentThread().interrupt();
			}
		}

		@Override
		public Optional<BoardLocation> getBoardByChip(int x, int y) {
			try (Connection conn = db.getConnection();
					Query findBoard = query(conn, findBoardByGlobalChip)) {
				return transaction(conn, () -> findBoard.call1(id, x, y)
						.map(row -> new BoardLocationImpl(row, id)));
			}
		}

		@Override
		public Optional<BoardLocation> getBoardByPhysicalCoords(int cabinet,
				int frame, int board) {
			try (Connection conn = db.getConnection();
					Query findBoard = query(conn, findBoardByPhysicalCoords)) {
				return transaction(conn,
						() -> findBoard.call1(id, cabinet, frame, board)
								.map(row -> new BoardLocationImpl(row, id)));
			}
		}

		@Override
		public Optional<BoardLocation> getBoardByLogicalCoords(int x, int y,
				int z) {
			try (Connection conn = db.getConnection();
					Query findBoard = query(conn, findBoardByLogicalCoords)) {
				return transaction(conn, () -> findBoard.call1(id, x, y, z)
						.map(row -> new BoardLocationImpl(row, id)));
			}
		}

		@Override
		public Optional<BoardLocation> getBoardByIPAddress(String address) {
			try (Connection conn = db.getConnection();
					Query findBoard = query(conn, findBoardByIPAddress)) {
				return transaction(conn, () -> findBoard.call1(id, address)
						.map(row -> new BoardLocationImpl(row, id)));
			}
		}

		@Override
		public String getRootBoardBMPAddress() {
			try (Connection conn = db.getConnection();
					Query rootBMPaddr = query(conn, GET_ROOT_BMP_ADDRESS)) {
				return transaction(conn, () -> rootBMPaddr.call1(id)
						.map(row -> row.getString("address")).orElse(null));
			}
		}

		@Override
		public List<Integer> getBoardNumbers() {
			try (Connection conn = db.getConnection();
					Query boardNumbers = query(conn, GET_BOARD_NUMBERS)) {
				return transaction(conn, () -> rowsAsList(boardNumbers.call(id),
						row -> row.getInteger("board_num")));
			}
		}

		@Override
		public List<BoardCoords> getDeadBoards() {
			// Assume that the list doesn't change for the duration of this obj
			synchronized (this) {
				if (nonNull(downBoardsCache)) {
					return unmodifiableList(downBoardsCache);
				}
			}
			try (Connection conn = db.getConnection();
					Query boardNumbers = query(conn, GET_DEAD_BOARDS)) {
				List<BoardCoords> downBoards = transaction(conn,
						() -> rowsAsList(boardNumbers.call(id),
								row -> new BoardCoords(row.getInt("x"),
										row.getInt("y"), row.getInt("z"),
										row.getInt("cabinet"),
										row.getInt("frame"),
										row.getInteger("board_num"),
										row.getString("address"))));
				synchronized (this) {
					if (isNull(downBoardsCache)) {
						downBoardsCache = downBoards;
					}
				}
				return unmodifiableList(downBoards);
			}
		}

		@Override
		public List<DownLink> getDownLinks() {
			// Assume that the list doesn't change for the duration of this obj
			synchronized (this) {
				if (nonNull(downLinksCache)) {
					return unmodifiableList(downLinksCache);
				}
			}
			try (Connection conn = db.getConnection();
					Query boardNumbers = query(conn, getDeadLinks)) {
				List<DownLink> downLinks = transaction(conn,
						() -> rowsAsList(boardNumbers.call(id),
								this::makeDownLinkFromRow));
				synchronized (this) {
					if (isNull(downLinksCache)) {
						downLinksCache = downLinks;
					}
				}
				return unmodifiableList(downLinks);
			}
		}

		private DownLink makeDownLinkFromRow(Row row) {
			BoardCoords board1 = new BoardCoords(row.getInt("board_1_x"),
					row.getInt("board_1_y"), row.getInt("board_1_z"),
					row.getInt("board_1_c"), row.getInt("board_1_f"),
					row.getInteger("board_1_b"), row.getString("board_1_addr"));
			BoardCoords board2 = new BoardCoords(row.getInt("board_2_x"),
					row.getInt("board_2_y"), row.getInt("board_2_z"),
					row.getInt("board_2_c"), row.getInt("board_2_f"),
					row.getInteger("board_2_b"), row.getString("board_2_addr"));
			return new DownLink(board1, row.getEnum("dir_1", Direction.class),
					board2, row.getEnum("dir_2", Direction.class));
		}

		@Override
		public List<Integer> getAvailableBoards() {
			try (Connection conn = db.getConnection();
					Query boardNumbers =
							query(conn, GET_AVAILABLE_BOARD_NUMBERS)) {
				return transaction(conn, () -> rowsAsList(boardNumbers.call(id),
						row -> row.getInteger("board_num")));
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
		public Set<String> getTags() {
			return unmodifiableSet(tags);
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
		public void waitForChange(Duration timeout) {
			if (isNull(epoch)) {
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

		void addJobs(Iterable<Row> rows) {
			jobs = rowsAsList(rows, this::makeJob);
		}

		/**
		 * Makes "partial" jobs; some fields are shrouded, modifications are
		 * disabled.
		 *
		 * @param row
		 *            The row to make the job from.
		 */
		private Job makeJob(Row row) {
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
			this.epoch = epoch;
			this.id = row.getInt("job_id");
			this.machineId = row.getInt("machine_id");
			width = row.getInteger("width");
			height = row.getInteger("height");
			depth = row.getInteger("depth");
			root = row.getInteger("root_id");
			owner = row.getString("owner");
			if (nonNull(root)) {
				try (Query boardRoot = query(conn, GET_ROOT_OF_BOARD)) {
					boardRoot.call1(root).ifPresent(subrow -> {
						chipRoot = new ChipLocation(subrow.getInt("root_x"),
								subrow.getInt("root_y"));
					});
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
		}

		@Override
		public void access(String keepaliveAddress) {
			if (partial) {
				throw new PartialJobException();
			}
			try (Connection conn = db.getConnection();
					Update keepAlive = update(conn, UPDATE_KEEPALIVE)) {
				transaction(conn, () -> keepAlive.call(keepaliveAddress, id));
			}
		}

		@Override
		public void destroy(String reason) {
			if (partial) {
				throw new PartialJobException();
			}
			powerController.destroyJob(id, reason);
		}

		@Override
		public void waitForChange(Duration timeout) {
			if (isNull(epoch)) {
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
		public Optional<byte[]> getOriginalRequest() {
			if (partial) {
				return Optional.empty();
			}
			return Optional.ofNullable(request);
		}

		@Override
		public Optional<SubMachine> getMachine() {
			if (isNull(root)) {
				return Optional.empty();
			}
			try (Connection conn = db.getConnection()) {
				return Optional.of(new SubMachineImpl(conn));
			}
		}

		@Override
		public Optional<BoardLocation> whereIs(int x, int y) {
			if (isNull(root)) {
				return Optional.empty();
			}
			try (Connection conn = db.getConnection();
					Query findBoard = query(conn, findBoardByJobChip)) {
				return transaction(conn, () -> findBoard.call1(id, root, x, y)
						.map(row -> new BoardLocationImpl(row, machineId)));
			}
		}

		private final class BoardReportSQL implements AutoCloseable {
			final Query findBoardByChip;

			final Query findBoardByTriad;

			final Query findBoardPhys;

			final Query findBoardNet;

			final Update insertReport;

			final Query getReported;

			final Update setFunctioning;

			BoardReportSQL(Connection conn) {
				findBoardByChip = query(conn, findBoardByJobChip);
				findBoardByTriad = query(conn, findBoardByLogicalCoords);
				findBoardPhys = query(conn, findBoardByPhysicalCoords);
				findBoardNet = query(conn, findBoardByIPAddress);

				insertReport = update(conn, INSERT_BOARD_REPORT);
				getReported = query(conn, getReportedBoards);
				setFunctioning = update(conn, SET_FUNCTIONING_FIELD);
			}

			@Override
			public void close() {
				findBoardByChip.close();
				findBoardByTriad.close();
				findBoardPhys.close();
				findBoardNet.close();
				insertReport.close();
				getReported.close();
				setFunctioning.close();
			}
		}

		private final class EmailBuilder {
			/**
			 * More efficient than several String.format() calls, and much
			 * clearer than a mess of direct {@link StringBuilder} calls!
			 */
			private final Formatter b = new Formatter(Locale.UK);

			void header(String issue, int numBoards, String who) {
				b.format("Issues \"%s\" with %d boards reported by %s\n\n",
						issue, numBoards, who);
			}

			void chip(ReportedBoard board) {
				b.format("\tBoard for job (%d) chip %s\n", //
						id, board.chip);
			}

			void triad(ReportedBoard board) {
				b.format("\tBoard for job (%d) board (X:%d,Y:%d,Z:%d)\n", //
						id, board.x, board.y, board.z);
			}

			void phys(ReportedBoard board) {
				b.format(
						"\tBoard for job (%d) board "
								+ "[Cabinet:%d,Frame:%d,Board:%d]\n", //
						id, board.cabinet, board.frame, board.board);
			}

			void ip(ReportedBoard board) {
				b.format("\tBoard for job (%d) board (IP: %s)\n", //
						id, board.address);
			}

			void issue(int issueId) {
				b.format("\t\tAction: noted as issue #%d\n", //
						issueId);
			}

			void footer(int numActions) {
				b.format("\nSummary: %d boards taken out of service.\n",
						numActions);
			}

			void serviceActionDone(Row r) {
				b.format(
						"\tAction: board (X:%d,Y:%d,Z:) (IP: %s) "
								+ "taken out of service once not in use "
								+ "(%d problems reported)\n",
						r.getInt("x"), r.getInt("y"), r.getInt("z"),
						r.getString("address"), r.getInt("numReports"));
			}

			@Override
			public String toString() {
				return b.toString();
			}
		}

		@Override
		public String reportIssue(IssueReportRequest report, Permit permit) {
			try (Connection conn = db.getConnection();
					BoardReportSQL q = new BoardReportSQL(conn)) {
				EmailBuilder email = new EmailBuilder();
				email.header(report.issue, report.boards.size(), permit.name);

				String result = transaction(conn, () -> {
					int userId = getUser(conn, permit.name)
							.orElseThrow(() -> new RuntimeException(
									"no such user: " + permit.name));
					for (ReportedBoard board : report.boards) {
						int boardId = getJobBoardForReport(q, board, email);
						addIssueReport(q, boardId, report.issue, userId, email);
					}
					int acted = takeBoardsOutOfService(q, email);
					if (acted > 0) {
						email.footer(acted);
						return String.format("%d boards taken out of service",
								acted);
					}
					return "report noted";
				});

				// NB: email sending is OUTSIDE the transaction
				ReportProperties mail = props.getReportEmail();
				if (nonNull(emailSender) && nonNull(mail.getTo())
						&& mail.isSend()) {
					SimpleMailMessage message = new SimpleMailMessage();
					if (nonNull(mail.getFrom())) {
						message.setFrom(mail.getFrom());
					}
					message.setTo(mail.getTo());
					if (nonNull(mail.getSubject())) {
						message.setSubject(mail.getSubject());
					}
					message.setText(email.toString());
					try {
						if (!mail.getTo().isEmpty()) {
							emailSender.send(message);
						}
					} catch (MailException e) {
						log.warn("problem when sending email", e);
					}
				}
				return result;
			} catch (ReportRollbackExn e) {
				return e.getMessage();
			}
		}

		/**
		 * Convert a board locator (for an issue report) into a board ID.
		 *
		 * @param q
		 *            How to touch the DB
		 * @param board
		 *            What board are we talking about
		 * @param email
		 *            The email we are building.
		 * @return The board ID
		 * @throws ReportRollbackExn
		 *             If the board can't be converted to an ID
		 */
		private int getJobBoardForReport(BoardReportSQL q, ReportedBoard board,
				EmailBuilder email) throws ReportRollbackExn {
			Row r;
			if (nonNull(board.chip)) {
				r = q.findBoardByChip
						.call1(id, root, board.chip.getX(), board.chip.getY())
						.orElseThrow(() -> new ReportRollbackExn(board.chip));
				email.chip(board);
			} else if (nonNull(board.x)) {
				r = q.findBoardByTriad
						.call1(machineId, board.x, board.y, board.z)
						.orElseThrow(() -> new ReportRollbackExn(
								"triad (%s,%s,%s) not in machine", board.x,
								board.y, board.z));
				Integer j = r.getInteger("job_id");
				if (isNull(j) || id != j) {
					throw new ReportRollbackExn(
							"triad (%s,%s,%s) not allocated to job %d", board.x,
							board.y, board.z, id);
				}
				email.triad(board);
			} else if (nonNull(board.cabinet)) {
				r = q.findBoardPhys
						.call1(machineId, board.cabinet, board.frame,
								board.board)
						.orElseThrow(() -> new ReportRollbackExn(
								"physical board [%s,%s,%s] not in machine",
								board.cabinet, board.frame, board.board));
				Integer j = r.getInteger("job_id");
				if (isNull(j) || id != j) {
					throw new ReportRollbackExn(
							"physical board [%s,%s,%s] not allocated to job %d",
							board.cabinet, board.frame, board.board, id);
				}
				email.phys(board);
			} else if (nonNull(board.address)) {
				r = q.findBoardNet.call1(machineId, board.address)
						.orElseThrow(() -> new ReportRollbackExn(
								"board at %s not in machine", board.address));
				Integer j = r.getInteger("job_id");
				if (isNull(j) || id != j) {
					throw new ReportRollbackExn(
							"board at %s not allocated to job %d",
							board.address, id);
				}
				email.ip(board);
			} else {
				throw new UnsupportedOperationException();
			}
			return r.getInt("board_id");
		}

		/**
		 * Record a reported issue with a board.
		 *
		 * @param u
		 *            How to touch the DB
		 * @param boardId
		 *            What board has the issue?
		 * @param issue
		 *            What is the issue?
		 * @param userId
		 *            Who is doing the report?
		 * @param email
		 *            The email we are building.
		 */
		private void addIssueReport(BoardReportSQL u, int boardId, String issue,
				int userId, EmailBuilder email) {
			u.insertReport.key(boardId, id, issue, userId)
					.ifPresent(email::issue);
		}

		/**
		 * Take boards out of service if they've been reported frequently
		 * enough.
		 *
		 * @param u
		 *            How to touch the DB
		 * @param email
		 *            The email we are building.
		 * @return The number of boards taken out of service
		 */
		private int takeBoardsOutOfService(BoardReportSQL u,
				EmailBuilder email) {
			int acted = 0;
			for (Row r : u.getReported.call(props.getReportActionThreshold())) {
				int boardId = r.getInt("board_id");
				if (u.setFunctioning.call(false, boardId) > 0) {
					email.serviceActionDone(r);
					acted++;
				}
			}
			return acted;
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

			private SubMachineImpl(Connection conn) {
				machine = Spalloc.this.getMachine(machineId, conn).get();
				try (Query getRootXY = query(conn, GET_ROOT_COORDS);
						Query getBoardInfo =
								query(conn, GET_BOARD_CONNECT_INFO)) {
					getRootXY.call(root).forEach(row -> {
						rootX = row.getInt("x");
						rootY = row.getInt("y");
						rootZ = row.getInt("z");
					});
					int capacityEstimate = width * height;
					connections = new ArrayList<>(capacityEstimate);
					boards = new ArrayList<>(capacityEstimate);
					boardIds = new ArrayList<>(capacityEstimate);
					getBoardInfo.call(id).forEach(row -> {
						boardIds.add(row.getInt("board_id"));
						boards.add(new BoardCoordinates(row.getInt("x"),
								row.getInt("y"), row.getInt("z")));
						connections.add(new ConnectionInfo(
								new ChipLocation(
										row.getInt("root_x") - chipRoot.getX(),
										row.getInt("root_y") - chipRoot.getY()),
								row.getString("address")));
					});
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
			public PowerState getPower() {
				try (Connection conn = db.getConnection();
						Query power = query(conn, GET_BOARD_POWER)) {
					return transaction(conn, () -> power.call1(id)
							.map(row -> row.getInt("total_on") < boardIds.size()
									? PowerState.OFF
									: PowerState.ON)
							.orElse(null));
				}
			}

			@Override
			public void setPower(PowerState ps) {
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
			machine = row.getString("machine_name");
			logical = new BoardCoordinates(row.getInt("x"), row.getInt("y"),
					row.getInt("z"));
			physical = new BoardPhysicalCoordinates(row.getInt("cabinet"),
					row.getInt("frame"), row.getInteger("board_num"));
			chip = new ChipLocation(row.getInt("chip_x"), row.getInt("chip_y"));
			Integer boardX = row.getInteger("board_chip_x");
			if (nonNull(boardX)) {
				boardChip =
						new ChipLocation(boardX, row.getInt("board_chip_y"));
			} else {
				boardChip = chip;
			}

			Integer jobId = row.getInteger("job_id");
			if (nonNull(jobId)) {
				// No epoch; can't wait on this
				job = new JobImpl(null, jobId, machineId);
				job.chipRoot = new ChipLocation(row.getInt("job_root_chip_x"),
						row.getInt("job_root_chip_y"));
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

class ReportRollbackExn extends RuntimeException {
	private static final long serialVersionUID = 1L;

	ReportRollbackExn(String msg, Object... args) {
		super(String.format(msg, args));
	}

	ReportRollbackExn(HasChipLocation chip) {
		this("chip at (%d,%d) not in job's allocation", chip.getX(),
				chip.getY());
	}
}
