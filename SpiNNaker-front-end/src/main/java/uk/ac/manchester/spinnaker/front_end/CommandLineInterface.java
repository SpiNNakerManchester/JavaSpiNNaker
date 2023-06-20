/*
 * Copyright (c) 2018 The University of Manchester
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
package uk.ac.manchester.spinnaker.front_end;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.stream.Collectors.toList;
import org.slf4j.Logger;
import static org.slf4j.LoggerFactory.getLogger;
import static picocli.CommandLine.ExitCode.USAGE;
import static uk.ac.manchester.spinnaker.alloc.client.SpallocClientFactory.getJobFromProxyInfo;
import static uk.ac.manchester.spinnaker.front_end.CommandDescriptions.DOWNLOAD_DESC;
import static uk.ac.manchester.spinnaker.front_end.CommandDescriptions.DSE_APP_DESC;
import static uk.ac.manchester.spinnaker.front_end.CommandDescriptions.DSE_DESC;
import static uk.ac.manchester.spinnaker.front_end.CommandDescriptions.DSE_MON_DESC;
import static uk.ac.manchester.spinnaker.front_end.CommandDescriptions.DSE_SYS_DESC;
import static uk.ac.manchester.spinnaker.front_end.CommandDescriptions.GATHER_DESC;
import static uk.ac.manchester.spinnaker.front_end.CommandDescriptions.IOBUF_DESC;
import static uk.ac.manchester.spinnaker.front_end.CommandDescriptions.LISTEN_DESC;
import static uk.ac.manchester.spinnaker.front_end.Constants.PARALLEL_SIZE;
import static uk.ac.manchester.spinnaker.front_end.LogControl.setLoggerDir;
import static uk.ac.manchester.spinnaker.front_end.ParamDescriptions.DBFILE;
import static uk.ac.manchester.spinnaker.front_end.ParamDescriptions.DSFILE;
import static uk.ac.manchester.spinnaker.front_end.ParamDescriptions.GATHER;
import static uk.ac.manchester.spinnaker.front_end.ParamDescriptions.MACHINE;
import static uk.ac.manchester.spinnaker.front_end.ParamDescriptions.MAP;
import static uk.ac.manchester.spinnaker.front_end.ParamDescriptions.PLACEMENT;
import static uk.ac.manchester.spinnaker.front_end.ParamDescriptions.REPORT;
import static uk.ac.manchester.spinnaker.front_end.ParamDescriptions.RUN;
import static uk.ac.manchester.spinnaker.machine.bean.MapperFactory.createMapper;
import static uk.ac.manchester.spinnaker.messages.Constants.SCP_SCAMP_PORT;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.ExecutionException;
import java.util.function.Supplier;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.errorprone.annotations.MustBeClosed;

import picocli.CommandLine;
import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.Command;
import picocli.CommandLine.IModelTransformer;
import picocli.CommandLine.ITypeConverter;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.TypeConversionException;
import uk.ac.manchester.spinnaker.alloc.client.SpallocClient;
import uk.ac.manchester.spinnaker.connections.LocateConnectedMachineIPAddress;
import uk.ac.manchester.spinnaker.connections.MachineAware;
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
import uk.ac.manchester.spinnaker.storage.DatabaseEngine;
import uk.ac.manchester.spinnaker.storage.ProxyAwareStorage;
import uk.ac.manchester.spinnaker.storage.StorageException;
import uk.ac.manchester.spinnaker.transceiver.SpinnmanException;
import uk.ac.manchester.spinnaker.transceiver.Transceiver;
import uk.ac.manchester.spinnaker.transceiver.TransceiverInterface;
import uk.ac.manchester.spinnaker.transceiver.Transceiver.ConnectionDescriptor;
import uk.ac.manchester.spinnaker.utils.ValueHolder;

/**
 * The main command line interface.
 *
 * @author Donal Fellows
 */
