/*
 * Copyright (c) 2019 The University of Manchester
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

import static difflib.DiffUtils.diff;
import static java.lang.Integer.toUnsignedLong;
import static java.lang.String.format;
import static java.lang.System.getProperty;
import static java.lang.System.nanoTime;
import static java.net.InetAddress.getByName;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.stream.Collectors.toList;
import static java.util.stream.IntStream.range;
import static org.slf4j.LoggerFactory.getLogger;
import static uk.ac.manchester.spinnaker.messages.Constants.NBBY;
import static uk.ac.manchester.spinnaker.utils.ByteBufferUtils.sliceUp;
import static uk.ac.manchester.spinnaker.utils.UnitConstants.MEGABYTE;
import static uk.ac.manchester.spinnaker.utils.UnitConstants.NSEC_PER_SEC;
import static uk.ac.manchester.spinnaker.front_end.dse.FastDataInCommandID.RECEIVE_FINISHED_DATA_IN;
import static uk.ac.manchester.spinnaker.messages.scp.SCPResult.RC_P2P_BUSY;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.SocketTimeoutException;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.ArrayDeque;
import java.util.BitSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;

import com.google.errorprone.annotations.MustBeClosed;

import difflib.ChangeDelta;
import difflib.Chunk;
import difflib.DeleteDelta;
import difflib.InsertDelta;
import uk.ac.manchester.spinnaker.connections.SCPConnection;
import uk.ac.manchester.spinnaker.front_end.NoDropPacketContext;
import uk.ac.manchester.spinnaker.front_end.download.request.Gather;
import uk.ac.manchester.spinnaker.front_end.download.request.Monitor;
import uk.ac.manchester.spinnaker.machine.ChipLocation;
import uk.ac.manchester.spinnaker.machine.CoreSubsets;
import uk.ac.manchester.spinnaker.machine.HasChipLocation;
import uk.ac.manchester.spinnaker.machine.HasCoreLocation;
import uk.ac.manchester.spinnaker.machine.Machine;
import uk.ac.manchester.spinnaker.machine.MemoryLocation;
import uk.ac.manchester.spinnaker.storage.DSEDatabaseEngine;
import uk.ac.manchester.spinnaker.storage.DSEStorage;
import uk.ac.manchester.spinnaker.storage.DSEStorage.Ethernet;
import uk.ac.manchester.spinnaker.storage.StorageException;
import uk.ac.manchester.spinnaker.transceiver.ProcessException;
import uk.ac.manchester.spinnaker.transceiver.TransceiverInterface;
import uk.ac.manchester.spinnaker.utils.MathUtils;

/**
 * Implementation of the Data Specification Executor that uses the Fast Data In
 * protocol to upload the results to a SpiNNaker machine.
 *
 * @author Donal Fellows
 * @author Alan Stokes
 */
public class FastCopyExecuteDataSpecification extends ExecuteDataSpecification {
	private static final Logger log =
			getLogger(FastCopyExecuteDataSpecification.class);

	private static final String SPINNAKER_COMPARE_UPLOAD =
			getProperty("spinnaker.compare.upload");

	private static final String IN_REPORT_NAME =
			"speeds_gained_in_speed_up_process.tsv";

	private final Map<ChipLocation, Gather> gathererForChip;

	private final Map<ChipLocation, Monitor> monitorForChip;

	private final Map<ChipLocation, CoreSubsets> monitorsForBoard;

	private boolean writeReports = false;

	private File reportPath = null;

