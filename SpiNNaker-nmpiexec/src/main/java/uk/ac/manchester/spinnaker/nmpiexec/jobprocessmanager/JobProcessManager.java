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
package uk.ac.manchester.spinnaker.nmpiexec.jobprocessmanager;

import static com.fasterxml.jackson.databind.PropertyNamingStrategies.SNAKE_CASE;
import static java.lang.String.format;
import static java.lang.System.exit;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNull;
import static org.apache.commons.io.FileUtils.deleteQuietly;
import static org.eclipse.jgit.util.FileUtils.createTempDir;
import static uk.ac.manchester.spinnaker.nmpiexec.utils.FileDownloader.downloadFile;
import static uk.ac.manchester.spinnaker.nmpiexec.utils.Log.log;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.Timer;

import org.apache.cxf.jaxrs.client.JAXRSClientFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.jakarta.rs.json.JacksonJsonProvider;

import uk.ac.manchester.spinnaker.nmpi.model.job.JobManagerInterface;
import uk.ac.manchester.spinnaker.nmpi.model.job.JobParameters;
import uk.ac.manchester.spinnaker.nmpi.model.job.RemoteStackTrace;
import uk.ac.manchester.spinnaker.nmpi.model.job.nmpi.Job;
import uk.ac.manchester.spinnaker.nmpi.model.job.pynn.PyNNJobParameters;
import uk.ac.manchester.spinnaker.nmpi.model.machine.SpinnakerMachine;
import uk.ac.manchester.spinnaker.nmpiexec.job_parameters.JobParametersFactory;
import uk.ac.manchester.spinnaker.nmpiexec.job_parameters.JobParametersFactoryException;
import uk.ac.manchester.spinnaker.nmpiexec.jobprocess.JobProcess;
import uk.ac.manchester.spinnaker.nmpiexec.jobprocess.JobProcessFactory;
import uk.ac.manchester.spinnaker.nmpiexec.jobprocess.LogWriter;
import uk.ac.manchester.spinnaker.nmpiexec.jobprocess.PyNNJobProcess;

/**
 * Manages a running job process. This is run as a separate process from the
 * command line, and it assumes input is passed via {@link System#in}.
 */
@SpringBootApplication
public class JobProcessManager implements CommandLineRunner {

	/**
	 * The main method.
	 * @param args Program arguments.
	 */
	public static void main(String[] args) {
		SpringApplication.run(JobProcessManager.class, args);
	}

	/**
	 * How to run a Job Process Manager. This is the execution entry point for
	 * this Maven module.
	 *
	 * @param args
	 *            The command line arguments.
	 * @throws IllegalArgumentException
	 *            If an unrecognized argument is found.
	 * @throws IOException
	 *            If an authentication token can't be read.
	 */
	public void run(String... args) throws IOException {
		String serverUrl = null;
		boolean deleteOnExit = false;
		boolean isLocal = false;
		String executerId = null;
		boolean liveUploadOutput = false;
		boolean requestMachine = false;

		for (int i = 0; i < args.length; i++) {
			switch (args[i]) {
			case "--serverUrl" :
				serverUrl = args[++i];
				break;
			case "--executerId" :
				executerId = args[++i];
				break;
			case "--deleteOnExit" :
				deleteOnExit = true;
				break;
			case "--local" :
				isLocal = true;
				break;
			case "--liveUploadOutput" :
				liveUploadOutput = true;
				break;
			case "--requestMachine" :
				requestMachine = true;
				break;
			default :
				throw new IllegalArgumentException(
						"unknown option: " + args[i]);
			}
		}

		new JobProcessRunner(serverUrl, deleteOnExit, isLocal, executerId,
				liveUploadOutput, requestMachine).runJob();
		exit(0);
	}
}

/**
 * Actually runs the job.
 */
class JobProcessRunner {

	/**
	 * The interval at which the log is updated.
	 */
	private static final int UPDATE_INTERVAL = 500;

	/**
	 * The maximum size of cached log messages before a forced send is done.
	 */
	private static final int MAX_LOG_CACHED = 1000000;

	/**
	 * Default parameters for getting a machine.
	 */
	private static final int DEFAULT = -1;

	/**
	 * The factory for converting parameters into processes.
	 */
	private static final JobProcessFactory JOB_PROCESS_FACTORY =
			new JobProcessFactory("JobProcess");

	static {
		JOB_PROCESS_FACTORY.addMapping(PyNNJobParameters.class,
				PyNNJobProcess::new);
	}

