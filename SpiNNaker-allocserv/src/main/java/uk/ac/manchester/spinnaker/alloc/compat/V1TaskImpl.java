/*
 * Copyright (c) 2021 The University of Manchester
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package uk.ac.manchester.spinnaker.alloc.compat;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.StringUtils.isAsciiPrintable;
import static org.slf4j.LoggerFactory.getLogger;
import static uk.ac.manchester.spinnaker.alloc.allocator.SpallocAPI.CreateBoard.triad;
import static uk.ac.manchester.spinnaker.alloc.compat.Utils.parseDec;
import static uk.ac.manchester.spinnaker.alloc.compat.Utils.state;
import static uk.ac.manchester.spinnaker.alloc.compat.Utils.timestamp;
import static uk.ac.manchester.spinnaker.alloc.model.PowerState.ON;
import static uk.ac.manchester.spinnaker.machine.MachineDefaults.HALF_SIZE;
import static uk.ac.manchester.spinnaker.machine.MachineDefaults.SIZE_X_OF_ONE_BOARD;
import static uk.ac.manchester.spinnaker.machine.MachineDefaults.SIZE_Y_OF_ONE_BOARD;
import static uk.ac.manchester.spinnaker.machine.MachineDefaults.TRIAD_HEIGHT;
import static uk.ac.manchester.spinnaker.machine.MachineDefaults.TRIAD_WIDTH;
import static uk.ac.manchester.spinnaker.utils.CollectionUtils.collectToArray;
import static uk.ac.manchester.spinnaker.utils.UnitConstants.NSEC_PER_SEC;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.net.Socket;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.Future;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Component;

import uk.ac.manchester.spinnaker.alloc.ServiceVersion;
import uk.ac.manchester.spinnaker.alloc.SpallocProperties;
import uk.ac.manchester.spinnaker.alloc.allocator.Epochs;
import uk.ac.manchester.spinnaker.alloc.allocator.SpallocAPI;
import uk.ac.manchester.spinnaker.alloc.allocator.SpallocAPI.BoardLocation;
import uk.ac.manchester.spinnaker.alloc.allocator.SpallocAPI.CreateDimensions;
import uk.ac.manchester.spinnaker.alloc.allocator.SpallocAPI.CreateNumBoards;
import uk.ac.manchester.spinnaker.alloc.allocator.SpallocAPI.Job;
import uk.ac.manchester.spinnaker.alloc.compat.Utils.Notifier;
import uk.ac.manchester.spinnaker.alloc.db.DatabaseAwareBean;
import uk.ac.manchester.spinnaker.alloc.model.PowerState;
import uk.ac.manchester.spinnaker.alloc.model.Prototype;
import uk.ac.manchester.spinnaker.alloc.security.Permit;
import uk.ac.manchester.spinnaker.machine.ChipLocation;
import uk.ac.manchester.spinnaker.machine.CoreLocation;
import uk.ac.manchester.spinnaker.machine.HasChipLocation;
import uk.ac.manchester.spinnaker.machine.board.PhysicalCoords;
import uk.ac.manchester.spinnaker.machine.board.TriadCoords;
import uk.ac.manchester.spinnaker.spalloc.messages.BoardCoordinates;
import uk.ac.manchester.spinnaker.spalloc.messages.BoardPhysicalCoordinates;
import uk.ac.manchester.spinnaker.spalloc.messages.Connection;
import uk.ac.manchester.spinnaker.spalloc.messages.JobDescription;
import uk.ac.manchester.spinnaker.spalloc.messages.JobMachineInfo;
import uk.ac.manchester.spinnaker.spalloc.messages.JobState;
import uk.ac.manchester.spinnaker.spalloc.messages.Machine;
import uk.ac.manchester.spinnaker.spalloc.messages.WhereIs;

/**
 * Concrete implementation of a task handling a single client connection. This
 * is a <em>prototype bean</em>; Spring will instantiate it whenever needed.
 * It's consumed by {@link V1CompatService}, which uses it to implement the
 * handler for a particular connection.
 *
 * @author Donal Fellows
 */
