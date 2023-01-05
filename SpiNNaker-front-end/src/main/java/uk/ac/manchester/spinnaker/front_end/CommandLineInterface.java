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

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.slf4j.LoggerFactory.getLogger;
import static picocli.CommandLine.ExitCode.SOFTWARE;
import static uk.ac.manchester.spinnaker.alloc.client.SpallocClientFactory.getJobFromProxyInfo;
import static uk.ac.manchester.spinnaker.front_end.Constants.PARALLEL_SIZE;
import static uk.ac.manchester.spinnaker.front_end.LogControl.setLoggerDir;
import static uk.ac.manchester.spinnaker.front_end.ParamDescriptions.GATHER;
import static uk.ac.manchester.spinnaker.front_end.ParamDescriptions.MACHINE;
import static uk.ac.manchester.spinnaker.front_end.ParamDescriptions.MAP;
import static uk.ac.manchester.spinnaker.front_end.ParamDescriptions.PLACEMENT;
import static uk.ac.manchester.spinnaker.front_end.ParamDescriptions.REPORT;
import static uk.ac.manchester.spinnaker.front_end.ParamDescriptions.RUN;
import static uk.ac.manchester.spinnaker.machine.bean.MapperFactory.createMapper;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.ExecutionException;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.errorprone.annotations.MustBeClosed;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.ITypeConverter;
import picocli.CommandLine.Parameters;
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
// NB: Must match with build/finalName in pom.xml
@Command(name = "java -jar spinnaker-exe.jar", //
		subcommands = CommandLine.HelpCommand.class)
public final class CommandLineInterface {
	private CommandLineInterface() {
	}

	private static final String VERSION;

	private static final ObjectMapper MAPPER = createMapper();

	private static final String BUFFER_DB_FILE = "buffer.sqlite3";

	private static final String DSE_DB_FILE = "ds.sqlite3";

	private static final int MISBUILT_EXIT_CODE = 3;

	static {
		var cls = CommandLineInterface.class;
		var prop = new Properties();
		try {
			prop.load(cls.getResourceAsStream("command-line.properties"));
		} catch (IOException | NullPointerException e) {
			getLogger(cls).error("failed to read properties", e);
			System.exit(MISBUILT_EXIT_CODE);
		}
		VERSION = prop.getProperty("version");
	}

	@Command(name = "gather", description = "Retrieve recording "
			+ "regions using the fast data movement protocol. Requires system "
			+ "cores to be fully configured.")
	private void gather(//
			@Parameters(description = GATHER, //
					converter = GatherersReader.class) List<Gather> gatherFile,
			@Parameters(description = MACHINE, //
					converter = MachineReader.class) Machine machineFile,
			@Parameters(description = RUN) File runFolder) throws Exception {
		setLoggerDir(runFolder);
		gatherRun(gatherFile, machineFile, runFolder);
	}

	@Command(name = "download", description = "Retrieve recording "
			+ "regions using classic SpiNNaker control protocol transfers.")
	private void download(//
			@Parameters(description = PLACEMENT, //
					converter = PlacementsReader.class) //
			List<Placement> placementFile, //
			@Parameters(description = MACHINE, //
					converter = MachineReader.class) Machine machineFile,
			@Parameters(description = RUN) File runFolder) throws Exception {
		setLoggerDir(runFolder);
		downloadRun(placementFile, machineFile, runFolder);
	}

	@Command(name = "dse", description = "Evaluate data "
			+ "specifications for all cores and upload the results to "
			+ "SpiNNaker using the classic protocol.")
	private void dseAllCores(//
			@Parameters(description = MACHINE, //
					converter = MachineReader.class) Machine machineFile,
			@Parameters(description = RUN) File runFolder) throws Exception {
		setLoggerDir(runFolder);
		dseRun(machineFile, runFolder, null);
	}

	@Command(name = "dse_sys", description = "Evaluate data "
			+ "specifications for system cores and upload the results to "
			+ "SpiNNaker (always uses the classic protocol).")
	private void dseSystemCores(//
			@Parameters(description = MACHINE, //
					converter = MachineReader.class) Machine machineFile,
			@Parameters(description = RUN) File runFolder) throws Exception {
		setLoggerDir(runFolder);
		dseRun(machineFile, runFolder, false);
	}