	/**
	 * A log writer that uploads to the server.
	 */
	class UploadingJobManagerLogWriter extends JobManagerLogWriter {

		/**
		 * The timer for the interval.
		 */
		private final Timer sendTimer;

		/**
		 * An object to synchronise on when sending data.
		 */
		private final Object sendSync = new Object();

		/**
		 * Make a log writer that uploads the log every half second.
		 */
		UploadingJobManagerLogWriter() {
			sendTimer = new Timer(UPDATE_INTERVAL, e -> sendLog());
		}

		/**
		 * Send the log now if changed.
		 */
		private void sendLog() {
			synchronized (sendSync) {
				String toWrite = null;
				synchronized (this) {
					if (isPopulated()) {
						toWrite = takeCache();
					}
				}
				if (nonNull(toWrite) && !toWrite.isEmpty()) {
					log("Sending cached data to job manager");
					jobManager.appendLog(job.getId(), toWrite);
				}
			}
		}

		@Override
		public void append(final String logMsg) {
			log("Process Output: " + logMsg);
			synchronized (this) {
				sendTimer.restart();
				appendCache(logMsg);
				if (cacheSize() >= MAX_LOG_CACHED) {
					sendLog();
				}
			}
		}

		@Override
		public void stop() {
			synchronized (sendSync) {
				sendTimer.stop();
			}
		}
	}

	/**
	 * The URL of the Job Manager server.
	 */
	private final String serverUrl;

	/**
	 * True if the working directory should be cleaned on exit.
	 */
	private final boolean deleteOnExit;

	/**
	 * True if the process is running on the same machine as the server.
	 */
	private final boolean isLocal;

	/**
	 * The ID of the execution.
	 */
	private final String executerId;

	/**
	 * True if the output should be uploaded as it is produced.
	 */
	private final boolean liveUploadOutput;

	/**
	 * True if a machine should be requested for the job.
	 */
	private final boolean requestMachine;

	/**
	 * The connection to the Job Manager.
	 */
	private JobManagerInterface jobManager;

	/**
	 * The writer of the log.
	 */
	private JobManagerLogWriter logWriter;

	/**
	 * The job being executed.
	 */
	private Job job;

	/**
	 * The ID of the project in which the job exists.
	 */
	private String projectId;

	/**
	 * Create an object that manages the running of a single job.
	 *
	 * @param serverUrlParam
	 *            The URL to the server, used for writing back results.
	 * @param deleteOnExitParam
	 *            Whether to delete the job's resources on termination.
	 * @param isLocalParam
	 *            Whether the job is local.
	 * @param executerIdParam
	 *            The ID of the executer.
	 * @param liveUploadOutputParam
	 *            Whether to do live upload of output data.
	 * @param requestMachineParam
	 *            Whether to request a machine.
	 * @param authTokenParam
	 *            The authorisation token for the server.
	 */
	JobProcessRunner(final String serverUrlParam,
			final boolean deleteOnExitParam, final boolean isLocalParam,
			final String executerIdParam, final boolean liveUploadOutputParam,
			final boolean requestMachineParam) {
		this.serverUrl = requireNonNull(
				serverUrlParam, "--serverUrl must be specified");
		this.executerId = requireNonNull(
				executerIdParam, "--executerId must be specified");
		this.deleteOnExit = deleteOnExitParam;
		this.isLocal = isLocalParam;
		this.liveUploadOutput = liveUploadOutputParam;
		this.requestMachine = requestMachineParam;
	}

	/**
	 * Run a single job.
	 */
	public void runJob() {
		try {
			ObjectMapper mapper = new ObjectMapper();
			mapper.setPropertyNamingStrategy(SNAKE_CASE);
			jobManager = JAXRSClientFactory.create(serverUrl,
					JobManagerInterface.class,
					List.of(new JacksonJsonProvider()));

			// Read the job
			log("Getting job...");
			job = jobManager.getNextJob(executerId);
			projectId = job.getCollab();
			log("Going to run job " + job.getId() + " in collab " + projectId);

			// Create a temporary location for the job
			final var workingDirectory = createTempDir("job", ".tmp", null);
			log("Running in temporary directory " + workingDirectory);

			// Download the setup script
			var downloadUrl = serverUrl + JobManagerInterface.PATH
					+ "/" + JobManagerInterface.SETUP_SCRIPT;
			log("Downloading setup script from " + downloadUrl);
			final var setupScript = downloadFile(downloadUrl,
					workingDirectory, JobManagerInterface.SETUP_SCRIPT);

			final var parameters = getJobParameters(
					workingDirectory, setupScript.getAbsolutePath());

			// Create a process to process the request
			log("Creating process from parameters");
			final var process = JOB_PROCESS_FACTORY.createProcess(parameters);
			logWriter = getLogWriter();

			// Read the machine
			final var machine = getMachine();

			// Execute the process
			log("Running job " + job.getId() + " on " + machine + " using "
					+ parameters.getClass() + " reporting to " + serverUrl);
			process.execute(machine.getURL(), machine.getMachine(), parameters,
					logWriter);
			logWriter.stop();

			// Get the exit status
			processOutcome(workingDirectory, process, logWriter.getLog());
		} catch (final Exception error) {
			log(error);
			reportFailure(error);
			exit(1);
		}
	}

