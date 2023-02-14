/*
 * Copyright (c) 2018-2020 The University of Manchester
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
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
import static uk.ac.manchester.spinnaker.data_spec.Constants.APP_PTR_TABLE_BYTE_SIZE;
import static uk.ac.manchester.spinnaker.front_end.Constants.CORE_DATA_SDRAM_BASE_TAG;
import static uk.ac.manchester.spinnaker.front_end.dse.FastDataInProtocol.computeNumPackets;
import static uk.ac.manchester.spinnaker.messages.Constants.NBBY;
import static uk.ac.manchester.spinnaker.utils.ByteBufferUtils.sliceUp;
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
import uk.ac.manchester.spinnaker.data_spec.DataSpecificationException;
import uk.ac.manchester.spinnaker.data_spec.Executor;
import uk.ac.manchester.spinnaker.data_spec.MemoryRegion;
import uk.ac.manchester.spinnaker.data_spec.MemoryRegionReal;
import uk.ac.manchester.spinnaker.front_end.NoDropPacketContext;
import uk.ac.manchester.spinnaker.front_end.Progress;
import uk.ac.manchester.spinnaker.front_end.download.request.Gather;
import uk.ac.manchester.spinnaker.front_end.download.request.Monitor;
import uk.ac.manchester.spinnaker.machine.ChipLocation;
import uk.ac.manchester.spinnaker.machine.CoreLocation;
import uk.ac.manchester.spinnaker.machine.CoreSubsets;
import uk.ac.manchester.spinnaker.machine.HasChipLocation;
import uk.ac.manchester.spinnaker.machine.Machine;
import uk.ac.manchester.spinnaker.machine.MemoryLocation;
import uk.ac.manchester.spinnaker.messages.model.AppID;
import uk.ac.manchester.spinnaker.storage.DSEDatabaseEngine;
import uk.ac.manchester.spinnaker.storage.DSEStorage;
import uk.ac.manchester.spinnaker.storage.DSEStorage.CoreToLoad;
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

	private static final String LOADING_MSG =
			"loading data specifications onto SpiNNaker";

	private static final String IN_REPORT_NAME =
			"speeds_gained_in_speed_up_process.tsv";

	/** One kilo-binary unit multiplier. */
	private static final int ONE_KI = 1024;

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
	 *
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
	public void loadCores()
			throws StorageException, IOException, ProcessException,
			DataSpecificationException, InterruptedException {
		var storage = db.getStorageInterface();
		var ethernets = storage.listEthernetsToLoad();
		int opsToRun = storage.countWorkRequired();
		try (var bar = new Progress(opsToRun, LOADING_MSG)) {
			processTasksInParallel(ethernets, board -> {
				return () -> loadBoard(board, storage, bar);
			});
		}
	}

	private void loadBoard(Ethernet board, DSEStorage storage, Progress bar)
			throws IOException, ProcessException,
			DataSpecificationException, StorageException, InterruptedException {
		var cores = storage.listCoresToLoad(board, false);
		if (cores.isEmpty()) {
			log.info("no cores need loading on board; skipping");
			return;
		}
		log.info("loading data onto {} cores on board", cores.size());
		try (var worker = new BoardWorker(board, storage, bar)) {
			var addresses = new HashMap<CoreToLoad, MemoryLocation>();
			for (var ctl : cores) {
				var start = malloc(ctl, ctl.sizeToWrite);
				txrx.writeUser0(ctl.core, start.address);
				addresses.put(ctl, start);
			}

			try (var routers = worker.systemRouterTables();
					var context = worker.dontDropPackets(
							gathererForChip.get(board.location))) {
				for (var ctl : cores) {
					worker.loadCore(ctl, gathererForChip.get(board.location),
							addresses.get(ctl));
				}
				log.info("finished sending data in for this board");
			} catch (Exception e) {
				log.warn("failure in core loading", e);
				throw e;
			}
		}
	}

	private MemoryLocation malloc(CoreToLoad ctl, Integer bytesUsed)
			throws IOException, ProcessException, InterruptedException {
		return txrx.mallocSDRAM(ctl.core.getScampCore(), bytesUsed,
				new AppID(ctl.appID),
				ctl.core.getP() + CORE_DATA_SDRAM_BASE_TAG);
	}

	private static MemoryRegionReal getRealRegionOrNull(MemoryRegion reg) {
		if (!(reg instanceof MemoryRegionReal)) {
			return null;
		}
		var r = (MemoryRegionReal) reg;
		if (r.isUnfilled() || r.getMaxWritePointer() <= 0) {
			return null;
		}
		return r;
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
		float megabits = (size * (long) NBBY) / (float) (ONE_KI * ONE_KI);
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
	private class BoardWorker implements AutoCloseable {
		private Ethernet board;

		private DSEStorage storage;

		private Progress bar;

		private ThrottledConnection connection;

		private MissingRecorder missingSequenceNumbers;

		private BoardLocal logContext;

		private ExecutionContext execContext;

		@MustBeClosed
		@SuppressWarnings("MustBeClosed")
		BoardWorker(Ethernet board, DSEStorage storage, Progress bar)
				throws IOException, ProcessException, InterruptedException {
			this.board = board;
			this.logContext = new BoardLocal(board.location);
			this.storage = storage;
			this.bar = bar;
			this.execContext = new ExecutionContext(txrx);
			this.connection = new ThrottledConnection(txrx, board,
					gathererForChip.get(board.location).getIptag());
		}

		@Override
		public void close() throws IOException, ProcessException,
				DataSpecificationException, InterruptedException {
			execContext.close();
			logContext.close();
			connection.close();
		}

		/**
		 * Execute a data specification and load the results onto a core.
		 *
		 * @param ctl
		 *            The definition of what to run and where to send the
		 *            results.
		 * @param gather
		 *            Where the relevant packet gatherer is that we will be
		 *            routing data via.
		 * @param start
		 *            Start of the memory chunk to put the data in.
		 * @throws IOException
		 *             If anything goes wrong with I/O.
		 * @throws ProcessException
		 *             If SCAMP rejects the request.
		 * @throws DataSpecificationException
		 *             If the instructions to build the data are wrong.
		 * @throws StorageException
		 *             If the database access fails.
		 * @throws InterruptedException
		 *             If communications are interrupted.
		 */
		protected void loadCore(CoreToLoad ctl, Gather gather,
				MemoryLocation start) throws IOException, ProcessException,
				DataSpecificationException, StorageException,
				InterruptedException {
			// Get what we're going to run...
			var dataSpec = getDataSpec(ctl);

			try (var executor = new Executor(dataSpec,
					machine.getChipAt(ctl.core).sdram)) {
				// ... run it...
				execContext.execute(executor, ctl.core, start);
				// ... and write the results to the machine
				int writes = loadCoreFromExecutor(ctl, gather, start, executor);
				log.info("loaded {} memory regions (including metadata "
						+ "pseudoregion) for {}", writes, ctl.core);
			} catch (DataSpecificationException e) {
				throw new DataSpecificationException(format(
						"failed to execute data specification on "
								+ "core %s of board %s (%s)",
						ctl.core, board.location, board.ethernetAddress), e);
			} catch (IOException e) {
				throw new IOException(format(
						"failed to upload built data to "
								+ "core %s of board %s (%s)",
						ctl.core, board.location, board.ethernetAddress), e);
			} catch (StorageException e) {
				throw new StorageException(format(
						"failed to record results of data specification for "
								+ "core %s of board %s (%s)",
						ctl.core, board.location, board.ethernetAddress), e);
			}
		}

		/**
		 * Wrap {@link CoreToLoad#getDataSpec()} with core location info on
		 * failure.
		 *
		 * @param ctl
		 *            Record from the database describing a core to be loaded.
		 * @return The data specification for the given core.
		 * @throws DataSpecificationException
		 *             If something goes wrong.
		 */
		private ByteBuffer getDataSpec(CoreToLoad ctl)
				throws DataSpecificationException {
			try {
				return ctl.getDataSpec();
			} catch (StorageException | RuntimeException e) {
				throw new DataSpecificationException(format(
						"failed to read data specification on "
								+ "core %s of board %s (%s)",
						ctl.core, board.location, board.ethernetAddress), e);
			}
		}

		private int loadCoreFromExecutor(CoreToLoad ctl, Gather gather,
				MemoryLocation start, Executor executor) throws IOException,
				ProcessException, StorageException, InterruptedException {
			int size = executor.getConstructedDataSize();
			if (log.isInfoEnabled()) {
				log.info("generated {} bytes to load onto {} into memory "
						+ "starting at {}",
						size, ctl.core, start);
			}
			int written = APP_PTR_TABLE_BYTE_SIZE;
			int writeCount = 1;

			for (var reg : executor.regions()) {
				var r = getRealRegionOrNull(reg);
				if (r == null) {
					continue;
				}

				written += writeRegion(ctl.core, r, r.getRegionBase(), gather);
				writeCount++;
				if (SPINNAKER_COMPARE_UPLOAD != null) {
					var readBack = txrx.readMemory(ctl.core,
							r.getRegionBase(), r.getRegionData().remaining());
					compareBuffers(r.getRegionData(), readBack);
				}
			}

			bar.update();
			storage.saveLoadingMetadata(ctl, start, size, written);
			return writeCount;
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
			void report(CoreLocation core, long time, int size,
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
		private int writeRegion(CoreLocation core, MemoryRegionReal region,
				MemoryLocation baseAddress, Gather gather)
				throws IOException, ProcessException, InterruptedException {
			var data = region.getRegionData().duplicate();

			data.flip();
			int written = data.remaining();
			try (var recorder = new MissingRecorder()) {
				long start = nanoTime();
				fastWrite(core, baseAddress, data, gather);
				long end = nanoTime();
				recorder.report(core, end - start, data.limit(), baseAddress);
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
		 * @param gather
		 *            The information about packet routing. In particular,
		 *            responsible for Fast Data In transaction ID issuing.
		 * @throws IOException
		 *             If IO fails.
		 * @throws InterruptedException
		 *            If communications are interrupted.
		 */
		private void fastWrite(CoreLocation core, MemoryLocation baseAddress,
				ByteBuffer data, Gather gather)
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
				ByteBuffer data, GathererProtocol protocol, int transactionId,
				int numPackets) throws IOException {
			log.info("streaming {} bytes in {} packets using transaction {}",
					data.remaining(), numPackets, transactionId);
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
				ByteBuffer dataToSend, BitSet missingSeqNums, int transactionId)
				throws IOException {
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
	private static class SeenFlags {
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
