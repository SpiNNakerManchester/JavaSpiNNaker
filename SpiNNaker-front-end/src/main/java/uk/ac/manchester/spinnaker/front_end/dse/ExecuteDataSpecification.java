/*
 * Copyright (c) 2022 The University of Manchester
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
package uk.ac.manchester.spinnaker.front_end.dse;

import static org.slf4j.LoggerFactory.getLogger;
import static uk.ac.manchester.spinnaker.front_end.Constants.PARALLEL_SIZE;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;
import java.util.function.Function;

import org.slf4j.Logger;

import com.google.errorprone.annotations.MustBeClosed;

import uk.ac.manchester.spinnaker.alloc.client.SpallocClient;
import uk.ac.manchester.spinnaker.alloc.client.SpallocClientFactory;
import uk.ac.manchester.spinnaker.data_spec.DataSpecificationException;
import uk.ac.manchester.spinnaker.front_end.BasicExecutor;
import uk.ac.manchester.spinnaker.front_end.BoardLocalSupport;
import uk.ac.manchester.spinnaker.front_end.BasicExecutor.SimpleCallable;
import uk.ac.manchester.spinnaker.machine.Machine;
import uk.ac.manchester.spinnaker.storage.StorageException;
import uk.ac.manchester.spinnaker.storage.DSEDatabaseEngine;
import uk.ac.manchester.spinnaker.storage.DSEStorage.Ethernet;
import uk.ac.manchester.spinnaker.transceiver.ProcessException;
import uk.ac.manchester.spinnaker.transceiver.SpinnmanException;
import uk.ac.manchester.spinnaker.transceiver.Transceiver;
import uk.ac.manchester.spinnaker.transceiver.TransceiverInterface;

/**
 * Common base for executing data specifications.
 *
 * @author Donal Fellows
 */
public abstract class ExecuteDataSpecification extends BoardLocalSupport
		implements AutoCloseable {
	private static final Logger log =
			getLogger(ExecuteDataSpecification.class);

	/** The description of the SpiNNaker machine. */
	protected final Machine machine;

	/** The transceiver for talking to the SpiNNaker machine. */
	protected final TransceiverInterface txrx;

	/** The database. */
	protected final DSEDatabaseEngine db;

	/** A spalloc job, or null if not used. */
	protected final SpallocClient.Job job;

	/** How to run tasks in parallel. */
	private final BasicExecutor executor;

	/**
	 * @param machine
	 *            The description of the SpiNNaker machine.
	 * @param db
	 *            The DSE database.
	 * @throws IOException
	 *             If the transceiver can't talk to its sockets.
	 * @throws ProcessException
	 *             If SpiNNaker rejects a message.
	 * @throws InterruptedException
	 *             If communications are interrupted.
	 * @throws IllegalStateException
	 *             If something really strange occurs with talking to the BMP;
	 *             this constructor should not be doing that!
	 * @throws StorageException
	 *             If the database cannot be read.
	 * @throws URISyntaxException
	 *             If the proxy URI is specified but not valid.
	 */
	@MustBeClosed
	@SuppressWarnings("MustBeClosed")
	protected ExecuteDataSpecification(Machine machine, DSEDatabaseEngine db)
			throws IOException, ProcessException, InterruptedException,
			StorageException, URISyntaxException {
		super(machine);
		this.machine = machine;
		this.db = db;
		executor = new BasicExecutor(PARALLEL_SIZE);
		try {
			if (db == null) {
				// For testing only
				job = null;
				txrx = null;
				return;
			}
			var proxy = db.getStorageInterface().getProxyInformation();
			if (proxy == null) {
				log.debug("Using direct machine access for transceiver");
				job = null;
				txrx = new Transceiver(machine);
			} else {
				log.debug("Getting transceiver via proxy on {}",
						proxy.spallocUrl());
				job = SpallocClientFactory.getJobFromProxyInfo(proxy);
				txrx = job.getTransceiver();
			}
		} catch (ProcessException e) {
			throw e;
		} catch (SpinnmanException e) {
			throw new IllegalStateException("failed to talk to BMP, "
					+ "but that shouldn't have happened at all", e);
		}
	}

	@Override
	public final void close() throws IOException, InterruptedException {
		if (txrx != null) {
			txrx.close();
		}
		executor.close();
	}

	/**
	 * Run the tasks in parallel. Submits to {@link #executor} and detoxifies
	 * the exceptions.
	 *
	 * @param tasks
	 *            The tasks to run.
	 * @param mapper
	 *            Gets how to implement a task.
	 * @throws StorageException
	 *             If the database can't be talked to.
	 * @throws IOException
	 *             If the transceiver can't talk to its sockets.
	 * @throws ProcessException
	 *             If SpiNNaker rejects a message.
	 * @throws DataSpecificationException
	 *             If a data specification in the database is invalid.
	 * @throws InterruptedException
	 *             If communications are interrupted.
	 * @throws IllegalStateException
	 *             If an unexpected exception occurs in any of the parallel
	 *             tasks.
	 */
	protected final void processTasksInParallel(List<Ethernet> tasks,
			Function<Ethernet, SimpleCallable> mapper) throws StorageException,
			IOException, ProcessException, DataSpecificationException,
			InterruptedException {
		try {
			executor.submitTasks(tasks, mapper).awaitAndCombineExceptions();
		} catch (StorageException | IOException | ProcessException
				| DataSpecificationException | InterruptedException
				| RuntimeException e) {
			throw e;
		} catch (Exception e) {
			throw new IllegalStateException("unexpected exception", e);
		}
	}
}
