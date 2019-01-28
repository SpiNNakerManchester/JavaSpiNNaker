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

import static org.slf4j.LoggerFactory.getLogger;
import static uk.ac.manchester.spinnaker.front_end.Constants.PARALLEL_SIZE;
import static uk.ac.manchester.spinnaker.front_end.LogControl.setLoggerDir;
import static uk.ac.manchester.spinnaker.machine.bean.MapperFactory.createMapper;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ExecutionException;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import uk.ac.manchester.spinnaker.data_spec.exceptions.DataSpecificationException;
import uk.ac.manchester.spinnaker.front_end.download.DataGatherer;
import uk.ac.manchester.spinnaker.front_end.download.DataReceiver;
import uk.ac.manchester.spinnaker.front_end.download.RecordingRegionDataGatherer;
import uk.ac.manchester.spinnaker.front_end.download.request.Gather;
import uk.ac.manchester.spinnaker.front_end.download.request.Placement;
import uk.ac.manchester.spinnaker.front_end.dse.HostExecuteDataSpecification;
import uk.ac.manchester.spinnaker.front_end.dse.HostExecuteDataSpecification.Completion;
import uk.ac.manchester.spinnaker.front_end.iobuf.IobufRequest;
import uk.ac.manchester.spinnaker.front_end.iobuf.IobufRetriever;
import uk.ac.manchester.spinnaker.front_end.iobuf.NotableMessages;
import uk.ac.manchester.spinnaker.machine.Machine;
import uk.ac.manchester.spinnaker.machine.bean.MachineBean;
import uk.ac.manchester.spinnaker.storage.BufferManagerDatabaseEngine;
import uk.ac.manchester.spinnaker.storage.BufferManagerStorage;
import uk.ac.manchester.spinnaker.storage.DSEDatabaseEngine;
import uk.ac.manchester.spinnaker.storage.StorageException;
import uk.ac.manchester.spinnaker.transceiver.SpinnmanException;
import uk.ac.manchester.spinnaker.transceiver.Transceiver;
import uk.ac.manchester.spinnaker.transceiver.processes.ProcessException;

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
	private static final String BUFFER_DB_FILE = "buffer.sqlite3";

	static {
		Class<?> cls = CommandLineInterface.class;
		Properties prop = new Properties();
		try {
			prop.load(cls.getClassLoader()
					.getResourceAsStream("command-line.properties"));
		} catch (IOException e) {
			getLogger(cls).error("failed to read properties", e);
			System.exit(2);
		}
		JAR_FILE = prop.getProperty("jar");
		MAIN_CLASS = prop.getProperty("mainClass");
		VERSION = prop.getProperty("version");
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
			System.err.printf(
					"wrong # args: must be \"java -jar %s <command> ...\"\n",
					JAR_FILE);
			System.exit(1);
		}
		try {
			switch (args[0]) {
			case "gather":
				if (args.length != NUM_DOWNLOAD_ARGS) {
					System.err.printf("wrong # args: must be \"java -jar %s "
							+ "gather <gatherFile> <machineFile> "
							+ "<runFolder>\"\n", JAR_FILE);
					System.exit(1);
				}
				setLoggerDir(args[THIRD]);
				gather(args[1], args[2], args[THIRD]);
				System.exit(0);

			case "download":
				if (args.length != NUM_DOWNLOAD_ARGS) {
					System.err.printf("wrong # args: must be \"java -jar %s "
							+ "download <placementFile> <machineFile> "
							+ "<runFolder>\"\n", JAR_FILE);
					System.exit(1);
				}
				setLoggerDir(args[THIRD]);
				receive(args[1], args[2], args[THIRD]);
				System.exit(0);

			case "dse":
				if (args.length != NUM_DSE_ARGS) {
					System.err.printf("wrong # args: must be \"java -jar %s "
							+ "dse <machineFile> <runFolder>\"\n", JAR_FILE);
					System.exit(1);
				}
				setLoggerDir(args[2]);
				dseRun(args[1], args[2]);
				System.exit(0);

			case "iobuf":
				if (args.length != NUM_IOBUF_ARGS) {
					System.err.printf("wrong # args: must be \"java -jar %s "
							+ "iobuf <machineFile> <iobufMapFile> "
							+ "<runFolder>\"\n", JAR_FILE);
					System.exit(1);
				}
				setLoggerDir(args[THIRD]);
				iobufRun(args[1], args[2], args[THIRD]);
				System.exit(0);

			case "version":
				System.out.println(VERSION);
				System.exit(0);

			default:
				System.err.printf("unknown command \"%s\": must be one of %s\n",
						args[0], "download, dse, gather, or version");
				System.exit(1);
			}
		} catch (Throwable t) {
			t.printStackTrace(System.err);
			System.exit(2);
		}
	}

	private static final int NUM_DOWNLOAD_ARGS = 4;
	private static final int THIRD = 3;
	private static final int NUM_DSE_ARGS = 3;
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
	 * @throws IOException
	 *             If the communications fail.
	 * @throws SpinnmanException
	 *             If a BMP is uncontactable.
	 * @throws ProcessException
	 *             If SpiNNaker rejects a message.
	 * @throws StorageException
	 *             If the database is in an illegal state.
	 * @throws ExecutionException
	 *             If there was a problem in the parallel queue.
	 * @throws InterruptedException
	 *             If the wait for everything to complete is interrupted.
	 * @throws DataSpecificationException
	 *             If an invalid data specification file is executed.
	 */
	private static void dseRun(String machineJsonFile, String runFolder)
			throws IOException, SpinnmanException,
			ProcessException, StorageException, ExecutionException,
			InterruptedException, DataSpecificationException {
		Machine machine = getMachine(machineJsonFile);
		DSEDatabaseEngine database =
				new DSEDatabaseEngine(new File(runFolder, DSE_DB_FILE));

		HostExecuteDataSpecification dseExec =
				new HostExecuteDataSpecification(machine);
		Completion c = dseExec.loadAll(database);
		getLogger(CommandLineInterface.class)
				.info("launched all DSE tasks; waiting for completion");
		c.waitForCompletion();
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
	 *             If a BMP is uncontactable.
	 * @throws ProcessException
	 *             If SpiNNaker rejects a message.
	 */
	private static void iobufRun(String machineJsonFile, String iobufMapFile,
			String runFolder)
			throws IOException, SpinnmanException, ProcessException {
		Machine machine = getMachine(machineJsonFile);
		IobufRequest request = getIobufRequest(iobufMapFile);

		IobufRetriever retriever = new IobufRetriever(new Transceiver(machine),
				machine, PARALLEL_SIZE);
		NotableMessages result =
				retriever.retrieveIobufContents(request, runFolder);
		MAPPER.writeValue(System.out, result);
	}

	/**
	 * Download data without using data gatherer cores.
	 *
	 * @param placementsJsonFile
	 *            Name of file containing JSON description of placements.
	 * @param machineJsonFile
	 *            Name of file containing JSON description of overall machine.
	 * @param runFolder
	 *            Name of directory containing per-run information (i.e., the
	 *            database that receives the output).
	 * @throws IOException
	 *             If the communications fail
	 * @throws SpinnmanException
	 *             If a BMP is uncontactable
	 * @throws ProcessException
	 *             If SpiNNaker rejects a message
	 * @throws StorageException
	 *             If the database is in an illegal state
	 */
	private static void receive(String placementsJsonFile,
			String machineJsonFile, String runFolder) throws IOException,
			SpinnmanException, StorageException, ProcessException {
		List<Placement> placements = getPlacements(placementsJsonFile);
		Machine machine = getMachine(machineJsonFile);
		Transceiver trans = new Transceiver(machine);
		BufferManagerStorage database = getDatabase(runFolder);

		DataReceiver receiver = new DataReceiver(trans, machine, database);
		receiver.getDataForPlacements(placements);
	}

	/**
	 * Download data using data gatherer cores.
	 *
	 * @param gatherersJsonFile
	 *            Name of file containing JSON description of gatherers.
	 * @param machineJsonFile
	 *            Name of file containing JSON description of overall machine.
	 * @param runFolder
	 *            Name of directory containing per-run information (i.e., the
	 *            database that receives the output).
	 * @throws IOException
	 *             If the communications fail
	 * @throws SpinnmanException
	 *             If a BMP is uncontactable
	 * @throws ProcessException
	 *             If SpiNNaker rejects a message
	 * @throws StorageException
	 *             If the database is in an illegal state
	 * @throws InterruptedException
	 *             If things are interrupted while waiting for all the downloads
	 *             to be done
	 */
	private static void gather(String gatherersJsonFile, String machineJsonFile,
			String runFolder) throws IOException, SpinnmanException,
			ProcessException, StorageException, InterruptedException {
		List<Gather> gathers = getGatherers(gatherersJsonFile);
		Machine machine = getMachine(machineJsonFile);
		Transceiver trans = new Transceiver(machine);
		BufferManagerStorage database = getDatabase(runFolder);

		DataGatherer runner =
				new RecordingRegionDataGatherer(trans, machine, database);
		runner.addTasks(gathers);
		int misses = runner.waitForTasksToFinish();
		getLogger(CommandLineInterface.class).info("total misses: {}", misses);
	}

	private static Machine getMachine(String filename)
			throws JsonParseException, JsonMappingException, IOException {
		try (FileReader machineReader = new FileReader(filename)) {
			return new Machine(
					MAPPER.readValue(machineReader, MachineBean.class));
		}
	}

	private static IobufRequest getIobufRequest(String filename)
			throws IOException {
		try (FileReader gatherReader = new FileReader(filename)) {
			return MAPPER.readValue(gatherReader, IobufRequest.class);
		}
	}

	private static List<Gather> getGatherers(String filename)
			throws IOException {
		try (FileReader gatherReader = new FileReader(filename)) {
			return MAPPER.readValue(gatherReader,
					new TypeReference<List<Gather>>() {
					});
		}
	}

	private static List<Placement> getPlacements(String placementsFile)
			throws IOException {
		try (FileReader placementReader = new FileReader(placementsFile)) {
			return MAPPER.readValue(placementReader,
					new TypeReference<List<Placement>>() {
					});
		}
	}

	private static BufferManagerStorage getDatabase(String runFolder) {
		return new BufferManagerDatabaseEngine(
				new File(runFolder, BUFFER_DB_FILE)).getStorageInterface();
	}
}
