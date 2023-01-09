/*
 * Copyright (c) 2018-2019 The University of Manchester
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
package uk.ac.manchester.spinnaker.front_end;

import static java.lang.System.err;
import static java.lang.System.exit;
import static java.lang.System.out;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.slf4j.LoggerFactory.getLogger;
import static uk.ac.manchester.spinnaker.alloc.client.SpallocClientFactory.getJobFromProxyInfo;
import static uk.ac.manchester.spinnaker.front_end.Constants.PARALLEL_SIZE;
import static uk.ac.manchester.spinnaker.front_end.LogControl.setLoggerDir;
import static uk.ac.manchester.spinnaker.machine.bean.MapperFactory.createMapper;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ExecutionException;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.errorprone.annotations.MustBeClosed;

import uk.ac.manchester.spinnaker.alloc.client.SpallocClient;
import uk.ac.manchester.spinnaker.connections.LocateConnectedMachineIPAddress;
import uk.ac.manchester.spinnaker.data_spec.DataSpecificationException;
import uk.ac.manchester.spinnaker.front_end.download.DataReceiver;
import uk.ac.manchester.spinnaker.front_end.download.RecordingRegionDataGatherer;
import uk.ac.manchester.spinnaker.front_end.download.request.Gather;
import uk.ac.manchester.spinnaker.front_end.download.request.Placement;
import uk.ac.manchester.spinnaker.front_end.dse.FastExecuteDataSpecification;
import uk.ac.manchester.spinnaker.front_end.dse.HostExecuteDataSpecification;
import uk.ac.manchester.spinnaker.front_end.iobuf.IobufRequest;
import uk.ac.manchester.spinnaker.front_end.iobuf.IobufRetriever;
import uk.ac.manchester.spinnaker.machine.Machine;
import uk.ac.manchester.spinnaker.machine.bean.MachineBean;
import uk.ac.manchester.spinnaker.storage.BufferManagerDatabaseEngine;
import uk.ac.manchester.spinnaker.storage.BufferManagerStorage;
import uk.ac.manchester.spinnaker.storage.DSEDatabaseEngine;
import uk.ac.manchester.spinnaker.storage.ProxyAwareStorage;
import uk.ac.manchester.spinnaker.storage.StorageException;
import uk.ac.manchester.spinnaker.transceiver.SpinnmanException;
import uk.ac.manchester.spinnaker.transceiver.Transceiver;
import uk.ac.manchester.spinnaker.transceiver.TransceiverInterface;

/**
 * The main command line interface.
 *
 * @author Donal Fellows
 */
public final class CommandLineInterface {
	private CommandLineInterface() {
	}

	private static final String JAR_FILE;

	@SuppressWarnings("unused")
	private static final String MAIN_CLASS;

	private static final String VERSION;

	private static final ObjectMapper MAPPER = createMapper();

	static {
		var cls = CommandLineInterface.class;
		var prop = new Properties();
		try {
			prop.load(cls.getResourceAsStream("command-line.properties"));
		} catch (IOException | NullPointerException e) {
			getLogger(cls).error("failed to read properties", e);
			exit(2);
		}
		JAR_FILE = prop.getProperty("jar");
		MAIN_CLASS = prop.getProperty("mainClass");
		VERSION = prop.getProperty("version");
	}

	private static void wrongArgs(String cmd, String args) {
		err.printf("wrong # args: must be \"java -jar %s %s %s\"\n",
				JAR_FILE, cmd, args);
		exit(1);
	}

