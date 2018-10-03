package uk.ac.manchester.spinnaker.front_end.download;

import static java.lang.Math.ceil;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.slf4j.LoggerFactory.getLogger;
import static uk.ac.manchester.spinnaker.front_end.download.Constants.DATA_PER_FULL_PACKET_WITH_SEQUENCE_NUM;
import static uk.ac.manchester.spinnaker.front_end.download.Constants.END_FLAG_SIZE_IN_BYTES;
import static uk.ac.manchester.spinnaker.front_end.download.Constants.LAST_MESSAGE_FLAG_BIT_MASK;
import static uk.ac.manchester.spinnaker.front_end.download.Constants.QUEUE_CAPACITY;
import static uk.ac.manchester.spinnaker.front_end.download.Constants.SEQUENCE_NUMBER_SIZE;
import static uk.ac.manchester.spinnaker.front_end.download.Constants.TIMEOUT_PER_SENDING_IN_MILLISECONDS;
import static uk.ac.manchester.spinnaker.front_end.download.Constants.TIMEOUT_RETRY_LIMIT;
import static uk.ac.manchester.spinnaker.front_end.download.MissingSequenceNumbersMessage.computeNumberOfPackets;
import static uk.ac.manchester.spinnaker.front_end.download.MissingSequenceNumbersMessage.createFirst;
import static uk.ac.manchester.spinnaker.front_end.download.MissingSequenceNumbersMessage.createNext;
import static uk.ac.manchester.spinnaker.messages.Constants.SCP_SCAMP_PORT;
import static uk.ac.manchester.spinnaker.messages.Constants.WORD_SIZE;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.Inet4Address;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.BitSet;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import org.slf4j.Logger;

import uk.ac.manchester.spinnaker.connections.SCPConnection;
import uk.ac.manchester.spinnaker.machine.HasChipLocation;
import uk.ac.manchester.spinnaker.machine.HasCoreLocation;
import uk.ac.manchester.spinnaker.messages.scp.IPTagSet;
import uk.ac.manchester.spinnaker.utils.InetFactory;

/**
 * Implementation of the SpiNNaker Fast Data Download Protocol.
 *
 * @author Alan Stokes
 * @author Donal Fellows
 */
public class HostDataReceiver extends Thread {
	private static final Logger log = getLogger(HostDataReceiver.class);

	private final int portConnection;
	private final HasCoreLocation placement;
	private final Inet4Address machineIP;
	private final int length;
	private final int address;
	private final HasChipLocation chip;
	private final int iptag;
	private final BlockingQueue<ByteBuffer> messQueue;
	private final byte[] buffer;
	private final int maxSeqNum;
	private boolean finished;
	private int missCount;
	private BitSet receivedSeqNums;

	/**
	 * Create an instance of the protocol implementation.
	 *
	 * @param portConnection
	 *            What port we talk to.
	 * @param placement
	 *            What core on SpiNNaker do we download from.
	 * @param hostname
	 *            Where the SpiNNaker machine is.
	 * @param lengthInBytes
	 *            How many bytes to download.
	 * @param memoryAddress
	 *            Where in the SpiNNaker core's memory to download from
	 * @param chip
	 *            Used for reprogramming the IP tag.
	 * @param iptag
	 *            The ID of the IP tag.
	 * @throws UnknownHostException
	 *             If the hostname can't be resolved to an IPv4 address.
	 */
	public HostDataReceiver(int portConnection, HasCoreLocation placement,
			String hostname, int lengthInBytes, int memoryAddress,
			HasChipLocation chip, int iptag) throws UnknownHostException {
		this.portConnection = portConnection;
		this.placement = placement;
		this.chip = chip;
		this.iptag = iptag;
		machineIP = InetFactory.getByName(hostname);
		length = lengthInBytes;
		address = memoryAddress;
		// allocate queue for messages
		messQueue = new ArrayBlockingQueue<>(QUEUE_CAPACITY);
		buffer = new byte[length];
		maxSeqNum = calculateMaxSeqNum(length);
		receivedSeqNums = new BitSet(maxSeqNum);
		finished = false;
		missCount = 0;
	}

	/**
	 * Divide one integer by another with rounding up. For example,
	 * <tt>ceildiv(5,3) == 2</tt>
	 *
	 * @param numerator
	 *            The value to be divided.
	 * @param denominator
	 *            The value to divide by.
	 * @return The value got by dividing the two, and rounding any floating
	 *         remainder <i>up</i>.
	 */
	static final int ceildiv(int numerator, int denominator) {
		return (int) ceil((float) numerator / (float) denominator);
	}

	/**
	 * Downloads the data from the configured location on a SpiNNaker system.
	 *
	 * @return The data.
	 * @throws InterruptedException
	 *             If the thread gets interrupted.
	 * @throws IOException
	 *             If any I/O fails.
	 */
	public byte[] getData() throws InterruptedException, IOException {
		// create connection
		SCPConnection sender = null;
		try {
			sender = new SCPConnection(placement, machineIP, SCP_SCAMP_PORT);
		} catch (SocketException ex) {
			log.error("failed to create UDP connection", ex);
			return null;
		}

		// send the initial command to start data transmission
		sendInitialCommand(sender, sender);
		ReaderThread reader = new ReaderThread(sender);
		ProcessorThread processor = new ProcessorThread(sender);
		reader.start();
		processor.start();
		reader.join();
		processor.join();
		log.debug("done!!!");
		return buffer;
	}

