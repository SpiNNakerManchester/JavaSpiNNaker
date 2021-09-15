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
package uk.ac.manchester.spinnaker.alloc.compat;

import static java.util.Collections.singleton;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.stream.Collectors.toList;
import static org.slf4j.LoggerFactory.getLogger;
import static uk.ac.manchester.spinnaker.alloc.compat.Utils.mapToArray;
import static uk.ac.manchester.spinnaker.alloc.compat.Utils.state;
import static uk.ac.manchester.spinnaker.alloc.compat.Utils.timestamp;
import static uk.ac.manchester.spinnaker.alloc.model.PowerState.ON;

import java.io.IOException;
import java.net.Socket;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.Future;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.springframework.dao.DataAccessException;

import com.fasterxml.jackson.databind.ObjectMapper;

import uk.ac.manchester.spinnaker.alloc.SecurityConfig.Permit;
import uk.ac.manchester.spinnaker.alloc.admin.MachineDefinitionLoader.TriadCoords;
import uk.ac.manchester.spinnaker.alloc.allocator.SpallocAPI;
import uk.ac.manchester.spinnaker.alloc.allocator.SpallocAPI.BoardLocation;
import uk.ac.manchester.spinnaker.alloc.allocator.SpallocAPI.CreateBoard;
import uk.ac.manchester.spinnaker.alloc.allocator.SpallocAPI.CreateDimensions;
import uk.ac.manchester.spinnaker.alloc.allocator.SpallocAPI.CreateNumBoards;
import uk.ac.manchester.spinnaker.alloc.allocator.SpallocAPI.Job;
import uk.ac.manchester.spinnaker.alloc.allocator.SpallocAPI.SubMachine;
import uk.ac.manchester.spinnaker.alloc.compat.Utils.Function;
import uk.ac.manchester.spinnaker.alloc.model.PowerState;
import uk.ac.manchester.spinnaker.spalloc.messages.BoardCoordinates;
import uk.ac.manchester.spinnaker.spalloc.messages.BoardPhysicalCoordinates;
import uk.ac.manchester.spinnaker.spalloc.messages.Connection;
import uk.ac.manchester.spinnaker.spalloc.messages.JobDescription;
import uk.ac.manchester.spinnaker.spalloc.messages.JobMachineInfo;
import uk.ac.manchester.spinnaker.spalloc.messages.JobState;
import uk.ac.manchester.spinnaker.spalloc.messages.Machine;
import uk.ac.manchester.spinnaker.spalloc.messages.WhereIs;

/**
 * Concrete implementation of a task handling a single client connection.
 *
 * @author Donal Fellows
 */
class ServiceImpl extends V1CompatService.Task {
	private static final int ONE_MINUTE = 60;

	private static final int LOTS = 10000;

	private static final Duration NOTIFIER_WAIT_TIME =
			Duration.ofSeconds(ONE_MINUTE);

	private static final Duration DEFAULT_KEEPALIVE =
			Duration.ofSeconds(ONE_MINUTE);

	private static final Logger log = getLogger(V1CompatService.class);

	private Map<Integer, Future<Void>> jobNotifiers = new HashMap<>();

	private Map<String, Future<Void>> machNotifiers = new HashMap<>();

	private final V1CompatService srv;

	private final SpallocAPI spalloc;

	private final ObjectMapper mapper;

	/** Encoded form of our special permissions token. */
	private final Permit serviceUserPermit;

	ServiceImpl(V1CompatService srv, Socket sock) throws IOException {
		srv.super(sock);
		this.srv = srv;
		this.spalloc = srv.spalloc;
		this.mapper = srv.mapper;
		serviceUserPermit = new Permit(srv.props.getServiceUser());
	}

	@Override
	protected void closeNotifiers() {
		for (Future<Void> n : jobNotifiers.values()) {
			n.cancel(true);
		}
		for (Future<Void> n : machNotifiers.values()) {
			n.cancel(true);
		}
	}

	@Override
	protected String version() {
		return srv.version.toString();
	}

	@Override
	protected JobMachineInfo getJobMachineInfo(int jobId) throws Exception {
		Job job = getJob(jobId);
		job.access(host());
		SubMachine machine = job.getMachine()
				.orElseThrow(() -> new Exception("boards not allocated"));
		JobMachineInfo jmi = new JobMachineInfo();
		jmi.setMachineName(machine.getMachine().getName());
		jmi.setBoards(machine.getBoards());
		jmi.setConnections(machine.getConnections().stream().map(ci -> {
			Connection c = new Connection();
			c.setChip(ci.getChip());
			c.setHostname(ci.getHostname());
			return c;
		}).collect(toList()));
		jmi.setWidth(job.getWidth().orElse(0));
		jmi.setHeight(job.getHeight().orElse(0));
		return null;
	}

