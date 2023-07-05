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
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.xml.transform.stream.StreamSource;

import org.apache.commons.io.filefilter.AbstractFileFilter;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.ini4j.Ini;
import org.ini4j.Profile.Section;

import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import uk.ac.manchester.spinnaker.nmpi.model.job.Status;
import uk.ac.manchester.spinnaker.nmpi.model.job.pynn.PyNNJobParameters;
import uk.ac.manchester.spinnaker.nmpi.model.machine.SpinnakerMachine;
import uk.ac.manchester.spinnaker.nmpiexec.utils.ThreadUtils;

/**
 * A process for running PyNN jobs.
 */
public class PyNNJobProcess implements JobProcess<PyNNJobParameters> {
	/**
	 * The error level that represents a signal.
	 */
	private static final int MIN_SIGNAL_OFFSET = 128;

	/**
	 * The directory containing provenance within the reports.
	 */
	private static final String PROVENANCE_DIRECTORY = "provenance_data";

	/**
	 * The section of the config where the machine is contained.
	 */
	private static final String SECTION = "Machine";

	/**
	 * The command to call to run the process.
	 */
	private static final String SUBPROCESS_RUNNER = "python";

	/**
	 * The command to call to run the setup process.
	 */
	private static final String SETUP_RUNNER = "bash";

	/**
	 * The time to wait for the process to finish.
	 */
	private static final int FINALIZATION_DELAY = 1000;

	/**
	 * The set of ignored file extensions in the outputs.
	 */
	private static final Set<String> IGNORED_EXTENSIONS = new HashSet<>();

	/**
	 * The set of ignored directories in the outputs.
	 */
	private static final Set<String> IGNORED_DIRECTORIES = new HashSet<>();

	/** The timeout for running jobs, in <em>hours.</em> */
	private static final int RUN_TIMEOUT = 7 * 24;

	/** The parameter to request a change in the timeout (also in hours). */
	private static final String TIMEOUT_PARAMETER = "timeout";

	/**
	 * A pattern for finding arguments of the command to execute.
	 */
	private static final Pattern ARGUMENT_FINDER =
			Pattern.compile("([^\"]\\S*|\".+?\")\\s*");

	static {
		IGNORED_EXTENSIONS.add("pyc");
		IGNORED_DIRECTORIES.add("application_generated_data_files");
		IGNORED_DIRECTORIES.add("reports");
	}

	/**
	 * Provenance data items to be added to final provenance data.
	 */
	private static final String[] PROVENANCE_ITEMS_TO_ADD = new String[]{
		"version_data/.*", "router_provenance/total_multi_cast_sent_packets",
		"router_provenance/total_created_packets",
		"router_provenance/total_dropped_packets",
		"router_provenance/total_missed_dropped_packets",
		"router_provenance/total_lost_dropped_packets"};

	/**
	 * The directory where the process is executed.
	 */
	private File workingDirectory = null;

	/**
	 * The current status of the process.
	 */
	private Status status = null;

	/**
	 * Any error that the process has exited with.
	 */
	private Throwable error = null;

	/**
	 * Output files from the process.
	 */
	private final List<File> outputs = new ArrayList<>();

	/**
	 * Provenance items of the process.
	 */
	private final List<ProvenanceItem> provenance = new ArrayList<>();

	/**
	 * A thread group for the log monitoring.
	 */
	private ThreadGroup threadGroup;

	/**
	 * Gathers files in a directory and sub-directories.
	 *
	 * @param directory The directory to find files in.
	 * @return The set of files found.
	 */
	private static Set<File> gatherFiles(final File directory) {
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
			public boolean accept(final File file) {
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
			public boolean accept(final File file) {
				return !IGNORED_DIRECTORIES.contains(file.getName());
			}
		};
	}

