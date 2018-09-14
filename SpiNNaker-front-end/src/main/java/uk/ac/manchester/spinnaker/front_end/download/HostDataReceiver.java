package uk.ac.manchester.spinnaker.front_end.download;

import static java.lang.Math.ceil;
import static java.lang.Math.min;
import static java.nio.ByteBuffer.allocate;
import static java.nio.ByteOrder.LITTLE_ENDIAN;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetAddress;
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
import uk.ac.manchester.spinnaker.messages.sdp.SDPHeader;
import uk.ac.manchester.spinnaker.messages.sdp.SDPMessage;

public class HostDataReceiver extends Thread {
	private static final Logger log = getLogger(HostDataReceiver.class);

	private static final int QUEUE_CAPACITY = 1024;
	// consts for data and converting between words and bytes
	private static final int DATA_PER_FULL_PACKET = 68;
	private static final int DATA_PER_FULL_PACKET_WITH_SEQUENCE_NUM =
			DATA_PER_FULL_PACKET - 1;
	private static final int BYTES_PER_WORD = 4;
	private static final int END_FLAG_SIZE_IN_BYTES = 4;
	private static final int SEQUENCE_NUMBER_SIZE = 4;
	private static final int LAST_MESSAGE_FLAG_BIT_MASK = 0x80000000;
	private static final int SDP_PACKET_START_SENDING_COMMAND_ID = 100;
	private static final int SDP_PACKET_START_MISSING_SEQ_COMMAND_ID = 1000;
	private static final int SDP_PACKET_MISSING_SEQ_COMMAND_ID = 1001;
	// time out constants
	public static final int TIMEOUT_RETRY_LIMIT = 20;
	private static final int TIMEOUT_PER_SENDING_IN_MILLISECONDS = 10;
	private static final int TIMEOUT_PER_RECEIVE_IN_MILLISECONDS = 250;

	private final int portConnection;
	private final HasCoreLocation placement;
	private final InetAddress hostname;
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

	public HostDataReceiver(int portConnection, HasCoreLocation placement,
			String hostname, int lengthInBytes, int memoryAddress,
			HasChipLocation chip, int iptag) throws UnknownHostException {
		this.portConnection = portConnection;
		this.placement = placement;
		this.hostname = InetAddress.getByName(hostname);
		this.length = lengthInBytes;
		this.address = memoryAddress;
		this.chip = chip;
		this.iptag = iptag;
		// allocate queue for messages
		messQueue = new ArrayBlockingQueue<>(QUEUE_CAPACITY);
		buffer = new byte[lengthInBytes];
		maxSeqNum = calculateMaxSeqNum(lengthInBytes);
		receivedSeqNums = new BitSet(maxSeqNum);
		finished = false;
		missCount = 0;
	}

	/**
	 * Divide one integer by another with rounding up.
	 */
	private static final int ceildiv(int numerator, int denominator) {
		return (int) ceil((float) numerator / (float) denominator);
	}

	int timeout = TIMEOUT_PER_RECEIVE_IN_MILLISECONDS;

