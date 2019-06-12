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
import static java.lang.String.format;
import static java.lang.Thread.sleep;
import static java.util.Collections.unmodifiableList;
import static java.util.stream.Collectors.toList;
import static org.slf4j.LoggerFactory.getLogger;
import static uk.ac.manchester.spinnaker.front_end.Constants.PARALLEL_SIZE;
import static uk.ac.manchester.spinnaker.front_end.Constants.SMALL_RETRIEVE_THRESHOLD;
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

import difflib.ChangeDelta;
import difflib.Chunk;
import difflib.DeleteDelta;
import difflib.Delta;
import difflib.InsertDelta;
import uk.ac.manchester.spinnaker.connections.selectors.ConnectionSelector;
import uk.ac.manchester.spinnaker.connections.selectors.MostDirectConnectionSelector;
import uk.ac.manchester.spinnaker.front_end.BasicExecutor;
import uk.ac.manchester.spinnaker.front_end.BasicExecutor.SimpleCallable;
import uk.ac.manchester.spinnaker.front_end.BoardLocalSupport;
import uk.ac.manchester.spinnaker.front_end.NoDropPacketContext;
import uk.ac.manchester.spinnaker.front_end.Progress;
import uk.ac.manchester.spinnaker.front_end.download.request.Gather;
import uk.ac.manchester.spinnaker.front_end.download.request.Monitor;
import uk.ac.manchester.spinnaker.front_end.download.request.Placement;
import uk.ac.manchester.spinnaker.machine.ChipLocation;
import uk.ac.manchester.spinnaker.machine.CoreLocation;
import uk.ac.manchester.spinnaker.machine.Machine;
import uk.ac.manchester.spinnaker.machine.tags.IPTag;
import uk.ac.manchester.spinnaker.machine.tags.TrafficIdentifier;
import uk.ac.manchester.spinnaker.storage.BufferManagerStorage;
import uk.ac.manchester.spinnaker.storage.BufferManagerStorage.Region;
import uk.ac.manchester.spinnaker.storage.StorageException;
import uk.ac.manchester.spinnaker.transceiver.Transceiver;
import uk.ac.manchester.spinnaker.transceiver.processes.ProcessException;
import uk.ac.manchester.spinnaker.utils.DefaultMap;
import uk.ac.manchester.spinnaker.utils.MathUtils;
import uk.ac.manchester.spinnaker.utils.ValueHolder;

/**
 * Implementation of the SpiNNaker Fast Data Download Protocol.
 *
 * @author Donal Fellows
 * @author Alan Stokes
 */
