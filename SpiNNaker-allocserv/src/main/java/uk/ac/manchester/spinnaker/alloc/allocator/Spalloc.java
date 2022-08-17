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

import static java.lang.Math.max;
import static java.lang.String.format;
import static java.lang.Thread.currentThread;
import static java.util.Collections.unmodifiableList;
import static java.util.Collections.unmodifiableSet;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.stream.Collectors.toList;
import static org.slf4j.LoggerFactory.getLogger;
import static uk.ac.manchester.spinnaker.alloc.Constants.TRIAD_CHIP_SIZE;
import static uk.ac.manchester.spinnaker.alloc.Constants.TRIAD_DEPTH;
import static uk.ac.manchester.spinnaker.alloc.db.Row.int64;
import static uk.ac.manchester.spinnaker.alloc.db.Row.integer;
import static uk.ac.manchester.spinnaker.alloc.db.Row.string;
import static uk.ac.manchester.spinnaker.alloc.model.JobState.READY;
import static uk.ac.manchester.spinnaker.alloc.model.PowerState.OFF;
import static uk.ac.manchester.spinnaker.alloc.model.PowerState.ON;
import static uk.ac.manchester.spinnaker.alloc.model.Utils.chip;
import static uk.ac.manchester.spinnaker.alloc.security.SecurityConfig.MAY_SEE_JOB_DETAILS;
import static uk.ac.manchester.spinnaker.utils.OptionalUtils.apply;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Formatter;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PostFilter;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.annotation.JsonIgnore;

import uk.ac.manchester.spinnaker.alloc.SpallocProperties.AllocatorProperties;
import uk.ac.manchester.spinnaker.alloc.SpallocProperties.PriorityScale;
import uk.ac.manchester.spinnaker.alloc.admin.ReportMailSender;
import uk.ac.manchester.spinnaker.alloc.allocator.Epochs.Epoch;
import uk.ac.manchester.spinnaker.alloc.db.DatabaseAwareBean;
import uk.ac.manchester.spinnaker.alloc.db.DatabaseEngine.Connection;
import uk.ac.manchester.spinnaker.alloc.db.DatabaseEngine.Query;
import uk.ac.manchester.spinnaker.alloc.db.DatabaseEngine.Update;
import uk.ac.manchester.spinnaker.alloc.db.Row;
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
import uk.ac.manchester.spinnaker.alloc.proxy.ProxyCore;
import uk.ac.manchester.spinnaker.alloc.security.Permit;
import uk.ac.manchester.spinnaker.alloc.web.IssueReportRequest;
import uk.ac.manchester.spinnaker.alloc.web.IssueReportRequest.ReportedBoard;
import uk.ac.manchester.spinnaker.machine.ChipLocation;
import uk.ac.manchester.spinnaker.machine.HasChipLocation;
import uk.ac.manchester.spinnaker.machine.HasCoreLocation;
import uk.ac.manchester.spinnaker.messages.bmp.BMPCoords;
import uk.ac.manchester.spinnaker.spalloc.messages.BoardCoordinates;
import uk.ac.manchester.spinnaker.spalloc.messages.BoardPhysicalCoordinates;
import uk.ac.manchester.spinnaker.utils.MappableIterable;

/**
 * The core implementation of the Spalloc service.
 *
 * @author Donal Fellows
 */
@Service
public class Spalloc extends DatabaseAwareBean implements SpallocAPI {
	private static final String NO_BOARD_MSG =
			"request does not identify an existing board "
					+ "or uses a prohibited coordinate";

	private static final Logger log = getLogger(Spalloc.class);

	@Autowired
	private PowerController powerController;

	@Autowired
	private Epochs epochs;

	@Autowired
	private QuotaManager quotaManager;

	@Autowired
	private ReportMailSender emailSender;

	@Autowired
	private AllocatorProperties props;

	@Autowired
	private ProxyRememberer rememberer;

	private transient Map<String, List<BoardCoords>> downBoardsCache =
			new HashMap<>();

	private transient Map<String, List<DownLink>> downLinksCache =
			new HashMap<>();

	@Override
	public Map<String, Machine> getMachines(boolean allowOutOfService) {
		return execute(c -> getMachines(c, allowOutOfService));
	}

	private Map<String, Machine> getMachines(Connection conn,
			boolean allowOutOfService) {
		Epoch me = epochs.getMachineEpoch();
		try (Query listMachines = conn.query(GET_ALL_MACHINES)) {
			return listMachines.call(allowOutOfService).toMap(
					string("machine_name"),
					row -> new MachineImpl(conn, row, me));
		}
	}

	private final class ListMachinesSQL extends AbstractSQL {
		private Query listMachines = conn.query(GET_ALL_MACHINES);

