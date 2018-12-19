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

import static java.lang.Thread.sleep;
import static java.util.concurrent.Executors.newFixedThreadPool;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.slf4j.LoggerFactory.getLogger;
import static uk.ac.manchester.spinnaker.front_end.interfaces.buffer_management.MissingSequenceNumbersMessage.computeNumberOfPackets;
import static uk.ac.manchester.spinnaker.front_end.interfaces.buffer_management.MissingSequenceNumbersMessage.createFirst;
import static uk.ac.manchester.spinnaker.front_end.interfaces.buffer_management.MissingSequenceNumbersMessage.createNext;
import static uk.ac.manchester.spinnaker.messages.Constants.SCP_SCAMP_PORT;
import static uk.ac.manchester.spinnaker.messages.Constants.WORD_SIZE;
import static uk.ac.manchester.spinnaker.utils.MathUtils.ceildiv;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;

import uk.ac.manchester.spinnaker.connections.SCPConnection;
import uk.ac.manchester.spinnaker.machine.Chip;
import uk.ac.manchester.spinnaker.machine.ChipLocation;
import uk.ac.manchester.spinnaker.machine.CoreLocation;
import uk.ac.manchester.spinnaker.machine.Machine;
import uk.ac.manchester.spinnaker.machine.tags.IPTag;
import uk.ac.manchester.spinnaker.machine.tags.TrafficIdentifier;
import uk.ac.manchester.spinnaker.storage.BufferManagerStorage.Region;
import uk.ac.manchester.spinnaker.storage.StorageException;
import uk.ac.manchester.spinnaker.transceiver.Transceiver;
import uk.ac.manchester.spinnaker.transceiver.processes.ProcessException;

/**
 * Implementation of the SpiNNaker Fast Data Download Protocol.
 *
 * @author Donal Fellows
 * @author Alan Stokes
 */
public abstract class DataGatherer {
	/**
	 * Logger for the gatherer.
	 */
	protected static final Logger log = getLogger(DataGatherer.class);
	/**
	 * The number of parallel downloads that we do. The size of the thread pool
	 * used to parallelise them.
	 */
	public static final int PARALLEL_SIZE = 1; // TODO
	/**
	 * Retrieves of data that is less than this many bytes are done via a normal
	 * SCAMP memory read.
	 */
	public static final int SMALL_RETRIEVE_THRESHOLD = 256; // TODO
	/**
	 * Maximum number of messages in the message queue. Per parallel download.
	 */
	private static final int QUEUE_CAPACITY = 1024;
	/** The maximum number of times to retry. */
	private static final int TIMEOUT_RETRY_LIMIT = 20;
	/**
	 * The time delay between sending each message. In
	 * {@linkplain TimeUnit#MILLISECONDS milliseconds}.
	 */
	private static final int DELAY_PER_SEND = 10;
	/**
	 * The timeout when receiving a message. In
	 * {@linkplain TimeUnit#MILLISECONDS milliseconds}.
	 */
	private static final int TIMEOUT_PER_RECEIVE = 250;
	/** What is the maximum number of <em>words</em> in a packet? */
	private static final int DATA_PER_FULL_PACKET = 68;
	/**
	 * What is the maximum number of payload <em>words</em> in a packet that
	 * also has a sequence number?
	 */
	private static final int DATA_PER_FULL_PACKET_WITH_SEQUENCE_NUM =
			DATA_PER_FULL_PACKET - 1;
	/** How many bytes for the end-flag? */
	private static final int END_FLAG_SIZE = WORD_SIZE;
	/** How many bytes for the sequence number? */
	private static final int SEQUENCE_NUMBER_SIZE = WORD_SIZE;
	/**
	 * Mask used to pick out the bit that says whether a sequence number is the
	 * last in a stream.
	 */
	private static final int LAST_MESSAGE_FLAG_BIT_MASK = 0x80000000;
	/**
	 * An empty buffer. Used so we don't try to read zero bytes.
	 */
	private static final ByteBuffer EMPTY_DATA = ByteBuffer.allocate(0);
	/**
	 * Message used to report problems.
	 */
	private static final String TIMEOUT_MESSAGE = "failed to hear from the "
			+ "machine (please try removing firewalls)";

	private final Transceiver txrx;
	private final Machine machine;
	private final ExecutorService pool;
	private int missCount;
    private Exception caught;

