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
package uk.ac.manchester.spinnaker.nmpiexec.jobprocess;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNull;
import static java.util.concurrent.TimeUnit.HOURS;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.apache.commons.io.FileUtils.listFiles;
import static org.apache.commons.io.FilenameUtils.getExtension;
import static org.apache.commons.io.IOUtils.buffer;
import static org.apache.commons.io.IOUtils.closeQuietly;
import static org.apache.commons.io.IOUtils.copy;
import static uk.ac.manchester.spinnaker.nmpi.model.job.Status.Error;
import static uk.ac.manchester.spinnaker.nmpi.model.job.Status.Finished;
import static uk.ac.manchester.spinnaker.nmpi.model.job.Status.Running;
import static uk.ac.manchester.spinnaker.nmpiexec.utils.Log.log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.commons.io.filefilter.AbstractFileFilter;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.ini4j.Ini;
import org.ini4j.Profile.Section;

import com.fasterxml.jackson.databind.ObjectMapper;

import uk.ac.manchester.spinnaker.nmpi.model.job.Status;
import uk.ac.manchester.spinnaker.nmpi.model.job.pynn.PyNNJobParameters;
import uk.ac.manchester.spinnaker.nmpi.model.machine.SpinnakerMachine;
import uk.ac.manchester.spinnaker.utils.ThreadUtils;

/**
 * A process for running PyNN jobs.
 */
public class PyNNJobProcess implements JobProcess<PyNNJobParameters> {
	/** The error level that represents a signal. */
	private static final int MIN_SIGNAL_OFFSET = 128;

	/** The section of the config where the machine is contained. */
	private static final String SECTION = "Machine";

	/** The command to call to run the process. */
	private static final String SUBPROCESS_RUNNER = "python";

	/** The command to call to run the setup process. */
	private static final String SETUP_RUNNER = "bash";

	/** The time to wait for the process to finish. */
	private static final int FINALIZATION_DELAY = 1000;

	/** The set of ignored file extensions in the outputs. */
	private static final Set<String> IGNORED_EXTENSIONS = Set.of("pyc");

	/** The set of ignored directories in the outputs. */
	private static final Set<String> IGNORED_DIRECTORIES =
			Set.of("application_generated_data_files", "reports");

	/** The timeout for running jobs, in <em>hours.</em> */
	private static final int RUN_TIMEOUT = 7 * 24;

	/** The parameter to request a change in the timeout (also in hours). */
	private static final String TIMEOUT_PARAMETER = "timeout";

	/** A pattern for finding arguments of the command to execute. */
	private static final Pattern ARGUMENT_FINDER =
			Pattern.compile("([^\"]\\S*|\".+?\")\\s*");

	/** The directory where the process is executed. */
	private File workingDirectory = null;

	/** The current status of the process. */
	private Status status = null;

	/** Any error that the process has exited with. */
	private Throwable error = null;

	/** Output files from the process. */
	private final List<File> outputs = new ArrayList<>();

	/** Provenance items of the process. */
	private final List<ProvenanceItem> provenance = new ArrayList<>();

	/** A thread group for the log monitoring. */
	private ThreadGroup threadGroup;

	/**
	 * Gathers files in a directory and sub-directories.
	 *
	 * @param directory
	 *            The directory to find files in.
	 * @return The set of files found.
	 */
	private static Set<File> gatherFiles(File directory) {
		return new LinkedHashSet<>(
				listFiles(directory, fileFilter(), directoryFilter()));
	}

	/**
	 * A filter to remove files with ignored instructions.
	 *
	 * @return The file filter
	 */
	private static IOFileFilter fileFilter() {
		return new AbstractFileFilter() {
			@Override
			public boolean accept(File file) {
				return !IGNORED_EXTENSIONS
						.contains(getExtension(file.getName()));
			}
		};
	}

	/**
	 * A filter to remove ignored directories.
	 *
	 * @return The directory filter
	 */
	private static IOFileFilter directoryFilter() {
		return new AbstractFileFilter() {
			@Override
			public boolean accept(File file) {
				return !IGNORED_DIRECTORIES.contains(file.getName());
			}
		};
	}