	/**
	 * The main command line interface. Dispatches to other classes based on the
	 * first argument, which is a command word.
	 *
	 * @param args
	 *            The command line arguments.
	 */
	public static void main(String... args) {
		if (args.length < 1) {
			err.printf(
					"wrong # args: must be \"java -jar %s <command> ...\"\n",
					JAR_FILE);
			exit(1);
		}
		try {
			switch (args[0]) {
			case CLICommands.GATHER:
				if (args.length != NUM_DOWNLOAD_ARGS) {
					wrongArgs(CLICommands.GATHER,
							"<gatherFile> <machineFile> <runFolder>");
				}
				setLoggerDir(args[FOURTH]);
				gatherRun(args[1], args[2], args[THIRD]);
				exit(0);

			case CLICommands.DOWNLOAD:
				if (args.length != NUM_DOWNLOAD_ARGS) {
					wrongArgs(CLICommands.DOWNLOAD,
							"<placementFile> <machineFile> <runFolder>");
				}
				setLoggerDir(args[FOURTH]);
				downloadRun(args[1], args[2], args[THIRD]);
				exit(0);

			case CLICommands.DSE:
				if (args.length != NUM_DSE_ARGS) {
					wrongArgs(CLICommands.DSE, "<machineFile> <runFolder>");
				}
				setLoggerDir(args[2]);
				dseRun(args[1], args[2], null);
				exit(0);

			case CLICommands.DSE_SYS:
				if (args.length != NUM_DSE_ARGS) {
					wrongArgs(CLICommands.DSE_SYS, "<machineFile> <runFolder>");
				}
				setLoggerDir(args[2]);
				dseRun(args[1], args[2], false);
				exit(0);

			case CLICommands.DSE_APP:
				if (args.length != NUM_DSE_ARGS) {
					wrongArgs(CLICommands.DSE_APP, "<machineFile> <runFolder>");
				}
				setLoggerDir(args[2]);
				dseRun(args[1], args[2], true);
				exit(0);

			case CLICommands.DSE_APP_MON:
				if (args.length != NUM_DSE_APP_MON_ARGS
						&& args.length != NUM_DSE_APP_MON_ARGS + 1) {
					wrongArgs(CLICommands.DSE_APP_MON,
							"<gatherFile> <machineFile> <runFolder> "
									+ "?<reportFolder>?");
				}
				setLoggerDir(args[THIRD]);
				dseAppMonRun(args[1], args[2], args[THIRD],
						args.length > NUM_DSE_APP_MON_ARGS ? args[FOURTH]
								: null);
				exit(0);

			case CLICommands.IOBUF:
				if (args.length != NUM_IOBUF_ARGS) {
					wrongArgs(CLICommands.IOBUF,
							"<machineFile> <iobufMapFile> <runFolder>");
				}
				setLoggerDir(args[THIRD]);
				iobufRun(args[1], args[2], args[THIRD]);
				exit(0);

			case CLICommands.LISTEN:
				LocateConnectedMachineIPAddress.main(args);
				return;

			case CLICommands.VERSION:
				System.out.println(VERSION);
				exit(0);

			default:
				err.printf("unknown command \"%s\": must be one of %s\n",
						args[0], CLICommands.list());
				exit(1);
			}
		} catch (Throwable t) {
			t.printStackTrace(err);
			exit(2);
		}
	}

	private static final int NUM_DOWNLOAD_ARGS = 5;

	private static final int THIRD = 3;

	private static final int FOURTH = 4;

	private static final int NUM_DSE_ARGS = 3;

	private static final int NUM_DSE_APP_MON_ARGS = 4;

	private static final String DSE_DB_FILE = "ds.sqlite3";

	private static final int NUM_IOBUF_ARGS = 4;

	/**
	 * Run the data specifications in parallel.
	 *
	 * @param machineJsonFile
	 *            Name of file containing JSON description of overall machine.
	 * @param runFolder
	 *            Name of directory containing per-run information (i.e., the
	 *            database that holds the data specifications to execute).
	 * @param filterSystemCores
	 *            If {@code true}, only run the DSE for application vertices.
	 *            If {@code false}, only run the DSE for system vertices. If
	 *            {@code null}, run the DSE for all vertices.
	 * @throws IOException
	 *             If the communications fail.
	 * @throws SpinnmanException
	 *             If a BMP is uncontactable or SpiNNaker rejects a message.
	 * @throws StorageException
	 *             If the database is in an illegal state.
	 * @throws ExecutionException
	 *             If there was a problem in the parallel queue.
	 * @throws InterruptedException
	 *             If the wait for everything to complete is interrupted.
	 * @throws DataSpecificationException
	 *             If an invalid data specification file is executed.
	 * @throws URISyntaxException
	 *             If the proxy URI is provided but not valid.
	 */
	public static void dseRun(String machineJsonFile, String runFolder,
			Boolean filterSystemCores) throws IOException, SpinnmanException,
			StorageException, ExecutionException, InterruptedException,
			DataSpecificationException, URISyntaxException {
		var machine = getMachine(machineJsonFile);
		var db = new DSEDatabaseEngine(new File(runFolder, DSE_DB_FILE));

		try (var dseExec = new HostExecuteDataSpecification(machine, db)) {
			if (filterSystemCores == null) {
				dseExec.loadAllCores();
			} else if (filterSystemCores) {
				dseExec.loadApplicationCores();
			} else {
				dseExec.loadSystemCores();
			}
		}
	}