	/**
	 * Create an instance of the protocol implementation. (Subclasses handle
	 * where to put it afterwards.)
	 *
	 * @param transceiver
	 *            How to talk to the SpiNNaker system via SCP. Where the system
	 *            is located.
	 * @param machine
	 *            The description of the SpiNNaker machine being talked to.
	 * @throws ProcessException
	 *             If we can't discover the machine details due to SpiNNaker
	 *             rejecting messages
	 * @throws IOException
	 *             If we can't discover the machine details due to I/O problems
	 */
	public DataGatherer(Transceiver transceiver, Machine machine)
			throws IOException, ProcessException {
		this.txrx = transceiver;
		this.machine = machine;
		this.pool = newFixedThreadPool(PARALLEL_SIZE);
		this.missCount = 0;
        this.caught = null;
	}

	/**
	 * Request that a particular board be downloaded.
	 *
	 * @param gather
	 *            The reference to the speed up data gatherer for the board,
	 *            together with the information about which bits on the board
	 *            should be downloaded.
	 */
	public void addTask(Gather gather) {
        // No need to keep adding if there is already an exception
		// Note: don't care about synchronisation; it's purely an optimisation
        if (caught == null) {
            pool.execute(() -> downloadBoard(gather));
        }
	}

	/**
	 * Indicates that all tasks have been submitted and waits for them all to
	 * finish (up to a day).
	 *
	 * @return The total number of missed packets. Misses are retried, so this
	 *         is just an assessment of data transfer quality.
	 * @throws IOException
	 *             If IO fails.
	 * @throws ProcessException
	 *             If SpiNNaker rejects a message.
	 * @throws StorageException
	 *             If DB access goes wrong.
	 * @throws InterruptedException
	 *             If the wait is interrupted.
	 */
	public int waitForTasksToFinish() throws IOException, ProcessException,
			StorageException, InterruptedException {
		pool.shutdown();
		pool.awaitTermination(1, TimeUnit.DAYS);
		try {
			if (caught != null) {
				throw caught;
			}
		} catch (IOException | ProcessException | StorageException
				| RuntimeException e) {
			throw e;
		} catch (Exception e) {
			throw new RuntimeException("unexpected exception", e);
		}
		return missCount;
	}

	/**
	 * A connection for handling the Data Speed Up protocol.
	 *
	 * @author Donal Fellows
	 */
	private static class GatherDownloadConnection extends SCPConnection {
		/**
		 * Create an instance.
		 *
		 * @param location
		 *            Where the connection is talking to.
		 * @param iptag
		 *            What IPtag the Data Speed Up protocol is working on.
		 * @throws IOException
		 *             If anything goes wrong with socket setup.
		 */
		GatherDownloadConnection(ChipLocation location, IPTag iptag)
				throws IOException {
			super(location, iptag.getBoardAddress(), SCP_SCAMP_PORT);
		}

		void sendStart(CoreLocation extraMonitorCore, int address, int length)
				throws IOException {
			sendSDPMessage(StartSendingMessage.create(extraMonitorCore, length,
					length));
		}

		void sendFirstMissing(CoreLocation extraMonitorCore,
				IntBuffer missingSeqs, int numPackets) throws IOException {
			sendSDPMessage(
					createFirst(extraMonitorCore, missingSeqs, numPackets));
		}

		void sendNextMissing(CoreLocation extraMonitorCore,
				IntBuffer missingSeqs) throws IOException {
			sendSDPMessage(createNext(extraMonitorCore, missingSeqs));
		}

		BlockingQueue<ByteBuffer> launchReaderThread() {
			BlockingQueue<ByteBuffer> messQueue =
					new ArrayBlockingQueue<>(QUEUE_CAPACITY);
			ReaderThread t = new ReaderThread(messQueue);
			t.start();
			return messQueue;
		}

		/**
		 * The thread that listens for Data Speed Up messages.
		 *
		 * @author Donal Fellows
		 */
		private class ReaderThread extends Thread {
			private final BlockingQueue<ByteBuffer> messQueue;

			ReaderThread(BlockingQueue<ByteBuffer> messQueue) {
				super("ReadThread");
				setDaemon(true);
				this.messQueue = messQueue;
			}

			@Override
			public void run() {
				// While socket is open add messages to the queue
				try {
					mainLoop();
				} catch (InterruptedException e) {
					log.error("failed to offer packet to queue");
				} catch (IOException e) {
					log.error("failed to receive packet", e);
				}
			}

