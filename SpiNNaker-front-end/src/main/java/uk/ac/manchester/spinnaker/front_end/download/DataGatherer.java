/*
 * Copyright (c) 2018 The University of Manchester
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
package uk.ac.manchester.spinnaker.front_end.download;

import static java.lang.String.format;
import static java.lang.System.getProperty;
import static java.lang.Thread.sleep;
import static java.nio.ByteBuffer.allocate;
import static java.util.stream.Collectors.toUnmodifiableList;
import static org.slf4j.LoggerFactory.getLogger;
import static uk.ac.manchester.spinnaker.front_end.Constants.PARALLEL_SIZE;
import static uk.ac.manchester.spinnaker.front_end.DebuggingUtils.compareBuffers;
import static uk.ac.manchester.spinnaker.front_end.download.MissingSequenceNumbersMessage.createMessages;
import static uk.ac.manchester.spinnaker.messages.Constants.SDP_PAYLOAD_WORDS;
import static uk.ac.manchester.spinnaker.messages.Constants.WORD_SIZE;
import static uk.ac.manchester.spinnaker.utils.MathUtils.ceildiv;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.slf4j.Logger;

import com.google.errorprone.annotations.ForOverride;
import com.google.errorprone.annotations.MustBeClosed;
import com.google.errorprone.annotations.OverridingMethodsMustInvokeSuper;

import uk.ac.manchester.spinnaker.connections.MostDirectConnectionSelector;
import uk.ac.manchester.spinnaker.front_end.BasicExecutor;
import uk.ac.manchester.spinnaker.front_end.BasicExecutor.SimpleCallable;
import uk.ac.manchester.spinnaker.front_end.BoardLocalSupport;
import uk.ac.manchester.spinnaker.front_end.NoDropPacketContext;
import uk.ac.manchester.spinnaker.front_end.download.request.Gather;
import uk.ac.manchester.spinnaker.front_end.download.request.Monitor;
import uk.ac.manchester.spinnaker.front_end.download.request.Placement;
import uk.ac.manchester.spinnaker.front_end.dse.SystemRouterTableContext;
import uk.ac.manchester.spinnaker.machine.ChipLocation;
import uk.ac.manchester.spinnaker.machine.Machine;
import uk.ac.manchester.spinnaker.storage.BufferManagerStorage;
import uk.ac.manchester.spinnaker.storage.BufferManagerStorage.Region;
import uk.ac.manchester.spinnaker.storage.StorageException;
import uk.ac.manchester.spinnaker.transceiver.ProcessException;
import uk.ac.manchester.spinnaker.transceiver.TransceiverInterface;
import uk.ac.manchester.spinnaker.utils.UsedInJavadocOnly;

/**
 * Implementation of the SpiNNaker Fast Data Download Protocol.
 *
 * @author Donal Fellows
 * @author Alan Stokes
 */