@Command(subcommands = CommandLine.HelpCommand.class, //
		mixinStandardHelpOptions = true, //
		modelTransformer = CommandLineInterface.BuildPropsLoader.class)
public final class CommandLineInterface {

	private static final Logger log = getLogger(CommandLineInterface.class);

	private CommandLineInterface() {
	}

	private static final ObjectMapper MAPPER = createMapper();

	private static final String PROPS = "command-line.properties";

	static class BuildPropsLoader implements IModelTransformer {
		private Properties loadProps() throws IOException {
			var prop = new Properties();
			try (var clp = getClass().getResourceAsStream(PROPS)) {
				prop.load(clp);
			}
			return prop;
		}

		@Override
		public CommandSpec transform(CommandSpec commandSpec) {
			try {
				var prop = loadProps();
				var jar = prop.getProperty("jar");
				var ver = prop.getProperty("version");

				commandSpec.name("java -jar " + jar);
				commandSpec.version(jar + " version " + ver);
			} catch (IOException e) {
				e.printStackTrace();
				System.exit(USAGE);
			}
			return commandSpec;
		}
	}

	@Command(name = "listen_for_unbooted", description = LISTEN_DESC)
	private void listen() throws IOException {
		LocateConnectedMachineIPAddress.main();
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
		if (args.length == 0) {
			cmd.usage(cmd.getErr());
			System.exit(USAGE);
		} else {
			System.exit(cmd.execute(args));
		}
	}

	static CommandSpec getSpec() {
		return new CommandLine(new CommandLineInterface()).getCommandSpec();
	}

	@Command(name = "dse_sys", description = DSE_SYS_DESC)
	private void dseSystemCores(
			@Mixin MachineParam machine,
			@Mixin DsFileParam dsFile,
			@Mixin RunFolderParam runFolder)
			throws Exception {
		runDSEUploadingViaClassicTransfer(machine.get(), dsFile.get(),
				runFolder.get(), false);
	}

	@Command(name = "dse_app", description = DSE_APP_DESC)
	private void dseApplicationCores(
			@Mixin MachineParam machine,
			@Mixin DsFileParam dsFile,
			@Mixin RunFolderParam runFolder)
			throws Exception {
		runDSEUploadingViaClassicTransfer(machine.get(), dsFile.get(),
				runFolder.get(), true);
	}

	@FunctionalInterface
	interface HostDSEFactory {
		HostExecuteDataSpecification create(TransceiverInterface txrx,
				Machine m, DSEDatabaseEngine db)
				throws IOException, SpinnmanException, StorageException,
				ExecutionException, InterruptedException, URISyntaxException;
	}

	/**
	 * Makes {@link HostExecuteDataSpecification} instances. Allows for
	 * injection of debugging tooling.
	 */
	static HostDSEFactory hostFactory = HostExecuteDataSpecification::new;

	@FunctionalInterface
	interface FastDSEFactory {
		FastExecuteDataSpecification create(TransceiverInterface txrx,
				Machine machine, List<Gather> gatherers, File reportDir,
				DSEDatabaseEngine db)
				throws IOException, SpinnmanException, StorageException,
				ExecutionException, InterruptedException, URISyntaxException;
	}

	/**
	 * Makes {@link FastExecuteDataSpecification} instances. Allows for
	 * injection of debugging tooling.
	 */
	static FastDSEFactory fastFactory = FastExecuteDataSpecification::new;

	/**
	 * Run the data specifications in parallel.
	 *
	 * @param machine
	 *            Description of overall machine
	 * @param dsFile
	 *            Path to the dataspec database
	 * @param runFolder
	 *            Directory containing per-run information.
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
	 * @throws URISyntaxException
	 *             If the proxy URI is provided but not valid.
	 */
	public void runDSEUploadingViaClassicTransfer(Machine machine,
			File dsFile, File runFolder, Boolean filterSystemCores)
			throws IOException, SpinnmanException, StorageException,
			ExecutionException, InterruptedException, URISyntaxException {
		setLoggerDir(runFolder);
		var db = getDataSpecDB(dsFile);
		var job = getJob(db);

		try (var txrx = getTransceiver(machine, job);
				var dseExec = hostFactory.create(txrx, machine, db)) {
			if (filterSystemCores) {
				dseExec.loadCores(false);
			} else {
				dseExec.loadCores(true);
			}
		} catch (Exception ex) {
			log.error("DSE load failed", ex);
			throw ex;
		}
	}

