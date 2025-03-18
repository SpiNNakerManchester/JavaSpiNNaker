/*
 * Copyright (c) 2025 The University of Manchester
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
package uk.ac.manchester.spinnaker.protocols.download;

import static java.lang.String.format;
import static java.lang.Thread.sleep;
import static java.nio.ByteBuffer.allocate;
import static java.util.stream.Collectors.toUnmodifiableList;
import static org.slf4j.LoggerFactory.getLogger;
import static uk.ac.manchester.spinnaker.protocols.download.MissingSequenceNumbersMessage.createMessages;
import static uk.ac.manchester.spinnaker.messages.Constants.SDP_PAYLOAD_WORDS;
import static uk.ac.manchester.spinnaker.messages.Constants.WORD_SIZE;
import static uk.ac.manchester.spinnaker.utils.MathUtils.ceildiv;

import java.io.IOException;
import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import java.util.BitSet;
import java.util.List;

import org.slf4j.Logger;

import com.google.errorprone.annotations.MustBeClosed;

import uk.ac.manchester.spinnaker.machine.HasCoreLocation;
import uk.ac.manchester.spinnaker.machine.MemoryLocation;
import uk.ac.manchester.spinnaker.machine.tags.IPTag;
import uk.ac.manchester.spinnaker.transceiver.ProcessException;

/**
 * Class used to manage a download. Every instance <em>must only</em> ever
 * be used from one thread.
 *
 * @author Donal Fellows
 */
public class Downloader implements AutoCloseable {

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

	/**
	 * Logger for the gatherer.
	 */
	protected static final Logger log = getLogger(Downloader.class);

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

	private List<Integer> lastRequested;

	/**
	 * Create an instance.
	 * @param iptag
	 *            The tag to reprogram to talk to this connection.
	 * @throws IOException
	 *             If IO fails.
	 * @throws ProcessException
	 *             If SpiNNaker rejects the reprogramming.
	 * @throws InterruptedException
	 *             If communications are interrupted.
	 */
	@MustBeClosed
	@SuppressWarnings("MustBeClosed")
	public Downloader(IPTag iptag)
			throws IOException, ProcessException, InterruptedException {
		conn = new GatherDownloadConnection(iptag);
	}

	/**
	 * Prepare to reuse the downloader.
	 *
	 * @throws IOException
	 *             If IO fails.
	 * @throws ProcessException
	 *             If SpiNNaker rejects the reprogramming.
	 * @throws InterruptedException
	 *             If communications are interrupted.
	 */
	public void reuse()
			throws IOException, ProcessException, InterruptedException {
		conn.setupTag();
	}

	@Override
	public void close() throws IOException {
		conn.close();
	}