	/**
	 * Report a job failure.
	 *
	 * @param error The error of the failure.
	 */
	private void reportFailure(final Throwable error) {
		if (isNull(jobManager) || isNull(job)) {
			log(error);
			return;
		}

		try {
			var log = "";
			if (nonNull(logWriter)) {
				logWriter.stop();
				log = logWriter.getLog();
			}
			var message = error.getMessage();
			if (isNull(message)) {
				message = "No Error Message";
			}
			jobManager.setJobError(projectId, job.getId(), message, log, "",
					new ArrayList<String>(), new RemoteStackTrace(error));
		} catch (final Throwable t) {
			// Exception while reporting exception...
			log(t);
			log(error);
			exit(2);
		}
	}

	/**
	 * Get a machine for use in the job.
	 *
	 * @return a machine
	 */
	private Machine getMachine() {
		// (get a 3 board machine just now)
		if (requestMachine) {
			return new Machine(jobManager.getJobMachine(job.getId(), DEFAULT,
					DEFAULT, DEFAULT, DEFAULT));
		}
		return new Machine(serverUrl, job.getId());
	}

	/**
	 * Sort out the parameters to a job. Includes downloading any necessary
	 * files.
	 *
	 * @param workingDirectory
	 *            The working directory for the job, used to write files.
	 * @param setupScript
	 *            The setup script
	 * @return Description of the parameters.
	 * @throws IOException
	 *             If anything goes wrong, such as the parameters being
	 *             unreadable or the job being unsupported on the current
	 *             architectural configuration.
	 */
	private JobParameters getJobParameters(final File workingDirectory,
			final String setupScript) throws IOException {
		final var errors = new HashMap<String, JobParametersFactoryException>();
		final var parameters = JobParametersFactory.getJobParameters(
				job, workingDirectory, setupScript, errors);

		if (isNull(parameters)) {
			if (!errors.isEmpty()) {
				throw new JobErrorsException(errors);
			}
			// Miscellaneous other error
			throw new IOException(
					"The job did not appear to be supported on this system");
		}

		// Get any requested input files
		if (nonNull(job.getInputData())) {
			for (final var input : job.getInputData()) {
				downloadFile(input.getUrl(), workingDirectory, null);
			}
		}

		return parameters;
	}

	/**
	 * Get the log writer.
	 *
	 * @return The log writer
	 */
	private JobManagerLogWriter getLogWriter() {
		if (!liveUploadOutput) {
			return new SimpleJobManagerLogWriter();
		}
		return new UploadingJobManagerLogWriter();
	}

	/**
	 * Process the outcome of the job execution.
	 *
	 * @param workingDirectory The directory where the job was run
	 * @param process The process of the job
	 * @param log The log message of the job
	 * @throws IOException If there is an error reading or writing files
	 */
	private void processOutcome(final File workingDirectory,
			final JobProcess<?> process, final String log)
			throws IOException {
		final var status = process.getStatus();
		log("Process has finished with status " + status);

		final var outputs = process.getOutputs();
		final var outputsAsStrings = new ArrayList<String>();
		for (final var output : outputs) {
			if (isLocal) {
				outputsAsStrings.add(output.getAbsolutePath());
			} else {
				try (var input = new FileInputStream(output)) {
					jobManager.addOutput(projectId, job.getId(),
							output.getName(), input);
				}
			}
		}

		for (final var item : process.getProvenance()) {
			jobManager.addProvenance(
				job.getId(), item.getPath(), item.getValue());
		}

		switch (status) {
		case Error :
			final var error = process.getError();
			var message = error.getMessage();
			if (isNull(message)) {
				message = "No Error Message";
			}
			jobManager.setJobError(projectId, job.getId(), message, log,
					workingDirectory.getAbsolutePath(), outputsAsStrings,
					new RemoteStackTrace(error));
			break;
		case Finished :
			jobManager.setJobFinished(projectId, job.getId(), log,
					workingDirectory.getAbsolutePath(), outputsAsStrings);

			// Clean up
			process.cleanup();
			if (deleteOnExit) {
				deleteQuietly(workingDirectory);
			}
			break;
		default :
			throw new IllegalStateException("Unknown status returned!");
		}
	}
}

