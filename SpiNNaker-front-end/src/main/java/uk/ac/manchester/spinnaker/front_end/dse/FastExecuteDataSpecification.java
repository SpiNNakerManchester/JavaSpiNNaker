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
import static java.nio.ByteOrder.LITTLE_ENDIAN;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.stream.Collectors.toList;
import static java.util.stream.IntStream.range;
import static org.slf4j.LoggerFactory.getLogger;
import static uk.ac.manchester.spinnaker.front_end.dse.FastDataInProtocol.computeNumPackets;
import static uk.ac.manchester.spinnaker.messages.Constants.NBBY;
import static uk.ac.manchester.spinnaker.utils.ByteBufferUtils.sliceUp;
import static uk.ac.manchester.spinnaker.utils.UnitConstants.MEGABYTE;
import static uk.ac.manchester.spinnaker.utils.UnitConstants.NSEC_PER_SEC;

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

import com.google.errorprone.annotations.CheckReturnValue;
import com.google.errorprone.annotations.MustBeClosed;

import difflib.ChangeDelta;
import difflib.Chunk;
import difflib.DeleteDelta;
import difflib.InsertDelta;
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
public class FastExecuteDataSpecification extends ExecuteDataSpecification {
	private static final Logger log =
			getLogger(FastExecuteDataSpecification.class);

	private static final String SPINNAKER_COMPARE_UPLOAD =
			getProperty("spinnaker.compare.upload");

	private static final String IN_REPORT_NAME =
			"speeds_gained_in_speed_up_process.tsv";

	private static final int TIMEOUT_RETRY_LIMIT = 100;

	/** flag for saying missing all SEQ numbers. */
	private static final int FLAG_FOR_MISSING_ALL_SEQUENCES = 0xFFFFFFFE;

