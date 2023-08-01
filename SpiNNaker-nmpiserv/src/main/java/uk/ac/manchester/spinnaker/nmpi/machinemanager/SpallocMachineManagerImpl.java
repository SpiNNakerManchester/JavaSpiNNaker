/*
 * Copyright (c) 2014 The University of Manchester
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package uk.ac.manchester.spinnaker.nmpi.machinemanager;

import static com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES;
import static com.fasterxml.jackson.databind.PropertyNamingStrategies.SNAKE_CASE;
import static java.util.Arrays.asList;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.concurrent.Executors.newScheduledThreadPool;
import static java.util.concurrent.TimeUnit.SECONDS;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.io.IOUtils.buffer;
import static org.apache.commons.io.IOUtils.closeQuietly;
import static org.slf4j.LoggerFactory.getLogger;
import static uk.ac.manchester.spinnaker.nmpi.ThreadUtils.sleep;
import static uk.ac.manchester.spinnaker.nmpi.ThreadUtils.waitfor;
import static uk.ac.manchester.spinnaker.nmpi.machinemanager.responses.JobState.DESTROYED;
import static uk.ac.manchester.spinnaker.nmpi.machinemanager.responses.JobState.READY;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Value;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;

import uk.ac.manchester.spinnaker.nmpi.machinemanager.commands.Command;
import uk.ac.manchester.spinnaker.nmpi.machinemanager.commands.CreateJobCommand;
import uk.ac.manchester.spinnaker.nmpi.machinemanager.commands.DestroyJobCommand;
import uk.ac.manchester.spinnaker.nmpi.machinemanager.commands.GetJobMachineInfoCommand;
import uk.ac.manchester.spinnaker.nmpi.machinemanager.commands.GetJobStateCommand;
import uk.ac.manchester.spinnaker.nmpi.machinemanager.commands.JobKeepAliveCommand;
import uk.ac.manchester.spinnaker.nmpi.machinemanager.commands.ListMachinesCommand;
import uk.ac.manchester.spinnaker.nmpi.machinemanager.commands.NoNotifyJobCommand;
import uk.ac.manchester.spinnaker.nmpi.machinemanager.commands.NotifyJobCommand;
import uk.ac.manchester.spinnaker.nmpi.machinemanager.commands.PowerOffJobBoardsCommand;
import uk.ac.manchester.spinnaker.nmpi.machinemanager.commands.PowerOnJobBoardsCommand;
import uk.ac.manchester.spinnaker.nmpi.machinemanager.commands.WhereIsCommand;
import uk.ac.manchester.spinnaker.nmpi.machinemanager.responses.ExceptionResponse;
import uk.ac.manchester.spinnaker.nmpi.machinemanager.responses.JobMachineInfo;
import uk.ac.manchester.spinnaker.nmpi.machinemanager.responses.JobState;
import uk.ac.manchester.spinnaker.nmpi.machinemanager.responses.JobsChangedResponse;
import uk.ac.manchester.spinnaker.nmpi.machinemanager.responses.Machine;
import uk.ac.manchester.spinnaker.nmpi.machinemanager.responses.Response;
import uk.ac.manchester.spinnaker.nmpi.machinemanager.responses.ReturnResponse;
import uk.ac.manchester.spinnaker.nmpi.machinemanager.responses.WhereIs;
import uk.ac.manchester.spinnaker.nmpi.model.machine.ChipCoordinates;
import uk.ac.manchester.spinnaker.nmpi.model.machine.SpinnakerMachine;
import uk.ac.manchester.spinnaker.nmpi.rest.utils.PropertyBasedDeserialiser;

/**
 * A machine manager that interfaces to the spalloc service.
 */
public final class SpallocMachineManagerImpl implements MachineManager {

	/**
	 * The default version of a machine.
	 */
	private static final String MACHINE_VERSION = "5";

	/**
	 * The tag indicating the machine should be picked by default.
	 */
	private static final String DEFAULT_TAG = "default";

	/**
	 * The keep-alive period in seconds.
	 */
	private static final int PERIOD = 5;

	/**
	 * The scaling from width in triads to width in chips.
	 */
	private static final int MACHINE_WIDTH_FACTOR = 12;