	@Override
	protected JobState getJobState(int jobId) throws Exception {
		Job job = getJob(jobId);
		job.access(host());
		JobState js = new JobState();
		js.setKeepalive(timestamp(job.getKeepaliveTimestamp()));
		js.setKeepalivehost(job.getKeepaliveHost().orElse(""));
		js.setPower(job.getMachine().map(m -> {
			try {
				return m.getPower().equals(PowerState.ON);
			} catch (DataAccessException e) {
				log.warn("problem getting job power state", e);
				return false;
			}
		}).orElse(false));
		js.setReason(job.getReason().orElse(""));
		js.setStartTime(timestamp(job.getStartTime()));
		js.setState(state(job));
		return js;
	}

	@Override
	protected void jobKeepalive(int jobId) throws Exception {
		getJob(jobId).access(host());
	}

	private List<String> tags(Object src) {
		List<String> vals = new ArrayList<>();
		if (src instanceof List) {
			for (Object o : (List<?>) src) {
				vals.add(Objects.toString(o));
			}
		}
		return vals;
	}

	@Override
	protected Integer createJobNumBoards(int numBoards,
			Map<String, Object> kwargs, Object cmd) throws Exception {
		return createJob(new CreateNumBoards(numBoards), kwargs, cmd);
	}

	@Override
	protected Integer createJobRectangle(int width, int height,
			Map<String, Object> kwargs, Object cmd) throws Exception {
		return createJob(new CreateDimensions(width, height), kwargs, cmd);
	}

	@Override
	protected Integer createJobSpecificBoard(TriadCoords coords,
			Map<String, Object> kwargs, Object cmd) throws Exception {
		return createJob(CreateBoard.triad(coords.x, coords.y, coords.z),
				kwargs, cmd);
	}

	private Integer createJob(SpallocAPI.CreateDescriptor create,
			Map<String, Object> kwargs, Object cmd) throws Exception {
		Integer maxDead = (Integer) kwargs.get("max_dead_boards");
		Number keepalive = (Number) kwargs.get("keepalive");
		String machineName = (String) kwargs.get("machine");
		Job job = spalloc.createJob(srv.props.getServiceUser(), create,
				machineName, tags(kwargs.get("tags")),
				isNull(keepalive) ? DEFAULT_KEEPALIVE
						: Duration.ofSeconds(keepalive.intValue()),
				maxDead, mapper.writeValueAsBytes(cmd));
		return job.getId();
	}

	@Override
	protected void destroyJob(int jobId, String reason) throws Exception {
		getJob(jobId).destroy(reason);
	}

	@Override
	protected BoardCoordinates getBoardAtPhysicalPosition(String machineName,
			int cabinet, int frame, int board) throws Exception {
		BoardLocation loc = getMachine(machineName)
				.getBoardByPhysicalCoords(cabinet, frame, board)
				.orElseThrow(() -> new Exception("no such board"));
		return loc.getLogical();
	}

	@Override
	protected BoardPhysicalCoordinates getBoardAtLogicalPosition(
			String machineName, int x, int y, int z) throws Exception {
		BoardLocation loc =
				getMachine(machineName).getBoardByLogicalCoords(x, y, z)
						.orElseThrow(() -> new Exception("no such board"));
		return loc.getPhysical();
	}

	@Override
	protected JobDescription[] listJobs() {
		// Messy; hits the database many times
		return mapArrayTx(() -> spalloc.getJobs(false, LOTS, 0).jobs(),
				JobDescription.class, (job, jd) -> {
					jd.setJobID(job.getId());
					jd.setKeepAlive(timestamp(job.getKeepaliveTimestamp()));
					jd.setKeepAliveHost(job.getKeepaliveHost().orElse(""));
					jd.setReason(job.getReason().orElse(""));
					jd.setStartTime(timestamp(job.getStartTime()));
					jd.setState(state(job));
					getCommand(job).ifPresent(cmd -> {
						// In order to get here, this must be safe
						// Validation was when job was created
						@SuppressWarnings({
							"unchecked", "rawtypes"
						})
						List<Integer> args = (List) cmd.getArgs();
						jd.setArgs(args);
						jd.setKwargs(cmd.getKwargs());
					});

					Optional<SubMachine> osm = job.getMachine();
					if (osm.isPresent()) {
						SubMachine sm = osm.get();
						jd.setMachine(sm.getMachine().getName());
						jd.setBoards(sm.getBoards());
						jd.setPower(sm.getPower() == ON);
					}
				});
	}

	@Override
	protected Machine[] listMachines() {
		// Messy; hits the database many times
		return mapArrayTx(() -> spalloc.getMachines().values(), Machine.class,
				(m, md) -> {
					md.setName(m.getName());
					md.setTags(new ArrayList<>(m.getTags()));
					md.setWidth(m.getWidth());
					md.setHeight(m.getHeight());
					md.setDeadBoards(m.getDeadBoards().stream()
							.map(Utils::board).collect(toList()));
					md.setDeadLinks(m.getDownLinks().stream()
							.flatMap(Utils::boardLinks).collect(toList()));
				});
	}