	/** Sequence number that marks the end of a sequence number stream. */
	private static final int MISSING_SEQS_END = -1;

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
	public FastExecuteDataSpecification(TransceiverInterface txrx,
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
		private ThrottledConnection connection;

		private MissingRecorder missingSequenceNumbers;

		private BoardLocal logContext;

		private Gather gather;

		@MustBeClosed
		@SuppressWarnings("MustBeClosed")
		FastBoardWorker(TransceiverInterface txrx, Ethernet board,
				DSEStorage storage, Gather gather)
				throws IOException, ProcessException, InterruptedException,
				StorageException {
			super(txrx, board, storage);
			this.logContext = new BoardLocal(board.location);
			this.connection = new ThrottledConnection(txrx, board,
					gather.getIptag());
			this.gather = gather;
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
			MissingRecorder() {
				missingSequenceNumbers = this;
			}

			@Override
			public void close() {
				missingSequenceNumbers = null;
			}

			/**
			 * Give me a new bitfield, recorded in this class.
			 *
			 * @param expectedMax
			 *            How big should the bitfield be?
			 * @return The bitfield.
			 */
			BitSet issueNew(int expectedMax) {
				var s = new BitSet(expectedMax);
				addLast(s);
				return s;
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
			void report(HasCoreLocation core, long time, List<ByteBuffer> data,
					MemoryLocation addr) throws IOException {
				if (writeReports) {
					var size = data.stream().reduce(
							0, (r, b) -> r + b.limit(), Integer::sum);
					writeReport(core, time, size, addr, this);
				}
			}
		}

		@Override
		protected void writeRegion(HasCoreLocation core,
				List<ByteBuffer> content, MemoryLocation baseAddress)
				throws IOException, ProcessException, InterruptedException {
			try (var recorder = new MissingRecorder()) {
				long start = nanoTime();
				fastWrite(core, baseAddress, content);
				long end = nanoTime();
				recorder.report(core, end - start, content, baseAddress);
			}
			if (SPINNAKER_COMPARE_UPLOAD != null) {
				for (var buf : content) {
					var readBack = txrx.readMemory(
							core, baseAddress, buf.remaining());
					compareBuffers(buf, readBack);
				}
			}
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
		 */
		private void fastWrite(HasCoreLocation core, MemoryLocation baseAddress,
				List<ByteBuffer> data)
						throws IOException, InterruptedException {
			int timeoutCount = 0;
			int numPackets = computeNumPackets(data);
			var protocol = new GathererProtocol(core);
			int transactionId = gather.getNextTransactionId();

			outerLoop: while (true) {
				// Do the initial blast of data
				sendInitialPackets(baseAddress, data, protocol, transactionId,
						numPackets);
				/*
				 * Don't create a missing buffer until at least one packet has
				 * come back.
				 */
				BitSet missing = null;

				// Wait for confirmation and do required retransmits
				innerLoop: while (true) {
					try {
						var buf = connection.receive();
						var received = buf.order(LITTLE_ENDIAN).asIntBuffer();
						timeoutCount = 0; // Reset the timeout counter
						int command = received.get();
						try {
							// read transaction id
							var commandCode =
									FastDataInCommandID.forValue(command);
							int thisTransactionId = received.get();

							// if wrong transaction id, ignore packet
							if (thisTransactionId != transactionId) {
								continue innerLoop;
							}

							// Decide what to do with the packet
							switch (commandCode) {
							case RECEIVE_FINISHED_DATA_IN:
								// We're done!
								break outerLoop;

							case RECEIVE_MISSING_SEQ_DATA_IN:
								if (!received.hasRemaining()) {
									throw new BadDataInMessageException(
											received.get(0), received);
								}
								log.debug(
										"another packet (#{}) of missing "
												+ "sequence numbers;",
										received.get(1));
								break;
							default:
								throw new BadDataInMessageException(
										received.get(0), received);
							}

							/*
							 * The currently received packet has missing
							 * sequence numbers. Accumulate and dispatch
							 * transactionId when we've got them all.
							 */
							if (missing == null) {
								missing = missingSequenceNumbers.issueNew(
										numPackets);
							}
							var flags = addMissedSeqNums(
									received, missing, numPackets);

							/*
							 * Check that you've seen something that implies
							 * ready to retransmit.
							 */
							if (flags.seenAll || flags.seenEnd) {
								retransmitMissingPackets(protocol, data,
										missing, transactionId);
								missing.clear();
							}
						} catch (IllegalArgumentException e) {
							log.error("Unexpected command code " + command
									+ " received from "
									+ connection.getLocation());
						}
					} catch (SocketTimeoutException e) {
						if (timeoutCount++ > TIMEOUT_RETRY_LIMIT) {
							log.error(
									"ran out of attempts on transaction {}"
											+ " due to timeouts.",
									transactionId);
							throw e;
						}
						/*
						 * If we never received a packet, we will never have
						 * created the buffer, so send everything again
						 */
						if (missing == null) {
							log.debug("full timeout; resending initial "
									+ "packets for stream with transaction "
									+ "id {}", transactionId);
							continue outerLoop;
						}
						log.info(
								"timeout {} on transaction {} sending to {}"
										+ " via {}",
								timeoutCount, transactionId, core,
								gather.asCoreLocation());
						retransmitMissingPackets(protocol, data, missing,
								transactionId);
						missing.clear();
					}
				}
			}
		}

		@CheckReturnValue
		private SeenFlags addMissedSeqNums(IntBuffer received, BitSet seqNums,
				int expectedMax) {
			var flags = new SeenFlags();
			var addedEnd = "";
			var addedAll = "";
			int actuallyAdded = 0;
			while (received.hasRemaining()) {
				int num = received.get();

				if (num == MISSING_SEQS_END) {
					addedEnd = "and saw END marker";
					flags.seenEnd = true;
					break;
				}
				if (num == FLAG_FOR_MISSING_ALL_SEQUENCES) {
					addedAll = "by finding ALL missing marker";
					flags.seenAll = true;
					for (int seqNum = 0; seqNum < expectedMax; seqNum++) {
						seqNums.set(seqNum);
						actuallyAdded++;
					}
					break;
				}

				seqNums.set(num);
				actuallyAdded++;
				if (num < 0 || num > expectedMax) {
					throw new CrazySequenceNumberException(num, received);
				}
			}
			log.debug("added {} missed packets, {}{}", actuallyAdded, addedEnd,
					addedAll);
			return flags;
		}

		private int sendInitialPackets(MemoryLocation baseAddress,
				List<ByteBuffer> data, GathererProtocol protocol,
				int transactionId,	int numPackets) throws IOException {
			log.debug("streaming {} packets using transaction {}",
					numPackets, transactionId);
			log.debug("sending packet #{}", 0);
			connection.send(protocol.dataToLocation(baseAddress, numPackets,
					transactionId));
			for (int seqNum = 0; seqNum < numPackets; seqNum++) {
				log.debug("sending packet #{}", seqNum);
				connection.send(protocol.seqData(data, seqNum, transactionId));
			}
			log.debug("sending terminating packet");
			connection.send(protocol.tellDataIn(transactionId));
			return numPackets;
		}

		private void retransmitMissingPackets(GathererProtocol protocol,
				List<ByteBuffer> dataToSend, BitSet missingSeqNums,
				int transactionId) throws IOException {
			log.info("retransmitting {} packets", missingSeqNums.cardinality());

			missingSeqNums.stream().forEach(seqNum -> {
				log.debug("resending packet #{}", seqNum);
				try {
					connection.send(protocol.seqData(dataToSend, seqNum,
							transactionId));
				} catch (IOException e) {
					log.error(
							"missing sequence packet with id {}-{} "
									+ "failed to transmit",
							seqNum, transactionId, e);
				}
			});
			log.debug("sending terminating packet");
			connection.send(protocol.tellDataIn(transactionId));
		}
	}

	/**
	 * Manufactures Fast Data In protocol messages.
	 *
	 * @author Donal Fellows
	 */
	private class GathererProtocol extends FastDataInProtocol {
		private GathererProtocol(ChipLocation chip, boolean ignored) {
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
		GathererProtocol(HasChipLocation chip) {
			this(chip.asChipLocation(), false);
		}
	}

	/**
	 * Contains flags for seen missing sequence numbers.
	 *
	 * @author Alan Stokes
	 */
	private static final class SeenFlags {
		boolean seenEnd;

		boolean seenAll;
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

	/**
	 * Exception thrown when something mad comes back off SpiNNaker.
	 *
	 * @author Donal Fellows
	 * @author Alan Stokes
	 */
	static class CrazySequenceNumberException extends RuntimeException {
		private static final long serialVersionUID = 1L;

		CrazySequenceNumberException(int remaining, IntBuffer message) {
			super("crazy number of missing packets: "
					+ toUnsignedLong(remaining));
			log.warn("bad message payload: {}", range(0, message.limit())
					.map(i -> message.get(i)).boxed().collect(toList()));
		}
	}
}