	/**
	 * The scaling from height in triads to height in chips.
	 */
	private static final int MACHINE_HEIGHT_FACTOR = 12;

	/**
	 * The number of times to retry a spalloc request.
	 */
	private static final int N_RETRIES = 3;

	/**
	 * The time to wait for a response from spalloc.
	 */
	private static final long TIMEOUT_SECONDS = 1;

	/**
	 * The spalloc server address.
	 */
	@Value("${spalloc.server}")
	private String ipAddress;

	/**
	 * The spalloc server port.
	 */
	@Value("${spalloc.port}")
	private int port;

	/**
	 * The owner to give spalloc jobs from this client.
	 */
	@Value("${spalloc.user.name}")
	private String owner;

	/**
	 * Unmarshaller of objects.
	 */
	private final ObjectMapper mapper = new ObjectMapper();

	/**
	 * The machines that have been allocated by job ID.
	 */
	private final Map<Integer, SpinnakerMachine> machinesAllocated =
			new HashMap<>();

	/**
	 * A map from machine to job.
	 */
	private final Map<SpinnakerMachine, SpallocJob> jobByMachine =
			new HashMap<>();

	/**
	 * The state of the spalloc job by job ID.
	 */
	private final Map<Integer, JobState> machineState = new HashMap<>();

	/**
	 * Logging.
	 */
	private static final Logger logger =
			getLogger(SpallocMachineManagerImpl.class);

	/**
	 * Communication management.
	 */
	private final Comms comms = new Comms();

	/**
	 * True when the manager is finished with.
	 */
	private volatile boolean done = false;

	/**
	 * Deserialiser for spalloc responses.
	 */
	@SuppressWarnings("serial")
	private static class ResponseDeserializer
			extends PropertyBasedDeserialiser<Response> {
		/**
		 * Subclass initialiser.
		 */
		ResponseDeserializer() {
			super(Response.class);
			register("jobs_changed", JobsChangedResponse.class);
			register("return", ReturnResponse.class);
			register("exception", ExceptionResponse.class);
		}
	}

	/**
	 * Make a machine manager that talks to Spalloc to do its work.
	 */
	public SpallocMachineManagerImpl() {
		final var module = new SimpleModule();
		module.addDeserializer(Response.class, new ResponseDeserializer());
		mapper.registerModule(module);
		mapper.setPropertyNamingStrategy(SNAKE_CASE);
		mapper.configure(FAIL_ON_UNKNOWN_PROPERTIES, false);
	}

	/**
	 * Thread pool.
	 */
	private ScheduledExecutorService scheduler;

	/**
	 * Launch this manager's threads.
	 */
	@PostConstruct
	private void startThreads() {
		final var group = new ThreadGroup("Spalloc");
		scheduler = newScheduledThreadPool(1,
				r -> new Thread(group, r, "Spalloc Keep Alive Handler"));

		new Thread(group, this::comms, "Spalloc Comms Interface").start();

		final var t = new Thread(group, this::updateStateOfJobs,
				"Spalloc JobState Update Notification Handler");
		t.setDaemon(true);
		t.start();

		scheduler.scheduleAtFixedRate(this::keepAllJobsAlive,
				PERIOD, PERIOD, SECONDS);
	}

	// ------------------------------ COMMS ------------------------------

	/**
	 * Communications API wrapper.
	 */
	class Comms {

		/**
		 * Internal exception for a timeout.
		 */
		class TimeoutException extends IOException {

			private static final long serialVersionUID = 1L;

			/**
			 * Create an exception indicating a timeout happened.
			 *
			 * @param message The message of the exception
			 */
			TimeoutException(final String message) {
				super(message);
			}
		}

		/**
		 * The responses from spalloc to be read.
		 */
		private final BlockingQueue<Response> responses =
				new LinkedBlockingQueue<>();

		/**
		 * The notifications from spalloc to be raised.
		 */
		private final BlockingQueue<JobsChangedResponse> notifications =
				new LinkedBlockingQueue<>();

		/**
		 * Connection to server.
		 */
		private Socket socket;

		/**
		 * Reader from server.
		 */
		private BufferedReader reader;

