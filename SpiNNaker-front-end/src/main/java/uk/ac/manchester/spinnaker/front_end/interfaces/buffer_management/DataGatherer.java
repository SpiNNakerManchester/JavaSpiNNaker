package uk.ac.manchester.spinnaker.front_end.interfaces.buffer_management;

import static java.lang.Thread.sleep;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.slf4j.LoggerFactory.getLogger;
import static uk.ac.manchester.spinnaker.front_end.interfaces.buffer_management.MissingSequenceNumbersMessage.computeNumberOfPackets;
import static uk.ac.manchester.spinnaker.front_end.interfaces.buffer_management.MissingSequenceNumbersMessage.createFirst;
import static uk.ac.manchester.spinnaker.front_end.interfaces.buffer_management.MissingSequenceNumbersMessage.createNext;
import static uk.ac.manchester.spinnaker.messages.Constants.SCP_SCAMP_PORT;
import static uk.ac.manchester.spinnaker.messages.Constants.WORD_SIZE;
import static uk.ac.manchester.spinnaker.utils.MathUtils.ceildiv;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.BitSet;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;

import uk.ac.manchester.spinnaker.connections.SCPConnection;
import uk.ac.manchester.spinnaker.machine.ChipLocation;
import uk.ac.manchester.spinnaker.machine.CoreLocation;
import uk.ac.manchester.spinnaker.machine.tags.IPTag;
import uk.ac.manchester.spinnaker.storage.DatabaseEngine;
import uk.ac.manchester.spinnaker.storage.SQLiteStorage;
import uk.ac.manchester.spinnaker.transceiver.Transceiver;
import uk.ac.manchester.spinnaker.transceiver.processes.ProcessException;

public class DataGatherer {
	private static final Logger log = getLogger(DataGatherer.class);

	private final Transceiver txrx;
	private final SQLiteStorage database;
	private final ExecutorService pool;
	private int missCount;

	private static final int POOL_SIZE = 1; // TODO
	private static final int QUEUE_CAPACITY = 1024;
	/** The maximum number of times to retry. */
	private static final int TIMEOUT_RETRY_LIMIT = 20;
	/** The time delay between sending each message. */
	private static final int TIMEOUT_PER_SENDING_IN_MILLISECONDS = 10;
	/** The timeout when receiving message. */
	private static final int TIMEOUT_PER_RECEIVE_IN_MILLISECONDS = 250;
	/** What is the maximum number of <i>words</i> in a packet? */
	private static final int DATA_PER_FULL_PACKET = 68;
	/**
	 * What is the maximum number of payload <i>words</i> in a packet that also
	 * has a sequence number?
	 */
	private static final int DATA_PER_FULL_PACKET_WITH_SEQUENCE_NUM =
			DATA_PER_FULL_PACKET - 1;
	/** How many bytes for the end-flag? */
	private static final int END_FLAG_SIZE_IN_BYTES = WORD_SIZE;
	/** How many bytes for the sequence number? */
	private static final int SEQUENCE_NUMBER_SIZE = WORD_SIZE;
	/**
	 * Mask used to pick out the bit that says whether a sequence number is the
	 * last in a stream.
	 */
	private static final int LAST_MESSAGE_FLAG_BIT_MASK = 0x80000000;

	public DataGatherer(Transceiver trans, DatabaseEngine database) {
		this.txrx = trans;
		this.database = new SQLiteStorage(database);
		this.pool = Executors.newFixedThreadPool(POOL_SIZE);
		this.missCount = 0;
	}


	public void addTask(Gather g) {
		pool.execute(() -> downloadBoard(g));
	}

	public int waitForTasksToFinish() throws InterruptedException {
		pool.shutdown();
		pool.awaitTermination(1, TimeUnit.DAYS);
		return missCount;
	}

	private static class Region {
		CoreLocation core;
		int regionID;
		int startAddress;
		int size;
	}