	/**
	 * Write the received data to a file.
	 *
	 * @param dataFile
	 *            The file to write the data to.
	 * @param missingReportFile
	 *            The file to write a count of the number of misses to. This
	 *            measures the quality of the retrieval process.
	 * @throws IOException
	 *             If the I/O operations on the files fail or if the actual
	 *             reading of the data fails.
	 * @throws InterruptedException
	 *             If the process of doing the download is interrupted.
	 */
	public void writeData(String dataFile, String missingReportFile)
			throws IOException, InterruptedException {
		try (FileOutputStream fp1 = new FileOutputStream(dataFile);
				PrintWriter fp2 = new PrintWriter(missingReportFile)) {
			getData();
			fp1.write(buffer);
			fp2.println(missCount);
		}
	}

	/**
	 * Write the received data to a file.
	 *
	 * @param dataFile
	 *            The file to write the data to.
	 * @throws IOException
	 *             If the I/O operations on the file fail or if the actual
	 *             reading of the data fails.
	 * @throws InterruptedException
	 *             If the process of doing the download is interrupted.
	 */
	public void writeData(String dataFile)
			throws IOException, InterruptedException {
		try (FileOutputStream fp1 = new FileOutputStream(dataFile)) {
			getData();
			fp1.write(buffer);
		}
	}

	private boolean retransmitMissingSequences(SCPConnection sender,
			BitSet receivedSeqNums) throws InterruptedException, IOException {
		int numPackets;
		int i;
		// Calculate number of missing sequences based on difference between
		// expected and received
		IntBuffer missingSeqs =
				IntBuffer.allocate(maxSeqNum - receivedSeqNums.cardinality());
		// Calculate missing sequence numbers and add them to "missing"
		log.debug("max seq num of " + maxSeqNum);
		for (i = 0; i < maxSeqNum; i++) {
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
		numPackets = computeNumberOfPackets(missingSeqs.limit());

		// Transmit missing sequences as a new SDP Packet
		for (i = 0; i < numPackets; i++) {
			MissingSequenceNumbersMessage message;

			// If first, add n packets to list; otherwise just add data
			if (i == 0) {
				message = createFirst(placement, portConnection, missingSeqs,
						numPackets);
			} else {
				message = createNext(placement, portConnection, missingSeqs);
			}
			sender.sendSDPMessage(message);
			sleep(TIMEOUT_PER_SENDING_IN_MILLISECONDS);
		}
		return false;
	}

	private void processData(SCPConnection sender, ByteBuffer data)
			throws Exception {
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
		if (trueDataLength > length) {
			throw new IllegalStateException("received more data than expected");
		}

		if (!isEndOfStream || data.limit() != END_FLAG_SIZE_IN_BYTES) {
			data.position(SEQUENCE_NUMBER_SIZE);
			data.get(buffer, offset, trueDataLength - offset);
		}
		receivedSeqNums.set(seqNum);
		if (isEndOfStream) {
			if (!check()) {
				finished |= retransmitMissingSequences(sender, receivedSeqNums);
			} else {
				finished = true;
			}
		}
	}

	private void sendInitialCommand(SCPConnection sender,
			SCPConnection receiver) throws IOException {
		// Build an SCP request to set up the IP Tag associated to this socket
		sender.sendSCPRequest(
				new IPTagSet(chip, receiver.getLocalIPAddress().getAddress(),
						receiver.getLocalPort(), iptag, true));
		sender.receiveSCPResponse(null);

		// Create and send Data request SDP packet
		sender.sendSDPMessage(StartSendingMessage.create(placement,
				portConnection, address, length));
	}

	private static int calculateMaxSeqNum(int length) {
		return ceildiv(length,
				DATA_PER_FULL_PACKET_WITH_SEQUENCE_NUM * WORD_SIZE);
	}

	private boolean check() throws Exception {
		int recvsize = receivedSeqNums.length();
		if (recvsize > maxSeqNum + 1) {
			throw new Exception("Received more data than expected");
		}
		return recvsize == maxSeqNum + 1;
	}

	private static final String TIMEOUT_MESSAGE = "failed to hear from the "
			+ "machine (please try removing firewalls)";

	private class ProcessorThread extends Thread {
		private final SCPConnection connection;
		private int timeoutcount = 0;

		ProcessorThread(SCPConnection connection) {
			super("ProcessorThread");
			this.connection = connection;
		}

		private void processOnePacket(boolean reiceved) throws Exception {
			ByteBuffer p = messQueue.poll(1, SECONDS);
			if (p != null && p.hasRemaining()) {
				processData(connection, p);
				reiceved = true;
			} else {
				timeoutcount++;
				if (timeoutcount > TIMEOUT_RETRY_LIMIT && !reiceved) {
					log.error(TIMEOUT_MESSAGE);
					return;
				}
				if (!finished) {
					// retransmit missing packets
					log.debug("doing reinjection");
					finished = retransmitMissingSequences(connection,
							receivedSeqNums);
				}
			}
		}

		@Override
		public void run() {
			try {
				boolean reiceved = false;
				while (!finished) {
					processOnePacket(reiceved);
				}
			} catch (InterruptedException e) {
				// Do nothing
			} catch (Exception e) {
				log.error("problem in packet processing thread", e);
			} finally {
				// close socket and inform the reader that transmission is
				// completed
				try {
					connection.close();
				} catch (Exception e) {
					log.error("problem in packet processing thread", e);
				}
			}
			finished = true;
		}
	}

	private class ReaderThread extends Thread {
		private final SCPConnection connection;

		ReaderThread(SCPConnection connection) {
			super("ReadThread");
			this.connection = connection;
		}

		@Override
		public void run() {
			// While socket is open add messages to the queue
			try {
				do {
					ByteBuffer recvd = connection.receive();
					if (recvd != null) {
						messQueue.put(recvd);
						log.debug("pushed");
					}
				} while (!connection.isClosed());
			} catch (InterruptedException e) {
				log.error("failed to offer packet to queue");
			} catch (IOException e) {
				log.error("failed to receive packet", e);
			}
		}
	}
}