@Component
@Prototype
class V1TaskImpl extends V1CompatTask {
	private static final int LOTS = 10000;

	private static final Logger log = getLogger(V1CompatService.class);

	private final Map<Integer, Future<Void>> jobNotifiers = new HashMap<>();

	private final Map<String, Future<Void>> machNotifiers = new HashMap<>();

	/** The service version information. */
	@Autowired
	private ServiceVersion version;

	/** The overall service properties. */
	@Autowired
	private SpallocProperties mainProps;

	/** The core spalloc service. */
	@Autowired
	private SpallocAPI spalloc;

	/** The epoch manager. */
	@Autowired
	private Epochs epochs;

	@Autowired
	private CompatHelper helper;

	/** Encoded form of our special permissions token. */
	private Permit permit;

	/** Name of the group for accounting purposes. */
	private String groupName;

	V1TaskImpl(V1CompatService srv, Socket sock) throws IOException {
		super(srv, sock);
	}

	V1TaskImpl(V1CompatService srv, Reader in, Writer out) {
		super(srv, in, out);
	}

	@PostConstruct
	void initUser() {
		var props = mainProps.getCompat();
		permit = new Permit(props.getServiceUser());
		groupName = props.getServiceGroup();
	}

	@Override
	protected final void closeNotifiers() {
		for (var n : jobNotifiers.values()) {
			n.cancel(true);
		}
		for (var n : machNotifiers.values()) {
			n.cancel(true);
		}
	}

	@Override
	protected final String version() {
		return version.getVersion().toString();
	}

	@Override
	protected final JobMachineInfo getJobMachineInfo(int jobId)
			throws TaskException {
		// NB: The views in this method already handle whole-machine wraparounds
		var job = getJob(jobId);
		job.access(host());
		var machine = job.getMachine()
				.orElseThrow(() -> new TaskException("boards not allocated"));
		int w, h;
		if (machine.getDepth() == 1) {
			// Special case: single board alloc
			w = SIZE_X_OF_ONE_BOARD;
			h = SIZE_Y_OF_ONE_BOARD;
		} else {
			// Everything else is in terms of triads
			w = job.getWidth().orElse(0) * TRIAD_WIDTH + HALF_SIZE;
			h = job.getHeight().orElse(0) * TRIAD_HEIGHT + HALF_SIZE;
		}
		return new JobMachineInfo(w, h, machine.getConnections().stream()
				.map(ci -> new Connection(ci.getChip(), ci.getHostname()))
				.collect(toList()), machine.getMachine().getName(),
				machine.getBoards());
	}

	@Override
	protected final JobState getJobState(int jobId) throws TaskException {
		var job = getJob(jobId);
		job.access(host());
		var js = new JobState.Builder();
		js.setKeepalive(timestamp(job.getKeepaliveTimestamp()));
		js.setKeepalivehost(job.getKeepaliveHost().orElse(""));
		js.setPower(job.getMachine().map(m -> {
			try {
				return m.getPower() == ON;
			} catch (DataAccessException e) {
				log.warn("problem getting job power state", e);
				return false;
			}
		}).orElse(false));
		js.setReason(job.getReason().orElse(""));
		js.setStartTime(timestamp(job.getStartTime()));
		js.setState(state(job));
		return js.build();
	}

	@Override
	protected final void jobKeepalive(int jobId) throws TaskException {
		getJob(jobId).access(host());
	}

