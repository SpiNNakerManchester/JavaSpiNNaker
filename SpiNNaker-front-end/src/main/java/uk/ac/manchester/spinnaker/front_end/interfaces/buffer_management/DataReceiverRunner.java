/*
 * Copyright (c) 2018 The University of Manchester
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
package uk.ac.manchester.spinnaker.front_end.interfaces.buffer_management;

import static org.slf4j.LoggerFactory.getLogger;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import uk.ac.manchester.spinnaker.machine.Machine;
import uk.ac.manchester.spinnaker.machine.bean.MachineBean;
import uk.ac.manchester.spinnaker.machine.bean.MapperFactory;
import uk.ac.manchester.spinnaker.storage.BufferManagerDatabaseEngine;
import uk.ac.manchester.spinnaker.storage.BufferManagerStorage;
import uk.ac.manchester.spinnaker.storage.StorageException;
import uk.ac.manchester.spinnaker.transceiver.SpinnmanException;
import uk.ac.manchester.spinnaker.transceiver.Transceiver;
import uk.ac.manchester.spinnaker.transceiver.processes.ProcessException;

/**
 * Data dowloader runner.
 *
 * @author Christian-B
 * @author Donal Fellows
 */
public final class DataReceiverRunner {
	private static final ObjectMapper MAPPER = MapperFactory.createMapper();
	private static final String BUFFER_DB_FILE = "buffer.sqlite3";

	private DataReceiverRunner() {
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
	public static void receive(String placementsJsonFile,
			String machineJsonFile, String runFolder) throws IOException,
			SpinnmanException, StorageException, ProcessException {
		List<Placement> placements = getPlacements(placementsJsonFile);
		Machine machine = getMachine(machineJsonFile);
		Transceiver trans = new Transceiver(machine);
		BufferManagerStorage database = getDatabase(runFolder);

		DataReceiver receiver = new DataReceiver(trans, database);
		receiver.getDataForPlacements(placements, null);
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
	public static void gather(String gatherersJsonFile, String machineJsonFile,
			String runFolder) throws IOException, SpinnmanException,
			ProcessException, StorageException, InterruptedException {
		List<Gather> gathers = getGatherers(gatherersJsonFile);
		Machine machine = getMachine(machineJsonFile);
		Transceiver trans = new Transceiver(machine);
		BufferManagerStorage database = getDatabase(runFolder);

		DataGatherer runner =
				new RecordingRegionDataGatherer(trans, machine, database);
		for (Gather g : gathers) {
			runner.addTask(g);
		}
		int misses = runner.waitForTasksToFinish();
		getLogger(DataReceiverRunner.class).info("total misses: " + misses);
	}

	private static Machine getMachine(String filename)
			throws JsonParseException, JsonMappingException, IOException {
		try (FileReader machineReader = new FileReader(filename)) {
			return new Machine(
					MAPPER.readValue(machineReader, MachineBean.class));
		}
	}

	private static List<Gather> getGatherers(String filename)
			throws IOException, JsonParseException, JsonMappingException {
		try (FileReader gatherReader = new FileReader(filename)) {
			return MAPPER.readValue(gatherReader,
					new TypeReference<List<Gather>>() {
					});
		}
	}

	private static List<Placement> getPlacements(String placementsFile)
			throws IOException, JsonParseException, JsonMappingException,
			FileNotFoundException {
		try (FileReader placementReader = new FileReader(placementsFile)) {
			return MAPPER.readValue(placementReader,
					new TypeReference<List<Placement>>() {
					});
		}
	}

	private static BufferManagerStorage getDatabase(String runFolder) {
		return new BufferManagerDatabaseEngine(
				new File(runFolder, BUFFER_DB_FILE)).getBufferManagerStorage();
	}
}