/**
 * A description of a machine.
 */
class Machine {

	/** The machine. Knows its service URL. */
	private SpinnakerMachine machine;

	/** The service URL. */
	private String url;

	/**
	 * Create a machine known by object.
	 *
	 * @param machineParam
	 *            The machine object.
	 */
	Machine(final SpinnakerMachine machineParam) {
		this.machine = machineParam;
	}

	/**
	 * Create a machine known by service job name.
	 *
	 * @param baseUrl
	 *            The base URL for the machine.
	 * @param id
	 *            The ID for the job.
	 */
	Machine(final String baseUrl, final int id) {
		this.url = format("%sjob/%d/machine", baseUrl, id);
	}

	/**
	 * Get the machine.
	 *
	 * @return the machine
	 */
	SpinnakerMachine getMachine() {
		return machine;
	}

	/**
	 * Get the URL.
	 *
	 * @return the URL
	 */
	String getURL() {
		return url;
	}

	@Override
	public String toString() {
		if (nonNull(machine)) {
			return machine.toString();
		}
		return url;
	}
}

/**
 * How to write to the log.
 */
abstract class JobManagerLogWriter implements LogWriter {

	/**
	 * The cached message.
	 */
	private final StringBuilder cached = new StringBuilder();

	/**
	 * Does the log have anything in it?
	 *
	 * @return Whether the log cache is non-empty.
	 */
	protected synchronized boolean isPopulated() {
		return cached.length() > 0;
	}

	/**
	 * Adds a message to the cache.
	 *
	 * @param message The message to add
	 */
	protected synchronized void appendCache(final String message) {
		cached.append(message);
	}

	/**
	 * Get the current contents of the log cache.
	 *
	 * @return Copy of the log contents.
	 */
	public synchronized String getLog() {
		return cached.toString();
	}

	/**
	 * Get the current log contents and reset the internal buffer.
	 *
	 * @return The contents of the log prior to this call.
	 */
	public final synchronized String takeCache() {
		try {
			return cached.toString();
		} finally {
			cached.setLength(0);
		}
	}

	/**
	 * Get the number of characters in the current log.
	 *
	 * @return The number of characters in the log.
	 */
	public final int cacheSize() {
		return cached.length();
	}

	/**
	 * Stop any background activity associated with the log.
	 */
	void stop() {
	}
}

/**
 * A simple log implementation.
 */
class SimpleJobManagerLogWriter extends JobManagerLogWriter {
	@Override
	public void append(final String logMsg) {
		log("Process Output: " + logMsg);
		synchronized (this) {
			appendCache(logMsg);
		}
	}
}

/**
 * Exception indicating errors with the job factory.
 */
@SuppressWarnings("serial")
class JobErrorsException extends IOException {

	/**
	 * The error message of the exception.
	 */
	private static final String MAIN_MSG = "The job type was recognised"
			+ " by at least one factory, but could not be decoded.  The"
			+ " errors are as follows:";

	/**
	 * Builds an error message from a map of errors.
	 *
	 * @param errors The errors to use.
	 * @return An exception containing the errors.
	 */
	private static String
			buildMessage(final Map<String, ? extends Exception> errors) {
		var buffer = new StringWriter();
		var bufferWriter = new PrintWriter(buffer);
		bufferWriter.println(MAIN_MSG);
		for (final var key : errors.keySet()) {
			bufferWriter.print(key);
			bufferWriter.println(":");
			errors.get(key).printStackTrace(bufferWriter);
			bufferWriter.println();
		}
		return buffer.toString();
	}

	/**
	 * Creates an exception from a set of errors.
	 *
	 * @param errors The errors to build the exception from
	 */
	JobErrorsException(
			final Map<String, JobParametersFactoryException> errors) {
		super(buildMessage(errors));
	}
}