		/**
		 * Writer to server.
		 */
		private PrintWriter writer;

		/**
		 * True if connected.
		 */
		private volatile boolean connected = false;

		/**
		 * Get the next response.
		 *
		 * @param responseType The type expected.
		 * @param <T> The type of the response.
		 * @return The decoded response, or null if cancelled.
		 * @throws IOException If the response failed to be read.
		 */
		private <T> T getNextResponse(final Class<T> responseType)
				throws IOException {
			Response response;
			try {
				response = responses.poll(TIMEOUT_SECONDS, SECONDS);
				if (isNull(response)) {
					throw new TimeoutException(
							"No response from spalloc server");
				}
			} catch (final InterruptedException e) {
				return null;
			}
			if (response instanceof ExceptionResponse er) {
				throw new IOException(er.getException());
			}
			if (response instanceof ReturnResponse rr) {
				if (isNull(responseType)) {
					return null;
				}
				return mapper.readValue(rr.getReturnValue(), responseType);
			}
			// Should never happen!
			throw new IOException("Unknown Response " + response);
		}

		/**
		 * Wait for the connection to be established.
		 */
		private synchronized void waitForConnection() {
			while (!connected) {
				logger.debug("Waiting for connection");
				if (waitfor(this)) {
					break;
				}
			}
		}

		/**
		 * Write the given command as a request to the server.
		 *
		 * @param request The request to issue
		 * @throws IOException If an error occurs
		 */
		private void writeRequest(final Command<?> request) throws IOException {
			var message = mapper.writeValueAsString(request);
			logger.trace("Sending message {}", message);
			writer.println(message);
			writer.flush();
		}

		/**
		 * Read a response from the server.
		 *
		 * @throws IOException if an error occurred
		 */
		private void readResponse() throws IOException {
			// Note, assumes one response per line
			final var line = reader.readLine();
			if (isNull(line)) {
				synchronized (this) {
					connected = false;
					notifyAll();
				}
				return;
			}

			logger.trace("Received response: {}", line);
			final var response = mapper.readValue(line, Response.class);
			logger.trace("Received response of type {}", response);
			if (response instanceof ReturnResponse
					|| response instanceof ExceptionResponse) {
				responses.offer(response);
			} else if (response instanceof JobsChangedResponse jc) {
				notifications.offer(jc);
			} else {
				logger.error("Unrecognized response: {}", response);
			}
		}

		/**
		 * How long to wait after disconnecting before reconnecting. In
		 * milliseconds.
		 */
		private static final int POST_DISCONNECT_PAUSE = 1000;

		/**
		 * The main connection management loop. This will reconnect to the
		 * server if it gets disconnected by surprise.
		 */
		public void mainLoop() {
			while (!done) {
				try {
					connect();
				} catch (final IOException e) {
					if (!done) {
						logger.error("Could not connect to machine server", e);
					}
				}
				try {
					while (connected) {
						readResponse();
					}
				} catch (final IOException e) {
					if (!done) {
						logger.error("Error receiving", e);
						disconnect();
					}
				}
				if (!done) {
					logger.warn("Disconnected from machine server...");
					sleep(POST_DISCONNECT_PAUSE);
				}
			}
		}

		/**
		 * Connect to the Spalloc server.
		 *
		 * @throws IOException
		 *             If anything goes wrong
		 */
		public synchronized void connect() throws IOException {
			socket = new Socket(ipAddress, port);
			reader = buffer(new InputStreamReader(socket.getInputStream()));
			writer = new PrintWriter(socket.getOutputStream());

			connected = true;
			// Send an empty JCR over
			notifications.offer(new JobsChangedResponse());
			notifyAll();
		}

		/**
		 * Disconnect from the Spalloc server.
		 */
		public void disconnect() {
			connected = false;
			closeQuietly(writer);
			closeQuietly(reader);
			closeQuietly(socket);
		}