	/**
	 * Parse a value to get a keepalive duration.
	 *
	 * @param keepalive
	 *            The number to parse. May be {@code null} to get a default.
	 * @return The duration. Never {@code null}.
	 */
	private Duration parseKeepalive(Number keepalive) {
		if (isNull(keepalive)) {
			return mainProps.getCompat().getDefaultKeepalive();
		}
		var d = Duration.ofSeconds(keepalive.longValue());
		if (!(keepalive instanceof Double || keepalive instanceof Float)) {
			return d;
		}
		double fractionalPart = keepalive.doubleValue() - keepalive.longValue();
		return d.plusNanos((long) (fractionalPart * NSEC_PER_SEC));
	}

	/**
	 * Parse a value to get a list of machine tags to match.
	 *
	 * @param src
	 *            The value to parse.
	 * @param mayForceDefault
	 *            Whether we want to force a default value into the tags.
	 * @return The list of tags. Never {@code null}.
	 */
	private static List<String> tags(Object src, boolean mayForceDefault) {
		var vals = new ArrayList<String>();
		if (src instanceof List) {
			for (var o : (List<?>) src) {
				vals.add(Objects.toString(o));
			}
		} else if (src instanceof String) {
			vals.add((String) src);
		}
		if (vals.isEmpty() && mayForceDefault) {
			return List.of("default");
		}
		return vals;
	}

	@Override
	protected final Optional<Integer> createJobNumBoards(int numBoards,
			Map<String, Object> kwargs, byte[] cmd) throws TaskException {
		var maxDead = parseDec(kwargs.get("max_dead_boards"));
		return createJob(new CreateNumBoards(numBoards, maxDead), kwargs, cmd);
	}

	@Override
	protected final Optional<Integer> createJobRectangle(int width, int height,
			Map<String, Object> kwargs, byte[] cmd) throws TaskException {
		var maxDead = parseDec(kwargs.get("max_dead_boards"));
		return createJob(new CreateDimensions(width, height, maxDead), kwargs,
				cmd);
	}

	@Override
	protected final Optional<Integer> createJobSpecificBoard(TriadCoords coords,
			Map<String, Object> kwargs, byte[] cmd) throws TaskException {
		return createJob(triad(coords.x, coords.y, coords.z), kwargs, cmd);
	}

	private static String getOwner(Map<String, Object> kwargs)
			throws TaskException {
		var owner = Objects.toString(kwargs.get("owner"), "").strip();
		if (owner.isBlank()) {
			throw new TaskException(
					"invalid owner identifier; must be non-empty");
		}
		if (!isAsciiPrintable(owner)) {
			throw new TaskException(
					"invalid owner identifier; must be printable ASCII");
		}
		return owner;
	}

	private Optional<Integer> createJob(SpallocAPI.CreateDescriptor create,
			Map<String, Object> kwargs, byte[] cmd) throws TaskException {
		var owner = getOwner(kwargs);
		var keepalive = parseKeepalive((Number) kwargs.get("keepalive"));
		var machineName = (String) kwargs.get("machine");
		var ts = tags(kwargs.get("tags"), isNull(machineName));
		var result = permit.authorize(() -> spalloc.createJob(permit.name,
				groupName, create, machineName, ts, keepalive, cmd));
		result.ifPresent(
				j -> log.info(
						"made compatibility-mode job {} "
								+ "on behalf of claimed user {}",
						j.getId(), owner));
		return result.map(Job::getId);
	}

	@Override
	protected final void destroyJob(int jobId, String reason)
			throws TaskException {
		getJob(jobId).destroy(reason);
	}

	@Override
	protected final BoardCoordinates getBoardAtPhysicalPosition(
			String machineName, int cabinet, int frame, int board)
			throws TaskException {
		return getMachine(machineName)
				.getBoardByPhysicalCoords(
						new PhysicalCoords(cabinet, frame, board))
				.orElseThrow(() -> new TaskException("no such board"))
				.getLogical();
	}

	@Override
	protected final BoardPhysicalCoordinates getBoardAtLogicalPosition(
			String machineName, int x, int y, int z) throws TaskException {
		return getMachine(machineName)
				.getBoardByLogicalCoords(new TriadCoords(x, y, z))
				.orElseThrow(() -> new TaskException("no such board"))
				.getPhysical();
	}