	@Command(name = "dse_app", description = "Evaluate data "
			+ "specifications for application cores and upload the results to "
			+ "SpiNNaker using the classic protocol.")
	private void dseApplicationCores(//
			@Parameters(description = MACHINE, //
					converter = MachineReader.class) Machine machineFile,
			@Parameters(description = RUN) File runFolder) throws Exception {
		setLoggerDir(runFolder);
		dseRun(machineFile, runFolder, true);
	}

	@Command(name = "dse_app_mon", description = "Evaluate data "
			+ "specifications for application cores and upload the results to "
			+ "SpiNNaker using the fast data upload protocol. Requires system "
			+ "cores to be fully configured.")
	private void dseApplicationCoresViaMonitors(
			@Parameters(description = GATHER, //
					converter = GatherersReader.class) List<Gather> gatherFile,
			@Parameters(description = MACHINE, //
					converter = MachineReader.class) Machine machineFile,
			@Parameters(description = RUN) File runFolder,
			@Parameters(description = REPORT, arity = "0..1") Optional<
					File> reportFolder)
			throws Exception {
		setLoggerDir(runFolder);
		dseAppMonRun(gatherFile, machineFile, runFolder,
				reportFolder.orElse(null));
	}

	@Command(name = "iobuf", description = "Download the contents "
			+ "of the IOBUF buffers and process them.")
	private void readIobufs(//
			@Parameters(description = MACHINE, //
					converter = MachineReader.class) Machine machineFile,
			@Parameters(description = MAP, //
					converter = IobufRequestReader.class) //
			IobufRequest iobufMapFile,
			@Parameters(description = RUN) File runFolder) throws Exception {
		setLoggerDir(runFolder);
		iobufRun(machineFile, iobufMapFile, runFolder);
	}

	@Command(name = "listen_for_unbooted", description = "Listen for unbooted "
			+ "SpiNNaker boards on the local network. Depends on "
			+ "receiving broadcast UDP messages.")
	private void listen() throws IOException {
		LocateConnectedMachineIPAddress.main();
	}

	@Command(name = "version", description = "Print the software version.")
	private void version() {
		System.out.println(VERSION);
	}

	/**
	 * The main command line interface. Dispatches to other classes based on the
	 * first argument, which is a command word.
	 *
	 * @param args
	 *            The command line arguments.
	 */
	public static void main(String... args) {
		var cmd = new CommandLine(new CommandLineInterface());
		cmd.setExecutionExceptionHandler((ex, commandLine, parseResult) -> {
			ex.printStackTrace(commandLine.getErr());
			return SOFTWARE;
		});
		if (args.length == 0) {
			cmd.usage(cmd.getErr());
		} else {
			cmd.execute(args);
		}
	}