public abstract class DataGatherer extends BoardLocalSupport {
	/**
	 * Logger for the gatherer.
	 */
	protected static final Logger log = getLogger(DataGatherer.class);
	/** The maximum number of times to retry. */
	private static final int TIMEOUT_RETRY_LIMIT = 20;
	/**
	 * The time delay between sending each message. In
	 * {@linkplain java.util.concurrent.TimeUnit#MILLISECONDS milliseconds}.
	 */
	private static final int DELAY_PER_SEND = 10;
	/**
	 * The timeout when receiving a message. In
	 * {@linkplain java.util.concurrent.TimeUnit#MILLISECONDS milliseconds}.
	 */
	private static final int TIMEOUT_PER_RECEIVE = 1000;
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
	private static final int DATA_WORDS_PER_PACKET = SDP_PAYLOAD_WORDS - 1;
	/**
	 * Mask used to pick out the bit that says whether a sequence number is the
	 * last in a stream.
	 */
	private static final int LAST_MESSAGE_FLAG_BIT_MASK = 0x80000000;
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
		super(machine);
		this.txrx = transceiver;
		this.machine = machine;
		this.pool = new BasicExecutor(PARALLEL_SIZE);
		this.missCount = 0;
	}

	private static final String META_LABEL = "reading region metadata";
	private static final String FAST_LABEL = "high-speed transfers";
	private static final String SLOW_LABEL = "slow-speed transfers";

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
	 */
	public int gather(List<Gather> gatherers)
			throws IOException, ProcessException, StorageException {
		sanityCheck(gatherers);
		ValueHolder<Integer> workSize = new ValueHolder<>(0);
		Map<ChipLocation, List<WorkItems>> work;
		try (Progress bar =
				new Progress(countPlacements(gatherers), META_LABEL)) {
			work = discoverActualWork(gatherers, workSize, bar);
		}
		Map<ChipLocation, GatherDownloadConnection> conns =
				createConnections(gatherers, work);
		Map<ChipLocation, List<Region>> smallWork =
				new DefaultMap<>(ArrayList::new);
		try (NoDropPacketContext p = new NoDropPacketContext(txrx,
				gatherers.stream().flatMap(g -> g.getMonitors().stream()));
				Progress bar = new Progress(workSize.getValue(), FAST_LABEL)) {
			log.info("launching {} parallel high-speed download tasks",
					work.size());
			/*
			 * Checkstyle gets the indentation rules wrong for the next
			 * statement.
			 */
			// CHECKSTYLE:OFF
			parallel(work.keySet().stream()
					.map(key -> () -> fastDownload(work.get(key),
							conns.get(key), smallWork, bar)));
			// CHECKSTYLE:ON
		} finally {
			log.info("shutting down high-speed download connections");
			for (GatherDownloadConnection c : conns.values()) {
				c.close();
			}
		}
		log.info("launching {} parallel low-speed download tasks",
				smallWork.size());
		try (Progress bar = new Progress(
				smallWork.values().stream().mapToInt(List::size).sum(),
				SLOW_LABEL)) {
			// CHECKSTYLE:OFF
			parallel(smallWork.values().stream()
					.map(regions -> () -> slowDownload(regions, bar)));
			// CHECKSTYLE:ON
		}
		return missCount;
	}

	private int countPlacements(List<Gather> gatherers) {
		Stream<Gather> gaths = gatherers.parallelStream();
		Stream<Monitor> mons = gaths.flatMap(g -> g.getMonitors().stream());
		return mons.mapToInt(m -> m.getPlacements().size()).sum();
	}

	/**
	 * Trivial POJO holding the pairing of monitor and list of lists of memory
	 * blocks.
	 *
	 * @author Donal Fellows
	 */
	private static final class WorkItems {
		/**
		 * Monitor that is used to download the regions.
		 */
		private final Monitor monitor;
		/**
		 * List of information about where to download. The inner sub-lists are
		 * ordered, and are either one or two items long to represent what
		 * pieces of memory should really be downloaded. The outer list could
		 * theoretically be done in any order... but needs to be processed
		 * single-threaded anyway.
		 */
		private final List<List<Region>> regions;

		WorkItems(Monitor m, List<List<Region>> region) {
			this.monitor = m;
			this.regions = region;
		}
	}

	/**
	 * Query the machine to discover what actual pieces of memory the recording
	 * region IDs of the placements of the vertices attached to the monitors
	 * associated with the data speed up packet gatherers are.
	 *
	 * @param gatherers
	 *            The gatherer information.
	 * @param workSize
	 *            Where to write how much work there is to actually do.
	 * @param bar
	 *            The progress bar.
	 * @return What each board (as represented by the chip location of its data
	 *         speed up packet gatherer) has to be downloaded.
	 * @throws IOException
	 *             If IO fails.
	 * @throws ProcessException
	 *             If SpiNNaker rejects a message.
	 * @throws StorageException
	 *             If DB access goes wrong.
	 */
	private Map<ChipLocation, List<WorkItems>> discoverActualWork(
			List<Gather> gatherers, ValueHolder<Integer> workSize, Progress bar)
			throws IOException, ProcessException, StorageException {
		log.info("discovering regions to download");
		Map<ChipLocation, List<WorkItems>> work = new HashMap<>();
		int count = 0;
		for (Gather g : gatherers) {
			List<WorkItems> workitems = new ArrayList<>();
			for (Monitor m : g.getMonitors()) {
				for (Placement p : m.getPlacements()) {
					List<List<Region>> regions = new ArrayList<>();
					for (int id : p.getVertex().getRecordedRegionIds()) {
						List<Region> r = getRegion(p, id);
						if (!r.isEmpty()) {
							regions.add(r);
						}
						count += r.size();
					}
					if (!regions.isEmpty()) {
						workitems.add(new WorkItems(m, regions));
					}
					bar.update();
				}

			}
			// Totally empty boards can be ignored
			if (!workitems.isEmpty()) {
				work.put(g.asChipLocation(), workitems);
			}
		}
		log.info("found {} regions to download", count);
		workSize.setValue(count);
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
	 */
	private Map<ChipLocation, GatherDownloadConnection> createConnections(
			List<Gather> gatherers, Map<ChipLocation, ?> work)
			throws IOException, ProcessException {
		log.info("building high-speed data connections and configuring IPtags");
		Map<ChipLocation, GatherDownloadConnection> connections =
				new HashMap<>();
		for (Gather g : gatherers) {
			ChipLocation gathererChip = g.asChipLocation();
			if (!work.containsKey(gathererChip)) {
				continue;
			}
			GatherDownloadConnection conn =
					new GatherDownloadConnection(gathererChip, g.getIptag());
			reconfigureIPtag(g.getIptag(), gathererChip, conn);
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
	 */
	private void parallel(Stream<SimpleCallable> tasks)
			throws IOException, ProcessException, StorageException {
		try {
			pool.submitTasks(tasks).awaitAndCombineExceptions();
		} catch (IOException | StorageException | ProcessException
				| RuntimeException e) {
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
	 * @param smallWork
	 *            Where to store any small or problem downloads; they're going
	 *            to be processed later via SCP READs. <em>The order of items in
	 *            the inner list may matter.</em>
	 * @param bar
	 *            The progress bar.
	 * @throws IOException
	 *             If IO fails.
	 * @throws StorageException
	 *             If DB access goes wrong.
	 */
	private void fastDownload(List<WorkItems> work,
			GatherDownloadConnection conn,
			Map<ChipLocation, List<Region>> smallWork, Progress bar)
			throws IOException, StorageException {
		/**
		 * A class that manages how to postpone work for doing the slow way.
		 * This is a little tricky because the order of download regions for a
		 * single recording region (could be up to two) needs to be preserved so
		 * that they're concatenated in the DB correctly.
		 *
		 * @author Donal Fellows
		 */
		class PostponeControl {
			boolean addToSmall = false;

			boolean shouldPostpone(Region region) {
				return addToSmall || (region.size < SMALL_RETRIEVE_THRESHOLD);
			}

			private void doPostpone(Region region) {
				addToSmall = true;
				synchronized (smallWork) {
					smallWork.get(conn.getChip()).add(region);
				}
			}

			/**
			 * Postpone a region.
			 *
			 * @param region
			 *            The region to postpone.
			 */
			void postpone(Region region) {
				log.info("moving {} to low-speed download system", region);
				doPostpone(region);
			}

			/**
			 * Postpone a region.
			 *
			 * @param region
			 *            The region to postpone.
			 * @param reason
			 *            Why it is being postponed.
			 */
			void postpone(Region region, String reason) {
				log.info("moving {} to low-speed download system{}{}", region,
						" due to ", reason);
				doPostpone(region);
			}

			/**
			 * Postpone a region because of a failure.
			 *
			 * @param region
			 *            The region to postpone.
			 * @param locus
			 *            Where the failure was.
			 * @param exn
			 *            What the failure was.
			 */
			void postpone(Region region, String locus, Exception exn) {
				log.info("moving {} to low-speed download system{}{}", region,
						" due to failure in ", locus, exn);
				doPostpone(region);
			}
		}

		try (BoardLocal c = new BoardLocal(conn.getChip())) {
			log.info("processing fast downloads", conn.getChip());
			Downloader dl = new Downloader(conn);
			for (WorkItems item : work) {
				for (List<Region> regionsOnCore : item.regions) {
					/*
					 * Once there's something too small, all subsequent
					 * retrieves for that recording region have to be done the
					 * same way to get the data in the DB in the right order.
					 */
					PostponeControl ctl = new PostponeControl();
					for (Region region : regionsOnCore) {
						try {
							if (ctl.shouldPostpone(region)) {
								ctl.postpone(region);
							} else {
								ByteBuffer data = dl.doDownload(
										item.monitor.asCoreLocation(), region);
								if (SPINNAKER_COMPARE_DOWNLOAD != null) {
									compareDownloadWithSCP(region, data);
								}
								storeData(region, data);
							}
						} catch (TimeoutException e) {
							ctl.postpone(region, "timeout");
						} catch (ProcessException e) {
							ctl.postpone(region, "comparison code", e);
						} finally {
							bar.update();
						}
					}
				}
			}
		}
	}

	/**
	 * Do the slow downloads for a particular board.
	 *
	 * @param regions
	 *            The regions to be processed on that board. <em>The order may
	 *            be significant.</em>
	 * @param bar
	 *            The progress bar.
	 * @throws IOException
	 *             If IO fails.
	 * @throws ProcessException
	 *             If SpiNNaker rejects a message.
	 * @throws StorageException
	 *             If DB access goes wrong.
	 */
	private void slowDownload(List<Region> regions, Progress bar)
			throws IOException, ProcessException, StorageException {
		try (BoardLocal c = new BoardLocal(regions.get(0).core)) {
			log.info("processing {} slow downloads", regions.size());
			for (Region region : regions) {
				log.info("processing slow download from {}", region);
				storeData(region, txrx.readMemory(region.core.getScampCore(),
						region.startAddress, region.size));
				bar.update();
			}
		}
	}

	private void sanityCheck(List<Gather> gatherers) {
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
	 * Hack. Make BufferManagerStorage used for checkstyle.
	 *
	 * @param hack
	 *            this is a hack
	 * @return this is a hack.
	 */
	@Deprecated
	Class<?> hack(BufferManagerStorage hack) {
		return hack.getClass();
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
		private CoreLocation monitorCore;
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
			expectedSeqNums = new BitSet(maxSeqNum);
			expectedSeqNums.set(0, maxSeqNum - 1);
			lastRequested = expectedSeqs();
			received = false;
			timeoutcount = 0;
			conn.sendStart(monitorCore, region.startAddress, region.size);
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
		 * @return True if we have finished.
		 * @throws IOException
		 *             If packet reception or retransmission requesting fails.
		 * @throws TimeoutException
		 *             If we have a full timeout, or if we are flailing around,
		 *             making no progress.
		 */
		private boolean processOnePacket(int timeout)
				throws IOException, TimeoutException {
			ByteBuffer p = conn.getNextPacket(timeout + INTERNAL_DELAY);
			if (p.hasRemaining()) {
				received = true;
				return processData(p);
			}
			return processTimeout();
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
		 * @throws TimeoutException
		 *             If we are flailing around, making no progress.
		 */
		private boolean processData(ByteBuffer data)
				throws IOException, TimeoutException {
			int seqNum = data.getInt();
			boolean isEndOfStream =
					((seqNum & LAST_MESSAGE_FLAG_BIT_MASK) != 0);
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
		private boolean processTimeout() throws IOException, TimeoutException {
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
		 * @throws TimeoutException
		 *             If we are flailing around, making no progress.
		 */
		private boolean retransmitMissingSequences()
				throws IOException, TimeoutException {
			int numMissing = expectedSeqNums.cardinality();
			if (numMissing < 1) {
				return true;
			}
			log.info("there are {} missing packets in message from {}",
					numMissing, monitorCore);

			// Build a list of the sequence numbers of all missing packets
			List<Integer> missingSeqs = expectedSeqs();
			missCount += numMissing;

			log.debug("missing sequence numbers: {}", missingSeqs);
			if (missingSeqs.equals(lastRequested)) {
				log.info(
						"retransmission cycle for {} made no progress;"
								+ " bailing out to slow transfer mode",
						monitorCore);
				throw new TimeoutException();
			}
			if (missingSeqs.size() >= lastRequested.size()) {
				log.warn("what is going on?");
				log.warn("last:{}", lastRequested);
				log.warn("this:{}", missingSeqs);
			}
			lastRequested = missingSeqs;

			// Transmit missing sequences as a new SDP Packet
			for (MissingSequenceNumbersMessage msg : createMessages(monitorCore,
					missingSeqs)) {
				snooze(DELAY_PER_SEND);
				conn.sendMissing(msg);
			}
			return false;
		}

		/**
		 * @return The expected sequence numbers, as an ordered list.
		 */
		private List<Integer> expectedSeqs() {
			return unmodifiableList(
					expectedSeqNums.stream().boxed().collect(toList()));
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
	private static final String TMPL =
			"got insane sequence number %d: expected maximum %d (%s)";
	private static final String MID = "totally bad sequence";
	private static final String END = "non-empty terminal-only packet";

	InsaneSequenceNumberException(int maxNum, int seqNum) {
		super(format(TMPL, seqNum, maxNum, (maxNum == seqNum ? END : MID)));
	}
}