		private Query countMachineThings = conn.query(COUNT_MACHINE_THINGS);

		private Query getTags = conn.query(GET_TAGS);

		@Override
		public void close() {
			listMachines.close();
			countMachineThings.close();
			getTags.close();
			super.close();
		}

		private MachineListEntryRecord makeMachineListEntryRecord(Row row) {
			int id = row.getInt("machine_id");
			MachineListEntryRecord rec = new MachineListEntryRecord();
			rec.setName(row.getString("machine_name"));
			Row m = countMachineThings.call1(id).get();
			rec.setNumBoards(m.getInt("board_count"));
			rec.setNumInUse(m.getInt("in_use"));
			rec.setNumJobs(m.getInt("num_jobs"));
			rec.setTags(getTags.call(id).map(string("tag")).toList());
			return rec;
		}
	}

	@Override
	public List<MachineListEntryRecord>
			listMachines(boolean allowOutOfService) {
		try (ListMachinesSQL sql = new ListMachinesSQL()) {
			return sql.transaction(false,
					() -> sql.listMachines.call(allowOutOfService)
							.map(sql::makeMachineListEntryRecord).toList());
		}
	}

	@Override
	public Optional<Machine> getMachine(String name,
			boolean allowOutOfService) {
		return executeRead(
				conn -> getMachine(name, allowOutOfService, conn).map(m -> m));
	}

	private Optional<MachineImpl> getMachine(int id, boolean allowOutOfService,
			Connection conn) {
		Epoch me = epochs.getMachineEpoch();
		try (Query idMachine = conn.query(GET_MACHINE_BY_ID)) {
			return idMachine.call1(id, allowOutOfService)
					.map(row -> new MachineImpl(conn, row, me));
		}
	}

	private Optional<MachineImpl> getMachine(String name,
			boolean allowOutOfService, Connection conn) {
		Epoch me = epochs.getMachineEpoch();
		try (Query namedMachine = conn.query(GET_NAMED_MACHINE)) {
			return namedMachine.call1(name, allowOutOfService)
					.map(row -> new MachineImpl(conn, row, me));
		}
	}

	private class DescribeMachineSQL extends AbstractSQL {
		final Query namedMachine = conn.query(GET_NAMED_MACHINE);

		final Query countMachineThings = conn.query(COUNT_MACHINE_THINGS);

		final Query getTags = conn.query(GET_TAGS);

		final Query getJobs = conn.query(GET_MACHINE_JOBS);

		final Query getCoords = conn.query(GET_JOB_BOARD_COORDS);

		final Query getLive = conn.query(GET_LIVE_BOARDS);

		final Query getDead = conn.query(GET_DEAD_BOARDS);

		final Query getQuota = conn.query(GET_USER_QUOTA);

		@Override
		public void close() {
			namedMachine.close();
			countMachineThings.close();
			getTags.close();
			getJobs.close();
			getCoords.close();
			getLive.close();
			getDead.close();
			getQuota.close();
			super.close();
		}
	}

	@Override
	public Optional<MachineDescription> getMachineInfo(String machine,
			boolean allowOutOfService, Permit permit) {
		try (DescribeMachineSQL sql = new DescribeMachineSQL()) {
			return sql.transaction(false, () -> apply(
					sql.namedMachine.call1(machine, allowOutOfService)
							.map(Spalloc::getBasicMachineInfo),
					md -> sql.countMachineThings.call1(md.getId())
							.map(integer("in_use")).ifPresent(md::setNumInUse),
					md -> md.setTags(
							sql.getTags.call(md.getId()).map(string("tag"))),
					md -> md.setJobs(sql.getJobs.call(md.getId())
							.map(row -> getMachineJobInfo(permit, sql.getCoords,
									row))),
					md -> md.setLive(sql.getLive.call(md.getId())
							.map(row -> new BoardCoords(row, !permit.admin))),
					md -> md.setDead(sql.getDead.call(md.getId())
							.map(row -> new BoardCoords(row, !permit.admin))),
					md -> sql.getQuota.call1(permit.name)
							.map(int64("quota_total"))
							.ifPresent(md::setQuota)));
		}
	}

	private static MachineDescription getBasicMachineInfo(Row row) {
		MachineDescription md = new MachineDescription();
		md.setId(row.getInt("machine_id"));
		md.setName(row.getString("machine_name"));
		md.setWidth(row.getInt("width"));
		md.setHeight(row.getInt("height"));
		return md;
	}

	private static JobInfo getMachineJobInfo(Permit permit, Query getCoords,
			Row row) {
		int jobId = row.getInt("job_id");
		boolean mayUnveil = permit.unveilFor(row.getString("owner_name"));
		String owner = mayUnveil ? row.getString("owner_name") : null;

		JobInfo ji = new JobInfo();
		ji.setId(jobId);
		ji.setOwner(owner);
		ji.setBoards(
				getCoords.call(jobId).map(r -> new BoardCoords(r, !mayUnveil)));
		return ji;
	}