			/**
			 * The main loop of the thread.
			 *
			 * @throws IOException
			 *             If message reception fails.
			 * @throws InterruptedException
			 *             If the message queue is full and we get interrupted
			 *             when trying to put a message on it. (unexpected)
			 */
			private void mainLoop() throws IOException, InterruptedException {
				do {
					try {
						ByteBuffer recvd =
								receive(TIMEOUT_PER_RECEIVE);
						if (recvd != null) {
							messQueue.put(recvd);
							log.debug("pushed");
						}
					} catch (SocketTimeoutException e) {
						log.info("socket timed out");
						messQueue.put(EMPTY_DATA);
					}
				} while (!isClosed());
			}
		}
	}

	/**
	 * Do all the downloads for a board.
	 *
	 * @param gatherer
	 *            The particular gatherer that identifies a board.
	 */
	private void downloadBoard(Gather gatherer) {
		try {
			List<Region> smallRetrieves = new ArrayList<>();
			ChipLocation gathererLocation = gatherer.asChipLocation();
			try (GatherDownloadConnection conn = new GatherDownloadConnection(
					gathererLocation, gatherer.getIptag())) {
				reconfigureIPtag(gatherer.getIptag(), gathererLocation, conn);
				configureBoardRouterTimeouts(gathererLocation, false);
				doDownloads(gatherer.getMonitors(), smallRetrieves, conn,
						conn.launchReaderThread());
			} finally {
				configureBoardRouterTimeouts(gathererLocation, true);
			}
			for (Region r : smallRetrieves) {
				if (r.size > 0) {
					storeData(r, txrx.readMemory(r.core.asChipLocation(),
							r.startAddress, r.size));
				} else {
					storeData(r, EMPTY_DATA);
				}
			}
		} catch (IOException | ProcessException | StorageException
				| RuntimeException e) {
			log.warn("problem when downloading a board's data", e);
			this.caught = e;
		}
	}

	/**
	 * Process all the Data Speed Up activity for a board.
	 *
	 * @param monitors
	 *            The information about what downloads are wanted for a
	 *            particular Data Speed Up Packet Gatherer, by Extra Monitor
	 *            Core.
	 * @param smallRetrieves
	 *            Where to store small retrieves for later handling (via usual
	 *            SCP transfers).
	 * @param conn
	 *            The connection for talking to SpiNNaker
	 * @param messQueue
	 *            The queue of received Data Speed Up messages.
	 * @throws StorageException
	 *             If the database rejects something.
	 * @throws IOException
	 *             If IO on the network fails.
	 * @throws ProcessException
	 *             If SpiNNaker rejects a message.
	 */
	private void doDownloads(Collection<Monitor> monitors,
			List<Region> smallRetrieves, GatherDownloadConnection conn,
			BlockingQueue<ByteBuffer> messQueue)
			throws IOException, ProcessException, StorageException {
		Downloader d = null;
		for (Monitor mon : monitors) {
			for (Placement place : mon.getPlacements()) {
				for (int regionID : place.getVertex().getRecordedRegionIds()) {
					d = handleOneRecordingRegion(conn, messQueue,
							smallRetrieves, d, place, regionID);
				}
			}
		}
	}

	/**
	 * Process a single (recording) region.
	 *
	 * @param conn
	 *            The connection for talking to SpiNNaker
	 * @param messQueue
	 *            The queue of received Data Speed Up messages.
	 * @param smallRetrieves
	 *            Where to store small retrieves for later handling (via usual
	 *            SCP transfers).
	 * @param d
	 *            The downloader object. May be {@code null} if it hasn't been
	 *            allocated yet.
	 * @param place
	 *            Where this region is.
	 * @param regionID
	 *            The ID (index) of this region.
	 * @return The downloader object. May be {@code null} if it hasn't been
	 *         allocated yet.
	 * @throws StorageException
	 *             If the database rejects something.
	 * @throws IOException
	 *             If IO on the network fails.
	 * @throws ProcessException
	 *             If SpiNNaker rejects a message.
	 */
	private Downloader handleOneRecordingRegion(GatherDownloadConnection conn,
			BlockingQueue<ByteBuffer> messQueue, List<Region> smallRetrieves,
			Downloader d, Placement place, int regionID)
			throws StorageException, IOException, ProcessException {
		List<Region> rs = getRegion(place, regionID);
		if (rs.stream().allMatch(r -> r.size < SMALL_RETRIEVE_THRESHOLD)) {
			smallRetrieves.addAll(rs);
			return d;
		}
		for (Region r : rs) {
			if (r.size < 1) {
				smallRetrieves.add(r);
				continue;
			}
			if (r.size < SMALL_RETRIEVE_THRESHOLD) {
				storeData(r, txrx.readMemory(r.core.asChipLocation(),
						r.startAddress, r.size));
			} else {
				if (d == null) {
					d = new Downloader(conn, messQueue);
				}
				storeData(r, d.doDownload(place, r));
			}
		}
		return d;
	}