	/**
	 * Run the data specifications in parallel.
	 *
	 * @param gatherersJsonFile
	 *            Name of file containing JSON description of gatherers.
	 * @param machineJsonFile
	 *            Name of file containing JSON description of overall machine.
	 * @param runFolder
	 *            Name of directory containing per-run information (i.e., the
	 *            database that holds the data specifications to execute).
	 * @param reportFolder
	 *            Name of directory containing reports. If {@code null}, no
	 *            report will be written.
	 * @throws IOException
	 *             If the communications fail.
	 * @throws SpinnmanException
	 *             If a BMP is uncontactable or SpiNNaker rejects a message.
	 * @throws StorageException
	 *             If the database is in an illegal state.
	 * @throws ExecutionException
	 *             If there was a problem in the parallel queue.
	 * @throws InterruptedException
	 *             If the wait for everything to complete is interrupted.
	 * @throws DataSpecificationException
	 *             If an invalid data specification file is executed.
	 * @throws URISyntaxException
	 *             If a proxy URI is provided but invalid.
	 */
	public static void dseAppMonRun(String gatherersJsonFile,
			String machineJsonFile, String runFolder, String reportFolder)
			throws IOException, SpinnmanException, StorageException,
			ExecutionException, InterruptedException,
			DataSpecificationException, URISyntaxException {
		var gathers = getGatherers(gatherersJsonFile);
		var machine = getMachine(machineJsonFile);
		var db = new DSEDatabaseEngine(new File(runFolder, DSE_DB_FILE));
		var reportDir = reportFolder == null ? null : new File(reportFolder);

		try (var dseExec = new FastExecuteDataSpecification(
				machine, gathers, reportDir, db)) {
			dseExec.loadCores();
		}
	}

	/**
	 * Retrieve IOBUFs in parallel.
	 *
	 * @param machineJsonFile
	 *            Name of file containing JSON description of overall machine.
	 * @param iobufMapFile
	 *            Name of file containing mapping from APLX executable names
	 *            (full paths) to what cores are running those executables.
	 * @param runFolder
	 *            Name of directory containing per-run information (i.e., the
	 *            database that holds the data specifications to execute).
	 * @throws IOException
	 *             If the communications fail.
	 * @throws SpinnmanException
	 *             If a BMP is uncontactable or SpiNNaker rejects a message.
	 * @throws InterruptedException
	 *             If interrupted (not expected).
	 * @throws URISyntaxException
	 *             If the proxy URI is invalid
	 * @throws StorageException
	 *             If there is an error reading the database
	 */
	public static void iobufRun(String machineJsonFile, String iobufMapFile,
			String runFolder)
			throws IOException, SpinnmanException, InterruptedException,
			StorageException, URISyntaxException {
		var machine = getMachine(machineJsonFile);
		var request = getIobufRequest(iobufMapFile);
		var db = getDatabase(runFolder);
		var job = getJob(db);

		try (var txrx = getTransceiver(machine, job);
				var r = new IobufRetriever(txrx, machine, PARALLEL_SIZE)) {
			var result = r.retrieveIobufContents(request, runFolder);
			MAPPER.writeValue(out, result);
		}
	}

	/**
	 * Download data without using data gatherer cores.
	 *
	 * @param placementsJsonFile
	 *            Name of file containing JSON description of placements.
	 * @param machineJsonFile
	 *            Name of file containing JSON description of overall machine.
	 * @param databasePath
	 *            Path to the database that receives the output.
	 * @throws IOException
	 *             If the communications fail
	 * @throws SpinnmanException
	 *             If a BMP is uncontactable or SpiNNaker rejects a message
	 * @throws StorageException
	 *             If the database is in an illegal state
	 * @throws InterruptedException
	 *             If communications are interrupted.
	 * @throws URISyntaxException
	 *             If the proxy URI is invalid
	 */
	public static void downloadRun(String placementsJsonFile,
			String machineJsonFile, String databasePath) throws IOException,
			SpinnmanException, StorageException, InterruptedException,
			URISyntaxException {
		var placements = getPlacements(placementsJsonFile);
		var machine = getMachine(machineJsonFile);
		var db = getDatabase(databasePath);
		var job = getJob(db);

		try (var trans = getTransceiver(machine, job)) {
			var r = new DataReceiver(trans, machine, db);
			r.getDataForPlacementsParallel(placements, PARALLEL_SIZE);
		}
	}

