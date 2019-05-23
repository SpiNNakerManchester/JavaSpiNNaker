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

import static java.lang.Math.max;
import static java.lang.Math.min;
import static java.nio.ByteBuffer.allocate;
import static java.nio.ByteOrder.LITTLE_ENDIAN;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.slf4j.LoggerFactory.getLogger;
import static uk.ac.manchester.spinnaker.data_spec.Constants.APP_PTR_TABLE_HEADER_SIZE;
import static uk.ac.manchester.spinnaker.data_spec.Constants.MAX_MEM_REGIONS;
import static uk.ac.manchester.spinnaker.front_end.Constants.PARALLEL_SIZE;
import static uk.ac.manchester.spinnaker.front_end.dse.FastDataInCommandID.SEND_DATA_TO_LOCATION;
import static uk.ac.manchester.spinnaker.front_end.dse.FastDataInCommandID.SEND_LAST_DATA_IN;
import static uk.ac.manchester.spinnaker.front_end.dse.FastDataInCommandID.SEND_SEQ_DATA;
import static uk.ac.manchester.spinnaker.messages.Constants.NBBY;
import static uk.ac.manchester.spinnaker.messages.Constants.SDP_PAYLOAD_WORDS;
import static uk.ac.manchester.spinnaker.messages.Constants.WORD_SIZE;
import static uk.ac.manchester.spinnaker.messages.sdp.SDPHeader.Flag.REPLY_NOT_EXPECTED;
import static uk.ac.manchester.spinnaker.utils.MathUtils.ceildiv;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;

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
import uk.ac.manchester.spinnaker.messages.sdp.SDPHeader;
import uk.ac.manchester.spinnaker.messages.sdp.SDPMessage;
import uk.ac.manchester.spinnaker.messages.sdp.SDPPort;
import uk.ac.manchester.spinnaker.storage.ConnectionProvider;
import uk.ac.manchester.spinnaker.storage.DSEStorage;
import uk.ac.manchester.spinnaker.storage.DSEStorage.CoreToLoad;
import uk.ac.manchester.spinnaker.storage.DSEStorage.Ethernet;
import uk.ac.manchester.spinnaker.storage.StorageException;
import uk.ac.manchester.spinnaker.transceiver.SpinnmanException;
import uk.ac.manchester.spinnaker.transceiver.Transceiver;
import uk.ac.manchester.spinnaker.transceiver.processes.ProcessException;
import uk.ac.manchester.spinnaker.utils.UnitConstants;

/**
 * Implementation of the Data Specification Executor that uses the Fast Data In
 * protocol to upload the results to a SpiNNaker machine.
 *
 * @author Donal Fellows
 */