	/**
	 * Configure an IPTag for use in the Data Speed Up protocol.
	 *
	 * @param iptag
	 *            The tag to configure
	 * @param gathererLocation
	 *            Where the tag is.
	 * @param conn
	 *            How to talk to the gatherer.
	 * @throws IOException
	 *             If message sending or reception fails.
	 * @throws ProcessException
	 *             If SpiNNaker rejects a message.
	 */
	private void reconfigureIPtag(IPTag iptag, ChipLocation gathererLocation,
			GatherDownloadConnection conn)
			throws IOException, ProcessException {
		IPTag tag = new IPTag(iptag.getBoardAddress(), gathererLocation,
				iptag.getTag(), iptag.getIPAddress(), conn.getLocalPort(), true,
				TrafficIdentifier.getInstance("DATA_SPEED_UP"));
		txrx.setIPTag(tag);
	}

	/**
	 * Configure the watchdog timers of the chips on a board.
	 *
	 * @param boardEthernet
	 *            The board, by its ethernet chip.
	 * @param state
	 *            True to turn on the timers, false to switch them off.
	 * @throws IOException
	 *             If message sending or reception fails.
	 * @throws ProcessException
	 *             If SpiNNaker rejects a message.
	 */
	private void configureBoardRouterTimeouts(ChipLocation boardEthernet,
			boolean state) throws IOException, ProcessException {
		for (Chip chip : machine
				.iterChipsOnBoard(machine.getChipAt(boardEthernet))) {
			txrx.enableWatchDogTimerOnChip(chip, state);
		}
	}

	/**
	 * Work out exactly where is going to be downloaded.
	 *
	 * @param placement
	 *            The placement information.
	 * @param regionID
	 *            The region ID.
	 * @return The region descriptors that are the actual download instructions.
	 * @throws IOException
	 *             If communication fails.
	 * @throws ProcessException
	 *             If SpiNNaker rejects a message.
	 * @throws StorageException
	 *             If the database doesn't like something.
	 */
	protected abstract List<Region> getRegion(Placement placement,
			int regionID)
			throws IOException, ProcessException, StorageException;

	/**
	 * Store the data retrieved from a region.
	 *
	 * @param r
	 *            Where the data came from.
	 * @param data
	 *            The data that was retrieved.
	 * @throws StorageException
	 *             If the database refuses to do the store.
	 */
	protected abstract void storeData(Region r, ByteBuffer data)
			throws StorageException;

	/**
	 * Class used to manage a download. Every instance <em>must only</em> ever
	 * be used from one thread.
	 *
	 * @author Donal Fellows
	 */
	private class Downloader {
		private final GatherDownloadConnection conn;
		private final BlockingQueue<ByteBuffer> queue;

		private boolean received;
		private boolean finished;
		private int timeoutcount = 0;
		private BitSet receivedSeqNums;
		private int maxSeqNum;
		private ByteBuffer dataReceiver;
		private CoreLocation extraMonitor;

		/**
		 * Create an instance.
		 *
		 * @param connection
		 *            The connection used to send messages.
		 * @param messageQueue
		 *            The queue used to receive messages.
		 */
		private Downloader(GatherDownloadConnection connection,
				BlockingQueue<ByteBuffer> messageQueue) {
			conn = connection;
			queue = messageQueue;
		}

		/**
		 * Do the downloading.
		 *
		 * @param extraMonitor
		 *            Where to download from.
		 * @param region
		 *            What to download.
		 * @return The downloaded data.
		 * @throws IOException
		 *             If anything unexpected goes wrong.
		 */
		ByteBuffer doDownload(Placement extraMonitor, Region region)
				throws IOException {
			this.extraMonitor = extraMonitor.asCoreLocation();
			dataReceiver = ByteBuffer.allocate(region.size);
			maxSeqNum = ceildiv(region.size,
					DATA_PER_FULL_PACKET_WITH_SEQUENCE_NUM * WORD_SIZE);
			receivedSeqNums = new BitSet(maxSeqNum);
			conn.sendStart(this.extraMonitor, region.startAddress, region.size);
			finished = false;
			received = false;
			try {
				do {
					processOnePacket();
				} while (!finished);
				return dataReceiver;
			} catch (IOException e) {
				throw e;
			} catch (Exception e) {
				finished = true;
			}
			return null;
		}

