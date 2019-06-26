/*
 * Copyright (c) 2019 The University of Manchester
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

import static difflib.DiffUtils.diff;
import static java.lang.Integer.toUnsignedLong;
import static java.lang.Long.toHexString;
import static java.lang.System.getProperty;
import static java.lang.System.nanoTime;
import static java.nio.ByteBuffer.allocate;
import static java.nio.ByteOrder.LITTLE_ENDIAN;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.stream.Collectors.toList;
import static java.util.stream.IntStream.range;
import static org.slf4j.LoggerFactory.getLogger;
import static uk.ac.manchester.spinnaker.data_spec.Constants.APP_PTR_TABLE_HEADER_SIZE;
import static uk.ac.manchester.spinnaker.data_spec.Constants.MAX_MEM_REGIONS;
import static uk.ac.manchester.spinnaker.front_end.Constants.PARALLEL_SIZE;
import static uk.ac.manchester.spinnaker.front_end.dse.FastDataInProtocol.computeNumPackets;
import static uk.ac.manchester.spinnaker.messages.Constants.NBBY;
import static uk.ac.manchester.spinnaker.messages.Constants.WORD_SIZE;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;

import difflib.ChangeDelta;
import difflib.Chunk;
import difflib.DeleteDelta;
import difflib.Delta;
import difflib.InsertDelta;
import uk.ac.manchester.spinnaker.data_spec.Executor;
import uk.ac.manchester.spinnaker.data_spec.MemoryRegion;
import uk.ac.manchester.spinnaker.data_spec.exceptions.DataSpecificationException;
import uk.ac.manchester.spinnaker.front_end.BasicExecutor;
import uk.ac.manchester.spinnaker.front_end.BoardLocalSupport;
import uk.ac.manchester.spinnaker.front_end.NoDropPacketContext;
import uk.ac.manchester.spinnaker.front_end.Progress;
import uk.ac.manchester.spinnaker.front_end.download.request.Gather;
import uk.ac.manchester.spinnaker.front_end.download.request.Monitor;
import uk.ac.manchester.spinnaker.machine.ChipLocation;
import uk.ac.manchester.spinnaker.machine.CoreLocation;
import uk.ac.manchester.spinnaker.machine.CoreSubsets;
import uk.ac.manchester.spinnaker.machine.HasChipLocation;
import uk.ac.manchester.spinnaker.machine.HasCoreLocation;
import uk.ac.manchester.spinnaker.machine.Machine;
import uk.ac.manchester.spinnaker.messages.model.AppID;
import uk.ac.manchester.spinnaker.messages.model.UnexpectedResponseCodeException;
import uk.ac.manchester.spinnaker.storage.ConnectionProvider;
import uk.ac.manchester.spinnaker.storage.DSEStorage;
import uk.ac.manchester.spinnaker.storage.DSEStorage.CoreToLoad;
import uk.ac.manchester.spinnaker.storage.DSEStorage.Ethernet;
import uk.ac.manchester.spinnaker.storage.StorageException;
import uk.ac.manchester.spinnaker.transceiver.SpinnmanException;
import uk.ac.manchester.spinnaker.transceiver.Transceiver;
import uk.ac.manchester.spinnaker.transceiver.processes.ProcessException;
import uk.ac.manchester.spinnaker.utils.MathUtils;
import uk.ac.manchester.spinnaker.utils.UnitConstants;

/**
 * Implementation of the Data Specification Executor that uses the Fast Data In
 * protocol to upload the results to a SpiNNaker machine.
 *
 * @author Donal Fellows
 */