		/**
		 * Send a request that expects a response that needs to be deserialised.
		 *
		 * @param <T>
		 *            The type of the response.
		 * @param request
		 *            The request to send.
		 * @param responseType
		 *            The expected type of response.
		 * @return The response.
		 * @throws IOException
		 *             If anything goes wrong
		 */
		public <T> T sendRequest(final Command<?> request,
				final Class<T> responseType) throws IOException {
			synchronized (SpallocMachineManagerImpl.this) {
				int count = 0;
				while (true) {
					try {
						waitForConnection();
						writeRequest(request);
						return getNextResponse(responseType);
					} catch (final IOException e) {
						// Disconnect on an error to force reconnection
						disconnect();
						if (++count >= N_RETRIES) {
							throw e;
						}
					}
				}
			}
		}

		/**
		 * Send a request that doesn't expect a response that needs to be
		 * deserialised (that is, a generic OK).
		 *
		 * @param request
		 *            The request to send.
		 * @throws IOException
		 *             If anything goes wrong
		 */
		public void sendRequest(final Command<?> request) throws IOException {
			sendRequest(request, null);
		}

		/**
		 * Get the list of IDs of changed jobs from the next notification in the
		 * notification queue.
		 *
		 * @return list of IDs
		 * @throws InterruptedException
		 *             if interrupted
		 */
		public List<Integer> getJobsChanged() throws InterruptedException {
			return notifications.take().getJobsChanged();
		}
	}

	@Override
	public void close() {
		done = true;
		comms.disconnect();
	}

	private void comms() {
		try {
			comms.mainLoop();
		} finally {
			scheduler.shutdownNow();
		}
	}

	// ------------------------------ WIRE Job ------------------------------

	/**
	 * Interface to an existing spalloc job.
	 */
	final class SpallocJob {

		/**
		 * Used for the Hash code.
		 */
		private static final int MAGIC = 0xbadf00d;

		/** The spalloc job ID. */
		private final int id;

		/**
		 * Make a job handle.
		 *
		 * @param jobId
		 *            The ID code of the job.
		 */
		SpallocJob(final int jobId) {
			this.id = jobId;
		}

		/**
		 * Get what machine the job has been allocated to.
		 *
		 * @return The machine descriptor.
		 * @throws IOException
		 *             If anything goes wrong
		 */
		JobMachineInfo getMachineInfo() throws IOException {
			return comms.sendRequest(new GetJobMachineInfoCommand(id),
					JobMachineInfo.class);
		}

		/**
		 * Get the state of the job.
		 *
		 * @return The state descriptor.
		 * @throws IOException
		 *             If anything goes wrong
		 */
		JobState getState() throws IOException {
			return comms.sendRequest(new GetJobStateCommand(id),
					JobState.class);
		}

		/**
		 * Enable or disable notifications about this job's state changes.
		 *
		 * @param enable
		 *            True to turn the notifications on.
		 * @throws IOException
		 *             If anything goes wrong
		 */
		void notify(final boolean enable) throws IOException {
			if (enable) {
				comms.sendRequest(new NotifyJobCommand(id));
			} else {
				comms.sendRequest(new NoNotifyJobCommand(id));
			}
		}

		/**
		 * Keep the job alive (by sending effectively a NOP).
		 *
		 * @throws IOException
		 *             If anything goes wrong
		 */
		void keepAlive() throws IOException {
			comms.sendRequest(new JobKeepAliveCommand(id));
		}

		/**
		 * Destroy the job.
		 *
		 * @throws IOException
		 *             If anything goes wrong
		 */
		void destroy() throws IOException {
			comms.sendRequest(new DestroyJobCommand(id));
		}

		/**
		 * Turn power on or off for a job's boards.
		 *
		 * @param powerOn
		 *            True to turn the boards on, false to turn them off.
		 * @throws IOException
		 *             If anything goes wrong
		 */
		void power(final boolean powerOn) throws IOException {
			if (powerOn) {
				comms.sendRequest(new PowerOnJobBoardsCommand(id));
			} else {
				comms.sendRequest(new PowerOffJobBoardsCommand(id));
			}
		}

		/**
		 * Find one of his job's chips.
		 *
		 * @param chipX
		 *            The x coordinate of the chip
		 * @param chipY
		 *            The y coordinate of the chip
		 * @return The location description
		 * @throws IOException
		 *             If anything goes wrong
		 */
		WhereIs whereIs(final int chipX, final int chipY) throws IOException {
			return comms.sendRequest(new WhereIsCommand(id, chipX, chipY),
					WhereIs.class);
		}

