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
package uk.ac.manchester.spinnaker.nmpi.jobmanager;

import static java.io.File.createTempFile;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNull;
import static org.apache.commons.io.FileUtils.copyToFile;
import static org.apache.commons.io.FileUtils.forceMkdir;
import static org.apache.commons.io.IOUtils.buffer;
import static org.apache.commons.io.IOUtils.closeQuietly;
import static org.apache.commons.io.IOUtils.copy;
import static org.slf4j.LoggerFactory.getLogger;
import static uk.ac.manchester.spinnaker.nmpi.ThreadUtils.waitfor;
import static uk.ac.manchester.spinnaker.nmpi.model.job.JobManagerInterface.JOB_PROCESS_MANAGER;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import jakarta.annotation.PostConstruct;

import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Value;

/**
 * An executer that runs its subprocesses on the local machine.
 */
public class LocalJobExecuterFactory implements JobExecuterFactory {

	/**
	 * Get the java executable.
	 *
	 * @return The java executable.
	 * @throws IOException If the file can't be instantiated
	 */
	private static File getJavaExec() throws IOException {
		final var binDir = new File(System.getProperty("java.home"), "bin");
		var exec = new File(binDir, "java");
		if (!exec.canExecute()) {
			exec = new File(binDir, "java.exe");
		}
		return exec;
	}

	/**
	 * True if job files should be deleted on exit.
	 */
	@Value("${deleteJobsOnExit}")
	private boolean deleteOnExit;

	/**
	 * True if the job should live upload the logs.
	 */
	@Value("${liveUploadOutput}")
	private boolean liveUploadOutput;

	/**
	 * True if the job should request a SpiNNaker machine.
	 */
	@Value("${requestSpiNNakerMachine}")
	private boolean requestSpiNNakerMachine;

	/**
	 * A thread group for the executor.
	 */
	private final ThreadGroup threadGroup;

	/**
	 * The directory in which the executor should start.
	 */
	private File jobExecuterDirectory = null;

	/**
	 * Logging.
	 */
	private static final Logger logger = getLogger(Executer.class);

	/**
	 * Create a new local executor.
	 */
	public LocalJobExecuterFactory() {
		this.threadGroup = new ThreadGroup("LocalJob");
	}

	/**
	 * Initialise the system state.
	 *
	 * @throws IOException
	 *             if things go wrong.
	 */
	@PostConstruct
	private void installJobExecuter() throws IOException {
		// Find the JobManager resource
		final var jobManagerStream =
				getClass().getResourceAsStream("/" + JOB_PROCESS_MANAGER);
		if (isNull(jobManagerStream)) {
			throw new UnsatisfiedLinkError(
					"/" + JOB_PROCESS_MANAGER + " not found in classpath");
		}

		// Create a temporary folder
		jobExecuterDirectory = createTempFile("jobExecuter", "tmp");
		jobExecuterDirectory.delete();
		jobExecuterDirectory.mkdirs();
		jobExecuterDirectory.deleteOnExit();

		// Extract the JobManager resources
		forceMkdir(jobExecuterDirectory);
		copyToFile(jobManagerStream, new File(jobExecuterDirectory,
				JOB_PROCESS_MANAGER));
	}

	@Override
	public JobExecuter createJobExecuter(final JobManager manager,
			final URL baseUrl) throws IOException {
		final var uuid = UUID.randomUUID().toString();
		final var arguments = new ArrayList<String>();
		arguments.add("--serverUrl");
		arguments.add(requireNonNull(baseUrl).toString());
		arguments.add("--local");
		arguments.add("--executerId");
		arguments.add(uuid);
		if (deleteOnExit) {
			arguments.add("--deleteOnExit");
		}
		if (liveUploadOutput) {
			arguments.add("--liveUploadOutput");
		}
		if (requestSpiNNakerMachine) {
			arguments.add("--requestMachine");
		}

		return new Executer(requireNonNull(manager), arguments, uuid);
	}

	/**
	 * The executer thread.
	 */
	protected class Executer implements JobExecuter {

		/**
		 * The job manager to report to.
		 */
		private final JobManager jobManager;

		/**
		 * The arguments to send to the command line.
		 */
		private final List<String> arguments;

		/**
		 * The ID of the executor.
		 */
		private final String id;

		/**
		 * The java executable to run.
		 */
		private final File javaExec;

		/**
		 * The output log file.
		 */
		private final File outputLog = createTempFile("exec", ".log");

		/**
		 * The executing external process.
		 */
		private Process process;

		/**
		 * Any exception discovered when starting the process.
		 */
		private IOException startException;