	@Override
	protected void notifyJob(Integer jobId, boolean wantNotify)
			throws Exception {
		if (nonNull(jobId)) {
			Job job = getJob(jobId);
			job.access(host());
		}
		manageNotifier(jobNotifiers, jobId, wantNotify, () -> {
			spalloc.getJobs(false, LOTS, 0).waitForChange(NOTIFIER_WAIT_TIME);
			List<Integer> actual = spalloc.getJobs(false, LOTS, 0).ids();
			if (nonNull(jobId)) {
				actual.retainAll(singleton(jobId));
			}
			writeJobNotification(actual);
		});
	}

	@Override
	protected void notifyMachine(String machine, boolean wantNotify)
			throws Exception {
		if (nonNull(machine)) {
			// Validate
			getMachine(machine);
		}
		manageNotifier(machNotifiers, machine, wantNotify, () -> {
			srv.epochs.getMachineEpoch().waitForChange(NOTIFIER_WAIT_TIME);
			List<String> actual =
					new ArrayList<String>(spalloc.getMachines().keySet());
			if (nonNull(machine)) {
				actual.retainAll(singleton(machine));
			}
			writeMachineNotification(actual);
		});
	}

	@Override
	protected void powerJobBoards(int jobId, PowerState switchOn)
			throws Exception {
		Job job = getJob(jobId);
		job.access(host());
		job.getMachine()
				.orElseThrow(
						() -> new Exception("no boards currently allocated"))
				.setPower(switchOn);
	}

	@Override
	protected WhereIs whereIsJobChip(int jobId, int x, int y) throws Exception {
		Job job = getJob(jobId);
		job.access(host());
		return whereis(job.whereIs(x, y).orElseThrow(
				() -> new Exception("no boards currently allocated")));
	}

	private WhereIs whereis(BoardLocation bl) {
		WhereIs wi = new WhereIs();
		wi.setMachine(bl.getMachine());
		wi.setLogical(bl.getLogical());
		wi.setPhysical(bl.getPhysical());
		wi.setChip(bl.getChip());
		wi.setBoardChip(bl.getBoardChip());
		Job j = bl.getJob();
		if (nonNull(j)) {
			wi.setJobId(j.getId());
			wi.setJobChip(bl.getChipRelativeTo(j.getRootChip().get()));
		}
		return wi;
	}

	@Override
	protected WhereIs whereIsMachineChip(String machineName, int x, int y)
			throws Exception {
		return whereis(getMachine(machineName).getBoardByChip(x, y)
				.orElseThrow(() -> new Exception("no such board")));
	}

	@Override
	protected WhereIs whereIsMachineLogicalBoard(String machineName, int x,
			int y, int z) throws Exception {
		return whereis(getMachine(machineName).getBoardByLogicalCoords(x, y, z)
				.orElseThrow(() -> new Exception("no such board")));
	}

	@Override
	protected WhereIs whereIsMachinePhysicalBoard(String machineName, int c,
			int f, int b) throws Exception {
		return whereis(getMachine(machineName).getBoardByPhysicalCoords(c, f, b)
				.orElseThrow(() -> new Exception("no such board")));
	}

	// instance-aware utilities

	/**
	 * Map a collection of items to an array within a transaction.
	 *
	 * @param <T>
	 *            The type of source items.
	 * @param <U>
	 *            The type of target items.
	 * @param srcItems
	 *            How to get the collection of source items.
	 * @param targetCls
	 *            The type of target items created. Needed so we can make them
	 *            and make an array of them.
	 * @param itemMapper
	 *            How to fill out a target item given a source item.
	 * @return Array of items of target type.
	 */
	private <T, U> U[] mapArrayTx(Supplier<Collection<T>> srcItems,
			Class<U> targetCls, Function<T, U> itemMapper) {
		return srv.db.execute(
				c -> mapToArray(srcItems.get(), targetCls, itemMapper));
	}

	private Job getJob(int jobId) throws Exception {
		return spalloc.getJob(serviceUserPermit, jobId)
				.orElseThrow(() -> new Exception("no such job"));
	}

	private SpallocAPI.Machine getMachine(String machineName) throws Exception {
		return spalloc.getMachine(machineName)
				.orElseThrow(() -> new Exception("no such machine"));
	}

	private <T> void manageNotifier(Map<T, Future<Void>> notifiers, T key,
			boolean wantNotify, Utils.Notifier notifier) {
		if (wantNotify) {
			if (!notifiers.containsKey(key)) {
				notifiers.put(key, srv.executor.submit(notifier));
			}
		} else {
			Future<Void> n = notifiers.remove(key);
			if (nonNull(n)) {
				n.cancel(true);
			}
		}
	}

	private Optional<Command> getCommand(Job job) {
		Optional<byte[]> origReq = job.getOriginalRequest();
		if (origReq.isPresent()) {
			try {
				return Optional.ofNullable(
						mapper.readValue(origReq.get(), Command.class));
			} catch (IOException e) {
				log.error("unexpected failure parsing JSON", e);
			}
		}
		return Optional.empty();
	}
}