	/**
	 * Run the data specifications in parallel.
	 *
	 * @param gatherers
	 *            List of descriptions of gatherers.
	 * @param machine
	 *            Description of overall machine.
	 * @param dsFile
	 *            Path to the dataspec database
	 * @param runFolder
	 *            Directory containing per-run information.
	 * @param reportFolder
	 *            Directory containing reports. If {@link Optional#empty()}, no
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
	 * @throws URISyntaxException
	 *             If a proxy URI is provided but invalid.
	 */
	@Command(name = "dse_app_mon", description = DSE_MON_DESC)
	public void runDSEForAppCoresUploadingViaMonitorStreaming(
			@Mixin GatherersParam gatherers,
			@Mixin MachineParam machine,
			@Mixin DsFileParam dsFile,
			@Mixin RunFolderParam runFolder,
			@Parameters(description = REPORT, arity = "0..1", index = "3")
			Optional<File> reportFolder)
			throws IOException, SpinnmanException, StorageException,
			ExecutionException, InterruptedException, URISyntaxException {
		setLoggerDir(runFolder.get());
		var db = getDataSpecDB(dsFile.get());
		var job = getJob(db);

		try (var txrx = getTransceiver(machine.get(), job);
				var dseExec = fastFactory.create(txrx, machine.get(),
						gatherers.get(), reportFolder.orElse(null), db)) {
			dseExec.loadCores();
		}
	}

	/**
	 * Retrieve IOBUFs in parallel.
	 *
	 * @param machine
	 *            Description of overall machine.
	 * @param iobuf
	 *            Mapping from APLX executable names (full paths) to what cores
	 *            are running those executables, and which we will download
	 *            IOBUFs for.
	 * @param dbFile
	 *            The database that receives the output).
	 * @param runFolder
	 *            Directory containing per-run information (i.e., where to log).
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
	@Command(name = "iobuf", description = IOBUF_DESC)
	public void retrieveIOBUFs(
			@Mixin MachineParam machine,
			@Mixin IobufMapParam iobuf,
			@Mixin DbFileParam dbFile,
			@Mixin RunFolderParam runFolder)
			throws IOException, SpinnmanException, InterruptedException,
			StorageException, URISyntaxException {
		setLoggerDir(runFolder.get());
		var db = getBufferManagerDB(dbFile.get());
		var job = getJob(db);

		try (var txrx = getTransceiver(machine.get(), job);
				var r = new IobufRetriever(txrx, machine.get(),
						PARALLEL_SIZE)) {
			var result = r.retrieveIobufContents(iobuf.get(), runFolder.get());
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
	 * @param dbFile
	 *            The database that receives the output).
	 * @param runFolder
	 *            Directory containing per-run information (i.e., where to log).
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
	@Command(name = "download", description = DOWNLOAD_DESC)
	public void downloadRecordingChannelsViaClassicTransfer(
			@Mixin PlacementsParam placements,
			@Mixin MachineParam machine,
			@Mixin DbFileParam dbFile,
			@Mixin RunFolderParam runFolder)
			throws IOException, SpinnmanException, StorageException,
			InterruptedException, URISyntaxException {
		setLoggerDir(runFolder.get());
		var db = getBufferManagerDB(dbFile.get());
		var job = getJob(db);

		try (var trans = getTransceiver(machine.get(), job)) {
			var r = new DataReceiver(trans, machine.get(), db);
			r.getDataForPlacementsParallel(placements.get(), PARALLEL_SIZE);
		}
	}

	/**
	 * Download data using data gatherer cores.
	 *
	 * @param gatherers
	 *            List of descriptions of gatherers.
	 * @param machine
	 *            Description of overall machine.
	 * @param dbFile
	 *            The database that receives the output).
	 * @param runFolder
	 *            Directory containing per-run information (i.e., where to log).
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
	@Command(name = "gather", description = GATHER_DESC)
	public void downloadRecordingChannelsViaMonitorStreaming(
			@Mixin GatherersParam gatherers,
			@Mixin MachineParam machine,
			@Mixin DbFileParam dbFile,
			@Mixin RunFolderParam runFolder)
			throws IOException, SpinnmanException, StorageException,
			InterruptedException, URISyntaxException {
		setLoggerDir(runFolder.get());
		var db = getBufferManagerDB(dbFile.get());
		var job = getJob(db);

		try (var trans = getTransceiver(machine.get(), job);
				var r = new RecordingRegionDataGatherer(trans, machine.get(),
						db)) {
			int misses = r.gather(gatherers.get());
			getLogger(CommandLineInterface.class).info("total misses: {}",
					misses);
		}
	}

	/**
	 * Argument handler for the {@code <machineFile>} parameter.
	 * <p>
	 * Do not make instances of this class yourself; leave that to picocli.
	 *
	 * @author Donal Fellows
	 * @see ArgGroup
	 * @see Parameters
	 */
	public static class MachineParam implements Supplier<Machine> {
		/** The machine. */
		@Parameters(paramLabel = "<machineFile>", description = MACHINE, //
				converter = Converter.class)
		private Machine machine;