	/**
	 * Run the data specifications in parallel.
	 *
	 * @param machine
	 *            Description of overall machine.
	 * @param runFolder
	 *            Directory containing per-run information (i.e., the database
	 *            that holds the data specifications to execute).
	 * @param filterSystemCores
	 *            If {@code true}, only run the DSE for application vertices. If
	 *            {@code false}, only run the DSE for system vertices. If
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
	public static void dseRun(Machine machine, File runFolder,
			Boolean filterSystemCores) throws IOException, SpinnmanException,
			StorageException, ExecutionException, InterruptedException,
			DataSpecificationException, URISyntaxException {
		var db = getDataSpecDB(runFolder);

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
	 * @param gatherers
	 *            List of descriptions of gatherers.
	 * @param machine
	 *            Description of overall machine.
	 * @param runFolder
	 *            Directory containing per-run information (i.e., the database
	 *            that holds the data specifications to execute).
	 * @param reportDir
	 *            Directory containing reports. If {@code null}, no report will
	 *            be written.
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
	public static void dseAppMonRun(List<Gather> gatherers, Machine machine,
			File runFolder, File reportDir)
			throws IOException, SpinnmanException, StorageException,
			ExecutionException, InterruptedException,
			DataSpecificationException, URISyntaxException {
		var db = getDataSpecDB(runFolder);

		try (var dseExec = new FastExecuteDataSpecification(machine, gatherers,
				reportDir, db)) {
			dseExec.loadCores();
		}
	}

	/**
	 * Retrieve IOBUFs in parallel.
	 *
	 * @param machine
	 *            Description of overall machine.
	 * @param request
	 *            Mapping from APLX executable names (full paths) to what cores
	 *            are running those executables, and which we will download
	 *            IOBUFs for.
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
	public static void iobufRun(Machine machine, IobufRequest request,
			File runFolder) throws IOException, SpinnmanException,
			InterruptedException, StorageException, URISyntaxException {
		var db = getBufferManagerDB(runFolder);
		var job = getJob(db);

		try (var txrx = getTransceiver(machine, job);
				var r = new IobufRetriever(txrx, machine, PARALLEL_SIZE)) {
			var result = r.retrieveIobufContents(request, runFolder);
			MAPPER.writeValue(System.out, result);
		}
	}

	/**
	 * Download data without using data gatherer cores.
	 *
	 * @param placements
	 *            List of descriptions of binary placements.
	 * @param machine
	 *            Description of overall machine.
	 * @param runFolder
	 *            Directory containing per-run information (i.e., the database
	 *            that receives the output).
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
	public static void downloadRun(List<Placement> placements, Machine machine,
			File runFolder) throws IOException, SpinnmanException,
			StorageException, InterruptedException, URISyntaxException {
		var db = getBufferManagerDB(runFolder);
		var job = getJob(db);

		try (var trans = getTransceiver(machine, job)) {
			var r = new DataReceiver(trans, machine, db);
			r.getDataForPlacementsParallel(placements, PARALLEL_SIZE);
		}
	}

	/**
	 * Download data using data gatherer cores.
	 *
	 * @param gatherers
	 *            List of descriptions of gatherers.
	 * @param machine
	 *            Description of overall machine.
	 * @param runFolder
	 *            Directory containing per-run information (i.e., the database
	 *            that receives the output).
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
	public static void gatherRun(List<Gather> gatherers, Machine machine,
			File runFolder) throws IOException, SpinnmanException,
			StorageException, InterruptedException, URISyntaxException {
		var db = getBufferManagerDB(runFolder);
		var job = getJob(db);

		try (var trans = getTransceiver(machine, job);
				var r = new RecordingRegionDataGatherer(trans, machine, db,
						job)) {
			int misses = r.gather(gatherers);
			getLogger(CommandLineInterface.class).info("total misses: {}",
					misses);
		}
	}

	private static class MachineReader implements ITypeConverter<Machine> {
		@Override
		public Machine convert(String filename) throws IOException {
			try (var reader = new FileReader(filename, UTF_8)) {
				return new Machine(MAPPER.readValue(reader, MachineBean.class));
			}
		}
	}

	private static class IobufRequestReader
			implements ITypeConverter<IobufRequest> {
		@Override
		public IobufRequest convert(String filename) throws IOException {
			try (var reader = new FileReader(filename, UTF_8)) {
				return MAPPER.readValue(reader, IobufRequest.class);
			}
		}
	}

	private static class GatherersReader
			implements ITypeConverter<List<Gather>> {
		@Override
		public List<Gather> convert(String filename) throws IOException {
			try (var reader = new FileReader(filename, UTF_8)) {
				return MAPPER.readValue(reader, Gather.LIST);
			}
		}
	}

	private static class PlacementsReader
			implements ITypeConverter<List<Placement>> {
		@Override
		public List<Placement> convert(String filename) throws IOException {
			try (var reader = new FileReader(filename, UTF_8)) {
				return MAPPER.readValue(reader, Placement.LIST);
			}
		}
	}

	private static DSEDatabaseEngine getDataSpecDB(File runFolder) {
		return new DSEDatabaseEngine(new File(runFolder, DSE_DB_FILE));
	}

	private static BufferManagerStorage getBufferManagerDB(File runFolder) {
		return new BufferManagerDatabaseEngine(
				new File(runFolder, BUFFER_DB_FILE)).getStorageInterface();
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
 * The descriptions of various parameters to commands, many of which are shared.
 *
 * @author Donal Fellows
 */
interface ParamDescriptions {
	/** Description of {@code gatherFile} parameter. */
	String GATHER = "The name of the gatherer description JSON file.";

	/** Description of {@code machineFile} parameter. */
	String MACHINE = "The name of the machine description JSON file.";

	/** Description of {@code iobufMapFile} parameter. */
	String MAP = "The name of the IOBUF map description file.";

	/** Description of {@code placementFile} parameter. */
	String PLACEMENT = "The name of the placement description JSON file.";

	/** Description of {@code reportFolder} parameter. */
	String REPORT = "The name of the run's reporting folder. "
			+ "If not provided, no report will be written.";

	/** Description of {@code runFolder} parameter. */
	String RUN = "The name of the run data folder.";
}
