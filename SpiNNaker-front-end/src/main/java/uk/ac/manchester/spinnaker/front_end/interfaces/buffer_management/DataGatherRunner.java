/*
 * Copyright (c) 2018 The University of Manchester
 */
package uk.ac.manchester.spinnaker.front_end.interfaces.buffer_management;

import static uk.ac.manchester.spinnaker.messages.Constants.SCP_SCAMP_PORT;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import uk.ac.manchester.spinnaker.connections.SCPConnection;
import uk.ac.manchester.spinnaker.machine.ChipLocation;
import uk.ac.manchester.spinnaker.machine.Machine;
import uk.ac.manchester.spinnaker.machine.bean.MachineBean;
import uk.ac.manchester.spinnaker.machine.bean.MapperFactory;
import uk.ac.manchester.spinnaker.machine.tags.IPTag;
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
public final class DataGatherRunner {
    private final Transceiver txrx;
	private final SQLiteStorage database;
	private final ExecutorService pool;

	private static final int POOL_SIZE = 1; // TODO

	private DataGatherRunner(Transceiver trans, DatabaseEngine database) {
    	this.txrx = trans;
    	this.database = new SQLiteStorage(database);
    	this.pool = Executors.newFixedThreadPool(POOL_SIZE);
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
			throws IOException, SpinnmanException, StorageException,
			ProcessException, InterruptedException {
		// args 0 = instruction to run this
		ObjectMapper mapper = MapperFactory.createMapper();
		FileReader gatherReader = new FileReader(args[1]);
        List<Gather> gathers = mapper.readValue(
                gatherReader, new TypeReference<List<Gather>>() { });

		FileReader machineReader = new FileReader(args[2]);
		MachineBean fromJson =
				mapper.readValue(machineReader, MachineBean.class);
		Machine machine = new Machine(fromJson);

		Transceiver trans = new Transceiver(machine.getBootEthernetAddress(),
				machine.version);

		DatabaseEngine database = new DatabaseEngine(new File(args[THIRD]));

		DataGatherRunner runner = new DataGatherRunner(trans, database);
		for (Gather g : gathers) {
			runner.addTask(g);
		}
		runner.waitForTasksToFinish();
		System.exit(0);
	}

	private void addTask(Gather g) {
		pool.execute(() -> downloadBoard(g));
	}

	private void waitForTasksToFinish() throws InterruptedException {
		pool.shutdown();
		pool.awaitTermination(1, TimeUnit.DAYS);
	}

	private void downloadBoard(Gather g) {
		try {
			ChipLocation gathererLocation = g.asChipLocation();
			try (SCPConnection conn = new SCPConnection(gathererLocation,
					g.getIptag().getBoardAddress(), SCP_SCAMP_PORT)) {
				reconfigureIPtag(g.getIptag(), gathererLocation, conn);
				// TODO reconfigure router timeouts
				for (Monitor mon : g.getMonitors()) {
					for (Placement place : mon.getPlacements()) {
						doDownload(conn, mon, place);
					}
				}
				// TODO reconfigure router timeouts
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void reconfigureIPtag(IPTag iptag, ChipLocation gathererLocation,
			SCPConnection conn) throws IOException, ProcessException {
		txrx.setIPTag(new IPTag(iptag.getBoardAddress(), gathererLocation,
				conn.getLocalPort(), iptag.getIPAddress()));
	}

	private void doDownload(SCPConnection conn, Monitor mon, Placement place) {
		// TODO Donal's magic!
	}
}