		/** @hidden */
		public MachineParam() {
		}

		/** @return The machine parsed from the named file. */
		@Override
		public Machine get() {
			return machine;
		}

		static class Converter implements ITypeConverter<Machine> {
			@Override
			public Machine convert(String filename) throws IOException {
				try (var reader = new FileReader(filename, UTF_8)) {
					return new Machine(
							MAPPER.readValue(reader, MachineBean.class));
				}
			}
		}
	}

	/**
	 * Argument handler for the {@code <iobufMapFile>} parameter.
	 * <p>
	 * Do not make instances of this class yourself; leave that to picocli.
	 *
	 * @author Donal Fellows
	 * @see ArgGroup
	 * @see Parameters
	 */
	public static class IobufMapParam implements Supplier<IobufRequest> {
		/** The request. */
		@Parameters(paramLabel = "<iobufMapFile>", description = MAP, //
				converter = Converter.class)
		private IobufRequest request;

		/** @hidden */
		public IobufMapParam() {
		}

		/** @return The request parsed from the named file. */
		@Override
		public IobufRequest get() {
			return request;
		}

		static class Converter implements ITypeConverter<IobufRequest> {
			@Override
			public IobufRequest convert(String filename) throws IOException {
				try (var reader = new FileReader(filename, UTF_8)) {
					return MAPPER.readValue(reader, IobufRequest.class);
				}
			}
		}
	}

	/**
	 * Argument handler for the {@code <gatherFile>} parameter.
	 * <p>
	 * Do not make instances of this class yourself; leave that to picocli.
	 *
	 * @author Donal Fellows
	 * @see ArgGroup
	 * @see Parameters
	 */
	public static class GatherersParam implements Supplier<List<Gather>> {
		/** The gatherers. */
		@Parameters(paramLabel = "<gatherFile>", description = GATHER, //
				converter = Converter.class)
		private Supplier<List<Gather>> gatherers;

		/** @hidden */
		public GatherersParam() {
		}

		/** @return The gatherers parsed from the named file. */
		@Override
		public List<Gather> get() {
			return gatherers.get();
		}

		static class Converter
				implements ITypeConverter<Supplier<List<Gather>>> {
			@Override
			public Supplier<List<Gather>> convert(String filename)
					throws IOException {
				try (var reader = new FileReader(filename, UTF_8)) {
					var g = MAPPER.readValue(reader, Gather.LIST);
					return () -> g;
				}
			}
		}
	}