public class FastExecuteDataSpecification extends BoardLocalSupport
		implements AutoCloseable {
	private static final Logger log =
			getLogger(FastExecuteDataSpecification.class);
	private static final String SPINNAKER_COMPARE_UPLOAD =
			getProperty("spinnaker.compare.upload");

	private static final String LOADING_MSG =
			"loading data specifications onto SpiNNaker";
	private static final int REGION_TABLE_SIZE = MAX_MEM_REGIONS * WORD_SIZE;
	private static final String IN_REPORT_NAME =
			"speeds_gained_in_speed_up_process.tsv";
	/** One kilo-binary unit multiplier. */
	private static final int ONE_KI = 1024;

	private static final int TIMEOUT_RETRY_LIMIT = 20;

	/** Sequence number that marks the end of a sequence number stream. */
	private static final int MISSING_SEQS_END = -1;
	/**
	 * The point below which we use SCP anyway.
	 * <p>
	 * We also don't report writes below this size because they add a lot of
	 * noise and very little information.
	 */
	private static final int VERY_SMALL_WRITE_THRESHOLD = 256;

	private final Transceiver txrx;
	private final Map<ChipLocation, Gather> gathererForChip;
	private final Map<ChipLocation, Monitor> monitorForChip;
	private final Map<ChipLocation, CoreSubsets> monitorsForBoard;
	private final BasicExecutor executor;
	private final Machine machine;
	private boolean writeReports = false;
	private File reportPath = null;

	/**
	 * Create an instance of this class.
	 *
	 * @param machine
	 *            The SpiNNaker machine description.
	 * @param gatherers
	 *            The description of where the gatherers and monitors are.
	 * @param reportDir
	 *            Where to write reports, or {@code null} if no reports are to
	 *            be written.
	 * @throws IOException
	 *             If IO goes wrong.
	 * @throws ProcessException
	 *             If SpiNNaker rejects a message.
	 */
	public FastExecuteDataSpecification(Machine machine, List<Gather> gatherers,
			File reportDir) throws IOException, ProcessException {
		super(machine);
		this.machine = machine;
		executor = new BasicExecutor(PARALLEL_SIZE);

		if (reportDir != null) {
			writeReports = true;
			reportPath = new File(reportDir, IN_REPORT_NAME);
		}

		gathererForChip = new HashMap<>();
		monitorForChip = new HashMap<>();
		monitorsForBoard = new HashMap<>();
		buildMaps(gatherers);

		try {
			txrx = new Transceiver(machine);
		} catch (SpinnmanException e) {
			throw new IllegalStateException("failed to talk to BMP, "
					+ "but that shouldn't have happened at all", e);
		}
	}

	private void buildMaps(List<Gather> gatherers) {
		for (Gather g : gatherers) {
			ChipLocation gathererChip = g.asChipLocation();
			gathererForChip.put(gathererChip, g);
			CoreSubsets boardMonitorCores = monitorsForBoard
					.computeIfAbsent(gathererChip, x -> new CoreSubsets());
			for (Monitor m : g.getMonitors()) {
				ChipLocation monitorChip = m.asChipLocation();
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
	 * @param connection
	 *            The handle to the database.
	 * @throws StorageException
	 *             If the database can't be talked to.
	 * @throws IOException
	 *             If the transceiver can't talk to its sockets.
	 * @throws ProcessException
	 *             If SpiNNaker rejects a message.
	 * @throws DataSpecificationException
	 *             If a data specification in the database is invalid.
	 */
	public void loadCores(ConnectionProvider<DSEStorage> connection)
			throws StorageException, IOException, ProcessException,
			DataSpecificationException {
		DSEStorage storage = connection.getStorageInterface();
		List<Ethernet> ethernets = storage.listEthernetsToLoad();
		int opsToRun = storage.countWorkRequired();
		try (Progress bar = new Progress(opsToRun, LOADING_MSG)) {
			executor.submitTasks(ethernets.stream()
					.map(board -> () -> loadBoard(board, storage, bar)))
					.awaitAndCombineExceptions();
		} catch (StorageException | IOException | ProcessException
				| DataSpecificationException | RuntimeException e) {
			throw e;
		} catch (Exception e) {
			throw new IllegalStateException("unexpected exception", e);
		}
	}

	private void loadBoard(Ethernet board, DSEStorage storage, Progress bar)
			throws IOException, ProcessException, DataSpecificationException,
			StorageException {
		try (BoardWorker worker = new BoardWorker(board, storage, bar);
				SystemRouterTableContext routers = worker.systemRouterTables();
				NoDropPacketContext context = worker.dontDropPackets()) {
			List<CoreToLoad> cores = storage.listCoresToLoad(board, false);
			if (!cores.isEmpty()) {
				log.info("loading data onto {} cores on board", cores.size());
			}
			for (CoreToLoad ctl : cores) {
				worker.loadCore(ctl);
			}
		}
	}

	@Override
	public void close() throws IOException {
		try {
			txrx.close();
		} catch (IOException | RuntimeException e) {
			throw e;
		} catch (Exception e) {
			throw new IllegalStateException("unexpected failure in close", e);
		}
	}

	private static boolean isToBeIgnored(MemoryRegion r) {
		return r == null || r.isUnfilled() || r.getMaxWritePointer() <= 0;
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
			int size, int baseAddress, List<?> missingNumbers)
			throws IOException {
		if (!reportPath.exists()) {
			try (PrintWriter w = open(reportPath, false)) {
				w.println("x" + "\ty" + "\tSDRAM address" + "\tsize/bytes"
						+ "\ttime taken/s" + "\ttransfer rate/(Mb/s)"
						+ "\tmissing sequence numbers");
			}
		}

		float timeTaken = timeDiff / (float) UnitConstants.NSEC_PER_SEC;
		float megabits = (size * (long) NBBY) / (float) (ONE_KI * ONE_KI);
		String mbs;
		if (timeDiff == 0) {
			mbs = "unknown, below threshold";
		} else {
			mbs = String.format("%f", megabits / timeTaken);
		}
		try (PrintWriter w = open(reportPath, true)) {
			w.printf("%d\t%d\t%#08x\t%d\t%f\t%s\t%s\n", chip.getX(),
					chip.getY(), toUnsignedLong(baseAddress),
					toUnsignedLong(size), timeTaken, mbs, missingNumbers);
		}
	}

	private static void compareBuffers(ByteBuffer original,
			ByteBuffer downloaded) {
		for (int i = 0; i < original.remaining(); i++) {
			if (original.get(i) != downloaded.get(i)) {
				log.error("downloaded buffer contents different");
				for (Delta<Byte> delta : diff(list(original), list(downloaded))
						.getDeltas()) {
					if (delta instanceof ChangeDelta) {
						Chunk<Byte> delete = delta.getOriginal();
						Chunk<Byte> insert = delta.getRevised();
						log.warn(
								"swapped {} bytes (SCP) for {} (gather) "
										+ "at {}->{}",
								delete.getLines().size(),
								insert.getLines().size(), delete.getPosition(),
								insert.getPosition());
						log.info("change {} -> {}", describeChunk(delete),
								describeChunk(insert));
					} else if (delta instanceof DeleteDelta) {
						Chunk<Byte> delete = delta.getOriginal();
						log.warn("gather deleted {} bytes at {}",
								delete.getLines().size(), delete.getPosition());
						log.info("delete {}", describeChunk(delete));
					} else if (delta instanceof InsertDelta) {
						Chunk<Byte> insert = delta.getRevised();
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
		List<Byte> l = new ArrayList<>();
		ByteBuffer b = buffer.asReadOnlyBuffer();
		while (b.hasRemaining()) {
			l.add(b.get());
		}
		return l;
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
	 */
	private class BoardWorker implements AutoCloseable {
		private Ethernet board;
		private DSEStorage storage;
		private Progress bar;
		private ThrottledConnection connection;
		private LinkedList<LinkedList<Integer>> missingSequenceNumbers;
		private BoardLocal logContext;

		BoardWorker(Ethernet board, DSEStorage storage, Progress bar)
				throws IOException {
			this.board = board;
			this.logContext = new BoardLocal(board.location);
			this.storage = storage;
			this.bar = bar;
			connection = new ThrottledConnection(board);
			try {
				connection.reprogramTag(
						gathererForChip.get(board.location).getIptag());
			} catch (UnexpectedResponseCodeException e) {
				throw new IOException("failed to reprogram IPtag", e);
			}
		}

		@Override
		public void close() throws IOException {
			connection.close();
			logContext.close();
		}

		/**
		 * Execute a data specification and load the results onto a core.
		 *
		 * @param ctl
		 *            The definition of what to run and where to send the
		 *            results.
		 * @throws IOException
		 *             If anything goes wrong with I/O.
		 * @throws ProcessException
		 *             If SCAMP rejects the request.
		 * @throws DataSpecificationException
		 *             If the instructions to build the data are wrong.
		 * @throws StorageException
		 *             If the database access fails.
		 */
		protected void loadCore(CoreToLoad ctl) throws IOException,
				ProcessException, DataSpecificationException, StorageException {
			ByteBuffer ds;
			try {
				ds = ctl.getDataSpec();
			} catch (StorageException e) {
				throw new DataSpecificationException(String.format(
						"failed to read data specification on "
								+ "core %s of board %s (%s)",
						ctl.core, board.location, board.ethernetAddress), e);
			}
			try (Executor executor =
					new Executor(ds, machine.getChipAt(ctl.core).sdram)) {
				executor.execute();
				int size = executor.getConstructedDataSize();
				int start = malloc(ctl, size);
				log.info(
						"generated {} bytes to load onto {} into memory "
								+ "starting at {}",
						size, ctl.core, toHexString(toUnsignedLong(start)));
				int written = writeHeader(ctl.core, executor, start);

				for (MemoryRegion r : executor.regions()) {
					if (!isToBeIgnored(r)) {
						written += writeRegion(ctl.core, r, r.getRegionBase());
						if (SPINNAKER_COMPARE_UPLOAD != null) {
							ByteBuffer readBack =
									txrx.readMemory(ctl.core, r.getRegionBase(),
											r.getRegionData().remaining());
							compareBuffers(r.getRegionData(), readBack);
						}
					}
				}

				int user0 = txrx.getUser0RegisterAddress(ctl.core);
				txrx.writeMemory(ctl.core, user0, start);
				bar.update();
				storage.saveLoadingMetadata(ctl, start, size, written);
			} catch (DataSpecificationException e) {
				throw new DataSpecificationException(String.format(
						"failed to execute data specification on "
								+ "core %s of board %s (%s)",
						ctl.core, board.location, board.ethernetAddress), e);
			} catch (IOException e) {
				throw new IOException(String.format(
						"failed to upload built data to "
								+ "core %s of board %s (%s)",
						ctl.core, board.location, board.ethernetAddress), e);
			} catch (StorageException e) {
				throw new StorageException(String.format(
						"failed to record results of data specification for "
								+ "core %s of board %s (%s)",
						ctl.core, board.location, board.ethernetAddress), e);
			}
		}

		private int malloc(CoreToLoad ctl, int bytesUsed)
				throws IOException, ProcessException {
			return txrx.mallocSDRAM(ctl.core, bytesUsed, new AppID(ctl.appID));
		}

		/**
		 * Writes the header section.
		 *
		 * @param core
		 *            Which core to write to. Does not need to refer to a
		 *            monitor core.
		 * @param executor
		 *            The executor which generates the header.
		 * @param startAddress
		 *            Where to write the header.
		 * @return How many bytes were actually written.
		 * @throws IOException
		 *             If anything goes wrong with I/O.
		 * @throws ProcessException
		 *             If SCAMP rejects the request.
		 */
		private int writeHeader(HasCoreLocation core, Executor executor,
				int startAddress) throws IOException, ProcessException {
			ByteBuffer b =
					allocate(APP_PTR_TABLE_HEADER_SIZE + REGION_TABLE_SIZE)
							.order(LITTLE_ENDIAN);

			executor.addHeader(b);
			executor.addPointerTable(b, startAddress);

			b.flip();
			int written = b.remaining();
			txrx.writeMemory(core, startAddress, b);
			return written;
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
		 * @return How many bytes were actually written.
		 * @throws IOException
		 *             If anything goes wrong with I/O.
		 * @throws ProcessException
		 *             If SCAMP rejects the request.
		 */
		private int writeRegion(CoreLocation core, MemoryRegion region,
				int baseAddress) throws IOException, ProcessException {
			ByteBuffer data = region.getRegionData().duplicate();

			data.flip();
			int written = data.remaining();
			missingSequenceNumbers = new LinkedList<>();
			long start = nanoTime();
			if (written < VERY_SMALL_WRITE_THRESHOLD) {
				// Faster to use SCP to SCAMP when the data is "small".
				txrx.writeMemory(core.getScampCore(), baseAddress, data);
			} else {
				fastWrite(core, baseAddress, data);
			}
			long end = nanoTime();
			if (writeReports && written >= VERY_SMALL_WRITE_THRESHOLD) {
				writeReport(core, end - start, data.limit(), baseAddress,
						missingSequenceNumbers);
			}
			missingSequenceNumbers = null;
			return written;
		}

		/**
		 * Put the board in don't-drop-packets mode.
		 *
		 * @return An object that, when closed, will put the board back in
		 *         standard mode.
		 * @throws IOException
		 *             If anything goes wrong with communication.
		 * @throws ProcessException
		 *             If SpiNNaker rejects a message.
		 */
		NoDropPacketContext dontDropPackets()
				throws IOException, ProcessException {
			return new NoDropPacketContext(txrx,
					monitorsForBoard.get(board.location));
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
		 */
		SystemRouterTableContext systemRouterTables()
				throws IOException, ProcessException {
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
		 */
		private void fastWrite(CoreLocation core, int baseAddress,
				ByteBuffer data) throws IOException {
			boolean haveMissing = false;
			int remainingMissingPackets = 0;
			int timeoutCount = 0;
			LinkedList<Integer> seqNums = null;
			GathererProtocol protocol = new GathererProtocol(core);

			outerLoop: while (true) {
				// Do the initial blast of data
				int expectedMax =
						sendInitialPackets(baseAddress, data, protocol);

				// Wait for confirmation and do required retransmits
				while (true) {
					try {
						IntBuffer received = connection.receive();
						timeoutCount = 0; // Reset the timeout counter

						// Decide what to do with the packet
						switch (FastDataInCommandID.forValue(received.get())) {
						case RECEIVE_FINISHED_DATA_IN:
							// We're done!
							break outerLoop;

						case RECEIVE_FIRST_MISSING_SEQ_DATA_IN:
							if (!received.hasRemaining()) {
								throw new BadDataInMessageException(
										received.get(0), received);
							}
							remainingMissingPackets = received.get();
							if (remainingMissingPackets > expectedMax) {
								throw new CrazySequenceNumberException(
										remainingMissingPackets, received);
							}
							log.info(
									"expecting {} packets of missing "
											+ "sequence numbers",
									remainingMissingPackets);
							if (!received.hasRemaining()) {
								throw new BadDataInMessageException(
										received.get(0), received);
							}
							haveMissing = true;
							break;
						case RECEIVE_MISSING_SEQ_DATA_IN:
							if (!received.hasRemaining()) {
								throw new BadDataInMessageException(
										received.get(0), received);
							}
							remainingMissingPackets--;
							if (remainingMissingPackets < 0) {
								throw new CrazySequenceNumberException(
										remainingMissingPackets, received);
							}
							log.debug(
									"another packet (#{}) of missing "
											+ "sequence numbers; {} left",
									received.get(1), remainingMissingPackets);
							break;
						default:
							throw new BadDataInMessageException(received.get(0),
									received);
						}

						/*
						 * The currently received packet has missing sequence
						 * numbers. Accumulate and dispatch when we've got them
						 * all.
						 */

						if (seqNums == null) {
							seqNums = newSequenceNumberCollector();
						}
						addMissedSeqNums(received, seqNums, expectedMax);
						if ((haveMissing && remainingMissingPackets <= 0)
								|| (!seqNums.isEmpty() && seqNums
										.peekLast() == MISSING_SEQS_END)) {
							retransmitMissingPackets(protocol, data, seqNums);
							seqNums = null;
							remainingMissingPackets = 0;
							haveMissing = false;
						}
					} catch (SocketTimeoutException e) {
						if (timeoutCount++ > TIMEOUT_RETRY_LIMIT) {
							throw e;
						}
						// log.info("timeout; restarting socket");
						// connection.restart();
						remainingMissingPackets = 0;
						haveMissing = false;
						if (seqNums == null) {
							log.info("full timeout; resending initial packets");
							connection.restart();
							connection.increaseThrottleDelay();
							continue outerLoop;
						}
						retransmitMissingPackets(protocol, data, seqNums);
						seqNums = null;
					}
				}
			}
		}

		private void addMissedSeqNums(IntBuffer received, List<Integer> seqNums,
				int expectedMax) {
			int actuallyAdded = 0;
			String addedEnd = "";
			while (received.hasRemaining()) {
				int num = received.get();
				seqNums.add(num);
				if (num == MISSING_SEQS_END) {
					addedEnd = " (and END marker)";
					break;
				}
				actuallyAdded++;
				if (num < 0 || num > expectedMax) {
					throw new CrazySequenceNumberException(num, received);
				}
			}
			log.debug("added {} missed packets{}", actuallyAdded, addedEnd);
		}

		private LinkedList<Integer> newSequenceNumberCollector() {
			LinkedList<Integer> seqNums = new LinkedList<>();
			missingSequenceNumbers.addLast(seqNums);
			return seqNums;
		}

		private int sendInitialPackets(int baseAddress, ByteBuffer data,
				GathererProtocol protocol) throws IOException {
			int numPackets = computeNumPackets(data);
			log.debug("streaming {} bytes in {} packets", data.remaining(),
					numPackets);
			log.debug("sending packet #{}", 0);
			connection.send(
					protocol.dataToLocation(baseAddress, data, numPackets));
			for (int seqNum = 1; seqNum < numPackets; seqNum++) {
				log.debug("sending packet #{}", seqNum);
				connection.send(protocol.seqData(data, seqNum));
			}
			log.debug("sending terminating packet");
			connection.send(protocol.lastDataIn());
			return numPackets;
		}

		private void retransmitMissingPackets(GathererProtocol protocol,
				ByteBuffer dataToSend, List<Integer> missingSeqNums)
				throws IOException {
			log.info("retransmitting {} packets", missingSeqNums.size());
			for (int seqNum : missingSeqNums) {
				if (seqNum == MISSING_SEQS_END) {
					continue;
				}
				log.debug("resending packet #{}", seqNum);
				connection.send(protocol.seqData(dataToSend, seqNum));
			}
			log.debug("sending terminating packet");
			connection.send(protocol.lastDataIn());
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