	@Override
	public Jobs getJobs(boolean deleted, int limit, int start) {
		return executeRead(conn -> {
			JobCollection jc = new JobCollection(epochs.getJobsEpoch());
			if (deleted) {
				try (Query jobs = conn.query(GET_JOB_IDS)) {
					jc.setJobs(jobs.call(limit, start));
				}
			} else {
				try (Query jobs = conn.query(GET_LIVE_JOB_IDS)) {
					jc.setJobs(jobs.call(limit, start));
				}
			}
			return jc;
		});
	}

	@Override
	public List<JobListEntryRecord> listJobs(Permit permit) {
		return executeRead(conn -> {
			try (Query listLiveJobs = conn.query(LIST_LIVE_JOBS);
					Query countPoweredBoards =
							conn.query(COUNT_POWERED_BOARDS)) {
				return listLiveJobs.call()
						.map(row -> makeJobListEntryRecord(permit,
								countPoweredBoards, row))
						.toList();
			}
		});
	}

	private static JobListEntryRecord makeJobListEntryRecord(Permit permit,
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
		String owner = row.getString("user_name");
		if (permit.unveilFor(owner)) {
			rec.setOwner(owner);
			rec.setHost(row.getString("keepalive_host"));
		}
		return rec;
	}

	@Override
	@PostFilter(MAY_SEE_JOB_DETAILS)
	public Optional<Job> getJob(Permit permit, int id) {
		return executeRead(conn -> getJob(id, conn).map(j -> (Job) j));
	}

	private Optional<JobImpl> getJob(int id, Connection conn) {
		Epoch epoch = epochs.getJobsEpoch();
		try (Query s = conn.query(GET_JOB)) {
			return s.call1(id).map(row -> new JobImpl(epoch, conn, row));
		}
	}

	@Override
	@PostFilter(MAY_SEE_JOB_DETAILS)
	public Optional<JobDescription> getJobInfo(Permit permit, int id) {
		return execute(conn -> {
			try (Query s = conn.query(GET_JOB);
					Query chipDimensions = conn.query(GET_JOB_CHIP_DIMENSIONS);
					Query countPoweredBoards = conn.query(COUNT_POWERED_BOARDS);
					Query getCoords = conn.query(GET_JOB_BOARD_COORDS)) {
				return s.call1(id).map(job -> jobDescription(id, job,
						chipDimensions, countPoweredBoards, getCoords));
			}
		});
	}

	private static JobDescription jobDescription(int id, Row job,
			Query chipDimensions, Query countPoweredBoards, Query getCoords) {
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
		jd.setBoards(getCoords.call(id).map(r -> new BoardCoords(r, false))
				.toList());
		jd.setPowered(jd.getBoards().size() == poweredCount);
		return jd;
	}

	@Override
	public Optional<Job> createJob(String owner, String groupName,
			CreateDescriptor descriptor, String machineName, List<String> tags,
			Duration keepaliveInterval, byte[] req) {
		return execute(conn -> {
			int user = getUser(conn, owner).orElseThrow(
					() -> new RuntimeException("no such user: " + owner));
			int group = selectGroup(conn, owner, groupName);
			if (!quotaManager.mayCreateJob(group)) {
				// No quota left
				return Optional.empty();
			}

			Optional<MachineImpl> m = selectMachine(conn, machineName, tags);
			if (!m.isPresent()) {
				// Cannot find machine!
				return Optional.empty();
			}
			MachineImpl machine = m.get();

			Optional<Integer> id = insertJob(conn, machine, user, group,
					keepaliveInterval, req);
			if (!id.isPresent()) {
				// Insert failed
				return Optional.empty();
			}
			int jobId = id.get();

			epochs.nextJobsEpoch();
			PriorityScale scale = props.getPriorityScale();

			if (machine.getArea() < descriptor.getArea()) {
				throw new IllegalArgumentException(
						"request cannot fit on machine");
			}

			// Ask the allocator engine to do the allocation
			int numBoards = descriptor.visit(new CreateVisitor<Integer>() {
				@Override
				public Integer numBoards(CreateNumBoards nb) {
					try (Update insertReq = conn.update(INSERT_REQ_N_BOARDS)) {
						insertReq.call(jobId, nb.numBoards, nb.maxDead,
								(int) (nb.getArea() * scale.getSize()));
					}
					return nb.numBoards;
				}

				@Override
				public Integer dimensions(CreateDimensions d) {
					try (Update insertReq = conn.update(INSERT_REQ_SIZE)) {
						insertReq.call(jobId, d.width, d.height, d.maxDead,
								(int) (d.getArea() * scale.getDimensions()));
					}
					return max(1, d.getArea() - d.maxDead);
				}

				/*
				 * Request by area rooted at specific location; resolve to board
				 * ID now, as that doesn't depend on whether the board is
				 * currently in use.
				 */
				@Override
				public Integer dimensionsAt(CreateDimensionsAt da) {
					try (Update insertReq =
							conn.update(INSERT_REQ_SIZE_BOARD)) {
						insertReq.call(jobId,
								locateBoard(conn, machine.name, da, true),
								da.width, da.height, da.maxDead,
								(int) scale.getSpecificBoard());
					}
					return max(1, da.getArea() - da.maxDead);
				}

				/*
				 * Request by specific location; resolve to board ID now, as
				 * that doesn't depend on whether the board is currently in
				 * use.
				 */
				@Override
				public Integer board(CreateBoard b) {
					try (Update insertReq = conn.update(INSERT_REQ_BOARD)) {
						/*
						 * This doesn't pass along the max dead boards; only
						 * after one!
						 */
						insertReq.call(jobId,
								locateBoard(conn, machine.name, b, false),
								(int) scale.getSpecificBoard());
					}
					return 1;
				}
			});

			// DB now changed; can report success
			JobLifecycle.log.info(
					"created job {} on {} for {} asking for {} board(s)", jobId,
					machine.name, owner, numBoards);
			return getJob(jobId, conn).map(ji -> (Job) ji);
		});
	}