	/**
	 * Create an instance of this class.
	 *
	 * @param txrx
	 *            The transceiver for talking to the SpiNNaker machine.
	 * @param machine
	 *            The SpiNNaker machine description.
	 * @param gatherers
	 *            The description of where the gatherers and monitors are.
	 * @param reportDir
	 *            Where to write reports, or {@code null} if no reports are to
	 *            be written.
	 * @param db
	 *            The DSE Database.
	 * @throws IOException
	 *             If IO goes wrong.
	 * @throws ProcessException
	 *             If SpiNNaker rejects a message.
	 * @throws InterruptedException
	 *             If communications are interrupted.
	 * @throws URISyntaxException
	 *             If the proxy URI is provided but not valid.
	 * @throws StorageException
	 *             If there is an error reading the database.
	 * @throws IllegalStateException
	 *             If something really strange occurs with talking to the BMP;
	 *             this constructor should not be doing that!
	 */
	@MustBeClosed
	public FastCopyExecuteDataSpecification(TransceiverInterface txrx,
			Machine machine, List<Gather> gatherers, File reportDir,
			DSEDatabaseEngine db) throws IOException, ProcessException,
			InterruptedException, StorageException, URISyntaxException {
		super(txrx, machine, db);
		if (SPINNAKER_COMPARE_UPLOAD != null) {
			log.warn(
					"detailed comparison of uploaded data enabled; "
					+ "this may destabilize the protocol");
		}

		if (reportDir != null) {
			writeReports = true;
			reportPath = new File(reportDir, IN_REPORT_NAME);
		}

		gathererForChip = new HashMap<>();
		monitorForChip = new HashMap<>();
		monitorsForBoard = new HashMap<>();

		buildMaps(gatherers);
	}

	/**
	 * Construct the internal mappings for gatherers and monitors.
	 *
	 * @param gatherers
	 *            The descriptions of whether the gatherers are located.
	 * @throws IOException
	 *             If IDs can't be read from the machine for network reasons.
	 * @throws ProcessException
	 *             If IDs can't be read from the machine for machine reasons.
	 * @throws InterruptedException
	 *             If we are interrupted.
	 */
	protected void buildMaps(List<Gather> gatherers)
			throws IOException, ProcessException, InterruptedException {
		for (var g : gatherers) {
			g.updateTransactionIdFromMachine(txrx);
			var gathererChip = g.asChipLocation();
			gathererForChip.put(gathererChip, g);
			var boardMonitorCores = monitorsForBoard
					.computeIfAbsent(gathererChip, __ -> new CoreSubsets());
			for (var m : g.getMonitors()) {
				var monitorChip = m.asChipLocation();
				gathererForChip.put(monitorChip, g);
				monitorForChip.put(monitorChip, m);
				boardMonitorCores.addCore(m.asCoreLocation());
			}
		}
	}

	/**
	 * Execute all application data specifications that a particular connection
	 * knows about, storing back in the database the information collected about
	 * those executions. Data is transferred using the Fast Data In protocol.
	 * <p>
	 * Cannot load data for system cores; those are used by the implementation
	 * of this protocol.
	 *
	 * @throws StorageException
	 *             If the database can't be talked to.
	 * @throws IOException
	 *             If the transceiver can't talk to its sockets.
	 * @throws ProcessException
	 *             If SpiNNaker rejects a message.
	 * @throws InterruptedException
	 *             If communications are interrupted.
	 * @throws IllegalStateException
	 *             If an unexpected exception occurs in any of the parallel
	 *             tasks.
	 */
	public void loadCores()
			throws StorageException, IOException, ProcessException,
			InterruptedException {
		var storage = db.getStorageInterface();
		processTasksInParallel(storage.listEthernetsToLoad(), board -> {
			return () -> loadBoard(board, storage);
		});
	}

	private void loadBoard(Ethernet board, DSEStorage storage)
			throws IOException, ProcessException, StorageException,
			InterruptedException {
		var cores = storage.listCoresToLoad(board, false);
		if (cores.isEmpty()) {
			log.info("no cores need loading on board; skipping");
			return;
		}
		log.info("loading data onto {} cores on board", cores.size());
		var gather = gathererForChip.get(board.location);
		try (var worker =  new FastBoardWorker(
				txrx, board, storage, gather)) {
			for (var xyp : cores) {
				worker.mallocCore(xyp);
			}
			try (var routers = worker.systemRouterTables();
					var context = worker.dontDropPackets(gather)) {
				for (var xyp : cores) {
					worker.loadCore(xyp);
				}
				log.info("finished sending data in for this board");
			} catch (Exception e) {
				log.warn("failure in core loading", e);
				throw e;
			}
		}
	}

