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

import static java.util.Arrays.asList;
import static java.util.Collections.singleton;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.stream.Collectors.toList;
import static org.slf4j.LoggerFactory.getLogger;
import static org.springframework.beans.factory.config.ConfigurableBeanFactory.SCOPE_PROTOTYPE;
import static uk.ac.manchester.spinnaker.alloc.compat.Utils.mapToArray;
import static uk.ac.manchester.spinnaker.alloc.compat.Utils.state;
import static uk.ac.manchester.spinnaker.alloc.compat.Utils.timestamp;
import static uk.ac.manchester.spinnaker.alloc.model.PowerState.ON;
import static uk.ac.manchester.spinnaker.alloc.SecurityConfig.TrustLevel.USER;

import java.io.IOException;
import java.io.NotSerializableException;
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
import java.util.function.BiConsumer;
import java.util.function.Supplier;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.dao.DataAccessException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import uk.ac.manchester.spinnaker.alloc.ServiceVersion;
import uk.ac.manchester.spinnaker.alloc.SpallocProperties;
import uk.ac.manchester.spinnaker.alloc.SpallocProperties.CompatibilityProperties;
import uk.ac.manchester.spinnaker.alloc.SecurityConfig.Permit;
import uk.ac.manchester.spinnaker.alloc.admin.MachineDefinitionLoader.TriadCoords;
import uk.ac.manchester.spinnaker.alloc.allocator.Epochs;
import uk.ac.manchester.spinnaker.alloc.allocator.SpallocAPI;
import uk.ac.manchester.spinnaker.alloc.allocator.SpallocAPI.BoardLocation;
import uk.ac.manchester.spinnaker.alloc.allocator.SpallocAPI.CreateBoard;
import uk.ac.manchester.spinnaker.alloc.allocator.SpallocAPI.CreateDimensions;
import uk.ac.manchester.spinnaker.alloc.allocator.SpallocAPI.CreateNumBoards;
import uk.ac.manchester.spinnaker.alloc.allocator.SpallocAPI.Job;
import uk.ac.manchester.spinnaker.alloc.allocator.SpallocAPI.SubMachine;
import uk.ac.manchester.spinnaker.alloc.db.DatabaseEngine;
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
 * Concrete implementation of a task handling a single client connection. This
 * is a <em>prototype bean</em>; Spring will instantiate it whenever needed.
 * It's consumed by {@link V1CompatService}, which uses it to implement the
 * handler for a particular connection.
 *
 * @author Donal Fellows
 */
@Component
@Scope(SCOPE_PROTOTYPE)
class V1TaskImpl extends V1CompatTask {
	private static final int ONE_MINUTE = 60;

	private static final int LOTS = 10000;

	private static final Duration NOTIFIER_WAIT_TIME =
			Duration.ofSeconds(ONE_MINUTE);

	private static final Duration DEFAULT_KEEPALIVE =
			Duration.ofSeconds(ONE_MINUTE);

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

	/** The database. */
	@Autowired
	private DatabaseEngine db;

	private String serviceUser;

	/** Encoded form of our special permissions token. */
	private Permit serviceUserPermit;

	V1TaskImpl(V1CompatService srv, Socket sock) throws IOException {
		super(srv, sock);
	}

	@PostConstruct
	void initUser() {
		CompatibilityProperties props = mainProps.getCompat();
		serviceUser = props.getServiceUser();
		serviceUserPermit = new Permit(props.getServiceUser());
	}

