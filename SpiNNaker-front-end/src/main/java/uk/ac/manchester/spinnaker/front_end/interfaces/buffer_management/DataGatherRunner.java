/*
 * Copyright (c) 2018 The University of Manchester
 */
package uk.ac.manchester.spinnaker.front_end.interfaces.buffer_management;

import static org.slf4j.LoggerFactory.getLogger;
import static uk.ac.manchester.spinnaker.machine.bean.MapperFactory.createMapper;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;

import org.slf4j.Logger;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import uk.ac.manchester.spinnaker.machine.Machine;
import uk.ac.manchester.spinnaker.machine.bean.MachineBean;
import uk.ac.manchester.spinnaker.storage.DatabaseEngine;
import uk.ac.manchester.spinnaker.storage.SQLiteStorage;
import uk.ac.manchester.spinnaker.storage.StorageException;
import uk.ac.manchester.spinnaker.transceiver.SpinnmanException;
import uk.ac.manchester.spinnaker.transceiver.Transceiver;
import uk.ac.manchester.spinnaker.transceiver.processes.ProcessException;

/**
 * Prototype for early testing.
 *
 * @author Christian-B
 */
public abstract class DataGatherRunner {
	private static final Logger log = getLogger(DataGatherRunner.class);
	private static final ObjectMapper mapper = createMapper();

	private DataGatherRunner() {
	}

	private static final int THIRD = 3;

	/**
	 * Prototype for early testing.
	 *
	 * @param args
	 *            Arguements as received.
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
	public static void main(String... args)
			throws Exception {
		// args 0 = instruction to run this
		List<Gather> gathers = readGathererJson(args[1]);
		Machine machine = readMachineJson(args[2]);

		Transceiver trans = new Transceiver(machine.getBootEthernetAddress(),
				machine.version);

		DatabaseEngine database = new DatabaseEngine(new File(args[THIRD]));

		DataGatherer runner =
				new DirectDataGatherer(trans, new SQLiteStorage(database));
		for (Gather g : gathers) {
			runner.addTask(g);
		}
		int misses = runner.waitForTasksToFinish();
		log.info("total misses: " + misses);
		System.exit(0);
	}

	private static Machine readMachineJson(String filename)
			throws JsonParseException, JsonMappingException, IOException {
		try (FileReader machineReader = new FileReader(filename)) {
			return new Machine(
					mapper.readValue(machineReader, MachineBean.class));
		}
	}

	private static List<Gather> readGathererJson(String filename)
			throws IOException, JsonParseException, JsonMappingException {
		try (FileReader gatherReader = new FileReader(filename)) {
			return mapper.readValue(gatherReader,
					new TypeReference<List<Gather>>() {
					});
		}
	}
}