	@Component
	private static class CompatHelper extends DatabaseAwareBean {
		/** The core spalloc service. */
		@Autowired
		private SpallocAPI spalloc;

		private JobDescription[] listJobs(V1TaskImpl task) {
			// Messy; hits the database many times
			return spalloc.getJobs(false, LOTS, 0).jobs().stream()
					.map(job -> buildJobDescription(task,
							// NB: convert partial job description to full
							spalloc.getJob(task.permit, job.getId())
									.orElseThrow(IllegalStateException::new)))
					.collect(collectToArray(JobDescription[]::new));
		}

		private static JobDescription buildJobDescription(V1TaskImpl task,
				Job job) {
			var jd = new JobDescription.Builder();
			jd.setJobID(job.getId());
			jd.setOwner("");
			jd.setKeepAlive(timestamp(job.getKeepaliveTimestamp()));
			jd.setKeepAliveHost(job.getKeepaliveHost().orElse(""));
			jd.setReason(job.getReason().orElse(""));
			jd.setStartTime(timestamp(job.getStartTime()));
			jd.setState(state(job));
			job.getOriginalRequest().map(task::parseCommand).ifPresent(cmd -> {
				// In order to get here, this must be safe
				// Validation was when job was created
				@SuppressWarnings({ "unchecked", "rawtypes" })
				List<Integer> args = (List) cmd.getArgs();
				jd.setArgs(args);
				jd.setKwargs(cmd.getKwargs());
				// Override shrouded owner from above
				jd.setOwner(cmd.getKwargs().get("owner").toString());
			});
			job.getMachine().ifPresent(sm -> {
				jd.setMachine(sm.getMachine().getName());
				jd.setBoards(sm.getBoards());
				jd.setPower(sm.getPower() == ON);
			});
			return jd.build();
		}

		private Machine[] listMachines() {
			// Messy; hits the database many times
			return executeRead(c -> spalloc.getMachines(false).values().stream()
					.map(CompatHelper::buildMachineDescription)
					.collect(collectToArray(Machine[]::new)));
		}

		private static Machine buildMachineDescription(SpallocAPI.Machine m) {
			return new Machine(m.getName(), List.copyOf(m.getTags()),
					m.getWidth(), m.getHeight(),
					m.getDeadBoards().stream().map(Utils::board)
							.collect(toList()),
					m.getDownLinks().stream().flatMap(Utils::boardLinks)
							.collect(toList()));
		}
	}

	@Override
	protected final JobDescription[] listJobs() {
		return permit.authorize(() -> helper.listJobs(this));
	}

	@Override
	protected final Machine[] listMachines() {
		return permit.authorize(helper::listMachines);
	}

	@Override
	protected final void notifyJob(Integer jobId, boolean wantNotify)
			throws TaskException {
		if (nonNull(jobId)) {
			var job = getJob(jobId);
			job.access(host());
		}
		manageNotifier(jobNotifiers, jobId, wantNotify, () -> {
			var actual = permit.authorize(() -> {
				spalloc.getJobs(false, LOTS, 0).waitForChange(
						mainProps.getCompat().getNotifyWaitTime());
				return spalloc.getJobs(false, LOTS, 0).ids();
			});
			if (nonNull(jobId)) {
				actual.retainAll(List.of(jobId));
			}
			writeJobNotification(actual);
		});
	}

	@Override
	protected final void notifyMachine(String machine, boolean wantNotify)
			throws TaskException {
		if (nonNull(machine)) {
			// Validate
			getMachine(machine);
		}
		manageNotifier(machNotifiers, machine, wantNotify, () -> {
			epochs.getMachineEpoch()
					.waitForChange(mainProps.getCompat().getNotifyWaitTime());
			var actual = new ArrayList<>(permit
					.authorize(() -> spalloc.getMachines(false)).keySet());
			if (nonNull(machine)) {
				actual.retainAll(List.of(machine));
			}
			writeMachineNotification(actual);
		});
	}