	@Override
	protected final void closeNotifiers() {
		for (Future<Void> n : jobNotifiers.values()) {
			n.cancel(true);
		}
		for (Future<Void> n : machNotifiers.values()) {
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
		Job job = getJob(jobId);
		job.access(host());
		SubMachine machine = job.getMachine()
				.orElseThrow(() -> new TaskException("boards not allocated"));
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
	protected final JobState getJobState(int jobId) throws TaskException {
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
	protected final void jobKeepalive(int jobId) throws TaskException {
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
	protected final Integer createJobNumBoards(int numBoards,
			Map<String, Object> kwargs, byte[] cmd) {
		return createJob(new CreateNumBoards(numBoards), kwargs, cmd);
	}

	@Override
	protected final Integer createJobRectangle(int width, int height,
			Map<String, Object> kwargs, byte[] cmd) {
		return createJob(new CreateDimensions(width, height), kwargs, cmd);
	}

	@Override
	protected final Integer createJobSpecificBoard(TriadCoords coords,
			Map<String, Object> kwargs, byte[] cmd) {
		return createJob(CreateBoard.triad(coords.x, coords.y, coords.z),
				kwargs, cmd);
	}

	private Integer createJob(SpallocAPI.CreateDescriptor create,
			Map<String, Object> kwargs, byte[] cmd) {
		Integer maxDead = (Integer) kwargs.get("max_dead_boards");
		Number keepalive = (Number) kwargs.get("keepalive");
		String machineName = (String) kwargs.get("machine");
		return inAuthenticatedContext(() -> {
			Job job = spalloc.createJob(serviceUser, create, machineName,
					tags(kwargs.get("tags")),
					isNull(keepalive) ? DEFAULT_KEEPALIVE
							: Duration.ofSeconds(keepalive.intValue()),
					maxDead, cmd);
			return job.getId();
		});
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
				.getBoardByPhysicalCoords(cabinet, frame, board)
				.orElseThrow(() -> new TaskException("no such board"))
				.getLogical();
	}

	@Override
	protected final BoardPhysicalCoordinates getBoardAtLogicalPosition(
			String machineName, int x, int y, int z) throws TaskException {
		return getMachine(machineName).getBoardByLogicalCoords(x, y, z)
				.orElseThrow(() -> new TaskException("no such board"))
				.getPhysical();
	}

	@Override
	protected final JobDescription[] listJobs() {
		// Messy; hits the database many times
		return mapArrayTx(
				() -> inAuthenticatedContext(
						() -> spalloc.getJobs(false, LOTS, 0).jobs()),
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
					job.getMachine().ifPresent(sm -> {
						jd.setMachine(sm.getMachine().getName());
						jd.setBoards(sm.getBoards());
						jd.setPower(sm.getPower() == ON);
					});
				});
	}

	@Override
	protected final Machine[] listMachines() {
		// Messy; hits the database many times
		return mapArrayTx(
				() -> inAuthenticatedContext(
						() -> spalloc.getMachines().values()),
				Machine.class, (m, md) -> {
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
	protected final void notifyJob(Integer jobId, boolean wantNotify)
			throws TaskException {
		if (nonNull(jobId)) {
			Job job = getJob(jobId);
			job.access(host());
		}
		manageNotifier(jobNotifiers, jobId, wantNotify, () -> {
			List<Integer> actual = inAuthenticatedContext(() -> {
				spalloc.getJobs(false, LOTS, 0)
						.waitForChange(NOTIFIER_WAIT_TIME);
				return spalloc.getJobs(false, LOTS, 0).ids();
			});
			if (nonNull(jobId)) {
				actual.retainAll(singleton(jobId));
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
			epochs.getMachineEpoch().waitForChange(NOTIFIER_WAIT_TIME);
			List<String> actual = new ArrayList<>(
					inAuthenticatedContext(() -> spalloc.getMachines())
							.keySet());
			if (nonNull(machine)) {
				actual.retainAll(singleton(machine));
			}
			writeMachineNotification(actual);
		});
	}

	@Override
	protected final void powerJobBoards(int jobId, PowerState switchOn)
			throws TaskException {
		Job job = getJob(jobId);
		job.access(host());
		job.getMachine().orElseThrow(
				() -> new TaskException("no boards currently allocated"))
				.setPower(switchOn);
	}

	@Override
	protected final WhereIs whereIsJobChip(int jobId, int x, int y)
			throws TaskException {
		Job job = getJob(jobId);
		job.access(host());
		return whereis(job.whereIs(x, y).orElseThrow(
				() -> new TaskException("no boards currently allocated")));
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
	protected final WhereIs whereIsMachineChip(String machineName, int x, int y)
			throws TaskException {
		return whereis(getMachine(machineName).getBoardByChip(x, y)
				.orElseThrow(() -> new TaskException("no such board")));
	}

	@Override
	protected final WhereIs whereIsMachineLogicalBoard(String machineName,
			int x, int y, int z) throws TaskException {
		return whereis(getMachine(machineName).getBoardByLogicalCoords(x, y, z)
				.orElseThrow(() -> new TaskException("no such board")));
	}

	@Override
	protected final WhereIs whereIsMachinePhysicalBoard(String machineName,
			int c, int f, int b) throws TaskException {
		return whereis(getMachine(machineName).getBoardByPhysicalCoords(c, f, b)
				.orElseThrow(() -> new TaskException("no such board")));
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
			Class<U> targetCls, BiConsumer<T, U> itemMapper) {
		return db.execute(
				c -> mapToArray(srcItems.get(), targetCls, itemMapper));
	}

	private Job getJob(int jobId) throws TaskException {
		return inAuthenticatedContext(
				() -> spalloc.getJob(serviceUserPermit, jobId))
						.orElseThrow(() -> new TaskException("no such job"));
	}

	private SpallocAPI.Machine getMachine(String machineName)
			throws TaskException {
		return inAuthenticatedContext(() -> spalloc.getMachine(machineName))
				.orElseThrow(() -> new TaskException("no such machine"));
	}

	private <T> void manageNotifier(Map<T, Future<Void>> notifiers, T key,
			boolean wantNotify, Utils.Notifier notifier) {
		if (wantNotify) {
			if (!notifiers.containsKey(key)) {
				notifiers.put(key, getExecutor().submit(notifier));
			}
		} else {
			Future<Void> n = notifiers.remove(key);
			if (nonNull(n)) {
				n.cancel(true);
			}
		}
	}

	private Optional<Command> getCommand(Job job) {
		return job.getOriginalRequest().map(req -> {
			try {
				return parseCommand(req);
			} catch (IOException e) {
				log.error("unexpected failure parsing JSON", e);
				return null;
			}
		});
	}

	/**
	 * Used to protect access to the spalloc core service.
	 *
	 * @param <T>
	 *            The type of the result of the action.
	 * @author Donal Fellows
	 */
	@FunctionalInterface
	private interface InContext<T> {
		/**
		 * Perform the action.
		 *
		 * @return The result of the action.
		 */
		T act();
	}

	/**
	 * Push our special authentication object for the duration of the inner
	 * code. Used to satisfy Spring method security.
	 *
	 * @param <T>
	 *            The type of the result
	 * @param inContext
	 *            The inner code to run with an authentication object applied.
	 * @return Whatever the inner code returns
	 */
	private <T> T inAuthenticatedContext(InContext<T> inContext) {
		@SuppressWarnings("serial")
		final class TempAuth implements Authentication {
			@Override
			public String getName() {
				return serviceUser;
			}

			@Override
			public Collection<? extends GrantedAuthority> getAuthorities() {
				return asList(() -> USER.name());
			}

			@Override
			public Object getCredentials() {
				// Never any credentials; always authenticated if this is in use
				return null;
			}

			@Override
			public Object getDetails() {
				return serviceUserPermit;
			}

			@Override
			public Object getPrincipal() {
				return serviceUserPermit;
			}

			@Override
			public boolean isAuthenticated() {
				// Never any credentials; always authenticated if this is in use
				return true;
			}

			@Override
			public void setAuthenticated(boolean isAuthenticated) {
				throw new UnsupportedOperationException();
			}

			private void writeObject(java.io.ObjectOutputStream out)
					throws NotSerializableException {
				throw new NotSerializableException("not actually serializable");
			}
		}

		SecurityContext context = SecurityContextHolder.getContext();
		Authentication old = context.getAuthentication();
		try {
			context.setAuthentication(new TempAuth());
			return inContext.act();
		} finally {
			context.setAuthentication(old);
		}
	}
}