	/**
	 * Download data using data gatherer cores.
	 *
	 * @param gatherersJsonFile
	 *            Name of file containing JSON description of gatherers.
	 * @param machineJsonFile
	 *            Name of file containing JSON description of overall machine.
	 * @param databasePath
	 *            Path to the database that receives the output.
	 * @throws IOException
	 *             If the communications fail
	 * @throws SpinnmanException
	 *             If a BMP is uncontactable or SpiNNaker rejects a message
	 * @throws StorageException
	 *             If the database is in an illegal state
	 * @throws InterruptedException
	 *             If things are interrupted while waiting for all the downloads
	 *             to be done
	 * @throws URISyntaxException
	 *             If the URI of the proxy is invalid
	 */
	public static void gatherRun(String gatherersJsonFile,
			String machineJsonFile, String  databasePath) throws IOException,
			SpinnmanException, StorageException, InterruptedException,
			URISyntaxException {
		var gathers = getGatherers(gatherersJsonFile);
		var machine = getMachine(machineJsonFile);
		var db = getDatabase(databasePath);
		var job = getJob(db);

		try (var trans = getTransceiver(machine, job);
				var r = new RecordingRegionDataGatherer(
						trans, machine, db, job)) {
			int misses = r.gather(gathers);
			getLogger(CommandLineInterface.class).info("total misses: {}",
					misses);
		}
	}

	private static Machine getMachine(String filename)
			throws JsonParseException, JsonMappingException, IOException {
		try (var machineReader = new FileReader(filename, UTF_8)) {
			return new Machine(
					MAPPER.readValue(machineReader, MachineBean.class));
		}
	}

	private static IobufRequest getIobufRequest(String filename)
			throws IOException {
		try (var gatherReader = new FileReader(filename, UTF_8)) {
			return MAPPER.readValue(gatherReader, IobufRequest.class);
		}
	}

	private static List<Gather> getGatherers(String filename)
			throws IOException {
		try (var gatherReader = new FileReader(filename, UTF_8)) {
			return MAPPER.readValue(gatherReader, Gather.LIST);
		}
	}

	private static List<Placement> getPlacements(String placementsFile)
			throws IOException {
		try (var placementReader = new FileReader(placementsFile, UTF_8)) {
			return MAPPER.readValue(placementReader, Placement.LIST);
		}
	}

	private static BufferManagerStorage getDatabase(String databasePath) {
		return new BufferManagerDatabaseEngine(
				new File(databasePath)).getStorageInterface();
	}

	private static SpallocClient.Job getJob(ProxyAwareStorage storage)
			throws StorageException, IOException {
		return getJobFromProxyInfo(storage.getProxyInformation());
	}

	@MustBeClosed
	private static TransceiverInterface getTransceiver(Machine machine,
			SpallocClient.Job job)
			throws IOException, SpinnmanException, InterruptedException {
		if (job == null) {
			return new Transceiver(machine);
		}
		return job.getTransceiver();
	}
}

/**
 * The names of supported commands.
 *
 * @author Donal Fellows
 */
interface CLICommands {
	/** The fast-data-out download command name. */
	String GATHER = "gather";

	/** The SCP-based download command name. */
	String DOWNLOAD = "download";

	/** The basic DSE command name. */
	String DSE = "dse";

	/** The system DSE command name. */
	String DSE_SYS = "dse_sys";

	/** The application DSE command name. */
	String DSE_APP = "dse_app";

	/** The application DSE (with fast-data-in) command name. */
	String DSE_APP_MON = "dse_app_mon";

	/** The IOBUF-retrieval command name. */
	String IOBUF = "iobuf";

	/** The listen-for-an-unbooted-machine command name. */
	String LISTEN = "listen_for_unbooted";

	/** The version command name. */
	String VERSION = "version";

	/** All the command names. Sorted. */
	List<String> ALL = List.of(DOWNLOAD, DSE, DSE_APP, DSE_APP_MON, DSE_SYS,
			GATHER, IOBUF, LISTEN, VERSION);

	/**
	 * @return A human-readable list of all command names.
	 */
	static String list() {
		var sb = new StringBuilder();
		var sep = "";
		for (var item : ALL) {
			sb.append(sep);
			sep = ", ";
			if (item.equals(VERSION)) {
				sb.append("or ");
			}
			sb.append(item);
		}
		return sb.toString();
	}
}
