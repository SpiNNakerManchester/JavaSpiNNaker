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

import static java.lang.System.nanoTime;
import static org.slf4j.LoggerFactory.getLogger;
import static uk.ac.manchester.spinnaker.messages.Constants.SCP_SCAMP_PORT;
import static uk.ac.manchester.spinnaker.utils.WaitUtils.waitUntil;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;

import org.slf4j.Logger;

import uk.ac.manchester.spinnaker.connections.SDPConnection;
import uk.ac.manchester.spinnaker.machine.ChipLocation;
import uk.ac.manchester.spinnaker.machine.CoreLocation;
import uk.ac.manchester.spinnaker.machine.tags.IPTag;
import uk.ac.manchester.spinnaker.messages.sdp.SDPMessage;

/**
 * A connection for handling the Data Speed Up protocol.
 *
 * @author Donal Fellows
 */
final class GatherDownloadConnection extends SDPConnection {
	private long lastSend = 0L;
	private static final Logger log = getLogger(GatherDownloadConnection.class);
	/**
	 * Packet minimum send interval, in <em>nanoseconds</em>.
	 */
	private static final int INTER_SEND_INTERVAL_NS = 60000;
	/** An empty buffer. Used so we don't try to read zero bytes. */
	private static final ByteBuffer EMPTY_DATA = ByteBuffer.allocate(0);

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

	private void sendMsg(SDPMessage msg) throws IOException {
		waitUntil(lastSend + INTER_SEND_INTERVAL_NS);
		send(msg);
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
	void sendStart(CoreLocation extraMonitorCore, int address, int length,
			int transactionId) throws IOException {
		sendMsg(StartSendingMessage.create(extraMonitorCore, address, length,
				transactionId));
	}

	/**
	 * Sends a message telling the extra monitor to stop sending fixed route
	 * packets.
	 *
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
	 */
	ByteBuffer getNextPacket(int timeout) throws IOException {
		try {
			ByteBuffer b = receive(timeout);
			if (b == null) {
				return EMPTY_DATA;
			}
			return b;
		} catch (SocketTimeoutException ignored) {
			log.debug("received timeout");
			return EMPTY_DATA;
		}
	}
}