	/**
	 * Executes the process.
	 */
	@Override
	public void execute(String machineUrl, SpinnakerMachine machine,
			PyNNJobParameters parameters, LogWriter logWriter) {
		try {
			status = Running;
			workingDirectory = new File(parameters.getWorkingDirectory());

			// Run the setup
			int setupValue = runSetup(parameters, logWriter);
			if (setupValue != 0) {
				throw new Exception("Setup exited with non-zero error code + ("
						+ setupValue + ")");
			}

			// Create a spynnaker config file
			var cfgFile = new File(workingDirectory, "spynnaker.cfg");

			// Add the details of the machine
			var ini = new Ini();
			var config = ini.getConfig();
			config.setEscape(false);
			config.setLowerCaseSection(false);
			config.setLowerCaseOption(false);
			if (cfgFile.exists()) {
				ini.load(cfgFile);
			}

			Section section;
			if (!ini.containsKey(SECTION)) {
				section = ini.add(SECTION);
			} else {
				section = ini.get(SECTION);
			}
			if (nonNull(machine)) {
				section.put("machine_name", machine.getMachineName());
				section.put("version", machine.getVersion());
				var bmpDetails = machine.getBmpDetails();
				if (nonNull(bmpDetails)) {
					section.put("bmp_names", bmpDetails);
				}
			} else {
				section.put("remote_spinnaker_url", machineUrl);
			}
			ini.store(cfgFile);

			// Keep existing files to compare to later
			var existingFiles = gatherFiles(workingDirectory);

			// Get a lifetime if there is one
			var hwConfig = parameters.getHardwareConfiguration();
			int lifetimeHours = RUN_TIMEOUT;
			if (hwConfig != null) {
				lifetimeHours = (Integer) hwConfig.getOrDefault(
						TIMEOUT_PARAMETER, RUN_TIMEOUT);
			}

			// Execute the program
			int exitValue = runSubprocess(
					parameters, logWriter, lifetimeHours);

			// Get the provenance data
			gatherProvenance(workingDirectory);

			// Get any output files
			var allFiles = gatherFiles(workingDirectory);
			for (var file : allFiles) {
				if (!existingFiles.contains(file)) {
					outputs.add(file);
				}
			}

			// If the exit is an error, mark an error
			if (exitValue >= MIN_SIGNAL_OFFSET) {
				// Useful to distinguish this case
				throw new Exception("Python exited with signal ("
						+ (exitValue - MIN_SIGNAL_OFFSET) + ")");
			}
			if (exitValue != 0) {
				throw new Exception("Python exited with a non-zero code ("
						+ exitValue + ")");
			}
			status = Finished;
		} catch (Throwable e) {
			var stringWriter = new StringWriter();
			var printWriter = new PrintWriter(stringWriter);
			e.printStackTrace(printWriter);
			logWriter.append(stringWriter.toString());
			e.printStackTrace();
			error = e;
			status = Error;
		}
	}

	/**
	 * Run the setup process.
	 *
	 * @param parameters
	 *            The parameters to the setup process.
	 * @param logWriter
	 *            Where to send log messages.
	 * @return The exit value of the process
	 * @throws IOException
	 *             If there was an error starting the process
	 * @throws InterruptedException
	 *             If the process was interrupted before return
	 */
	private int runSetup(PyNNJobParameters parameters, LogWriter logWriter)
			throws IOException, InterruptedException {
		var command = new ArrayList<String>();
		command.add(SETUP_RUNNER);
		command.add(parameters.getSetupScript());

		// Build a process
		var builder = new ProcessBuilder(command);
		builder.directory(workingDirectory);
		builder.redirectErrorStream(true);
		var mapper = new ObjectMapper();
		var hardwareConfig = parameters.getHardwareConfiguration();
		if (hardwareConfig != null) {
			for (var entry: hardwareConfig.entrySet()) {
				String stringValue = null;
				var value = entry.getValue();
				if (value instanceof String sv) {
					stringValue = sv;
				} else {
					stringValue = mapper.writeValueAsString(value);
				}
				builder.environment().put(entry.getKey(), stringValue);
			}
		}
		var process = builder.start();

		// Run a thread to gather the log
		try (var logger =
				new ReaderLogWriter(process.getInputStream(), logWriter)) {
			logger.start();

			// Wait for the process to finish; 1 hour is very generous!
			return runProcess(process, 1, HOURS);
		}
	}

	/**
	 * How to actually run a subprocess.
	 *
	 * @param parameters
	 *            The parameters to the subprocess.
	 * @param logWriter
	 *            Where to send log messages.
	 * @param lifetime
	 *            How long to wait for the subprocess to run, in hours.
	 * @return The exit value of the process
	 * @throws IOException
	 *             If there was an error starting the process
	 * @throws InterruptedException
	 *             If the process was interrupted before return
	 */
	private int runSubprocess(PyNNJobParameters parameters, LogWriter logWriter,
			int lifetime) throws IOException, InterruptedException {
		var command = new ArrayList<String>();
		command.add(SUBPROCESS_RUNNER);

		var scriptMatcher = ARGUMENT_FINDER.matcher(parameters.getUserScript());
		while (scriptMatcher.find()) {
			command.add(
					scriptMatcher.group(1).replace("{system}", "spiNNaker"));
		}

		var builder = new ProcessBuilder(command);
		log("Running " + command + " in " + workingDirectory);
		builder.directory(workingDirectory);
		builder.redirectErrorStream(true);
		var process = builder.start();

		// Run a thread to gather the log
		try (var logger =
				new ReaderLogWriter(process.getInputStream(), logWriter)) {
			logger.start();

			// Wait for the process to finish
			return runProcess(process, lifetime, HOURS);
		}
	}