	/**
	 * Opens a file for writing text.
	 *
	 * @param file
	 *            The file to open
	 * @param append
	 *            Whether to open in append mode; if {@code false}, the file
	 *            will be created or overwritten.
	 * @return The stream to use to do the writing.
	 * @throws IOException
	 *             If anything goes wrong with creating or opening the file.
	 */
	private static PrintWriter open(File file, boolean append)
			throws IOException {
		return new PrintWriter(new BufferedWriter(new OutputStreamWriter(
				new FileOutputStream(file, append), UTF_8)));
	}

	/**
	 * Writes (part of) the report describing what data transfer rates were
	 * achieved.
	 *
	 * @param chip
	 *            Which chip was the data bound for?
	 * @param timeDiff
	 *            How long did the transfer take, in nanoseconds.
	 * @param size
	 *            How many bytes were transferred?
	 * @param baseAddress
	 *            Where were the bytes written to?
	 * @param missingNumbers
	 *            What were the missing sequence numbers at each stage.
	 * @throws IOException
	 *             If IO fails.
	 */
	public synchronized void writeReport(HasChipLocation chip, long timeDiff,
			int size, MemoryLocation baseAddress, Object missingNumbers)
			throws IOException {
		if (!reportPath.exists()) {
			try (var w = open(reportPath, false)) {
				w.println("x" + "\ty" + "\tSDRAM address" + "\tsize/bytes"
						+ "\ttime taken/s" + "\ttransfer rate/(Mb/s)"
						+ "\tmissing sequence numbers");
			}
		}

		float timeTaken = timeDiff / (float) NSEC_PER_SEC;
		float megabits = (size * (long) NBBY) / (float) MEGABYTE;
		String mbs;
		if (timeDiff == 0) {
			mbs = "unknown, below threshold";
		} else {
			mbs = format("%f", megabits / timeTaken);
		}
		try (var w = open(reportPath, true)) {
			w.printf("%d\t%d\t%s\t%d\t%f\t%s\t%s\n", chip.getX(), chip.getY(),
					baseAddress, toUnsignedLong(size), timeTaken, mbs,
					missingNumbers);
		}
	}

	private static void compareBuffers(ByteBuffer original,
			ByteBuffer downloaded) {
		for (int i = 0; i < original.remaining(); i++) {
			if (original.get(i) != downloaded.get(i)) {
				log.error("downloaded buffer contents different");
				for (var delta : diff(list(original), list(downloaded))
						.getDeltas()) {
					if (delta instanceof ChangeDelta) {
						var delete = delta.getOriginal();
						var insert = delta.getRevised();
						log.warn(
								"swapped {} bytes (SCP) for {} (gather) "
										+ "at {}->{}",
								delete.getLines().size(),
								insert.getLines().size(), delete.getPosition(),
								insert.getPosition());
						log.info("change {} -> {}", describeChunk(delete),
								describeChunk(insert));
					} else if (delta instanceof DeleteDelta) {
						var delete = delta.getOriginal();
						log.warn("gather deleted {} bytes at {}",
								delete.getLines().size(), delete.getPosition());
						log.info("delete {}", describeChunk(delete));
					} else if (delta instanceof InsertDelta) {
						var insert = delta.getRevised();
						log.warn("gather inserted {} bytes at {}",
								insert.getLines().size(), insert.getPosition());
						log.info("insert {}", describeChunk(insert));
					}
				}
				break;
			}
		}
	}

	private static List<Byte> list(ByteBuffer buffer) {
		return sliceUp(buffer, 1).map(ByteBuffer::get).toList();
	}