		@Override
		public int hashCode() {
			return id ^ MAGIC;
		}

		@Override
		public boolean equals(final Object o) {
			return (o instanceof SpallocJob job) && (job.id == id);
		}
	}

	/**
	 * Get the machines known to the Spalloc server.
	 *
	 * @return The known machines
	 * @throws IOException If anything goes wrong
	 */
	final Machine[] listMachines() throws IOException {
		return comms.sendRequest(new ListMachinesCommand(), Machine[].class);
	}

	/**
	 * Create a Spalloc job.
	 *
	 * @param nBoards
	 *            The number of boards to ask for
	 * @param jobOwner
	 *            The owner of the job
	 * @return The job handle
	 * @throws IOException
	 *             If anything goes wrong
	 */
	final SpallocJob createJob(final int nBoards, final String jobOwner)
			throws IOException {
		return new SpallocJob(comms.sendRequest(
				new CreateJobCommand(nBoards, owner + ":" + jobOwner),
				Integer.class));
	}

	// ------------------------------ Job ------------------------------

	/**
	 * Update the state of a job and notify any listeners.
	 *
	 * @param job The job to get the state of
	 * @throws IOException If there is an error getting the state
	 */
	private void updateJobState(final SpallocJob job) throws IOException {
		final JobState state;
		synchronized (machineState) {
			logger.debug("Getting state of {}", job.id);
			state = job.getState();
			logger.debug("Job {} is in state {}", job, state.getState());
			machineState.put(job.id, state);
			machineState.notifyAll();
		}

		if (state.getState() == DESTROYED) {
			final var machine = machinesAllocated.remove(job.id);
			if (isNull(machine)) {
				logger.error("Unrecognized job: {}", job);
				return;
			}
			jobByMachine.remove(machine);
		}
	}

	/**
	 * Get the machine of the job.
	 *
	 * @param job The job
	 * @return The machine of the job
	 * @throws IOException If an I/O error occurs
	 */
	private SpinnakerMachine getMachineForJob(final SpallocJob job)
			throws IOException {
		final var info = job.getMachineInfo();
		return new SpinnakerMachine(info.getConnections().get(0).getHostname(),
				MACHINE_VERSION, info.getWidth(), info.getHeight(),
				info.getConnections().size(), null);
	}

	/**
	 * Wait for a job to reach one of a given set of states.
	 *
	 * @param job The job
	 * @param states The states to wait for
	 * @return The state the job has reached
	 * @throws IOException If an I/O error occurs
	 */
	private JobState waitForStates(final SpallocJob job,
			final Integer... states) throws IOException {
		final var set = new HashSet<>(asList(states));
		synchronized (machineState) {
			final var state = job.getState();
			machineState.put(job.id, state);
			while (!machineState.containsKey(job.id)
					|| !set.contains(machineState.get(job.id).getState())) {
				logger.debug("Waiting for job {} to get to one of {}", job.id,
						Arrays.toString(states));
				if (waitfor(machineState)) {
					return null;
				}
			}
			return machineState.get(job.id);
		}
	}

	@Override
	public List<SpinnakerMachine> getMachines() {
		try {
			return Arrays.stream(listMachines())
					.filter(m -> m.getTags().contains(DEFAULT_TAG))
					.map(m -> new SpinnakerMachine(m.getName(), MACHINE_VERSION,
							m.getWidth() * MACHINE_WIDTH_FACTOR,
							m.getHeight() * MACHINE_HEIGHT_FACTOR,
							m.getWidth() * m.getHeight(), null))
					.collect(toList());
		} catch (final IOException e) {
			logger.error("Error getting machines", e);
			return null;
		}
	}

	@Override
	public SpinnakerMachine getNextAvailableMachine(final int nBoards,
			final String jobOwner, final int jobId) {
		SpallocJob job = null;
		SpinnakerMachine machineAllocated = null;

		while (isNull(job) || isNull(machineAllocated)) {
			try {
				job = startJob(nBoards, jobOwner);
				machineAllocated = getMachineForJob(job);
			} catch (final IOException e) {
				logger.error("Error getting machine - retrying", e);
			}
		}

		machinesAllocated.put(job.id, machineAllocated);
		jobByMachine.put(machineAllocated, job);
		return machineAllocated;
	}