	/**
	 * Executes the process.
	 */
	@Override
	public void execute(final String machineUrl, final SpinnakerMachine machine,
			final PyNNJobParameters parameters, final LogWriter logWriter) {
		try {
			status = Running;
			workingDirectory = new File(parameters.getWorkingDirectory());

			// Run the setup
			final int setupValue = runSetup(parameters, logWriter);
			if (setupValue != 0) {
				throw new Exception("Setup exited with non-zero error code + ("
						+ setupValue + ")");
			}

			// Create a spynnaker config file
			final var cfgFile = new File(workingDirectory, "spynnaker.cfg");

			// Add the details of the machine
			final var ini = new Ini();
			final var config = ini.getConfig();
			config.setEscape(false);
			config.setLowerCaseSection(false);
			config.setLowerCaseOption(false);
			if (cfgFile.exists()) {
				ini.load(cfgFile);
			}

			final Section section;
			if (!ini.containsKey(SECTION)) {
				section = ini.add(SECTION);
			} else {
				section = ini.get(SECTION);
			}
			if (nonNull(machine)) {
				section.put("machine_name", machine.getMachineName());
				section.put("version", machine.getVersion());
				final var bmpDetails = machine.getBmpDetails();
				if (nonNull(bmpDetails)) {
					section.put("bmp_names", bmpDetails);
				}
			} else {
				section.put("remote_spinnaker_url", machineUrl);
			}
			ini.store(cfgFile);

			// Keep existing files to compare to later
			final var existingFiles = gatherFiles(workingDirectory);

			// Get a lifetime if there is one
			var hwConfig = parameters.getHardwareConfiguration();
			int lifetimeHours = (Integer) hwConfig.getOrDefault(
					TIMEOUT_PARAMETER, RUN_TIMEOUT);

			// Execute the program
			final int exitValue = runSubprocess(
					parameters, logWriter, lifetimeHours);

			// Get the provenance data
			gatherProvenance(workingDirectory);

			// Get any output files
			final var allFiles = gatherFiles(workingDirectory);
			for (final var file : allFiles) {
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
		} catch (final Throwable e) {
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
	 * @return
	 *            The exit value of the process
	 * @throws IOException
	 *            If there was an error starting the process
	 * @throws InterruptedException
	 *            If the process was interrupted before return
	 */
	private int runSetup(final PyNNJobParameters parameters,
			final LogWriter logWriter)
			throws IOException, InterruptedException {
		final var command = new ArrayList<String>();
		command.add(SETUP_RUNNER);
		command.add(parameters.getSetupScript());

		// Build a process
		final var builder = new ProcessBuilder(command);
		builder.directory(workingDirectory);
		builder.redirectErrorStream(true);
		var mapper = new ObjectMapper();
		for (var entry: parameters.getHardwareConfiguration().entrySet()) {
			String stringValue = null;
			var value = entry.getValue();
			if (value instanceof String) {
				stringValue = (String) value;
			} else {
				stringValue = mapper.writeValueAsString(value);
			}
			builder.environment().put(entry.getKey(), stringValue);
		}
		final var process = builder.start();

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
	private int runSubprocess(final PyNNJobParameters parameters,
			final LogWriter logWriter, final int lifetime)
			throws IOException, InterruptedException {
		final var command = new ArrayList<String>();
		command.add(SUBPROCESS_RUNNER);

		final var scriptMatcher =
				ARGUMENT_FINDER.matcher(parameters.getUserScript());
		while (scriptMatcher.find()) {
			command.add(
					scriptMatcher.group(1).replace("{system}", "spiNNaker"));
		}

		final var builder = new ProcessBuilder(command);
		log("Running " + command + " in " + workingDirectory);
		builder.directory(workingDirectory);
		builder.redirectErrorStream(true);
		final var process = builder.start();

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
	private static int runProcess(final Process process, final int lifetime,
			final TimeUnit lifetimeUnits) throws InterruptedException {
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
	 * Enter some provenance into the provenance map.
	 *
	 * @param items
	 *            The items to insert.
	 * @param path
	 *            Where to insert these items relative to the current node, as a
	 *            string.
	 * @param pathList
	 *            Where to insert these items relative to the current node, as a
	 *            list.
	 */
	private void putProvenanceInMap(final ProvenanceDataItems items,
			final String path, final LinkedList<String> pathList) {

		// Create a path for this level in the tree
		final var myPath = path + items.getName();
		pathList.addLast(items.getName());

		// Add all nested items
		for (final var subItems : items.getProvenanceDataItems()) {
			putProvenanceInMap(subItems, myPath + "/", pathList);
		}

		// Add items from this level
		for (final var subItem : items.getProvenanceDataItem()) {
			final var itemPath = myPath + "/" + subItem.getName();
			pathList.addLast(subItem.getName());
			for (final var item : PROVENANCE_ITEMS_TO_ADD) {
				if (itemPath.matches(item)) {
					provenance.add(new ProvenanceItem(
							new ArrayList<>(pathList), subItem.getValue()));
				}
			}
			pathList.removeLast();
		}
		pathList.removeLast();
	}

	/**
	 * Add the provenance contained in the files in the given directory.
	 *
	 * @param provenanceDirectory
	 *            Where to look for XML files.
	 * @throws JAXBException
	 *             If things go wrong in deserialisation.
	 * @throws IOException
	 *             If anything goes wrong with I/O.
	 */
	private void addProvenance(final File provenanceDirectory)
			throws IOException, JAXBException {

		// Get provenance data from files
		for (final var file : provenanceDirectory.listFiles()) {

			// Only process XML files
			if (file.getName().endsWith(".xml")) {
				final var jaxbContext =
						JAXBContext.newInstance(ProvenanceDataItems.class);
				final var jaxbUnmarshaller =
						jaxbContext.createUnmarshaller();
				final var source = new StreamSource(file);
				final var items = (ProvenanceDataItems) jaxbUnmarshaller
						.unmarshal(source);
				putProvenanceInMap(items, "", new LinkedList<String>());
			}
		}
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
	private void zipProvenance(final ZipOutputStream reportsZip,
			final File directory, final String path)
			throws IOException, JAXBException {

		// Go through the report files and zip them up
		for (final var file : directory.listFiles()) {
			if (file.isDirectory()) {
				zipProvenance(reportsZip, file, path + "/" + file.getName());

				// If the directory is the provenance directory, process it
				if (file.getName().equals(PROVENANCE_DIRECTORY)) {
					addProvenance(file);
				}
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
	 * @param workingDirectoryParam
	 *            The job's working directory.
	 * @throws IOException
	 *             If anything goes wrong with I/O.
	 * @throws JAXBException
	 *             If anything goes wrong with deserialisation of XML.
	 */
	private void gatherProvenance(final File workingDirectoryParam)
			throws IOException, JAXBException {

		// Find the reports folder
		final var reportsFolder = new File(workingDirectoryParam, "reports");
		if (reportsFolder.isDirectory()) {

			// Create a zip file of the reports
			try (var reportsZip = new ZipOutputStream(new FileOutputStream(
					new File(workingDirectoryParam, "reports.zip")))) {
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

		/**
		 * The reader to read from.
		 */
		private final BufferedReader reader;

		/**
		 * The writer to write to.
		 */
		private final LogWriter writer;

		/**
		 * True when running, False to stop.
		 */
		private boolean running;

		/**
		 * Creates a new ReaderLogWriter with another reader.
		 *
		 * @param readerParam
		 *            The reader to read from
		 * @param writerParam
		 *            The writer to write to
		 */
		ReaderLogWriter(
				final Reader readerParam, final LogWriter writerParam) {
			super(threadGroup, "Reader Log Writer");
			this.reader = buffer(readerParam);
			this.writer = requireNonNull(writerParam);
			setDaemon(true);
		}

		/**
		 * Creates a new ReaderLogWriter with an input stream. This will be
		 * treated as a text stream using the system encoding.
		 *
		 * @param input
		 *            The input stream to read from.
		 * @param writerParam
		 *            The writer to write to.
		 */
		ReaderLogWriter(final InputStream input,
				final LogWriter writerParam) {
			this(new InputStreamReader(input), writerParam);
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
		 * @throws IOException If there is an error copying
		 */
		private void copyStream() throws IOException {
			while (!interrupted()) {
				final var line = reader.readLine();
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
				} catch (final InterruptedException e) {
					// Does Nothing
				}
			}

			log("Log writer has exited");
			closeQuietly(reader);
			ThreadUtils.sleep(FINALIZATION_DELAY);
		}
	}
}