@SuppressWarnings("deprecation")
public abstract sealed class DataGatherer extends BoardLocalSupport implements
		AutoCloseable permits DirectDataGatherer, RecordingRegionDataGatherer {
	/**
	 * Logger for the gatherer.
	 */
	protected static final Logger log = getLogger(DataGatherer.class);

	/** The maximum number of times to retry. */
	private static final int TIMEOUT_RETRY_LIMIT = 15;

	/**
	 * The time delay between sending each message. In
	 * {@linkplain java.util.concurrent.TimeUnit#MILLISECONDS milliseconds}.
	 */
	private static final int DELAY_PER_SEND = 10;

	/**
	 * The timeout when receiving a message. In
	 * {@linkplain java.util.concurrent.TimeUnit#MILLISECONDS milliseconds}.
	 */
	private static final int TIMEOUT_PER_RECEIVE = 2000;

	/**
	 * The <i>extra</i> timeout for processing the message queue. In
	 * {@linkplain java.util.concurrent.TimeUnit#MILLISECONDS milliseconds}.
	 */
	private static final int INTERNAL_DELAY = 100;

	/**
	 * What is the maximum number of payload <em>words</em> in a packet that
	 * also has a sequence number? This is one less than the total maximum
	 * number of words in an SDP packet; that extra word is the control word
	 * which encodes the sequence number and the end-of-stream flag.
	 */
	private static final int DATA_WORDS_PER_PACKET = SDP_PAYLOAD_WORDS - 2;

	/**
	 * Mask used to pick out the bit that says whether a sequence number is the
	 * last in a stream.
	 */
	private static final int LAST_MESSAGE_FLAG_BIT_MASK = 0x80000000;

	/** Message used to report problems. */
	private static final String TIMEOUT_MESSAGE = "failed to hear from the "
			+ "machine (please try removing firewalls)";

	private static final String SPINNAKER_COMPARE_DOWNLOAD =
			getProperty("spinnaker.compare.download");

	private final BasicExecutor pool;

	private int missCount;

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
	@MustBeClosed
	@SuppressWarnings("MustBeClosed")
	public DataGatherer(TransceiverInterface transceiver, Machine machine)
			throws IOException, ProcessException {
		super(transceiver, machine);
		this.pool = new BasicExecutor(PARALLEL_SIZE);
		this.missCount = 0;
	}

	@Override
	@OverridingMethodsMustInvokeSuper
	public void close() throws InterruptedException {
		pool.close();
	}

	/**
	 * Download he contents of the regions that are described through the data
	 * gatherers.
	 *
	 * @param gatherers
	 *            The data gatherer information for the boards.
	 * @return The total number of missed packets. Misses are retried, so this
	 *         is just an assessment of data transfer quality.
	 * @throws IOException
	 *             If IO fails.
	 * @throws ProcessException
	 *             If SpiNNaker rejects a message.
	 * @throws StorageException
	 *             If DB access goes wrong.
	 * @throws InterruptedException
	 *             If communications are interrupted.
	 */
	public int gather(List<Gather> gatherers) throws IOException,
			ProcessException, StorageException, InterruptedException {
		sanityCheck(gatherers);
		var work = discoverActualWork(gatherers);
		var conns = createConnections(gatherers, work);
		try (var s = new SystemRouterTableContext(txrx,
				gatherers.stream().flatMap(g -> g.getMonitors().stream()));
				var p = new NoDropPacketContext(txrx,
						gatherers.stream()
								.flatMap(g -> g.getMonitors().stream()),
						gatherers.stream())) {
			log.info("launching {} parallel high-speed download tasks",
					work.size());
			parallel(work.keySet().stream().map(key -> {
				return () -> fastDownload(work.get(key), conns.get(key));
			}));
		} finally {
			log.info("shutting down high-speed download connections");
			for (var c : conns.values()) {
				c.close();
			}
		}
		return missCount;
	}

	/**
	 * Trivial record holding the pairing of monitor and list of lists of memory
	 * blocks.
	 *
	 * @author Donal Fellows
	 * @param monitor
	 *            Monitor that is used to download the regions.
	 * @param regions
	 *            List of information about where to download. The inner
	 *            sub-lists are ordered, and are either one or two items long to
	 *            represent what pieces of memory should really be downloaded.
	 *            The outer list could theoretically be done in any order... but
	 *            needs to be processed single-threaded anyway.
	 */
	private record WorkItems(Monitor monitor, List<List<Region>> regions) {
	}

	/**
	 * Query the machine to discover what actual pieces of memory the recording
	 * region IDs of the placements of the vertices attached to the monitors
	 * associated with the data speed up packet gatherers are.
	 *
	 * @param gatherers
	 *            The gatherer information.
	 * @return What each board (as represented by the chip location of its data
	 *         speed up packet gatherer) has to be downloaded.
	 * @throws IOException
	 *             If IO fails.
	 * @throws ProcessException
	 *             If SpiNNaker rejects a message.
	 * @throws StorageException
	 *             If DB access goes wrong.
	 * @throws InterruptedException
	 *             If communications are interrupted.
	 */
	private Map<ChipLocation, List<WorkItems>> discoverActualWork(
			List<Gather> gatherers) throws IOException, ProcessException,
			StorageException, InterruptedException {
		log.info("discovering regions to download");
		var work = new HashMap<ChipLocation, List<WorkItems>>();
		int count = 0;
		for (var g : gatherers) {
			var workitems = new ArrayList<WorkItems>();
			for (var m : g.getMonitors()) {
				m.updateTransactionIdFromMachine(txrx);

				for (var p : m.getPlacements()) {
					var regions = new ArrayList<List<Region>>();
					for (int id : p.vertex().recordedRegionIds()) {
						var r = getRegion(p, id);
						if (!r.isEmpty()) {
							regions.add(r);
						}
						count += r.size();
					}
					if (!regions.isEmpty()) {
						workitems.add(new WorkItems(m, regions));
					}
				}

			}
			// Totally empty boards can be ignored
			if (!workitems.isEmpty()) {
				work.put(g.asChipLocation(), workitems);
			}
		}
		log.info("found {} regions to download", count);
		return work;
	}

	/**
	 * Build the connections to the gatherers and reprogram the IP tags on the
	 * machine to talk to them.
	 *
	 * @param gatherers
	 *            The data speed up gatherer descriptions.
	 * @param work
	 *            What work has been found to do. Used to filter out connections
	 *            to boards with nothing to do.
	 * @return The map from the location of the gatherer to the connection to
	 *         talk and listen to it.
	 * @throws IOException
	 *             If IO fails.
	 * @throws ProcessException
	 *             If SpiNNaker rejects a message.
	 * @throws InterruptedException
	 *             If communications are interrupted.
	 */
	@SuppressWarnings("MustBeClosed")
	private Map<ChipLocation, GatherDownloadConnection> createConnections(
			List<Gather> gatherers, Map<ChipLocation, ?> work)
			throws IOException, ProcessException, InterruptedException {
		log.info("building high-speed data connections and configuring IPtags");
		var connections = new HashMap<ChipLocation, GatherDownloadConnection>();
		for (var g : gatherers) {
			var gathererChip = g.asChipLocation();
			if (!work.containsKey(gathererChip)) {
				continue;
			}
			var conn = new GatherDownloadConnection(txrx.createScpConnection(
					gathererChip, g.getIptag().getBoardAddress()));
			conn.setIPTag(txrx, g.getIptag());
			connections.put(gathererChip, conn);
		}
		return connections;
	}

	/**
	 * Wrapper around the thread pool to sanitise the exceptions.
	 *
	 * @param tasks
	 *            The tasks to run in the pool.
	 * @throws IOException
	 *             If IO fails.
	 * @throws ProcessException
	 *             If SpiNNaker rejects a message.
	 * @throws StorageException
	 *             If DB access goes wrong.
	 * @throws InterruptedException
	 *             If communications are interrupted.
	 */
	private void parallel(Stream<SimpleCallable> tasks) throws IOException,
			ProcessException, StorageException, InterruptedException {
		try {
			pool.submitTasks(tasks).awaitAndCombineExceptions();
		} catch (IOException | StorageException | ProcessException
				| InterruptedException | RuntimeException e) {
			throw e;
		} catch (Exception e) {
			throw new RuntimeException("unexpected exception", e);
		}
	}

	/**
	 * Do the fast downloads for a particular board.
	 *
	 * @param work
	 *            The items to be downloaded for that board.
	 * @param conn
	 *            The connection for talking to the board.
	 * @throws IOException
	 *             If IO fails.
	 * @throws StorageException
	 *             If DB access goes wrong.
	 * @throws TimeoutException
	 *             If a download times out unrecoverably.
	 * @throws ProcessException
	 *             If anything unexpected goes wrong.
	 * @throws InterruptedException
	 *             If communications are interrupted.
	 */
	private void fastDownload(List<WorkItems> work,
			GatherDownloadConnection conn)
			throws IOException, StorageException, TimeoutException,
			ProcessException, InterruptedException {
		try (var c = new BoardLocal(conn.getChip())) {
			log.info("processing fast downloads for {}", conn.getChip());
			var dl = new Downloader(conn);
			for (var item : work) {
				for (var regionsOnCore : item.regions()) {
					/*
					 * Once there's something too small, all subsequent
					 * retrieves for that recording region have to be done the
					 * same way to get the data in the DB in the right order.
					 */
					for (var region : regionsOnCore) {
						var data = dl.doDownload(item.monitor(), region);
						if (SPINNAKER_COMPARE_DOWNLOAD != null) {
							compareDownloadWithSCP(region, data);
						}
						storeData(region, data);
					}
				}
			}
		}
	}

	private void sanityCheck(List<Gather> gatherers) {
		var sel = txrx.getScampConnectionSelector();
		var s = (sel instanceof MostDirectConnectionSelector<?> ss ? ss : null);

		// Sanity check the inputs
		for (var g : gatherers) {
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
	}

	private void compareDownloadWithSCP(Region r, ByteBuffer data)
			throws IOException, ProcessException, InterruptedException {
		var data2 = txrx.readMemory(r.core.asChipLocation(), r.startAddress,
				r.size);
		if (data.remaining() != data2.remaining()) {
			log.error("different buffer sizes: {} with gatherer, {} with SCP",
					data.remaining(), data2.remaining());
		}
		compareBuffers(data, data2, log);
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
	 *         May be unmodifiable.
	 * @throws IOException
	 *             If communication fails.
	 * @throws ProcessException
	 *             If SpiNNaker rejects a message.
	 * @throws StorageException
	 *             If the database doesn't like something.
	 * @throws InterruptedException
	 *             If communications are interrupted.
	 */
	@UsedInJavadocOnly(BufferManagerStorage.class)
	@ForOverride
	protected abstract List<Region> getRegion(Placement placement, int regionID)
			throws IOException, ProcessException, StorageException,
			InterruptedException;

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
	@ForOverride
	protected abstract void storeData(Region r, ByteBuffer data)
			throws StorageException;

	/**
	 * Class used to manage a download. Every instance <em>must only</em> ever
	 * be used from one thread.
	 *
	 * @author Donal Fellows
	 */
	private final class Downloader {
		private final GatherDownloadConnection conn;

		/**
		 * Whether a packet has previously been received on this connection
		 * since it was configured to talk to the current core; if not, it's
		 * probably a dead connection or problem with the core causing the
		 * failure.
		 */
		private boolean received;

		private int timeoutcount;

		/** A flag for each packet sequence number that is expected. */
		private BitSet expectedSeqNums;

		private int maxSeqNum;

		private ByteBuffer dataReceiver;

		private Monitor monitorCore;

		private List<Integer> lastRequested;

		/**
		 * Create an instance.
		 *
		 * @param connection
		 *            The connection used to send messages.
		 */
		private Downloader(GatherDownloadConnection connection) {
			conn = connection;
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
		 * @throws ProcessException
		 *             If anything unexpected goes wrong.
		 * @throws InterruptedException
		 *             If communications are interrupted.
		 */
		ByteBuffer doDownload(Monitor extraMonitor, Region region)
				throws IOException, TimeoutException, ProcessException,
				InterruptedException {
			monitorCore = extraMonitor;
			dataReceiver = allocate(region.size);
			/*
			 * Tricky point: where an amount of data to be downloaded is exactly
			 * a multiple of the number of payload words per packet, we need an
			 * extra packet because the data gatherer executable sends the data
			 * before it knows whether it has reached the end (and the extra
			 * monitors don't know about the chunking).
			 *
			 * This translates into needing to add one here.
			 */
			maxSeqNum = ceildiv(region.size, DATA_WORDS_PER_PACKET * WORD_SIZE);
			expectedSeqNums = new BitSet(maxSeqNum);
			expectedSeqNums.set(0, maxSeqNum);
			lastRequested = expectedSeqs();
			received = false;
			timeoutcount = 0;
			monitorCore.updateTransactionId();
			log.debug(
					"extracting data from {} with size {} with "
							+ "transaction id {}",
					region.startAddress, region.size,
					monitorCore.getTransactionId());
			conn.sendStart(monitorCore.asCoreLocation(), region.startAddress,
					region.size, monitorCore.getTransactionId());
			try {
				boolean finished;
				do {
					finished = processOnePacket(TIMEOUT_PER_RECEIVE,
							monitorCore.getTransactionId());
				} while (!finished);
				conn.sendClear(monitorCore.asCoreLocation(),
						monitorCore.getTransactionId());
			} catch (TimeoutException e) {
				if (received) {
					log.warn(
							"received only some of the packets from <{}> "
									+ "for {}; has something crashed?",
							monitorCore, region);
				}
				throw e;
			} finally {
				if (!received) {
					log.warn("never actually received any packets from "
							+ "<{}> for {}", monitorCore, region);
				}
			}
			dataReceiver.position(0);
			try {
				return dataReceiver;
			} finally {
				dataReceiver = null;
			}
		}

		/**
		 * Take one message off the queue and process it.
		 *
		 * @param timeout
		 *            How long to wait for the queue to deliver a packet, in
		 *            milliseconds.
		 * @param transactionId
		 *            The transaction id of this stream.
		 * @return True if we have finished.
		 * @throws IOException
		 *             If packet reception or retransmission requesting fails.
		 * @throws TimeoutException
		 *             If we have a full timeout, or if we are flailing around,
		 *             making no progress.
		 * @throws InterruptedException
		 *             If communications are interrupted.
		 */
		private boolean processOnePacket(int timeout, int transactionId)
				throws IOException, TimeoutException, InterruptedException {
			var p = conn.getNextPacket(timeout + INTERNAL_DELAY);
			if (p.hasRemaining()) {
				received = true;
				timeoutcount = 0;
				return processData(p, transactionId);
			}
			log.debug("failed to receive on connection {}.", conn);
			return processTimeout(transactionId);
		}

		/**
		 * Process a single received packet.
		 *
		 * @param data
		 *            The content of the packet.
		 * @param transactionId
		 *            The transaction id of this stream.
		 * @return True if we have finished.
		 * @throws IOException
		 *             If the packet is an end-of-stream packet yet there are
		 *             packets outstanding, and the retransmission causes an
		 *             error.
		 * @throws TimeoutException
		 *             If we are flailing around, making no progress.
		 * @throws InterruptedException
		 *             If we are interrupted.
		 */
		private boolean processData(ByteBuffer data, int transactionId)
				throws IOException, TimeoutException, InterruptedException {
			int seqNum = data.getInt();
			int responseTransactionId = data.getInt();

			if (responseTransactionId != transactionId) {
				return false;
			}

			var isEndOfStream = ((seqNum & LAST_MESSAGE_FLAG_BIT_MASK) != 0);
			seqNum &= ~LAST_MESSAGE_FLAG_BIT_MASK;

			if (seqNum > maxSeqNum || seqNum < 0) {
				throw new InsaneSequenceNumberException(maxSeqNum, seqNum);
			}
			int len = data.remaining();
			if (len != DATA_WORDS_PER_PACKET * WORD_SIZE && len != 0
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
				expectedSeqNums.clear(seqNum);
			}
			if (!isEndOfStream) {
				return false;
			}
			return retransmitMissingSequences(transactionId);
		}

		/**
		 * Process the fact that the message queue was in a timeout state.
		 *
		 * @param transactionId
		 *            The transaction id of this stream
		 * @return True if we have finished.
		 * @throws IOException
		 *             If there are packets outstanding, and the retransmission
		 *             causes an error.
		 * @throws TimeoutException
		 *             If we have a full timeout.
		 * @throws InterruptedException
		 *             If we are interrupted.
		 */
		private boolean processTimeout(int transactionId)
				throws IOException, TimeoutException, InterruptedException {
			if (++timeoutcount > TIMEOUT_RETRY_LIMIT) {
				log.error(TIMEOUT_MESSAGE);
				throw new TimeoutException();
			}

			// retransmit missing packets
			log.debug("doing reinjection");
			return retransmitMissingSequences(transactionId);
		}

		/**
		 * Request that the extra monitor core retransmit some packets. Does
		 * nothing if there are no packets missing.
		 *
		 * @param transactionId
		 *            The transaction id of this stream
		 * @return Whether there were really any packets to retransmit.
		 * @throws IOException
		 *             If there are failures.
		 * @throws TimeoutException
		 *             If we are flailing around, making no progress.
		 * @throws InterruptedException
		 *             If interrupted.
		 */
		private boolean retransmitMissingSequences(int transactionId)
				throws IOException, TimeoutException, InterruptedException {
			int numMissing = expectedSeqNums.cardinality();
			if (numMissing < 1) {
				return true;
			}
			log.debug("there are {} missing packets in message from {}",
					numMissing, monitorCore);

			// Build a list of the sequence numbers of all missing packets
			var missingSeqs = expectedSeqs();
			missCount += numMissing;

			log.debug("missing sequence numbers: {}", missingSeqs);
			if (missingSeqs.size() > lastRequested.size()) {
				log.warn("what is going on?");
				log.warn("last:{}", lastRequested);
				log.warn("this:{}", missingSeqs);
			}
			lastRequested = missingSeqs;

			// Transmit missing sequences as a new SDP Packet
			for (var msg : createMessages(monitorCore, missingSeqs,
					transactionId)) {
				sleep(DELAY_PER_SEND);
				conn.sendMissing(msg);
			}
			return false;
		}

		/**
		 * @return The expected sequence numbers, as an ordered list.
		 */
		private List<Integer> expectedSeqs() {
			return expectedSeqNums.stream().boxed()
					.collect(toUnmodifiableList());
		}
	}
}

/**
 * Exception that indicates a total (i.e., unrecoverable) failure to do a
 * download.
 *
 * @author Donal Fellows
 */
final class TimeoutException extends Exception {
	private static final long serialVersionUID = 1L;
}

/**
 * Exception that indicates a bad sequence number in a download.
 *
 * @author Donal Fellows
 */
final class InsaneSequenceNumberException extends IllegalStateException {
	private static final long serialVersionUID = 2L;

	private static final String MID = "totally bad sequence";

	private static final String END = "non-empty terminal-only packet";

	InsaneSequenceNumberException(int maxNum, int seqNum) {
		super(format("got insane sequence number %d: expected maximum %d (%s)",
				seqNum, maxNum, (maxNum == seqNum ? END : MID)));
	}
}