	/**
	 * Work out what the ID of the group that a job will be accounted against
	 * is.
	 *
	 * @param conn
	 *            DB connection
	 * @param user
	 *            Who is the user?
	 * @param groupName
	 *            What group did they specify? (May be {@code null} to say "pick
	 *            the unique valid possibility for the owner".)
	 * @return The group ID.
	 * @throws GroupsException
	 *             If we can't get a definite group to account against.
	 */
	private int selectGroup(Connection conn, String user, String groupName) {
		if (nonNull(groupName)) {
			try (Query getGroup = conn.query(GET_GROUP_BY_NAME_AND_MEMBER)) {
				return getGroup.call1(user, groupName).map(integer("group_id"))
						.orElseThrow(() -> new NoSuchGroupException(
								"group {} does not exist or {} "
										+ "is not a member of it",
								groupName, user));
			}
		}
		try (Query listGroups = conn.query(GET_GROUPS_AND_QUOTAS_OF_USER)) {
			// No name given; need to guess.
			return listGroups.call(user)
					.filter(r -> isNull(r.getLong("quota"))
							|| r.getLong("quota") > 0L)
					.map(integer("group_id")).first()
					.orElseThrow(() -> new NoSuchGroupException(
							"user {} is not a member of any "
									+ "groups with quota left",
							user));
		}
	}

	private static Optional<Integer> getUser(Connection conn, String userName) {
		try (Query getUser = conn.query(GET_USER_ID)) {
			return getUser.call1(userName).map(integer("user_id"));
		}
	}

	/**
	 * Resolve a machine name and {@link HasBoardCoords} to a board identifier.
	 *
	 * @param conn
	 *            How to get to the DB.
	 * @param machineName
	 *            The name of the machine.
	 * @param b
	 *            The request that is the coordinate holder.
	 * @param requireTriadRoot
	 *            Whether we require the Z coordinate to be zero.
	 * @return The board ID.
	 * @throws IllegalArgumentException
	 *             If the board doesn't exist or it is a board that is not a
	 *             root of a triad when a triad root is required.
	 */
	private Integer locateBoard(Connection conn, String machineName,
			HasBoardCoords b, boolean requireTriadRoot) {
		try (Query findTriad = conn.query(FIND_BOARD_BY_NAME_AND_XYZ);
				Query findPhysical = conn.query(FIND_BOARD_BY_NAME_AND_CFB);
				Query findIP = conn.query(FIND_BOARD_BY_NAME_AND_IP_ADDRESS)) {
			if (nonNull(b.triad)) {
				return findTriad
						.call1(machineName, b.triad.x, b.triad.y, b.triad.z)
						.filter(r -> !requireTriadRoot || r.getInt("z") == 0)
						.map(integer("board_id"))
						.orElseThrow(() -> new IllegalArgumentException(
								NO_BOARD_MSG));
			} else if (nonNull(b.physical)) {
				return findPhysical
						.call1(machineName, b.physical.cabinet,
								b.physical.frame, b.physical.board)
						.filter(r -> !requireTriadRoot || r.getInt("z") == 0)
						.map(integer("board_id"))
						.orElseThrow(() -> new IllegalArgumentException(
								NO_BOARD_MSG));
			} else {
				return findIP.call1(machineName, b.ip)
						.filter(r -> !requireTriadRoot || r.getInt("z") == 0)
						.map(integer("board_id"))
						.orElseThrow(() -> new IllegalArgumentException(
								NO_BOARD_MSG));
			}
		}
	}

