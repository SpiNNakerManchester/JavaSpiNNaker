/*
 * Copyright (c) 2022 The University of Manchester
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
package uk.ac.manchester.spinnaker.front_end.dse;

import static uk.ac.manchester.spinnaker.front_end.Constants.PARALLEL_SIZE;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;
import java.util.function.Function;

import com.google.errorprone.annotations.MustBeClosed;

import uk.ac.manchester.spinnaker.data_spec.DataSpecificationException;
import uk.ac.manchester.spinnaker.front_end.BasicExecutor;
import uk.ac.manchester.spinnaker.front_end.BoardLocalSupport;
import uk.ac.manchester.spinnaker.front_end.BasicExecutor.SimpleCallable;
import uk.ac.manchester.spinnaker.machine.Machine;
import uk.ac.manchester.spinnaker.storage.StorageException;
import uk.ac.manchester.spinnaker.storage.DSEDatabaseEngine;
import uk.ac.manchester.spinnaker.storage.DSEStorage.Ethernet;
import uk.ac.manchester.spinnaker.transceiver.ProcessException;
import uk.ac.manchester.spinnaker.transceiver.TransceiverInterface;

/**
 * Common base for executing data specifications.
 *
 * @author Donal Fellows
 */
public abstract class ExecuteDataSpecification extends BoardLocalSupport
		implements AutoCloseable {
	/** The database. */
	protected final DSEDatabaseEngine db;

	/** How to run tasks in parallel. */
	private final BasicExecutor executor;

	/**
	 * @param txrx
	 *            The transceiver for talking to the SpiNNaker machine.
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
	protected ExecuteDataSpecification(TransceiverInterface txrx,
			Machine machine, DSEDatabaseEngine db)
			throws IOException, ProcessException, InterruptedException,
			StorageException, URISyntaxException {
		super((db == null) ? null : txrx, machine);
		this.db = db;
		this.executor = new BasicExecutor(PARALLEL_SIZE);
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
