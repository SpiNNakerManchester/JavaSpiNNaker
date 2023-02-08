/*
 * Copyright (c) 2018-2019 The University of Manchester
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package uk.ac.manchester.spinnaker.front_end.download;

import static java.lang.System.nanoTime;
import static java.nio.ByteBuffer.allocate;
import static org.slf4j.LoggerFactory.getLogger;
import static uk.ac.manchester.spinnaker.utils.WaitUtils.waitUntil;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;

import org.slf4j.Logger;

import uk.ac.manchester.spinnaker.connections.SCPConnection;
import uk.ac.manchester.spinnaker.machine.ChipLocation;
import uk.ac.manchester.spinnaker.machine.CoreLocation;
import uk.ac.manchester.spinnaker.machine.MemoryLocation;
import uk.ac.manchester.spinnaker.machine.tags.IPTag;
import uk.ac.manchester.spinnaker.messages.sdp.SDPMessage;
import uk.ac.manchester.spinnaker.transceiver.ProcessException;
import uk.ac.manchester.spinnaker.transceiver.TransceiverInterface;

/**
 * A connection for handling the Data Speed Up protocol.
 *
 * @author Donal Fellows
 */
final class GatherDownloadConnection {
	private long lastSend = 0L;

	private static final Logger log = getLogger(GatherDownloadConnection.class);

	/**
	 * Packet minimum send interval, in <em>nanoseconds</em>.
	 */
	private static final int INTER_SEND_INTERVAL_NS = 60000;

	/** An empty buffer. Used so we don't try to read zero bytes. */
	private static final ByteBuffer EMPTY_DATA = allocate(0);

	private final SCPConnection connection;

	/**
	 * Create an instance.
	 *
	 * @param connection
	 *            What connection are we using to talk to the board.
	 * @throws IOException
	 *             If anything goes wrong with socket setup.
	 */
	GatherDownloadConnection(SCPConnection connection)
			throws IOException {
		this.connection = connection;
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

	/**
	 * Close the connection.
	 *
	 * @throws IOException If there is an error closing the connection.
	 */
	void close() throws IOException {
		connection.close();
	}

	/**
	 * Set the IP tag for this connection.
	 *
	 * @param txrx The transceiver to use to set the tag.
	 * @param iptag The tag to set
	 *
	 * @throws ProcessException If something goes wrong in the protocol.
	 * @throws IOException If something goes wrong in the comms.
	 * @throws InterruptedException If the comms are interrupted.
	 */
	void setIPTag(TransceiverInterface txrx, IPTag iptag)
			throws ProcessException, IOException, InterruptedException {
		txrx.setIPTag(iptag, connection);
	}
}