	public byte[] getData() throws InterruptedException, IOException {
		// create connection
		SCPConnection sender = null;
		try {
			sender = new SCPConnection(placement, hostname, 17893);
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

	public void getDataThreadable(String filepath_read, String filepath_missing)
			throws FileNotFoundException, IOException, InterruptedException {
		try (FileOutputStream fp1 = new FileOutputStream(filepath_read);
				PrintWriter fp2 = new PrintWriter(filepath_missing)) {
			getData();
			fp1.write(buffer);
			fp2.println(missCount);
		}
	}

	public boolean retransmitMissingSequences(SCPConnection sender,
			BitSet receivedSeqNums) throws InterruptedException, IOException {
		int wordsToSend;
		int numPackets;
		int i;
		// Calculate number of missing sequences based on difference between
		// expected and received
		int missDim = maxSeqNum - receivedSeqNums.cardinality();
		int[] missingSeqs = new int[missDim];
		int j = 0;
		// Calculate missing sequence numbers and add them to "missing"
		log.debug("max seq num of " + maxSeqNum);
		for (i = 0; i < maxSeqNum; i++) {
			if (!receivedSeqNums.get(i)) {
				missingSeqs[j++] = i;
				missCount++;
			}
		}
		if (log.isDebugEnabled()) {
			log.debug("missing" + missDim);
			for (i = 0; i < missDim; i++) {
				log.debug("missing seq " + missingSeqs[i]);
			}
		}
		// Set correct number of lost sequences
		missDim = j;
		// No missing sequences
		if (missDim == 0) {
			return true;
		}
		numPackets = 1;
		int lengthViaFormat2 = missDim - (DATA_PER_FULL_PACKET - 2);
		if (lengthViaFormat2 > 0) {
			numPackets += ceildiv(lengthViaFormat2, DATA_PER_FULL_PACKET - 1);
		}

		// Transmit missing sequences as a new SDP Packet
		int seqNumOffset = 0;
		j = 0;
		for (i = 0; i < numPackets; i++) {
			ByteBuffer data = null;
			int wordsLeftInPacket = DATA_PER_FULL_PACKET;

			// If first, add n packets to list; otherwise just add data
			if (i == 0) {
				// Get left over space / data size
				wordsToSend =
						min(wordsLeftInPacket - 2, missDim - seqNumOffset);
				data = allocate((wordsToSend + 2) * BYTES_PER_WORD)
						.order(LITTLE_ENDIAN);

				// Pack flag and n packets
				data.putInt(SDP_PACKET_START_MISSING_SEQ_COMMAND_ID);
				data.putInt(numPackets);

				// Update state
				wordsLeftInPacket -= 2;
			} else {
				// Get left over space / data size
				wordsToSend = min(DATA_PER_FULL_PACKET_WITH_SEQUENCE_NUM,
						missDim - seqNumOffset);
				data = allocate((wordsToSend + 1) * BYTES_PER_WORD)
						.order(LITTLE_ENDIAN);

				// Pack flag
				data.putInt(SDP_PACKET_MISSING_SEQ_COMMAND_ID);
				wordsLeftInPacket -= 1;
			}
			for (int element = 0; element < wordsToSend; element++) {
				data.putInt(missingSeqs[j++]);
			}
			seqNumOffset += wordsLeftInPacket;
			sender.sendSDPMessage(new SDPMessage(
					new SDPHeader(SDPHeader.Flag.REPLY_NOT_EXPECTED, placement,
							portConnection),
					data));
			Thread.sleep(TIMEOUT_PER_SENDING_IN_MILLISECONDS);
		}
		return false;
	}

	public void processData(SCPConnection sender, ByteBuffer data)
			throws Exception {
		int firstPacketElement = data.getInt();
		int seqNum = firstPacketElement & ~LAST_MESSAGE_FLAG_BIT_MASK;
		boolean isEndOfStream =
				((firstPacketElement & LAST_MESSAGE_FLAG_BIT_MASK) != 0);

		if (seqNum > maxSeqNum || seqNum < 0) {
			throw new IllegalStateException("Got insane sequence number");
		}
		int offset = seqNum * DATA_PER_FULL_PACKET_WITH_SEQUENCE_NUM
				* BYTES_PER_WORD;
		int trueDataLength = offset + data.limit() - SEQUENCE_NUMBER_SIZE;
		if (trueDataLength > length) {
			throw new IllegalStateException(
					"Receiving more data than expected");
		}

		if (isEndOfStream && data.limit() == END_FLAG_SIZE_IN_BYTES) {
			// empty
		} else {
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

	static class StartSending extends SDPMessage {
		static StartSending create(HasCoreLocation destination, int destPort,
				int address, int length) {
			ByteBuffer payload = allocate(3 * 4).order(LITTLE_ENDIAN);
			IntBuffer msgPayload = payload.asIntBuffer();
			msgPayload.put(SDP_PACKET_START_SENDING_COMMAND_ID);
			msgPayload.put(address);
			msgPayload.put(length);
			return new StartSending(destination, destPort, payload);
		}

		private StartSending(HasCoreLocation destination, int destPort,
				ByteBuffer payload) {
			super(new SDPHeader(SDPHeader.Flag.REPLY_NOT_EXPECTED, destination,
					destPort), payload);
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
		sender.sendSDPMessage(StartSending.create(placement, portConnection,
				address, length));
	}

	private static int calculateMaxSeqNum(int length) {
		return ceildiv(length,
				DATA_PER_FULL_PACKET_WITH_SEQUENCE_NUM * BYTES_PER_WORD);
	}

	private boolean check() throws Exception {
		int recvsize = receivedSeqNums.length();
		if (recvsize > maxSeqNum + 1) {
			throw new Exception("Received more data than expected");
		}
		return recvsize == maxSeqNum + 1;
	}

	public static final String TIMEOUT_MESSAGE = "Failed to hear from the "
			+ "machine. Please try removing firewalls.";

	private class ProcessorThread extends Thread {
		private final SCPConnection connection;
		private int timeoutcount = 0;

		public ProcessorThread(SCPConnection connection) {
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

		public ReaderThread(SCPConnection connection) {
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