	/**
	 * Run a subprocess until timeout or completion (whichever comes first). If
	 * timeout happens, the subprocess will be killed.
	 *
	 * @param process
	 *            The subprocess.
	 * @param lifetime
	 *            How long to wait.
	 * @param lifetimeUnits
	 *            The units for <em>lifetime</em>.
	 * @return The exit code of the subprocess
	 * @throws InterruptedException
	 *             If the process was interrupted before return
	 */
	private static int runProcess(Process process, int lifetime,
			TimeUnit lifetimeUnits) throws InterruptedException {
		if (!process.waitFor(lifetime, lifetimeUnits)) {
			process.destroy();
			if (!process.waitFor(FINALIZATION_DELAY, MILLISECONDS)) {
				process.destroyForcibly();
				Thread.sleep(FINALIZATION_DELAY);
			}
		}
		return process.exitValue();
	}

	/**
	 * Used for creating a ZIP of the provenance.
	 *
	 * @param reportsZip
	 *            Open handle to the ZIP being created.
	 * @param directory
	 *            Where to get provenance data from.
	 * @param path
	 *            The path within the ZIP.
	 * @throws IOException
	 *             If anything goes wrong with I/O.
	 * @throws JAXBException
	 *             If anything goes wrong with deserialisation of the XML.
	 */
	private void zipProvenance(ZipOutputStream reportsZip, File directory,
			String path) throws IOException {
		// Go through the report files and zip them up
		for (var file : directory.listFiles()) {
			if (file.isDirectory()) {
				zipProvenance(reportsZip, file, path + "/" + file.getName());
			} else {
				reportsZip.putNextEntry(
						new ZipEntry(path + "/" + file.getName()));
				try (var in = new FileInputStream(file)) {
					copy(in, reportsZip);
				}
			}
		}
	}

	/**
	 * Gather the provenance information from the job's reports directory.
	 *
	 * @param workingDirectory
	 *            The job's working directory.
	 * @throws IOException
	 *             If anything goes wrong with I/O.
	 * @throws JAXBException
	 *             If anything goes wrong with deserialisation of XML.
	 */
	private void gatherProvenance(File workingDirectory)
			throws IOException {
		// Find the reports folder
		var reportsFolder = new File(workingDirectory, "reports");
		if (reportsFolder.isDirectory()) {
			// Create a zip file of the reports
			try (var reportsZip = new ZipOutputStream(new FileOutputStream(
					new File(workingDirectory, "reports.zip")))) {
				// Gather items into the reports zip, keeping an eye out for
				// the "provenance data" folder
				zipProvenance(reportsZip, reportsFolder, "reports");
			}
		}
	}

	/**
	 * Get the status of the process.
	 */
	@Override
	public Status getStatus() {
		return status;
	}

	/**
	 * Get the process error.
	 */
	@Override
	public Throwable getError() {
		return error;
	}

	/**
	 * Get the outputs of the process.
	 */
	@Override
	public List<File> getOutputs() {
		return outputs;
	}

	/**
	 * Get the provenance of the process.
	 */
	@Override
	public List<ProvenanceItem> getProvenance() {
		return provenance;
	}

	/**
	 * Clean up the process after exit.
	 */
	@Override
	public void cleanup() {
		// Does Nothing
	}

	/**
	 * Thread for copying a {@link Reader} to a {@link LogWriter}.
	 */
	class ReaderLogWriter extends Thread implements AutoCloseable {
		/** The reader to read from. */
		private final BufferedReader reader;

		/** The writer to write to. */
		private final LogWriter writer;

		/** True when running, False to stop. */
		private boolean running;

		/**
		 * Creates a new ReaderLogWriter with another reader.
		 *
		 * @param reader
		 *            The reader to read from
		 * @param writer
		 *            The writer to write to
		 */
		ReaderLogWriter(Reader reader, LogWriter writer) {
			super(threadGroup, "Reader Log Writer");
			this.reader = buffer(reader);
			this.writer = requireNonNull(writer);
			setDaemon(true);
		}

		/**
		 * Creates a new ReaderLogWriter with an input stream. This will be
		 * treated as a text stream using the system encoding.
		 *
		 * @param input
		 *            The input stream to read from.
		 * @param writer
		 *            The writer to write to.
		 */
		ReaderLogWriter(InputStream input, LogWriter writer) {
			this(new InputStreamReader(input), writer);
		}

		@Override
		public void run() {
			try {
				copyStream();
			} catch (IOException | RuntimeException e) {
				return;
			} finally {
				synchronized (this) {
					running = false;
					notifyAll();
				}
			}
		}

		@Override
		public void start() {
			running = true;
			super.start();
		}

		/**
		 * Perform the copying of the stream.
		 *
		 * @throws IOException
		 *             If there is an error copying
		 */
		private void copyStream() throws IOException {
			while (!interrupted()) {
				var line = reader.readLine();
				if (isNull(line)) {
					return;
				}
				writer.append(line + "\n");
			}
		}

		/**
		 * Closes the reader/writer.
		 */
		@Override
		public void close() {
			log("Waiting for log writer to exit...");

			synchronized (this) {
				try {
					while (running) {
						wait();
					}
				} catch (InterruptedException e) {
					// Does Nothing
				}
			}

			log("Log writer has exited");
			closeQuietly(reader);
			ThreadUtils.sleep(FINALIZATION_DELAY);
		}
	}
}