	private static List<String> describeChunk(Chunk<Byte> chunk) {
		return chunk.getLines().stream().map(MathUtils::hexbyte)
				.collect(toList());
	}

	/**
	 * The worker class that handles a particular board of a SpiNNaker machine.
	 * Instances of this class are only ever used from a single thread.
	 *
	 * @author Donal Fellows
	 * @author Alan Stokes
	 */
	private class FastBoardWorker extends BoardWorker implements AutoCloseable {
		private SCPConnection connection;

		private BoardLocal logContext;

		@MustBeClosed
		@SuppressWarnings("MustBeClosed")
		FastBoardWorker(TransceiverInterface txrx, Ethernet board,
				DSEStorage storage, Gather gather)
				throws IOException, ProcessException, InterruptedException,
				StorageException {
			super(txrx, board, storage);
			this.logContext = new BoardLocal(board.location);
			this.connection = txrx.createScpConnection(board.location,
					getByName(board.ethernetAddress));
		}

		@Override
		public void close() throws IOException, ProcessException,
				InterruptedException {
			logContext.close();
			connection.close();
		}

		/**
		 * A list of bitfields. Knows how to install and uninstall itself from
		 * the general execution flow.
		 *
		 * @author Donal Fellows
		 */
		@SuppressWarnings("serial")
		private class MissingRecorder extends ArrayDeque<BitSet>
				implements AutoCloseable {

			@Override
			public void close() {
				// Does Nothing
			}

			/**
			 * Issue the report based on what we recorded, if appropriate.
			 *
			 * @param core
			 *            What core were we recording for?
			 * @param time
			 *            How long did the loading take?
			 * @param size
			 *            How much data was moved?
			 * @param addr
			 *            Where on the core was the data moved to?
			 * @throws IOException
			 *             If anything goes wrong with writing.
			 */
			void report(HasCoreLocation core, long time, int size,
					MemoryLocation addr) throws IOException {
				if (writeReports) {
					writeReport(core, time, size, addr, this);
				}
			}
		}

		/**
		 * Writes the contents of a region. Caller is responsible for ensuring
		 * this method has work to do.
		 *
		 * @param core
		 *            Which core to write to. Does not need to refer to a
		 *            monitor core.
		 * @param region
		 *            The region to write.
		 * @param baseAddress
		 *            Where to write the region.
		 * @param gather
		 *            The information about where messages are routed via.
		 * @return How many bytes were actually written.
		 * @throws IOException
		 *             If anything goes wrong with I/O.
		 * @throws ProcessException
		 *             If SCAMP rejects the request.
		 * @throws InterruptedException
		 *             If communications are interrupted.
		 */
		@Override
		protected int writeRegion(HasCoreLocation core, ByteBuffer content,
				MemoryLocation baseAddress)
				throws IOException, ProcessException, InterruptedException {
			int written = content.remaining();
			try (var recorder = new MissingRecorder()) {
				long start = nanoTime();
				fastWrite(core, baseAddress, content);
				long end = nanoTime();
				recorder.report(
						core, end - start, content.limit(), baseAddress);
			}
			if (SPINNAKER_COMPARE_UPLOAD != null) {
				var readBack = txrx.readMemory(
						core, baseAddress, content.remaining());
				compareBuffers(content, readBack);
			}
			return written;
		}

		/**
		 * Put the board in don't-drop-packets mode.
		 *
		 * @param core
		 *            The core location of the gatherer for the board to set to
		 *            don't drop packets.
		 * @return An object that, when closed, will put the board back in
		 *         standard mode.
		 * @throws IOException
		 *             If anything goes wrong with communication.
		 * @throws ProcessException
		 *             If SpiNNaker rejects a message.
		 * @throws InterruptedException
		 *             If communications are interrupted.
		 */
		@MustBeClosed
		NoDropPacketContext dontDropPackets(Gather core)
				throws IOException, ProcessException, InterruptedException {
			return new NoDropPacketContext(txrx,
					monitorsForBoard.get(board.location), core);
		}

