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
package uk.ac.manchester.spinnaker.protocols.download;

import static java.lang.System.nanoTime;
import static java.nio.ByteBuffer.allocate;
import static org.slf4j.LoggerFactory.getLogger;
import static uk.ac.manchester.spinnaker.messages.Constants.CPU_USER_1_START_ADDRESS;
import static uk.ac.manchester.spinnaker.messages.Constants.WORD_SIZE;
import static uk.ac.manchester.spinnaker.transceiver.Utils.getVcpuAddress;
import static uk.ac.manchester.spinnaker.utils.WaitUtils.waitUntil;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;

import org.slf4j.Logger;

import com.google.errorprone.annotations.MustBeClosed;

import uk.ac.manchester.spinnaker.connections.SCPConnection;
import uk.ac.manchester.spinnaker.connections.SingletonConnectionSelector;
import uk.ac.manchester.spinnaker.machine.ChipLocation;
import uk.ac.manchester.spinnaker.machine.CoreLocation;
import uk.ac.manchester.spinnaker.machine.HasCoreLocation;
import uk.ac.manchester.spinnaker.machine.MemoryLocation;
import uk.ac.manchester.spinnaker.machine.tags.IPTag;
import uk.ac.manchester.spinnaker.messages.scp.IPTagSet;
import uk.ac.manchester.spinnaker.messages.scp.ReadMemory;
import uk.ac.manchester.spinnaker.messages.sdp.SDPMessage;
import uk.ac.manchester.spinnaker.transceiver.ProcessException;
import uk.ac.manchester.spinnaker.transceiver.TxrxProcess;

/**
 * A connection for handling the Data Speed Up protocol.
 *
 * @author Donal Fellows
 */
final class GatherDownloadConnection implements AutoCloseable {
	private long lastSend = 0L;

	private static final Logger log = getLogger(GatherDownloadConnection.class);

	/**
	 * Packet minimum send interval, in <em>nanoseconds</em>.
	 */
	private static final int INTER_SEND_INTERVAL_NS = 60000;

	/** An empty buffer. Used so we don't try to read zero bytes. */
	private static final ByteBuffer EMPTY_DATA = allocate(0);

	/** cap of where a transaction id will get to. */
	private static final int TRANSACTION_ID_CAP = 0xFFFFFFFF;

	private final IPTag iptag;

	private final SCPConnection connection;

	/**
	 * Create an instance.
	 *
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
	GatherDownloadConnection(IPTag iptag)
			throws IOException, ProcessException, InterruptedException {
		this.iptag = iptag;
		this.connection = new SCPConnection(iptag.getDestination(), null, null,
						iptag.getBoardAddress());
		this.setupTag();
	}

	void setupTag() throws ProcessException, IOException, InterruptedException {
		var process = new TxrxProcess(new SingletonConnectionSelector<>(
				connection), null);
		process.call(new IPTagSet(connection.getChip(), null, 0,
					iptag.getTag(), iptag.isStripSDP(), true));
	}

	/**
	 * Get the next transaction ID to use.
	 *
	 * @param gatherCore The core being used to download data.
	 * @return The next transaction ID to use.
	 * @throws IOException
	 *             If IO fails.
	 * @throws ProcessException
	 *             If SpiNNaker rejects the reprogramming.
	 * @throws InterruptedException
	 *             If communications are interrupted.
	 */
	public int getNextTransactionId(HasCoreLocation gatherCore)
			throws ProcessException, IOException, InterruptedException {
		var addr = getVcpuAddress(gatherCore).add(CPU_USER_1_START_ADDRESS);
		var process = new TxrxProcess(
				new SingletonConnectionSelector<>(connection), null);
		var transactionId = process.retrieve(
				new ReadMemory(gatherCore.getScampCore(), addr, WORD_SIZE))
				.getInt();
		return (transactionId + 1) & TRANSACTION_ID_CAP;
	}

	private void sendMsg(SDPMessage msg) throws IOException {
		if (waitUntil(lastSend + INTER_SEND_INTERVAL_NS)) {
			throw new InterruptedIOException(
					"interrupted while waiting to send");
		}
		connection.send(msg);
		lastSend = nanoTime();
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
	 * @param transactionId
	 *            The transaction id of this stream.
	 * @throws IOException
	 *             If message sending fails.
	 */
	void sendStart(CoreLocation extraMonitorCore, MemoryLocation address,
			int length, int transactionId) throws IOException {
		sendMsg(StartSendingMessage.create(extraMonitorCore, address, length,
				transactionId));
	}

	/**
	 * Sends a message telling the extra monitor to stop sending fixed route
	 * packets.
	 *super(location, iptag.getBoardAddress(), SCP_SCAMP_PORT);
	 * @param extraMonitorCore
	 *            The location of the monitor.
	 * @param transactionId
	 *            The transaction id of this stream.
	 * @throws IOException
	 *             If message sending fails.
	 */
	void sendClear(CoreLocation extraMonitorCore, int transactionId)
			throws IOException {
		sendMsg(ClearMessage.create(extraMonitorCore, transactionId));
	}

	/**
	 * Send a message asking the extra monitor core to ask it to resend some
	 * data.
	 *
	 * @param msg
	 *            The message to send, built as part of a sequence of such
	 *            messages.
	 * @throws IOException
	 *             If message sending fails.
	 */
	void sendMissing(MissingSequenceNumbersMessage msg) throws IOException {
		sendMsg(msg);
	}

	/**
	 * Receive a data-speed-up payload packet.
	 *
	 * @param timeout
	 *            How long to wait for the packet.
	 * @return The packet's raw contents. On timeout, an empty buffer will be
	 *         returned. Never returns {@code null}.
	 * @throws IOException
	 *             If a non-recoverable error (e.g., closed channel) happens.
	 * @throws InterruptedException
	 *             If communications are interrupted.
	 */
	ByteBuffer getNextPacket(int timeout)
			throws IOException, InterruptedException {
		try {
			var b = connection.receive(timeout);
			if (b == null) {
				return EMPTY_DATA;
			}
			return b;
		} catch (SocketTimeoutException e) {
			log.debug("received timeout");
			return EMPTY_DATA;
		}
	}

	/**
	 * Get the chip this gatherer is downloading from.
	 *
	 * @return The chip of the gatherer.
	 */
	ChipLocation getChip() {
		return connection.getChip();
	}

	@Override
	public void close() throws IOException {
		connection.close();
	}
}