		/**
		 * Create a JobExecuter.
		 *
		 * @param jobManagerParam
		 *            The job manager that wanted an executer made.
		 * @param argumentsParam
		 *            The arguments to use
		 * @param idParam
		 *            The id of the executer
		 * @throws IOException
		 *             If there is an error creating the log file
		 */
		Executer(final JobManager jobManagerParam,
				final List<String> argumentsParam,
				final String idParam) throws IOException {
			this.jobManager = jobManagerParam;
			this.arguments = argumentsParam;
			this.id = idParam;
			javaExec = getJavaExec();
		}

		@Override
		public String getExecuterId() {
			return id;
		}

		@Override
		public void startExecuter() {
			new Thread(threadGroup, this::runSubprocess,
					"Executer (" + id + ")").start();
		}

		private void runSubprocess() {
			try (var pipe = startSubprocess(constructArguments())) {
				logger.debug("Waiting for process to finish");
				try {
					process.waitFor();
				} catch (final InterruptedException e) {
					// Do nothing; the thread will terminate shortly
				}
				logger.debug("Process finished, closing pipe");
			}

			reportResult();
		}

		/**
		 * Construct the arguments from the class properties.
		 *
		 * @return The arguments as a list of strings.
		 */
		private List<String> constructArguments() {
			final var command = new ArrayList<String>();
			command.add(javaExec.getAbsolutePath());
			command.add("-jar");
			command.add(JOB_PROCESS_MANAGER);
			for (final var argument : arguments) {
				command.add(argument);
				logger.debug("Argument: {}", argument);
			}
			return command;
		}

		/**
		 * Start executing the process.
		 *
		 * @param command The command and arguments
		 * @return The output of the process as a pipe
		 */
		private JobOutputPipe startSubprocess(final List<String> command) {
			final var builder = new ProcessBuilder(command);
			builder.directory(jobExecuterDirectory);
			logger.debug("Working directory: {}", jobExecuterDirectory);
			builder.redirectErrorStream(true);
			synchronized (this) {
				try {
					logger.debug("Starting execution process");
					process = builder.start();
					logger.debug("Starting pipe from process");
					var pipe = new JobOutputPipe(
							process.getInputStream(),
							new PrintWriter(outputLog));
					pipe.start();
					return pipe;
				} catch (final IOException e) {
					logger.error("Error running external job", e);
					startException = e;
					return null;
				} finally {
					notifyAll();
				}
			}
		}

		/**
		 * Report the results of the job using the log.
		 */
		private void reportResult() {
			var loggedOutput = new StringWriter();
			try (var reader = new FileReader(outputLog)) {
				copy(reader, loggedOutput);
			} catch (final IOException e) {
				logger.warn("problem in reporting log", e);
			}
			jobManager.setExecutorExited(id, loggedOutput.toString());
		}

		/**
		 * Gets an OutputStream which writes to the process stdin.
		 *
		 * @return An OutputStream
		 * @throws IOException If the output stream of the process can't be
		 *     obtained
		 */
		OutputStream getProcessOutputStream() throws IOException {
			synchronized (this) {
				while (isNull(process) && isNull(startException)) {
					waitfor(this);
				}
				if (nonNull(startException)) {
					throw startException;
				}
				return process.getOutputStream();
			}
		}

		/**
		 * Gets the location of the process log file.
		 *
		 * @return The location of the log file
		 */
		File getLogFile() {
			return outputLog;
		}
	}

	/**
	 * The pipe copier.
	 */
	class JobOutputPipe extends Thread implements AutoCloseable {
		/**
		 *  The input to the pipe.
		 */
		private final BufferedReader reader;

		/**
		 * The place where the output should be written.
		 */
		private final PrintWriter writer;

		/**
		 * True to stop execution.
		 */
		private volatile boolean done;

		/**
		 * Connect the input to the output.
		 *
		 * @param input
		 *            Where things are coming from.
		 * @param output
		 *            Where things are going to. This class will close this when
		 *            it is no longer required.
		 */
		JobOutputPipe(final InputStream input, final PrintWriter output) {
			super(threadGroup, "JobOutputPipe");
			reader = buffer(new InputStreamReader(input));
			writer = output;
			done = false;
			setDaemon(true);
		}

		private String readLine() {
			try {
				return reader.readLine();
			} catch (final IOException e) {
				return null;
			}
		}

		@Override
		public void run() {
			try {
				while (!done) {
					var line = readLine();
					if (isNull(line)) {
						break;
					} else if (!line.isEmpty()) {
						logger.debug("{}", line);
						writer.println(line);
					}
				}
			} finally {
				writer.close();
			}
		}

		@Override
		public void close() {
			done = true;
			closeQuietly(reader);
		}
	}
}