		/**
		 * Install the system router tables across the board.
		 *
		 * @return An object that, when closed, will put the board back in
		 *         application mode.
		 * @throws IOException
		 *             If anything goes wrong with communication.
		 * @throws ProcessException
		 *             If SpiNNaker rejects a message.
		 * @throws InterruptedException
		 *             If communications are interrupted.
		 */
		@MustBeClosed
		SystemRouterTableContext systemRouterTables()
				throws IOException, ProcessException, InterruptedException {
			return new SystemRouterTableContext(txrx,
					monitorsForBoard.get(board.location));
		}

		/**
		 * This is the implementation of the actual fast data in protocol.
		 *
		 * @param core
		 *            Where the data is going to.
		 * @param baseAddress
		 *            Whether the data will be written.
		 * @param data
		 *            The data to be written.
		 * @throws IOException
		 *             If IO fails.
		 * @throws InterruptedException
		 *            If communications are interrupted.
		 * @throws ProcessException
		 *            If the process fails.
		 */
		private void fastWrite(HasCoreLocation core, MemoryLocation baseAddress,
				ByteBuffer data)
						throws IOException, InterruptedException,
						ProcessException {

			// Allocate on 0, 0
			var nBytes = data.remaining();
			var addr = txrx.mallocSDRAM(connection.getChip(), nBytes);

			// Write to 0, 0 in the allocated space
			txrx.writeMemory(connection.getChip(), addr, data);

			var protocol = new Protocol(core);

			// Send message to trigger the copy, repeat until definitely sent
			boolean sent = false;
			boolean copyDone = false;
			int nRetries = 3;
			while (!sent) {
			    connection.send(protocol.copyFromSDRAM(addr.address,
				    	baseAddress.address, nBytes / 4));
			    try {
			        var response = connection.receiveMessage(1000);
			        var responseCommand = response.getData().getInt();
			        if (nRetries == 3 && responseCommand == RC_P2P_BUSY.value) {
			        	throw new IOException("Copy already in progress!");
			        }
			        if (responseCommand == RECEIVE_FINISHED_DATA_IN.value) {
			        	copyDone = true;
			        }
			    } catch (SocketTimeoutException e) {
			    	if (nRetries == 0) {
			    		throw e;
			    	}
			    	nRetries--;
			    }

			}

			// Wait for the copy to finish
			while (!copyDone) {
				var response = connection.receiveMessage(1000);
				var responseCommand = response.getData().getInt();
				if (responseCommand == RECEIVE_FINISHED_DATA_IN.value) {
		        	copyDone = true;
		        }
				connection.send(protocol.copyFromSDRAMCheck());
			}
		}
	}

	/**
	 * Manufactures Fast Data In protocol messages.
	 */
	private class Protocol extends FastDataInProtocol {
		private Protocol(ChipLocation chip, boolean ignored) {
			super(machine, gathererForChip.get(chip), monitorForChip.get(chip));
		}

		/**
		 * Create an instance of this for pushing data to a given chip's SDRAM.
		 *
		 * @param chip
		 *            The chip where the data is to be pushed. What extra
		 *            monitor and data gatherer to route it via are determined
		 *            from the board context.
		 */
		Protocol(HasChipLocation chip) {
			this(chip.asChipLocation(), false);
		}
	}

	/**
	 * Exception thrown when something mad comes back off SpiNNaker.
	 *
	 * @author Donal Fellows
	 */
	static class BadDataInMessageException extends RuntimeException {
		private static final long serialVersionUID = 1L;

		BadDataInMessageException(int code, IntBuffer message) {
			super("unexpected response code: " + toUnsignedLong(code));
			log.warn("bad message payload: {}", range(0, message.limit())
					.map(i -> message.get(i)).boxed().collect(toList()));
		}
	}
}