	/**
	 * Do the downloading.
	 *
	 * @param monitorCore
	 *           The core to download from.
	 * @param address
	 *           The address to download from.
	 * @param size
	 *          The size of the download.
	 * @return The downloaded data, or {@code null} if an unrecoverable
	 *         error occurred.
	 * @throws IOException
	 *             If anything unexpected goes wrong.
	 * @throws ProcessException
	 *             If anything unexpected goes wrong.
	 * @throws InterruptedException
	 *             If communications are interrupted.
	 */
	public ByteBuffer doDownload(
			HasCoreLocation monitorCore, MemoryLocation address,
			int size) throws IOException, ProcessException,
			InterruptedException {
		dataReceiver = allocate(size);
		/*
		 * Tricky point: where an amount of data to be downloaded is exactly
		 * a multiple of the number of payload words per packet, we need an
		 * extra packet because the data gatherer executable sends the data
		 * before it knows whether it has reached the end (and the extra
		 * monitors don't know about the chunking).
		 *
		 * This translates into needing to add one here.
		 */
		maxSeqNum = ceildiv(size, DATA_WORDS_PER_PACKET * WORD_SIZE);
		expectedSeqNums = new BitSet(maxSeqNum);
		expectedSeqNums.set(0, maxSeqNum);
		lastRequested = expectedSeqs();
		received = false;
		timeoutcount = 0;
		var transactionId = conn.getNextTransactionId(monitorCore);
		log.debug(
				"extracting data from {} with size {} with "
						+ "transaction id {} using {} packets",
				address, size, transactionId, maxSeqNum);
		conn.sendStart(monitorCore.asCoreLocation(), address, size,
				transactionId);
		try {
			boolean finished;
			do {
				finished = processOnePacket(TIMEOUT_PER_RECEIVE, transactionId,
						monitorCore);
			} while (!finished);
			conn.sendClear(monitorCore.asCoreLocation(), transactionId);
		} catch (TimeoutException e) {
			if (received) {
				log.warn(
						"received only some of the packets from <{}> "
								+ "for {}:{}; has something crashed?",
						monitorCore, address, size);
			}
			throw new IOException(e);
		} finally {
			if (!received) {
				log.warn("never actually received any packets from "
						+ "<{}> for {}:{}", monitorCore, address, size);
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
	private boolean processOnePacket(int timeout, int transactionId,
			HasCoreLocation monitorCore)
			throws IOException, TimeoutException, InterruptedException {
		var p = conn.getNextPacket(timeout + INTERNAL_DELAY);
		if (p.hasRemaining()) {
			received = true;
			timeoutcount = 0;
			return processData(p, transactionId, monitorCore);
		}
		log.debug("failed to receive on connection {}.", conn);
		return processTimeout(transactionId, monitorCore);
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
	private boolean processData(ByteBuffer data, int transactionId,
			HasCoreLocation monitorCore)
			throws IOException, TimeoutException, InterruptedException {
		int seqNum = data.getInt();
		int responseTransactionId = data.getInt();

		if (responseTransactionId != transactionId) {
			log.warn("received packet for transaction {} "
					+ "when expecting packet for transaction {}",
					responseTransactionId, transactionId);
			return false;
		}

		var isEndOfStream = ((seqNum & LAST_MESSAGE_FLAG_BIT_MASK) != 0);
		seqNum &= ~LAST_MESSAGE_FLAG_BIT_MASK;
		log.debug("received packet {} from {} for transaction {}, end? {}",
				seqNum, monitorCore, transactionId, isEndOfStream);

		if (seqNum > maxSeqNum || seqNum < 0) {
			throw new InsaneSequenceNumberException(maxSeqNum, seqNum);
		}
		int len = data.remaining();
		log.debug("packet length: {}", len);
		if (len != DATA_WORDS_PER_PACKET * WORD_SIZE && len != 0
				&& seqNum < maxSeqNum - 1) {
			log.warn("short packet ({} bytes) in non-terminal position "
					+ "(seq: {})", len, seqNum);
		}
		if (data.hasRemaining()) {
			int offset = seqNum * DATA_WORDS_PER_PACKET * WORD_SIZE;
			log.debug("writing to offset {}", offset);
			if (offset < 0) {
				// Off the start!
				throw new ReceivingBufferOverflowedException(
						len, offset, dataReceiver.limit());
			}
			// The IllegalArgumentException on failure here is useful
			dataReceiver.position(offset);
			try {
				dataReceiver.put(data);
			} catch (BufferOverflowException ignored) {
				// There's no info in that exception
				throw new ReceivingBufferOverflowedException(
						len, offset, dataReceiver.limit());
			}
			expectedSeqNums.clear(seqNum);
		}
		if (!isEndOfStream) {
			return false;
		}
		return retransmitMissingSequences(transactionId, monitorCore);
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
	private boolean processTimeout(int transactionId,
			HasCoreLocation monitorCore)
			throws IOException, TimeoutException, InterruptedException {
		if (++timeoutcount > TIMEOUT_RETRY_LIMIT) {
			log.error(TIMEOUT_MESSAGE);
			throw new TimeoutException();
		}

		// retransmit missing packets
		log.debug("doing reinjection");
		return retransmitMissingSequences(transactionId, monitorCore);
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
	private boolean retransmitMissingSequences(int transactionId,
			HasCoreLocation monitorCore)
			throws IOException, TimeoutException, InterruptedException {
		int numMissing = expectedSeqNums.cardinality();
		if (numMissing < 1) {
			return true;
		}
		log.debug("there are {} missing packets in message from {}",
				numMissing, monitorCore);

		// Build a list of the sequence numbers of all missing packets
		var missingSeqs = expectedSeqs();

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

/**
 * Exception that indicates a download tried to write off the end of the
 * receiving buffer or otherwise misbehave. Don't use the standard exceptions
 * for this; they don't have any useful info at all.
 *
 * @see BufferOverflowException
 * @author Donal Fellows
 */
final class ReceivingBufferOverflowedException extends RuntimeException {
	private static final long serialVersionUID = 3L;

	ReceivingBufferOverflowedException(int len, int start, int limit) {
		super(format(
				"failed to store %d bytes at position %d of %d "
						+ "(overflow = %d bytes)",
				len, start, limit, start + len - limit));
	}
}