public class FastExecuteDataSpecification extends BoardLocalSupport
		implements AutoCloseable {
	private static final String LOADING_MSG =
			"loading data specifications onto SpiNNaker";
	private static final Logger log =
			getLogger(FastExecuteDataSpecification.class);
	private static final int REGION_TABLE_SIZE = MAX_MEM_REGIONS * WORD_SIZE;
	private static final String IN_REPORT_NAME =
			"speeds_gained_in_speed_up_process.txt";
	/** One kilo-binary unit multiplier. */
	private static final int ONE_KI = 1024;

	/** Items of data a SDP packet can hold when SCP header removed. */
	private static final int BYTES_PER_FULL_PACKET =
			SDP_PAYLOAD_WORDS * WORD_SIZE;
	// 272 bytes as removed SCP header

	/**
	 * Offset where data in starts on first command (command, base_address,
	 * x&amp;y, max_seq_number), in bytes.
	 */
	private static final int OFFSET_AFTER_COMMAND_AND_ADDRESS = 16;

	/**
	 * Offset where data starts after a command id and seq number, in bytes.
	 */
	private static final int OFFSET_AFTER_COMMAND_AND_SEQUENCE = 8;

	/** Size for data to store when first packet with command and address. */
	private static final int DATA_IN_FULL_PACKET_WITH_ADDRESS =
			BYTES_PER_FULL_PACKET - OFFSET_AFTER_COMMAND_AND_ADDRESS;

	/** Size for data in to store when not first packet. */
	private static final int DATA_IN_FULL_PACKET_WITHOUT_ADDRESS =
			BYTES_PER_FULL_PACKET - OFFSET_AFTER_COMMAND_AND_SEQUENCE;

	private static final int TIMEOUT_RETRY_LIMIT = 20;

	/** Sequence number that marks the end of a sequence number stream. */
	private static final int MISSING_SEQS_END = -1;

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
		try (BoardWorker worker = new BoardWorker(board, storage, bar)) {
			List<CoreToLoad> cores = storage.listCoresToLoad(board, false);
			try (NoDropPacketContext context = worker.dontDropPackets()) {
				for (CoreToLoad ctl : cores) {
					log.info("loading data onto {}", ctl.core);
					worker.loadCore(ctl);
				}
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
				w.println("x" + "\t\t y" + "\t\t SDRAM address"
						+ "\t\t size in bytes" + "\t\t\t time took"
						+ "\t\t\t Mb/s" + "\t\t\t missing sequence numbers");
				w.println("------------------------------------------------"
						+ "------------------------------------------------"
						+ "-------------------------------------------------");
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
			w.printf("%d\t\t %d\t\t %#08x\t\t %d\t\t\t\t %f\t\t\t %s\t\t %s\n",
					chip.getX(), chip.getY(),
					Integer.toUnsignedLong(baseAddress),
					Integer.toUnsignedLong(size), timeTaken, mbs,
					missingNumbers);
		}
	}

	private class BoardWorker implements AutoCloseable {
		/**
		 * We don't report writes below this size because they add a lot of
		 * noise and very little information.
		 */
		private static final int VERY_SMALL_WRITE_THRESHOLD = 256;//64;
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
				log.info("generated {} bytes to load onto {}", size, ctl.core);
				int start = malloc(ctl, size);
				int written = writeHeader(ctl.core, executor, start);

				for (MemoryRegion r : executor.regions()) {
					if (!isToBeIgnored(r)) {
						written += writeRegion(ctl.core, r, r.getRegionBase());
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
			long start = System.nanoTime();
			missingSequenceNumbers = new LinkedList<>();
			if (written < VERY_SMALL_WRITE_THRESHOLD) {
				// Faster to use SCP to SCAMP when the data is "small".
				txrx.writeMemory(core.getScampCore(), baseAddress, data);
			} else {
				fastWrite(core, baseAddress, data);
			}
			long end = System.nanoTime();
			if (writeReports && written >= VERY_SMALL_WRITE_THRESHOLD) {
				writeReport(core, end - start, data.limit(), baseAddress,
						missingSequenceNumbers);
			}
			missingSequenceNumbers = null;
			return written;
		}

		NoDropPacketContext dontDropPackets()
				throws IOException, ProcessException {
			return new NoDropPacketContext(txrx,
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
				int expectedMax = sendInitialPackets(baseAddress, data, protocol);

				// Wait for confirmation and do required retransmits
				while (true) {
					try {
						ByteBuffer received = connection.receive();
						timeoutCount = 0; // Reset the timeout counter

						// Decide what to do with the packet
						switch (FastDataInCommandID
								.forValue(received.getInt())) {
						case RECEIVE_FINISHED_DATA_IN:
							// We're done!
							break outerLoop;

						case RECEIVE_FIRST_MISSING_SEQ_DATA_IN:
							remainingMissingPackets = received.getInt();
							log.info(
									"expecting {} packets of missing sequence"
											+ " numbers",
									remainingMissingPackets);
							haveMissing = true;
							break;
						case RECEIVE_MISSING_SEQ_DATA_IN:
							remainingMissingPackets--;
							log.info(
									"another packet of missing sequence "
											+ "numbers; {} left to go",
									remainingMissingPackets);
							if (remainingMissingPackets < 0) {
								log.warn("crazy number of missing packets: {}",
										remainingMissingPackets);
							}
							break;
						default:
							throw new RuntimeException(
									"unexpected response code: "
											+ received.getInt(0));
						}

						/*
						 * The currently received packet has missing sequence
						 * numbers. Accumulate and dispatch when we've got them
						 * all.
						 */

						if (seqNums == null) {
							seqNums = newSequenceNumberCollector();
						}
						log.info("adding {} missed packets",
								received.remaining() / WORD_SIZE);
						while (received.hasRemaining()) {
							int num = received.getInt();
							seqNums.add(num);
							if (num == MISSING_SEQS_END) {
								break;
							}
							if (num < 0 || num > expectedMax) {
								log.warn(
										"crazy expected sequence number {}; "
												+ "expected max is {}",
										num, expectedMax);
							}
						}
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
						//log.info("timeout; restarting socket");
						//connection.restart();
						remainingMissingPackets = 0;
						haveMissing = false;
						if (seqNums == null) {
							log.info("resending initial packets");
							continue outerLoop;
						}
						retransmitMissingPackets(protocol, data, seqNums);
						seqNums = null;
					}
				}
			}
		}

		private LinkedList<Integer> newSequenceNumberCollector() {
			LinkedList<Integer> seqNums = new LinkedList<>();
			missingSequenceNumbers.addLast(seqNums);
			return seqNums;
		}

		private int sendInitialPackets(int baseAddress, ByteBuffer data,
				GathererProtocol protocol) throws IOException {
			int numPackets = ceildiv(
					max(data.remaining() - DATA_IN_FULL_PACKET_WITH_ADDRESS, 0),
					DATA_IN_FULL_PACKET_WITHOUT_ADDRESS) + 1;
			log.info("streaming {} bytes in {} packets", data.remaining(),
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

	/** Manufactures Fast Data In protocol messages. */
	class GathererProtocol {
		private final HasCoreLocation gathererCore;
		private final HasChipLocation monitorCore;

		GathererProtocol(HasChipLocation chip) {
			ChipLocation chipLoc = chip.asChipLocation();
			gathererCore = gathererForChip.get(chipLoc);
			monitorCore = monitorForChip.get(chipLoc);
		}

		private SDPHeader header() {
			return new SDPHeader(REPLY_NOT_EXPECTED, gathererCore,
					SDPPort.GATHERER_DATA_SPEED_UP.value);
		}

		/**
		 * @param baseAddress
		 *            Where the data is to be written.
		 * @param data
		 *            The overall data to be transmitted.
		 * @param numPackets
		 *            How many SDP packets will be sent.
		 * @return The message indicating the start of the data.
		 */
		SDPMessage dataToLocation(int baseAddress, ByteBuffer data,
				int numPackets) {
			HasChipLocation boardDestination = calculateFakeChipID(monitorCore);

			ByteBuffer payload =
					allocate(BYTES_PER_FULL_PACKET).order(LITTLE_ENDIAN);
			payload.putInt(SEND_DATA_TO_LOCATION.value);
			payload.putInt(baseAddress);
			payload.putShort((short) boardDestination.getY());
			payload.putShort((short) boardDestination.getX());
			payload.putInt(numPackets - 1);
			putBuffer(data, 0, payload);
			return new SDPMessage(header(), payload);
		}

		/**
		 * @param data
		 *            The overall data to be transmitted.
		 * @param seqNum
		 *            The sequence number of this chunk.
		 * @return The message containing a chunk of the data.
		 */
		SDPMessage seqData(ByteBuffer data, int seqNum) {
			ByteBuffer payload =
					allocate(BYTES_PER_FULL_PACKET).order(LITTLE_ENDIAN);
			int position = calculatePositionFromSequenceNumber(seqNum);
			if (position >= data.limit()) {
				throw new RuntimeException(
						"attempt to write off end of buffer due to "
								+ "over-large sequence number (" + seqNum
								+ ") given that only " + data.limit()
								+ " bytes are to be sent");
			}
			payload.putInt(SEND_SEQ_DATA.value);
			payload.putInt(seqNum);
			putBuffer(data, position, payload);
			return new SDPMessage(header(), payload);
		}

		private int putBuffer(ByteBuffer data, int position,
				ByteBuffer payload) {
			ByteBuffer tmp = data.asReadOnlyBuffer();
			tmp.position(position);
			ByteBuffer slice = tmp.slice();
			slice.limit(min(slice.limit(), payload.remaining()));
			payload.put(slice).flip();
			return slice.position();
		}

		private int calculatePositionFromSequenceNumber(int seqNum) {
			if (seqNum < 1) {
				return 0;
			}
			return DATA_IN_FULL_PACKET_WITH_ADDRESS
					+ DATA_IN_FULL_PACKET_WITHOUT_ADDRESS * (seqNum - 1);
		}

		/**
		 * @return The message indicating the end of the data.
		 */
		SDPMessage lastDataIn() {
			ByteBuffer payload = allocate(WORD_SIZE).order(LITTLE_ENDIAN);
			payload.putInt(SEND_LAST_DATA_IN.value);
			payload.flip();
			return new SDPMessage(header(), payload);
		}

		/**
		 * Converts between real and board based fake chip IDs.
		 *
		 * @param chip
		 *            the real chip coordinates in the real machine
		 * @return chip coordinates for the real chip as if it was 1 board
		 *         machine
		 */
		private ChipLocation calculateFakeChipID(HasChipLocation chip) {
			int fakeX = chip.getX() - gathererCore.getX();
			if (fakeX < 0) {
				fakeX += machine.maxChipX() + 1;
			}
			int fakeY = chip.getY() - gathererCore.getY();
			if (fakeY < 0) {
				fakeY += machine.maxChipY() + 1;
			}
			return new ChipLocation(fakeX, fakeY);
		}
	}
}