	private static class GatherDownloadConnection extends SCPConnection {
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
					do {
						ByteBuffer recvd =
								receive(TIMEOUT_PER_RECEIVE_IN_MILLISECONDS);
						if (recvd != null) {
							messQueue.put(recvd);
							log.debug("pushed");
						}
					} while (!isClosed());
				} catch (InterruptedException e) {
					log.error("failed to offer packet to queue");
				} catch (IOException e) {
					log.error("failed to receive packet", e);
				}
			}
		}
	}

	private void downloadBoard(Gather g) {
		try {
			// getRegionLocations();
			ChipLocation gathererLocation = g.asChipLocation();
			try (GatherDownloadConnection conn = new GatherDownloadConnection(
					gathererLocation, g.getIptag())) {
				reconfigureIPtag(g.getIptag(), gathererLocation, conn);
				setBoardRouterTimeoutsOff(gathererLocation);
				BlockingQueue<ByteBuffer> messQueue = conn.launchReaderThread();
				for (Monitor mon : g.getMonitors()) {
					for (Placement place : mon.getPlacements()) {
						for (int regionID : place.getVertex()
								.getRecordedRegionIds()) {
							Downloader d = new Downloader(conn, messQueue);
							Region r = getRegion(place, regionID);
							ByteBuffer data = d.doDownload(place, r);
							database.appendRegionContents(r.core, r.regionID,
									data);
						}
					}
				}
			} finally {
				setBoardRouterTimeoutsOn(gathererLocation);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void reconfigureIPtag(IPTag iptag, ChipLocation gathererLocation,
			GatherDownloadConnection conn) throws IOException, ProcessException {
		txrx.setIPTag(new IPTag(iptag.getBoardAddress(), gathererLocation,
				conn.getLocalPort(), iptag.getIPAddress()));
	}

	private void setBoardRouterTimeoutsOff(ChipLocation boardEthernet) {
		// TODO reconfigure router timeouts
	}

	private void setBoardRouterTimeoutsOn(ChipLocation boardEthernet) {
		// TODO reconfigure router timeouts
	}

	private Map<CoreLocation, Map<Integer, ByteBuffer>> coreTableCache =
			new HashMap<>();
	/** The number of memory regions in the DSE model. */
	private static final int MAX_MEM_REGIONS = 16;
	/** Application data magic number. */
	static final int APPDATA_MAGIC_NUM = 0xAD130AD6;
	/** Version of the file produced by the DSE. */
	static final int DSE_VERSION = 0x00010000;

	private IntBuffer getCoreRegionTable(CoreLocation core, Vertex vertex)
			throws IOException, ProcessException {
		// TODO get this info from the database
		Map<Integer, ByteBuffer> map;
		synchronized (coreTableCache) {
			map = coreTableCache.get(core);
			if (map == null) {
				map = new HashMap<>();
				coreTableCache.put(core, map);
			}
		}
		// Individual cores are only ever handled from one thread
		ByteBuffer buffer = map.get(vertex.recordingRegionBaseAddress);
		if (buffer == null) {
			buffer = txrx.readMemory(core, vertex.recordingRegionBaseAddress,
					WORD_SIZE * (MAX_MEM_REGIONS + 2));
			int word = buffer.getInt();
			if (word != APPDATA_MAGIC_NUM) {
				throw new IllegalStateException(
						String.format("unexpected magic number: %08x", word));
			}
			word = buffer.getInt();
			if (word != DSE_VERSION) {
				throw new IllegalStateException(
						String.format("unexpected DSE version: %08x", word));
			}
			map.put(vertex.recordingRegionBaseAddress, buffer);
		}
		return buffer.asIntBuffer();
	}

	private Region getRegion(Placement placement, int regionID)
			throws IOException, ProcessException {
		Region r = new Region();
		r.core = placement.asCoreLocation();
		r.regionID = regionID;
		IntBuffer b = getCoreRegionTable(r.core, placement.vertex);
		r.startAddress = b.get(regionID);
		// TODO This is probably wrong!
		r.size = b.get(regionID + 1) - r.startAddress;
		return r;
	}

	private static final String TIMEOUT_MESSAGE = "failed to hear from the "
			+ "machine (please try removing firewalls)";

	private class Downloader {
		private final GatherDownloadConnection conn;
		private final BlockingQueue<ByteBuffer> messQueue;

		private boolean received;
		private boolean finished;
		private int timeoutcount = 0;
		private BitSet receivedSeqNums;
		private int maxSeqNum;
		private ByteBuffer dataReceiver;
		private CoreLocation extraMonitor;

		Downloader(GatherDownloadConnection conn,
				BlockingQueue<ByteBuffer> messQueue) {
			this.conn = conn;
			this.messQueue = messQueue;
		}

		ByteBuffer doDownload(Placement extraMonitor, Region region)
				throws IOException {
			this.extraMonitor = extraMonitor.asCoreLocation();
			dataReceiver = ByteBuffer.allocate(region.size);
			maxSeqNum = calculateMaxSeqNum(region.size);
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

		private int calculateMaxSeqNum(int size) {
			return ceildiv(size,
					DATA_PER_FULL_PACKET_WITH_SEQUENCE_NUM * WORD_SIZE);
		}

		private void processOnePacket() throws Exception {
			ByteBuffer p = messQueue.poll(1, SECONDS);
			if (p != null && p.hasRemaining()) {
				processData(p);
				received = true;
			} else {
				timeoutcount++;
				if (timeoutcount > TIMEOUT_RETRY_LIMIT && !received) {
					log.error(TIMEOUT_MESSAGE);
					finished = true;
					return;
				}
				if (!finished) {
					// retransmit missing packets
					log.debug("doing reinjection");
					finished = retransmitMissingSequences();
				}
			}
		}

		private void processData(ByteBuffer data)
				throws IOException, InterruptedException {
			int firstPacketElement = data.getInt();
			int seqNum = firstPacketElement & ~LAST_MESSAGE_FLAG_BIT_MASK;
			boolean isEndOfStream =
					((firstPacketElement & LAST_MESSAGE_FLAG_BIT_MASK) != 0);

			if (seqNum > maxSeqNum || seqNum < 0) {
				throw new IllegalStateException("Got insane sequence number");
			}
			int offset =
					seqNum * DATA_PER_FULL_PACKET_WITH_SEQUENCE_NUM * WORD_SIZE;
			int trueDataLength = offset + data.limit() - SEQUENCE_NUMBER_SIZE;
			if (trueDataLength > dataReceiver.capacity()) {
				throw new IllegalStateException(
						"received more data than expected");
			}

			if (!isEndOfStream || data.limit() != END_FLAG_SIZE_IN_BYTES) {
				data.position(SEQUENCE_NUMBER_SIZE);
				data.get(dataReceiver.array(), offset, trueDataLength - offset);
			}
			receivedSeqNums.set(seqNum);
			if (isEndOfStream) {
				if (!check()) {
					finished |= retransmitMissingSequences();
				} else {
					finished = true;
				}
			}
		}

		private boolean check() {
			int recvsize = receivedSeqNums.length();
			if (recvsize > maxSeqNum + 1) {
				throw new IllegalStateException(
						"Received more data than expected");
			}
			return recvsize == maxSeqNum + 1;
		}

		private boolean retransmitMissingSequences()
				throws IOException, InterruptedException {
			// Calculate number of missing sequences based on difference between
			// expected and received
			IntBuffer missingSeqs = IntBuffer
					.allocate(maxSeqNum - receivedSeqNums.cardinality());
			// Calculate missing sequence numbers and add them to "missing"
			log.debug("max seq num of " + maxSeqNum);
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
				sleep(TIMEOUT_PER_SENDING_IN_MILLISECONDS);
				conn.sendNextMissing(extraMonitor, missingSeqs);
			}
			return false;
		}
	}
}
