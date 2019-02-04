/*
 * Copyright (c) 2018-2019 The University of Manchester
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
package uk.ac.manchester.spinnaker.front_end.download;

import static difflib.DiffUtils.diff;
import static java.lang.Thread.sleep;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.stream.Collectors.toList;
import static org.slf4j.LoggerFactory.getLogger;
import static uk.ac.manchester.spinnaker.front_end.Constants.PARALLEL_SIZE;
import static uk.ac.manchester.spinnaker.front_end.download.MissingSequenceNumbersMessage.createMessages;
import static uk.ac.manchester.spinnaker.messages.Constants.SCP_SCAMP_PORT;
import static uk.ac.manchester.spinnaker.messages.Constants.WORD_SIZE;
import static uk.ac.manchester.spinnaker.messages.model.CPUState.RUNNING;
import static uk.ac.manchester.spinnaker.utils.MathUtils.ceildiv;

import java.io.EOFException;
import java.io.IOException;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import org.slf4j.Logger;

import difflib.ChangeDelta;
import difflib.Chunk;
import difflib.DeleteDelta;
import difflib.Delta;
import difflib.InsertDelta;
import uk.ac.manchester.spinnaker.connections.SCPConnection;
import uk.ac.manchester.spinnaker.connections.selectors.ConnectionSelector;
import uk.ac.manchester.spinnaker.connections.selectors.MostDirectConnectionSelector;
import uk.ac.manchester.spinnaker.front_end.BasicExecutor;
import uk.ac.manchester.spinnaker.front_end.BasicExecutor.Tasks;
import uk.ac.manchester.spinnaker.front_end.download.request.Gather;
import uk.ac.manchester.spinnaker.front_end.download.request.Monitor;
import uk.ac.manchester.spinnaker.front_end.download.request.Placement;
import uk.ac.manchester.spinnaker.machine.ChipLocation;
import uk.ac.manchester.spinnaker.machine.CoreLocation;
import uk.ac.manchester.spinnaker.machine.CoreSubsets;
import uk.ac.manchester.spinnaker.machine.Machine;
import uk.ac.manchester.spinnaker.machine.tags.IPTag;
import uk.ac.manchester.spinnaker.machine.tags.TrafficIdentifier;
import uk.ac.manchester.spinnaker.messages.model.ReinjectionStatus;
import uk.ac.manchester.spinnaker.messages.model.RouterTimeout;
import uk.ac.manchester.spinnaker.storage.BufferManagerStorage.Region;
import uk.ac.manchester.spinnaker.storage.StorageException;
import uk.ac.manchester.spinnaker.transceiver.Transceiver;
import uk.ac.manchester.spinnaker.transceiver.processes.ProcessException;
import uk.ac.manchester.spinnaker.utils.MathUtils;

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
	 * Retrieves of data that is less than this many bytes are done via a normal
	 * SCAMP memory read.
	 */
	public static final int SMALL_RETRIEVE_THRESHOLD = 40000;
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
	private static final int TIMEOUT_PER_RECEIVE = 1000;
	/**
	 * The <i>extra</i> timeout for processing the message queue. In
	 * {@linkplain TimeUnit#MILLISECONDS milliseconds}.
	 */
	private static final int INTERNAL_DELAY = 100;
	/** What is the maximum number of <em>words</em> in a packet? */
	private static final int WORDS_PER_PACKET = 68;
	/**
	 * What is the maximum number of payload <em>words</em> in a packet that
	 * also has a sequence number? This is one less than the total maximum
	 * number of words in an SDP packet; that extra word is the control word
	 * which encodes the sequence number and the end-of-stream flag.
	 */
	private static final int DATA_WORDS_PER_PACKET = WORDS_PER_PACKET - 1;
	/**
	 * Mask used to pick out the bit that says whether a sequence number is the
	 * last in a stream.
	 */
	private static final int LAST_MESSAGE_FLAG_BIT_MASK = 0x80000000;
	/** An empty buffer. Used so we don't try to read zero bytes. */
	private static final ByteBuffer EMPTY_DATA = ByteBuffer.allocate(0);
	/**
	 * An empty buffer used to mark the end of the stream. <i>Only check for
	 * this by reference equality, not by calling any method.</i>
	 */
	private static final ByteBuffer EOF = ByteBuffer.allocate(0);
	/** Message used to report problems. */
	private static final String TIMEOUT_MESSAGE = "failed to hear from the "
			+ "machine (please try removing firewalls)";
	/** The traffic ID for this protocol. */
	private static final TrafficIdentifier TRAFFIC_ID =
			TrafficIdentifier.getInstance("DATA_SPEED_UP");
	private static final String SPINNAKER_COMPARE_DOWNLOAD =
			System.getProperty("spinnaker.compare.download");

	private final Transceiver txrx;
	private final BasicExecutor pool;
	private int missCount;
	private Machine machine;
	private Tasks tasks;

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
		this.pool = new BasicExecutor(PARALLEL_SIZE);
		this.missCount = 0;
	}

	/**
	 * Request that a collection of boards be downloaded.
	 *
	 * @param gatherers
	 *            The data gatherer information for the boards.
	 */
	public void addTasks(List<Gather> gatherers) {
		ConnectionSelector<?> sel = txrx.getScampConnectionSelector();
		MostDirectConnectionSelector<?> s = null;
		if (sel instanceof MostDirectConnectionSelector) {
			s = (MostDirectConnectionSelector<?>) sel;
		}

		// Sanity check the inputs
		for (Gather g : gatherers) {
			if (machine.getChipAt(g).ipAddress == null) {
				throw new IllegalStateException(
						"gatherer on chip without IP address: "
								+ g.asChipLocation());
			}
			if (s != null && !s.hasDirectConnectionFor(machine.getChipAt(g))) {
				throw new IllegalStateException(
						"gatherer at " + g.asCoreLocation()
								+ " without direct route in transceiver");
			}
		}

		// Do the actual submissions
		tasks = pool.submitTasks(
				gatherers.stream().map(g -> () -> downloadBoard(g)));
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
	 *             If the wait (for the internal thread pool to finish) is
	 *             interrupted.
	 */
	public int waitForTasksToFinish() throws IOException, ProcessException,
			StorageException, InterruptedException {
		try {
			tasks.awaitAndCombineExceptions();
		} catch (IOException | ProcessException | StorageException
				| RuntimeException e) {
			throw e;
		} catch (TimeoutException e) {
			System.exit(1);
		} catch (Exception e) {
			throw new RuntimeException("unexpected exception", e);
		}
		return missCount;
	}

	/**
	 * Do all the downloads for a board.
	 *
	 * @param gatherer
	 *            The particular gatherer that identifies a board.
	 */
	private void downloadBoard(Gather gatherer) throws Exception {
		List<Region> smallRetrieves = new ArrayList<>();
		ChipLocation gathererChip = gatherer.asChipLocation();
		CoreSubsets monitorCores = new CoreSubsets();
		for (Monitor monitor : gatherer.getMonitors()) {
			monitorCores.addCore(monitor.asCoreLocation());
		}
		try (GatherDownloadConnection conn = new GatherDownloadConnection(
				gathererChip, gatherer.getIptag())) {
			log.info("reconfiguring IPtag to point to receiving socket");
			reconfigureIPtag(gatherer.getIptag(), gathererChip, conn);
			try (DoNotDropPackets dnd =
					new DoNotDropPackets(gathererChip, monitorCores)) {
				doDownloads(gatherer.getMonitors(), smallRetrieves, conn);
			}
		}
		if (!smallRetrieves.isEmpty()) {
			log.info("performing additional retrievals of "
					+ "small data blocks");
		}
		for (Region r : smallRetrieves) {
			if (r.size > 0) {
				storeData(r, txrx.readMemory(r.core.asChipLocation(),
						r.startAddress, r.size));
			} else {
				storeData(r, EMPTY_DATA);
			}
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
	 *            The connection for talking to SpiNNaker.
	 * @throws StorageException
	 *             If the database rejects something.
	 * @throws IOException
	 *             If IO on the network fails.
	 * @throws ProcessException
	 *             If SpiNNaker rejects a message.
	 * @throws TimeoutException
	 *             If things time out unrecoverably.
	 */
	private void doDownloads(Collection<Monitor> monitors,
			List<Region> smallRetrieves, GatherDownloadConnection conn)
			throws IOException, ProcessException, StorageException,
			TimeoutException {
		Downloader dl = new Downloader(conn);
		for (Monitor mon : monitors) {
			CoreLocation monitor = mon.asCoreLocation();
			for (Placement place : mon.getPlacements()) {
				if (!place.onSameChipAs(monitor)) {
					throw new IllegalArgumentException(
							"cannot gather from placement of "
									+ place.getVertex().getLabel() + " ("
									+ place.asChipLocation()
									+ ") via monitor on " + monitor
									+ ": different chip");
				}
				if (log.isInfoEnabled()) {
					log.info(
							"downloading recording regions of vertex {} from "
									+ "{} via {}",
							place.getVertex().getLabel(),
							place.asCoreLocation(), monitor);
				}
				for (int regionID : place.getVertex().getRecordedRegionIds()) {
					handleOneRecordingRegion(smallRetrieves, dl, place,
							regionID, monitor);
				}
			}
		}
	}

	/**
	 * Process a single (recording) region.
	 *
	 * @param smallRetrieves
	 *            Where to store small retrieves for later handling (via usual
	 *            SCP transfers).
	 * @param downloader
	 *            The downloader object.
	 * @param place
	 *            Where this region is.
	 * @param regionID
	 *            The ID (index) of this region.
	 * @param extraMonitor
	 *            The monitor core to <i>actually</i> fetch the data from.
	 * @throws StorageException
	 *             If the database rejects something.
	 * @throws IOException
	 *             If IO on the network fails.
	 * @throws ProcessException
	 *             If SpiNNaker rejects a message.
	 * @throws TimeoutException
	 *             If things time out unrecoverably.
	 */
	private void handleOneRecordingRegion(List<Region> smallRetrieves,
			Downloader downloader, Placement place, int regionID,
			CoreLocation extraMonitor) throws StorageException, IOException,
			ProcessException, TimeoutException {
		List<Region> rs = getRegion(place, regionID);
		if (rs.stream().allMatch(r -> r.size < SMALL_RETRIEVE_THRESHOLD)) {
			smallRetrieves.addAll(rs);
			return;
		}
		boolean haveTimedOut = false;
		for (Region r : rs) {
			// Skip zero-sized retrieves
			if (r.size < 1) {
				continue;
			}
			if (r.size < SMALL_RETRIEVE_THRESHOLD || haveTimedOut) {
				// Do this immediately; order matters!
				storeData(r, txrx.readMemory(r.core.asChipLocation(),
						r.startAddress, r.size));
			} else {
				try {
					ByteBuffer data = downloader.doDownload(extraMonitor, r);
					if (SPINNAKER_COMPARE_DOWNLOAD != null) {
						compareDownloadWithSCP(r, data);
					}
					storeData(r, data);
				} catch (TimeoutException e) {
					smallRetrieves.add(r);
					haveTimedOut = true;
				}
			}
		}
	}

	private void compareDownloadWithSCP(Region r, ByteBuffer data)
			throws IOException, ProcessException {
		ByteBuffer data2 = txrx.readMemory(r.core.asChipLocation(),
				r.startAddress, r.size);
		if (data.remaining() != data2.remaining()) {
			log.error("different buffer sizes: {} with gatherer, {} with SCP",
					data.remaining(), data2.remaining());
		}
		for (int i = 0; i < data.remaining(); i++) {
			if (data.get(i) != data2.get(i)) {
				log.error("downloaded buffer contents different");
				for (Delta<Byte> delta : diff(list(data2), list(data))
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
				TRAFFIC_ID);
		txrx.setIPTag(tag);
		log.info("reconfigured {} to {}", iptag, tag);
		if (log.isDebugEnabled()) {
			log.debug("all tags for board: {}", txrx.getTags(
					txrx.locateSpinnakerConnection(tag.getBoardAddress())));
		}
	}

	/**
	 * Standard short timeout for emergency routing.
	 */
	private static final RouterTimeout SHORT_TIMEOUT = new RouterTimeout(1, 1);

	/**
	 * Configures the routers of the chips on a board to never drop packets
	 * while an instance of this class is active.
	 *
	 * @author Donal Fellows
	 */
	private class DoNotDropPackets implements AutoCloseable {
		/** The location of the ethernet for the board. For logging. */
		private final ChipLocation gathererChip;
		/** The monitor cores on the board. */
		private final CoreSubsets monitorCores;
		/**
		 * The saved reinjection status of the first monitor core; all other
		 * cores are assumed to be the same.
		 */
		private ReinjectionStatus savedStatus;

		/**
		 * Configure the routers of the chips on a board to never drop packets.
		 *
		 * @param gathererChip
		 *            The location of the ethernet for the board.
		 * @param monitorCores
		 *            The monitor cores on the board.
		 * @throws IOException
		 *             If message sending or reception fails.
		 * @throws ProcessException
		 *             If SpiNNaker rejects a message.
		 */
		DoNotDropPackets(ChipLocation gathererChip, CoreSubsets monitorCores)
				throws IOException, ProcessException {
			log.info("disabling router timeouts on board based at {}",
					gathererChip);
			this.gathererChip = gathererChip;
			this.monitorCores = monitorCores;

			// Store the last reinjection status for resetting
			savedStatus = saveRouterStatus();
			// Set to not inject dropped packets
			txrx.setReinjectionTypes(monitorCores, false, false, false, false);
			// Clear any outstanding packets from reinjection
			txrx.clearReinjectionQueues(monitorCores);
			// Set timeouts
			txrx.setReinjectionTimeout(monitorCores, RouterTimeout.INF);
			txrx.setReinjectionEmergencyTimeout(monitorCores, SHORT_TIMEOUT);
		}

		private ReinjectionStatus saveRouterStatus()
				throws IOException, ProcessException {
			for (CoreLocation core : monitorCores) {
				return txrx.getReinjectionStatus(core);
			}
			throw new IllegalArgumentException(
					"no monitors could save their status");
		}

		/**
		 * Configure the routers of the chips on a board to be what they were
		 * previously.
		 *
		 * @throws IOException
		 *             If message sending or reception fails.
		 * @throws ProcessException
		 *             If SpiNNaker rejects a message.
		 */
		@Override
		public void close() throws IOException, ProcessException {
			log.info("enabling router timeouts on board {}", gathererChip);
			try {
				txrx.setReinjectionEmergencyTimeout(monitorCores,
						savedStatus.getEmergencyTimeout());
				txrx.setReinjectionTimeout(monitorCores,
						savedStatus.getTimeout());
				txrx.setReinjectionTypes(monitorCores,
						savedStatus.isReinjectingMulticast(),
						savedStatus.isReinjectingPointToPoint(),
						savedStatus.isReinjectingFixedRoute(),
						savedStatus.isReinjectingNearestNeighbour());
			} catch (IOException | ProcessException e) {
				log.error("problem resetting router timeouts; "
						+ "checking if the cores are OK...");
				checkCores(monitorCores, e);
				throw e;
			}
		}

		private void checkCores(CoreSubsets monitorCores, Exception mainExn) {
			try {
				Map<?, ?> errorCores =
						txrx.getCoresNotInState(monitorCores, RUNNING);
				if (!errorCores.isEmpty()) {
					log.error("cores in an unexpected state: {}", errorCores);
				}
			} catch (Exception e) {
				/*
				 * Correct behaviour here is to suppress the inner issue; this
				 * logs in the main exception that it occurred.
				 */
				mainExn.addSuppressed(e);
			}
		}
	}

	/**
	 * Work out exactly where is going to be downloaded. The elements of the
	 * list this method returns will end up directing what calls to
	 * {@link #storeData(BufferManagerStorage.Region,ByteBuffer) storeData(...)}
	 * are done, and the order in which they are done.
	 * <p>
	 * The recording region memory management scheme effectively requires this
	 * to be a list of zero, one or two elements, but the {@link DataGatherer}
	 * class does not care.
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
	protected abstract List<Region> getRegion(Placement placement, int regionID)
			throws IOException, ProcessException, StorageException;

	/**
	 * Store the data retrieved from a region. Called (at most) once for each
	 * element in the list returned by {@link #getRegion(Placement,int)
	 * getRegion(...)}, <i>in order</i>. No guarantee is made about which thread
	 * will call this method.
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
	 * Have a quiet sleep. Utility method.
	 *
	 * @param delay
	 *            How long to sleep, in milliseconds.
	 */
	private static void snooze(int delay) {
		try {
			sleep(delay);
		} catch (InterruptedException ignored) {
			/*
			 * This is only used in contexts where we don't actually interrupt
			 * the thread, so this exception isn't actually ever going to be
			 * thrown.
			 */
		}
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

		/**
		 * Send a message asking the extra monitor core to read from a region of
		 * SDRAM and send it to us (using the configured IPtag).
		 *
		 * @param extraMonitorCore
		 *            The location of the monitor.
		 * @param address
		 *            Where to read from.
		 * @param length
		 *            How many bytes to read.
		 * @throws IOException
		 *             If message sending fails.
		 */
		void sendStart(CoreLocation extraMonitorCore, int address, int length)
				throws IOException {
			sendSDPMessage(StartSendingMessage.create(extraMonitorCore, address,
					length));
		}

		/**
		 * Send a message asking the extra monitor core to ask it to resend some
		 * data.
		 *
		 * @param extraMonitorCore
		 *            The location of the monitor.
		 * @param missingSeqs
		 *            Description of what sequence numbers are missing.
		 * @param numPackets
		 *            How many resend messages will be used.
		 * @throws IOException
		 *             If message sending fails.
		 */
		void sendMissing(MissingSequenceNumbersMessage msg) throws IOException {
			sendSDPMessage(msg);
		}

		/**
		 * Start the reader thread.
		 *
		 * @return The queue containing the messages that will be received by
		 *         the reader thread. Timeouts are described by empty messages,
		 *         as SpiNNaker never sends those out.
		 */
		BlockingQueue<ByteBuffer> launchReaderThread() {
			BlockingQueue<ByteBuffer> messQueue =
					new ArrayBlockingQueue<>(QUEUE_CAPACITY);
			ReaderThread t = new ReaderThread(messQueue);
			t.start();
			return messQueue;
		}

		/**
		 * Asks the reader thread to continue reading. Does nothing if the
		 * reader thread isn't already waiting.
		 */
		void unstick() {
			synchronized (this) {
				notifyAll();
			}
		}

		private void waitForUnstick() throws InterruptedException {
			synchronized (this) {
				wait();
			}
		}

		@Override
		public void close() throws IOException {
			super.close();
			unstick();
		}

		/**
		 * The main loop of the thread. While the socket is open, messages will
		 * be added to the queue. When a timeout occurs, the reader pauses until
		 * the consumer is ready to handle things once again.
		 *
		 * @param messQueue
		 *            Where to store received messages. Timeouts are handled by
		 *            putting in an empty message; SpiNNaker never sends out an
		 *            empty message.
		 * @throws IOException
		 *             If message reception fails.
		 * @throws InterruptedException
		 *             If the message queue is full and we get interrupted when
		 *             trying to put a message on it. (unexpected)
		 */
		private void mainLoop(BlockingQueue<ByteBuffer> messQueue)
				throws IOException, InterruptedException {
			waitForUnstick();
			do {
				try {
					ByteBuffer recvd = receive(TIMEOUT_PER_RECEIVE);
					if (recvd != null) {
						messQueue.put(recvd);
                        if (log.isDebugEnabled()) {
    						log.debug("pushed message of {} bytes",
                                    recvd.limit());
                        }
					}
				} catch (SocketTimeoutException e) {
					log.info("socket timed out");
					messQueue.put(EMPTY_DATA);
					waitForUnstick();
				} catch (EOFException e) {
					// Race condition can occasionally close socket early
					messQueue.put(EOF);
					break;
				}
			} while (!isClosed());
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
				setPriority(MAX_PRIORITY);
				this.messQueue = messQueue;
			}

			@Override
			public void run() {
				// While socket is open add messages to the queue
				try {
					mainLoop(messQueue);
				} catch (InterruptedException e) {
					log.error("failed to offer packet to queue");
				} catch (IOException e) {
					log.error("failed to receive packet", e);
				}
			}
		}
	}

	/**
	 * Exception that indicates a total (i.e., unrecoverable) failure to do a
	 * download.
	 *
	 * @author Donal Fellows
	 */
	private static final class TimeoutException extends Exception {
		private static final long serialVersionUID = 1L;
	}

	/**
	 * Exception that indicates a bad sequence number in a download.
	 *
	 * @author Donal Fellows
	 */
	private static final class InsaneSequenceNumberException
			extends IllegalStateException {
		private static final long serialVersionUID = 2L;

		private InsaneSequenceNumberException(int maxNum, int seqNum) {
			super("got insane sequence number " + seqNum + ": expected maximum "
					+ maxNum
					+ (maxNum == seqNum ? " (non-empty terminal-only packet)"
							: " (totally bad sequence)"));
		}
	}

	/**
	 * Class used to manage a download. Every instance <em>must only</em> ever
	 * be used from one thread.
	 *
	 * @author Donal Fellows
	 */
	private final class Downloader {
		private final GatherDownloadConnection conn;
		private final BlockingQueue<ByteBuffer> queue;

		/**
		 * Whether a packet has previously been received on this connection
		 * since it was configured to talk to the current core; if not, it's
		 * probably a dead connection or problem with the core causing the
		 * failure.
		 */
		private boolean received;
		private int timeoutcount = 0;
		private BitSet receivedSeqNums;
		private int maxSeqNum;
		private ByteBuffer dataReceiver;
		private CoreLocation monitorCore;

		/**
		 * Create an instance.
		 *
		 * @param connection
		 *            The connection used to send messages.
		 */
		private Downloader(GatherDownloadConnection connection) {
			conn = connection;
			queue = connection.launchReaderThread();
		}

		/**
		 * Do the downloading.
		 *
		 * @param extraMonitor
		 *            Where to download from.
		 * @param region
		 *            What to download.
		 * @return The downloaded data, or {@code null} if an unrecoverable
		 *         error occurred.
		 * @throws IOException
		 *             If anything unexpected goes wrong.
		 * @throws TimeoutException
		 *             If a download times out unrecoverably.
		 */
		ByteBuffer doDownload(CoreLocation extraMonitor, Region region)
				throws IOException, TimeoutException {
			monitorCore = extraMonitor;
			dataReceiver = ByteBuffer.allocate(region.size);
			/*
			 * Tricky point: where an amount of data to be downloaded is exactly
			 * a multiple of the number of payload words per packet, we need an
			 * extra packet because the data gatherer executable sends the data
			 * before it knows whether it has reached the end (and the extra
			 * monitors don't know about the chunking).
			 *
			 * This translates into needing to add one here.
			 */
			maxSeqNum =
					ceildiv(region.size + 1, DATA_WORDS_PER_PACKET * WORD_SIZE);
			receivedSeqNums = new BitSet(maxSeqNum);
			conn.unstick();
			conn.sendStart(monitorCore, region.startAddress, region.size);
			received = false;
			try {
				boolean finished;
				do {
					finished = processOnePacket(TIMEOUT_PER_RECEIVE);
				} while (!finished);
			} finally {
				if (!received) {
					log.warn("never actually received any packets from "
							+ "<{}> for {}", monitorCore, region);
				}
			}
			dataReceiver.position(0);
			return dataReceiver;
		}

		/**
		 * Take one message off the queue and process it.
		 *
		 * @param timeout
		 *            How long to wait for the queue to deliver a packet, in
		 *            milliseconds.
		 * @return True if we have finished.
		 * @throws IOException
		 *             If packet retransmission requesting fails.
		 * @throws TimeoutException
		 *             If we have a full timeout.
		 */
		private boolean processOnePacket(int timeout)
				throws IOException, TimeoutException {
			ByteBuffer p = getNextPacket(timeout + INTERNAL_DELAY);
			if (p.hasRemaining()) {
				received = true;
				return processData(p);
			}
			return processTimeout();
		}

		private ByteBuffer getNextPacket(int timeout) throws EOFException {
			try {
				ByteBuffer b = queue.poll(timeout, MILLISECONDS);
				if (b == null) {
					return EMPTY_DATA;
				}
				if (b == EOF) {
					throw new EOFException("queue reader has been drained");
				}
				return b;
			} catch (InterruptedException ignored) {
				/*
				 * This is in a thread that isn't ever interrupted, but IN
				 * THEORY interruption is exactly like timing out as far as this
				 * thread is concerned anyway.
				 */
				return EMPTY_DATA;
			}
		}

		/**
		 * Process a single received packet.
		 *
		 * @param data
		 *            The content of the packet.
		 * @return True if we have finished.
		 * @throws IOException
		 *             If the packet is an end-of-stream packet yet there are
		 *             packets outstanding, and the retransmission causes an
		 *             error.
		 */
		private boolean processData(ByteBuffer data) throws IOException {
			int seqNum = data.getInt();
			boolean isEndOfStream =
					((seqNum & LAST_MESSAGE_FLAG_BIT_MASK) != 0);
			seqNum &= ~LAST_MESSAGE_FLAG_BIT_MASK;

			if (seqNum > maxSeqNum || seqNum < 0) {
				throw new InsaneSequenceNumberException(maxSeqNum, seqNum);
			}
			int len = data.remaining();
			if (len != DATA_WORDS_PER_PACKET * WORD_SIZE
					&& seqNum < maxSeqNum - 1) {
				log.warn("short packet ({} bytes) in non-terminal position "
						+ "(seq: {})", len, seqNum);
			}
			if (data.hasRemaining()) {
				int offset = seqNum * DATA_WORDS_PER_PACKET * WORD_SIZE;
				if (log.isDebugEnabled()) {
					log.debug("storing {} bytes at position {} of {}",
							data.remaining(), offset, dataReceiver.limit());
				}
				dataReceiver.position(offset);
				dataReceiver.put(data);
			}
			receivedSeqNums.set(seqNum);
			if (!isEndOfStream) {
				return false;
			}
			return retransmitMissingSequences();
		}

		/**
		 * Process the fact that the message queue was in a timeout state.
		 *
		 * @return True if we have finished.
		 * @throws IOException
		 *             If there are packets outstanding, and the retransmission
		 *             causes an error.
		 * @throws TimeoutException
		 *             If we have a full timeout.
		 */
		private boolean processTimeout()
				throws IOException, TimeoutException {
			if (++timeoutcount > TIMEOUT_RETRY_LIMIT && !received) {
				log.error(TIMEOUT_MESSAGE);
				throw new TimeoutException();
			}

			// retransmit missing packets
			log.debug("doing reinjection");
			return retransmitMissingSequences();
		}

		/**
		 * Request that the extra monitor core retransmit some packets. Does
		 * nothing if there are no packets missing.
		 *
		 * @return Whether there were really any packets to retransmit.
		 * @throws IOException
		 *             If there are failures.
		 */
		private boolean retransmitMissingSequences() throws IOException {
			int numMissing = maxSeqNum - receivedSeqNums.cardinality();
			if (numMissing < 1) {
				return true;
			}

			/*
			 * Build a buffer containing the sequence numbers of all missing
			 * packets.
			 */
			List<Integer> missingSeqs = new ArrayList<>(numMissing);
			for (int i = 0; i < maxSeqNum; i++) {
				if (!receivedSeqNums.get(i)) {
					missingSeqs.add(i);
					missCount++;
				}
			}

			if (log.isDebugEnabled()) {
				log.debug("missing sequence numbers: ", missingSeqs);
			}

			// Transmit missing sequences as a new SDP Packet
			for (MissingSequenceNumbersMessage msg : createMessages(monitorCore,
					missingSeqs)) {
				snooze(DELAY_PER_SEND);
				conn.sendMissing(msg);
			}
			conn.unstick();
			return false;
		}
	}
}