		/**
		 * Take one message off the queue and process it.
		 *
		 * @throws IOException
		 *             If packet retransmission requesting fails.
		 * @throws InterruptedException
		 *             If any wait is interrupted.
		 */
		private void processOnePacket() throws IOException, InterruptedException {
			ByteBuffer p =
					queue.poll(2 * TIMEOUT_PER_RECEIVE, MILLISECONDS);
			if (p != null && p.hasRemaining()) {
				processData(p);
				received = true;
			} else {
				processTimeout();
			}
		}

		/**
		 * Process a single received packet.
		 *
		 * @param data
		 *            The content of the packet.
		 * @throws IOException
		 *             If the packet is an end-of-stream packet yet there are
		 *             packets outstanding, and the retransmssion causes an
		 *             error.
		 * @throws InterruptedException
		 *             If a wait is interrupted (unexpectedly)
		 */
		private void processData(ByteBuffer data)
				throws IOException, InterruptedException {
			int firstPacketElement = data.getInt();
			int seqNum = firstPacketElement & ~LAST_MESSAGE_FLAG_BIT_MASK;
			boolean isEndOfStream =
					((firstPacketElement & LAST_MESSAGE_FLAG_BIT_MASK) != 0);

			if (seqNum > maxSeqNum || seqNum < 0) {
				throw new IllegalStateException("got insane sequence number");
			}
			int offset =
					seqNum * DATA_PER_FULL_PACKET_WITH_SEQUENCE_NUM * WORD_SIZE;
			int trueDataLength = offset + data.limit() - SEQUENCE_NUMBER_SIZE;
			if (trueDataLength > dataReceiver.capacity()) {
				throw new IllegalStateException(
						"received more data than expected");
			}

			if (!isEndOfStream || data.limit() != END_FLAG_SIZE) {
				data.position(SEQUENCE_NUMBER_SIZE);
				data.get(dataReceiver.array(), offset, trueDataLength - offset);
			}
			receivedSeqNums.set(seqNum);
			if (isEndOfStream) {
				if (!checkAllReceived()) {
					finished |= retransmitMissingSequences();
				} else {
					finished = true;
				}
			}
		}

		/**
		 * Process the fact that the message queue was in a timeout state.
		 *
		 * @throws IOException
		 *             If there are packets outstanding, and the retransmssion
		 *             causes an error.
		 * @throws InterruptedException
		 *             If a wait is interrupted (unexpectedly)
		 */
		private void processTimeout() throws IOException, InterruptedException {
			if (++timeoutcount > TIMEOUT_RETRY_LIMIT && !received) {
				log.error(TIMEOUT_MESSAGE);
				finished = true;
			} else if (!finished) {
				// retransmit missing packets
				log.debug("doing reinjection");
				finished = retransmitMissingSequences();
			}
		}

		private boolean checkAllReceived() {
			int recvsize = receivedSeqNums.length();
			if (recvsize > maxSeqNum + 1) {
				throw new IllegalStateException(
						"received more data than expected");
			}
			return recvsize == maxSeqNum + 1;
		}

		/**
		 * Request that the extra monitor core retransmit some packets.
		 *
		 * @return Whether there were really any packets to retransmit.
		 * @throws IOException
		 *             If there are failures.
		 * @throws InterruptedException
		 *             If a delay is interrupted (unexpectedly)
		 */
		private boolean retransmitMissingSequences()
				throws IOException, InterruptedException {
			/*
			 * Calculate number of missing sequences based on difference between
			 * expected and received, then determine what they really are by
			 * building a buffer of them.
			 */
			IntBuffer missingSeqs = IntBuffer
					.allocate(maxSeqNum - receivedSeqNums.cardinality());
			for (int i = 0; i < maxSeqNum; i++) {
				if (!receivedSeqNums.get(i)) {
					missingSeqs.put(i);
					missCount++;
				}
			}
			missingSeqs.flip();

			if (log.isDebugEnabled()) {
				IntBuffer ib = missingSeqs.asReadOnlyBuffer();
				log.debug("missing " + ib.limit());
				while (ib.hasRemaining()) {
					log.debug("missing seq " + ib.get());
				}
			}

			if (missingSeqs.limit() == 0) {
				// No missing sequences; transfer is complete
				return true;
			}
			int numPackets = computeNumberOfPackets(missingSeqs.limit());

			// Transmit missing sequences as a new SDP Packet
			conn.sendFirstMissing(extraMonitor, missingSeqs, numPackets);
			for (int i = 1; i < numPackets; i++) {
				sleep(DELAY_PER_SEND);
				conn.sendNextMissing(extraMonitor, missingSeqs);
			}
			return false;
		}
	}
}