	private static Optional<Integer> insertJob(Connection conn, MachineImpl m,
			int owner, int group, Duration keepaliveInterval, byte[] req) {
		try (Update makeJob = conn.update(INSERT_JOB)) {
			return makeJob.key(m.id, owner, group, keepaliveInterval, req);
		}
	}

	private Optional<MachineImpl> selectMachine(Connection conn,
			String machineName, List<String> tags) {
		if (nonNull(machineName)) {
			return getMachine(machineName, false, conn);
		} else if (!tags.isEmpty()) {
			for (Machine m : getMachines(conn, false).values()) {
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

	@Override
	public void purgeDownCache() {
		synchronized (this) {
			downBoardsCache.clear();
			downLinksCache.clear();
		}
	}

	private static String mergeDescription(HasChipLocation coreLocation,
			String description) {
		if (isNull(description)) {
			description = "<null>";
		}
		if (coreLocation instanceof HasCoreLocation) {
			HasCoreLocation loc = (HasCoreLocation) coreLocation;
			description += format(" (at core %d of chip %s)", loc.getP(),
					loc.asChipLocation());
		} else if (nonNull(coreLocation)) {
			description +=
					format(" (at chip %s)", coreLocation.asChipLocation());
		}
		return description;
	}

	@Override
	public void reportProblem(String address, HasChipLocation coreLocation,
			String description, Permit permit) {
		try (BoardReportSQL sql = new BoardReportSQL()) {
			String desc = mergeDescription(coreLocation, description);
			Optional<EmailBuilder> email = sql.transaction(() -> {
				Collection<Machine> machines =
						getMachines(sql.getConnection(), true).values();
				for (Machine m : machines) {
					Optional<EmailBuilder> mail =
							sql.findBoardNet.call1(m.getId(), address)
									.flatMap(row -> reportProblem(row, desc,
											permit, sql));
					if (mail.isPresent()) {
						return mail;
					}
				}
				return Optional.empty();
			});
			// Outside the transaction!
			email.ifPresent(emailSender::sendServiceMail);
		} catch (ReportRollbackExn e) {
			log.warn("failed to handle problem report", e);
		}
	}

	private Optional<EmailBuilder> reportProblem(Row row, String description,
			Permit permit, BoardReportSQL sql) {
		EmailBuilder email = new EmailBuilder(row.getInt("job_id"));
		email.header(description, 1, permit.name);
		int userId = getUser(sql.getConnection(), permit.name).orElseThrow(
				() -> new ReportRollbackExn("no such user: " + permit.name));
		sql.insertReport.key(row.getInt("board_id"), row.getInt("job_id"),
				description, userId).ifPresent(email::issue);
		return takeBoardsOutOfService(sql, email).map(acted -> {
			email.footer(acted);
			return email;
		});
	}

	/**
	 * Take boards out of service if they've been reported frequently enough.
	 *
	 * @param sql
	 *            How to touch the DB
	 * @param email
	 *            The email we are building.
	 * @return The number of boards taken out of service
	 */
	private Optional<Integer> takeBoardsOutOfService(BoardReportSQL sql,
			EmailBuilder email) {
		int acted = 0;
		for (Row r : sql.getReported.call(props.getReportActionThreshold())) {
			int boardId = r.getInt("board_id");
			if (sql.setFunctioning.call(false, boardId) > 0) {
				email.serviceActionDone(r);
				acted++;
			}
		}
		if (acted > 0) {
			purgeDownCache();
			epochs.nextMachineEpoch();
		}
		return acted > 0 ? Optional.of(acted) : Optional.empty();
	}

	private static DownLink makeDownLinkFromRow(Row row) {
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

	private class MachineImpl implements Machine {
		private final int id;

		private final boolean inService;

		private final String name;

		private final Set<String> tags;

		private final int width;

		private final int height;

		@JsonIgnore
		private final Epoch epoch;

		MachineImpl(Connection conn, Row rs, Epoch epoch) {
			this.epoch = epoch;
			id = rs.getInt("machine_id");
			name = rs.getString("machine_name");
			width = rs.getInt("width");
			height = rs.getInt("height");
			inService = rs.getBoolean("in_service");
			try (Query getTags = conn.query(GET_TAGS)) {
				tags = getTags.call(id).map(string("tag")).toSet();
			}
		}

		private int getArea() {
			return width * height * TRIAD_DEPTH;
		}

		@Override
		public void waitForChange(Duration timeout) {
			if (isNull(epoch)) {
				return;
			}
			try {
				epoch.waitForChange(timeout);
			} catch (InterruptedException ignored) {
				currentThread().interrupt();
			}
		}

		@Override
		public Optional<BoardLocation> getBoardByChip(int x, int y) {
			try (Connection conn = getConnection();
					Query findBoard = conn.query(findBoardByGlobalChip)) {
				return conn.transaction(false, () -> findBoard.call1(id, x, y)
						.map(row -> new BoardLocationImpl(row, this)));
			}
		}

		@Override
		public Optional<BoardLocation> getBoardByPhysicalCoords(int cabinet,
				int frame, int board) {
			try (Connection conn = getConnection();
					Query findBoard = conn.query(findBoardByPhysicalCoords)) {
				return conn.transaction(false,
						() -> findBoard.call1(id, cabinet, frame, board)
								.map(row -> new BoardLocationImpl(row, this)));
			}
		}

		@Override
		public Optional<BoardLocation> getBoardByLogicalCoords(int x, int y,
				int z) {
			try (Connection conn = getConnection();
					Query findBoard = conn.query(findBoardByLogicalCoords)) {
				return conn.transaction(false,
						() -> findBoard.call1(id, x, y, z)
								.map(row -> new BoardLocationImpl(row, this)));
			}
		}

		@Override
		public Optional<BoardLocation> getBoardByIPAddress(String address) {
			try (Connection conn = getConnection();
					Query findBoard = conn.query(findBoardByIPAddress)) {
				return conn.transaction(false,
						() -> findBoard.call1(id, address)
								.map(row -> new BoardLocationImpl(row, this)));
			}
		}

		@Override
		public String getRootBoardBMPAddress() {
			try (Connection conn = getConnection();
					Query rootBMPaddr = conn.query(GET_ROOT_BMP_ADDRESS)) {
				return conn.transaction(false, () -> rootBMPaddr.call1(id)
						.map(string("address")).orElse(null));
			}
		}

		@Override
		public List<Integer> getBoardNumbers() {
			try (Connection conn = getConnection();
					Query boardNumbers = conn.query(GET_BOARD_NUMBERS)) {
				return conn.transaction(false, () -> boardNumbers.call(id)
						.map(integer("board_num")).toList());
			}
		}

		@Override
		public List<BoardCoords> getDeadBoards() {
			// Assume that the list doesn't change for the duration of this obj
			synchronized (Spalloc.this) {
				List<BoardCoords> down = downBoardsCache.get(name);
				if (nonNull(down)) {
					return unmodifiableList(down);
				}
			}
			try (Connection conn = getConnection();
					Query boardNumbers = conn.query(GET_DEAD_BOARDS)) {
				List<BoardCoords> downBoards = conn.transaction(false,
						() -> boardNumbers.call(id)
								.map(row -> new BoardCoords(row.getInt("x"),
										row.getInt("y"), row.getInt("z"),
										row.getInt("cabinet"),
										row.getInt("frame"),
										row.getInteger("board_num"),
										row.getString("address")))
								.toList());
				synchronized (Spalloc.this) {
					downBoardsCache.putIfAbsent(name, downBoards);
				}
				return unmodifiableList(downBoards);
			}
		}

		@Override
		public List<DownLink> getDownLinks() {
			// Assume that the list doesn't change for the duration of this obj
			synchronized (Spalloc.this) {
				List<DownLink> down = downLinksCache.get(name);
				if (nonNull(down)) {
					return unmodifiableList(down);
				}
			}
			try (Connection conn = getConnection();
					Query boardNumbers = conn.query(getDeadLinks)) {
				List<DownLink> downLinks =
						conn.transaction(false, () -> boardNumbers.call(id)
								.map(Spalloc::makeDownLinkFromRow).toList());
				synchronized (Spalloc.this) {
					downLinksCache.putIfAbsent(name, downLinks);
				}
				return unmodifiableList(downLinks);
			}
		}

		@Override
		public List<Integer> getAvailableBoards() {
			try (Connection conn = getConnection();
					Query boardNumbers =
							conn.query(GET_AVAILABLE_BOARD_NUMBERS)) {
				return conn.transaction(false, () -> boardNumbers.call(id)
						.map(integer("board_num")).toList());
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

		@Override
		public boolean isInService() {
			return inService;
		}

		@Override
		public String getBMPAddress(BMPCoords bmp) {
			try (Connection conn = getConnection();
					Query bmpAddr = conn.query(GET_BMP_ADDRESS)) {
				return conn.transaction(false,
						() -> bmpAddr
								.call1(id, bmp.getCabinet(), bmp.getFrame())
								.map(string("address")).orElse(null));
			}
		}

		@Override
		public List<Integer> getBoardNumbers(BMPCoords bmp) {
			try (Connection conn = getConnection();
					Query boardNumbers = conn.query(GET_BMP_BOARD_NUMBERS)) {
				return conn.transaction(false,
						() -> boardNumbers
								.call(id, bmp.getCabinet(), bmp.getFrame())
								.map(integer("board_num")).toList());
			}
		}

		@Override
		public String toString() {
			return "Machine(" + name + ")";
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
				currentThread().interrupt();
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

		private void setJobs(MappableIterable<Row> rows) {
			jobs = rows.map(this::makeJob).toList();
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

	private final class BoardReportSQL extends AbstractSQL {
		final Query findBoardByChip = conn.query(findBoardByJobChip);

		final Query findBoardByTriad = conn.query(findBoardByLogicalCoords);

		final Query findBoardPhys = conn.query(findBoardByPhysicalCoords);

		final Query findBoardNet = conn.query(findBoardByIPAddress);

		final Update insertReport = conn.update(INSERT_BOARD_REPORT);

		final Query getReported = conn.query(getReportedBoards);

		final Update setFunctioning = conn.update(SET_FUNCTIONING_FIELD);

		@Override
		public void close() {
			findBoardByChip.close();
			findBoardByTriad.close();
			findBoardPhys.close();
			findBoardNet.close();
			insertReport.close();
			getReported.close();
			setFunctioning.close();
			super.close();
		}
	}

	/** Used to assemble an issue-report email for sending. */
	private static final class EmailBuilder {
		/**
		 * More efficient than several String.format() calls, and much clearer
		 * than a mess of direct {@link StringBuilder} calls!
		 */
		private final Formatter b = new Formatter(Locale.UK);

		private final int id;

		/**
		 * @param id
		 *            The job ID
		 */
		EmailBuilder(int id) {
			this.id = id;
		}

		void header(String issue, int numBoards, String who) {
			b.format("Issues \"%s\" with %d boards reported by %s\n\n", issue,
					numBoards, who);
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

		/** @return The assembled message body. */
		@Override
		public String toString() {
			return b.toString();
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
				try (Query boardRoot = conn.query(GET_ROOT_OF_BOARD)) {
					chipRoot = boardRoot.call1(root)
							.map(chip("root_x", "root_y")).orElse(null);
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
			try (Connection conn = getConnection();
					Update keepAlive = conn.update(UPDATE_KEEPALIVE)) {
				conn.transaction(() -> keepAlive.call(keepaliveAddress, id));
			}
		}

		@Override
		public void destroy(String reason) {
			if (partial) {
				throw new PartialJobException();
			}
			powerController.destroyJob(id, reason);
			rememberer.killProxies(id);
		}

		@Override
		public void waitForChange(Duration timeout) {
			if (isNull(epoch)) {
				return;
			}
			try {
				epoch.waitForChange(timeout);
			} catch (InterruptedException ignored) {
				currentThread().interrupt();
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
			return executeRead(conn -> Optional.of(new SubMachineImpl(conn)));
		}

		@Override
		public Optional<BoardLocation> whereIs(int x, int y) {
			if (isNull(root)) {
				return Optional.empty();
			}
			try (Connection conn = getConnection();
					Query findBoard = conn.query(findBoardByJobChip)) {
				return conn.transaction(false, () -> findBoard
						.call1(id, root, x, y)
						.map(row -> new BoardLocationImpl(row, Spalloc.this
								.getMachine(machineId, true, conn).get())));
			}
		}

		// -------------------------------------------------------------
		// Bad board report handling

		@Override
		public String reportIssue(IssueReportRequest report, Permit permit) {
			try (BoardReportSQL q = new BoardReportSQL()) {
				EmailBuilder email = new EmailBuilder(id);
				String result = q.transaction(
						() -> reportIssue(report, permit, email, q));
				emailSender.sendServiceMail(email);
				return result;
			} catch (ReportRollbackExn e) {
				return e.getMessage();
			}
		}

		/**
		 * Report an issue with some boards and assemble the email to send. This
		 * may result in boards being taken out of service (i.e., no longer
		 * being available to be allocated; their current allocation will
		 * continue).
		 * <p>
		 * <strong>NB:</strong> The sending of the email sending is
		 * <em>outside</em> the transaction that this code is executed in.
		 *
		 * @param report
		 *            The report from the user.
		 * @param permit
		 *            Who the user is.
		 * @param email
		 *            The email we're assembling.
		 * @param q
		 *            SQL access queries.
		 * @return Summary of action taken message, to go to user.
		 * @throws ReportRollbackExn
		 *             If the report is bad somehow.
		 */
		private String reportIssue(IssueReportRequest report, Permit permit,
				EmailBuilder email, BoardReportSQL q) throws ReportRollbackExn {
			email.header(report.issue, report.boards.size(), permit.name);
			int userId = getUser(q.getConnection(), permit.name)
					.orElseThrow(() -> new ReportRollbackExn(
							"no such user: " + permit.name));
			for (ReportedBoard board : report.boards) {
				addIssueReport(q, getJobBoardForReport(q, board, email),
						report.issue, userId, email);
			}
			return takeBoardsOutOfService(q, email).map(acted -> {
				email.footer(acted);
				return format("%d boards taken out of service", acted);
			}).orElse("report noted");
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

		// -------------------------------------------------------------

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

		@Override
		public void rememberProxy(ProxyCore proxy) {
			rememberer.rememberProxyForJob(id, proxy);
		}

		@Override
		public void forgetProxy(ProxyCore proxy) {
			rememberer.removeProxyForJob(id, proxy);
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
				machine = Spalloc.this.getMachine(machineId, true, conn).get();
				try (Query getRootXY = conn.query(GET_ROOT_COORDS);
						Query getBoardInfo =
								conn.query(GET_BOARD_CONNECT_INFO)) {
					getRootXY.call1(root).ifPresent(row -> {
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
								relativeChipLocation(row.getInt("root_x"),
										row.getInt("root_y")),
								row.getString("address")));
					});
				}
			}

			private ChipLocation relativeChipLocation(int x, int y) {
				x -= chipRoot.getX();
				y -= chipRoot.getY();
				// Allow for wrapping
				if (x < 0) {
					x += machine.getWidth();
				}
				if (y < 0) {
					y += machine.getHeight();
				}
				return new ChipLocation(x, y);
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
				try (Connection conn = getConnection();
						Query power = conn.query(GET_SUM_BOARDS_POWERED)) {
					return conn.transaction(false, () -> power.call1(id)
							.map(row -> row.getInt("total_on") < boardIds.size()
									? OFF
									: ON)
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

	/**
	 * Board location implementation. Does not retain database connections after
	 * creation.
	 *
	 * @author Donal Fellows
	 */
	private final class BoardLocationImpl implements BoardLocation {
		private JobImpl job;

		private final String machineName;

		private final int machineWidth;

		private final int machineHeight;

		private final ChipLocation chip;

		private final ChipLocation boardChip;

		private final BoardCoordinates logical;

		private final BoardPhysicalCoordinates physical;

		// Transaction is open
		private BoardLocationImpl(Row row, Machine machine) {
			machineName = row.getString("machine_name");
			logical = new BoardCoordinates(row.getInt("x"), row.getInt("y"),
					row.getInt("z"));
			physical = new BoardPhysicalCoordinates(row.getInt("cabinet"),
					row.getInt("frame"), row.getInteger("board_num"));
			chip = chip(row, "chip_x", "chip_y");
			machineWidth = machine.getWidth();
			machineHeight = machine.getHeight();
			Integer boardX = row.getInteger("board_chip_x");
			if (nonNull(boardX)) {
				boardChip = chip(row, "board_chip_x", "board_chip_y");
			} else {
				boardChip = chip;
			}

			Integer jobId = row.getInteger("job_id");
			if (nonNull(jobId)) {
				job = new JobImpl(epochs.getJobsEpoch(), jobId,
						machine.getId());
				job.chipRoot = chip(row, "job_root_chip_x", "job_root_chip_y");
			}
		}

		@Override
		public ChipLocation getBoardChip() {
			return boardChip;
		}

		@Override
		public ChipLocation getChipRelativeTo(ChipLocation rootChip) {
			int x = chip.getX() - rootChip.getX();
			if (x < 0) {
				x += machineWidth * TRIAD_CHIP_SIZE;
			}
			int y = chip.getY() - rootChip.getY();
			if (y < 0) {
				y += machineHeight * TRIAD_CHIP_SIZE;
			}
			return new ChipLocation(x, y);
		}

		@Override
		public String getMachine() {
			return machineName;
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
		super(format(msg, args));
	}

	ReportRollbackExn(HasChipLocation chip) {
		this("chip at (%d,%d) not in job's allocation", chip.getX(),
				chip.getY());
	}
}

abstract class GroupsException extends RuntimeException {
	private static final long serialVersionUID = 6607077117924279611L;

	GroupsException(String message) {
		super(message);
	}

	GroupsException(String message, Throwable cause) {
		super(message, cause);
	}
}

class NoSuchGroupException extends GroupsException {
	private static final long serialVersionUID = 5193818294198205503L;

	NoSuchGroupException(String msg, Object... args) {
		super(format(msg, args));
	}
}

class MultipleGroupsException extends GroupsException {
	private static final long serialVersionUID = 6284332340565334236L;

	MultipleGroupsException(String msg, Object... args) {
		super(format(msg, args));
	}
}