	private SpallocJob startJob(final int nBoards, final String jobOwner)
			throws IOException {
		var job = createJob(nBoards, jobOwner);
		logger.debug("Got machine {}, requesting notifications", job.id);
		job.notify(true);
		var state = job.getState();
		synchronized (machineState) {
			machineState.put(job.id, state);
			machineState.notifyAll();
		}
		logger.debug("Notifications for {} are on", job.id);
		state = waitForStates(job, READY, DESTROYED);
		if (state.getState() == DESTROYED) {
			throw new RuntimeException(state.getReason());
		}
		return job;
	}

	@Override
	public void releaseMachine(final SpinnakerMachine machine) {
		final var job = jobByMachine.remove(machine);
		try {
			if (nonNull(job)) {
				stopJob(job);
			}
		} catch (final IOException e) {
			logger.error("Error releasing machine for {}", job.id);
		}
	}

	private void stopJob(final SpallocJob job) throws IOException {
		logger.debug("Turning off notification for {}", job.id);
		job.notify(false);
		logger.debug("Notifications for {} are off", job.id);
		machinesAllocated.remove(job.id);
		synchronized (machineState) {
			machineState.remove(job.id);
		}
		job.destroy();
		logger.debug("Job {} destroyed", job.id);
	}

	@Override
	public boolean isMachineAvailable(final SpinnakerMachine machine) {
		final var job = jobByMachine.get(machine);
		if (isNull(job)) {
			return false;
		}
		logger.debug("Job {} still available", job.id);
		return true;
	}

	@Override
	public boolean waitForMachineStateChange(final SpinnakerMachine machine,
			final int waitTime) {
		final var job = jobByMachine.get(machine);
		if (isNull(job)) {
			return true;
		}

		synchronized (machineState) {
			final var state = machineState.get(job.id);
			waitfor(machineState, waitTime);
			final var newState = machineState.get(job.id);
			return nonNull(newState) && newState.equals(state);
		}
	}

	@Override
	public void setMachinePower(final SpinnakerMachine machine,
			final boolean powerOn) {
		final var job = jobByMachine.get(machine);
		if (isNull(job)) {
			return;
		}
		try {
			job.power(powerOn);
			if (powerOn) {
				logger.debug("Waiting for powered on machine");
				waitForStates(job, READY, DESTROYED);
				logger.debug("Machine ready");
			}
		} catch (IOException e) {
			throw new RuntimeException("Could not change power state", e);
		}
	}

	@Override
	public ChipCoordinates getChipCoordinates(final SpinnakerMachine machine,
			final int x, final int y) {
		final var job = jobByMachine.get(machine);
		if (isNull(job)) {
			return null;
		}
		try {
			var whereIs = job.whereIs(x, y);
			int[] location = whereIs.getPhysical();
			return new ChipCoordinates(location[0], location[1], location[2]);
		} catch (IOException e) {
			throw new RuntimeException("Error getting coordinates", e);
		}
	}

	/**
	 * Call keep-alive on any active jobs.
	 */
	private void keepAllJobsAlive() {
		List<Integer> jobIds;
		synchronized (machineState) {
			jobIds = new ArrayList<>(machineState.keySet());
		}
		for (final int jobId : jobIds) {
			try {
				new SpallocJob(jobId).keepAlive();
			} catch (final IOException e) {
				logger.error("Error keeping machine {} alive", jobId);
			}
		}
	}

	/**
	 * Update the state of any active jobs.
	 */
	private void updateStateOfJobs() {
		try {
			while (!done) {
				for (final int jobId : comms.getJobsChanged()) {
					try {
						updateJobState(new SpallocJob(jobId));
					} catch (final IOException e) {
						logger.error("Error getting job state", e);
					}
				}
			}
		} catch (final InterruptedException e) {
			logger.warn("interrupt of job state updating");
		}
	}
}