	@Override
	protected final void powerJobBoards(int jobId, PowerState switchOn)
			throws TaskException {
		var job = getJob(jobId);
		job.access(host());
		job.getMachine().orElseThrow(
				() -> new TaskException("no boards currently allocated"))
				.setPower(switchOn);
	}

	@Override
	protected void reportProblem(String address, Integer x, Integer y,
			Integer p, String description) {
		HasChipLocation locus;
		if (nonNull(x) && nonNull(y)) {
			if (nonNull(p)) {
				locus = new CoreLocation(x, y, p);
			} else {
				locus = new ChipLocation(x, y);
			}
		} else {
			locus = null;
		}
		permit.authorize(() -> {
			spalloc.reportProblem(address, locus, description, permit);
			return this; // Ignored value
		});
	}

	private static WhereIs makeWhereIs(BoardLocation bl) {
		var wi = new WhereIs.Builder();
		wi.setMachine(bl.getMachine());
		wi.setLogical(bl.getLogical());
		wi.setPhysical(bl.getPhysical());
		wi.setChip(bl.getChip());
		wi.setBoardChip(bl.getBoardChip());
		var j = bl.getJob();
		if (nonNull(j)) {
			wi.setJobId(j.getId());
			wi.setJobChip(bl.getChipRelativeTo(j.getRootChip().orElseThrow()));
		}
		return wi.build();
	}

	@Override
	protected final WhereIs whereIsJobChip(int jobId, int x, int y)
			throws TaskException {
		var job = getJob(jobId);
		job.access(host());
		return job.whereIs(x, y).map(V1TaskImpl::makeWhereIs).orElseThrow(
				() -> new TaskException("no boards currently allocated"));
	}

	@Override
	protected final WhereIs whereIsMachineChip(String machineName, int x, int y)
			throws TaskException {
		return getMachine(machineName).getBoardByChip(new ChipLocation(x, y))
				.map(V1TaskImpl::makeWhereIs)
				.orElseThrow(() -> new TaskException("no such board"));
	}

	@Override
	protected final WhereIs whereIsMachineLogicalBoard(String machineName,
			int x, int y, int z) throws TaskException {
		return getMachine(machineName)
				.getBoardByLogicalCoords(new TriadCoords(x, y, z))
				.map(V1TaskImpl::makeWhereIs)
				.orElseThrow(() -> new TaskException("no such board"));
	}

	@Override
	protected final WhereIs whereIsMachinePhysicalBoard(String machineName,
			int c, int f, int b) throws TaskException {
		return getMachine(machineName)
				.getBoardByPhysicalCoords(new PhysicalCoords(c, f, b))
				.map(V1TaskImpl::makeWhereIs)
				.orElseThrow(() -> new TaskException("no such board"));
	}

	// instance-aware utilities

	private Job getJob(int jobId) throws TaskException {
		return permit.authorize(() -> spalloc.getJob(permit, jobId))
				.orElseThrow(() -> new TaskException("no such job"));
	}

	private SpallocAPI.Machine getMachine(String machineName)
			throws TaskException {
		return permit.authorize(() -> spalloc.getMachine(machineName, false))
				.orElseThrow(() -> new TaskException("no such machine"));
	}

	private <T> void manageNotifier(Map<T, Future<Void>> notifiers, T key,
			boolean wantNotify, Notifier notifier) {
		if (wantNotify) {
			if (!notifiers.containsKey(key)) {
				notifiers.put(key,
						getExecutor().submit(Notifier.toCallable(notifier)));
			}
		} else {
			var n = notifiers.remove(key);
			if (nonNull(n)) {
				n.cancel(true);
			}
		}
	}
}