	/**
	 * Argument handler for the {@code <placementFile>} parameter.
	 * <p>
	 * Do not make instances of this class yourself; leave that to picocli.
	 *
	 * @author Donal Fellows
	 * @see ArgGroup
	 * @see Parameters
	 */
	public static class PlacementsParam implements Supplier<List<Placement>> {
		/** The placements. */
		@Parameters(paramLabel = "<placementFile>", description = PLACEMENT, //
				converter = Converter.class)
		private Supplier<List<Placement>> placements;

		/** @hidden */
		public PlacementsParam() {
		}

		/** @return The placements parsed from the named file. */
		@Override
		public List<Placement> get() {
			return placements.get();
		}

		static class Converter
				implements ITypeConverter<Supplier<List<Placement>>> {
			@Override
			public Supplier<List<Placement>> convert(String filename)
					throws IOException {
				try (var reader = new FileReader(filename, UTF_8)) {
					var p = MAPPER.readValue(reader, Placement.LIST);
					return () -> p;
				}
			}
		}
	}

	/**
	 * Argument handler for the {@code <runFolder>} parameter.
	 * <p>
	 * Do not make instances of this class yourself; leave that to picocli.
	 *
	 * @author Donal Fellows
	 * @see ArgGroup
	 * @see Parameters
	 */
	public static class RunFolderParam implements Supplier<File> {
		@Parameters(description = RUN, converter = Converter.class, arity = "1")
		private ValueHolder<File> runFolder = new ValueHolder<>();

		/** @hidden */
		public RunFolderParam() {
		}

		/** @return The folder for the run. */
		@Override
		public File get() {
			return runFolder.getValue();
		}

		static class Converter implements ITypeConverter<ValueHolder<File>> {
			@Override
			public ValueHolder<File> convert(String filename)
					throws IOException {
				var f = new File(filename);
				if (!f.isDirectory()) {
					throw new TypeConversionException(
							"<runFolder> must be a directory");
				}
				return new ValueHolder<>(f);
			}
		}
	}

	/**
	 * Argument handler for the {@code <dbFile>} parameter.
	 * <p>
	 * Do not make instances of this class yourself; leave that to picocli.
	 *
	 * @author Christian Brenninkmeijer
	 * @see ArgGroup
	 * @see Parameters
	 */
	public static class DbFileParam implements Supplier<File> {
		@Parameters(description = DBFILE, converter = Converter.class,
				arity = "1")
		private ValueHolder<File> dbFile = new ValueHolder<>();

		/** @hidden */
		public DbFileParam() {
		}

		/** @return The file of the buffer database. */
		@Override
		public File get() {
			return dbFile.getValue();
		}

		static class Converter implements ITypeConverter<ValueHolder<File>> {
			@Override
			public ValueHolder<File> convert(String filename)
					throws IOException {
				var f = new File(filename);
				if (!f.isFile()) {
					throw new TypeConversionException(
							"<dbFile> must be a file");
				}
				return new ValueHolder<>(f);
			}
		}
	}

	/**
	 * Argument handler for the {@code <dsFile>} parameter.
	 * <p>
	 * Do not make instances of this class yourself; leave that to picocli.
	 *
	 * @author Christian Brenninkmeijer
	 * @see ArgGroup
	 * @see Parameters
	 */
	public static class DsFileParam implements Supplier<File> {
		@Parameters(description = DSFILE, converter = Converter.class,
				arity = "1")
		private ValueHolder<File> dsFile = new ValueHolder<>();

		/** @hidden */
		public DsFileParam() {
		}

		/** @return The file of the dataspec database. */
		@Override
		public File get() {
			return dsFile.getValue();
		}

		static class Converter implements ITypeConverter<ValueHolder<File>> {
			@Override
			public ValueHolder<File> convert(String filename)
					throws IOException {
				var f = new File(filename);
				if (!f.isFile()) {
					throw new TypeConversionException(
							"<dbFile> must be a file");
				}
				return new ValueHolder<>(f);
			}
		}
	}

	private static DSEDatabaseEngine getDataSpecDB(File dsFile) {
		return new DSEDatabaseEngine(dsFile);
	}

	private static BufferManagerStorage getBufferManagerDB(File dbFile) {
		return new BufferManagerDatabaseEngine(dbFile).getStorageInterface();
	}

	private static SpallocClient.Job getJob(
			DatabaseEngine<? extends ProxyAwareStorage> databaseEngine)
			throws StorageException, IOException {
		return getJob(databaseEngine.getStorageInterface());
	}

	private static SpallocClient.Job getJob(ProxyAwareStorage storage)
			throws StorageException, IOException {
		return getJobFromProxyInfo(storage.getProxyInformation());
	}

	@MustBeClosed
	@SuppressWarnings("MustBeClosed")
	private static TransceiverInterface getTransceiver(Machine machine,
			SpallocClient.Job job)
			throws IOException, SpinnmanException, InterruptedException {
		final TransceiverInterface txrx;
		if (job == null) {
			// No job; must be a direct connection
			txrx = Transceiver.makeWithDescriptors(
					machine.version, generateScampConnections(machine));
		} else {
			txrx = job.getTransceiver();
		}
		var scpSelector = txrx.getScampConnectionSelector();
		if (scpSelector instanceof MachineAware) {
			((MachineAware) scpSelector).setMachine(machine);
		}
		return txrx;
	}

	private static List<ConnectionDescriptor> generateScampConnections(
			Machine machine) {
		return machine.ethernetConnectedChips().stream()
				.map(chip -> new ConnectionDescriptor(chip.ipAddress,
						SCP_SCAMP_PORT, chip.asChipLocation()))
				.collect(toList());
	}
}

/**
 * The descriptions of various commands.
 *
 * @author Donal Fellows
 */
interface CommandDescriptions {
	/** Description of {@code download} command. */
	String DOWNLOAD_DESC = "Retrieve recording regions using "
			+ "classic SpiNNaker control protocol transfers.";

	/** Description of {@code gather} command. */
	String GATHER_DESC = "Retrieve recording regions using the "
			+ "fast data streaming protocol. "
			+ "Requires system cores to be fully configured.";

	/** Description of {@code dse} command. */
	String DSE_DESC = "Evaluate data specifications for all cores and upload "
			+ "the results to SpiNNaker using the classic protocol.";

	/** Description of {@code dse_app} command. */
	String DSE_APP_DESC = "Evaluate data specifications for application cores "
			+ "and upload the results to SpiNNaker using the classic protocol.";

	/** Description of {@code dse_app_mon} command. */
	String DSE_MON_DESC = "Evaluate data specifications for application cores "
			+ "and upload the results to SpiNNaker using the fast data "
			+ "streaming protocol. "
			+ "Requires system cores to be fully configured, so "
			+ "can't be used to set up system cores.";

	/** Description of {@code dse_sys} command. */
	String DSE_SYS_DESC = "Evaluate data specifications for system cores and "
			+ "upload the results to SpiNNaker (always uses the classic "
			+ "protocol).";

	/** Description of {@code iobuf} command. */
	String IOBUF_DESC = "Download the contents "
			+ "of the IOBUF buffers and process them.";

	/** Description of {@code listen_for_unbooted} command. */
	String LISTEN_DESC = "Listen for unbooted SpiNNaker boards on the local "
			+ "network. Depends on receiving broadcast UDP messages.";
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

	/** Description of {@code dbFile} parameter. */
	String DBFILE = "The path of the buffer database.";

	/** Description of {@code dsFile} parameter. */
	String DSFILE = "The path of the dataspec database.";

	/** Description of {@code runFolder} parameter. */
	String RUN = "The name of the run data folder.";
}
