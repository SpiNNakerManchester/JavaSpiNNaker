/*
 * Copyright (c) 2019-2020 The University of Manchester
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
package uk.ac.manchester.spinnaker.front_end.dse;

import static java.lang.System.nanoTime;
import static java.net.InetAddress.getByName;
import static java.nio.ByteOrder.LITTLE_ENDIAN;
import static java.util.concurrent.Executors.newSingleThreadScheduledExecutor;
import static java.util.concurrent.TimeUnit.SECONDS;
import static java.util.stream.Collectors.toList;
import static java.util.stream.IntStream.range;
import static org.slf4j.LoggerFactory.getLogger;
import static uk.ac.manchester.spinnaker.messages.Constants.SCP_SCAMP_PORT;
import static uk.ac.manchester.spinnaker.utils.MathUtils.hexbyte;
import static uk.ac.manchester.spinnaker.utils.UnitConstants.NSEC_PER_USEC;
import static uk.ac.manchester.spinnaker.utils.WaitUtils.waitUntil;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.concurrent.ScheduledExecutorService;

import org.slf4j.Logger;

import uk.ac.manchester.spinnaker.connections.SCPConnection;
import uk.ac.manchester.spinnaker.machine.ChipLocation;
import uk.ac.manchester.spinnaker.machine.tags.IPTag;
import uk.ac.manchester.spinnaker.messages.sdp.SDPMessage;
import uk.ac.manchester.spinnaker.storage.DSEStorage.Ethernet;
import uk.ac.manchester.spinnaker.transceiver.ProcessException;
import uk.ac.manchester.spinnaker.transceiver.Transceiver;

/**
 * An SDP connection that uses a throttle to stop SCAMP from overloading. Note
 * that this does not bother to implement the full connection API.
 *
 * @author Donal Fellows
 */
class ThrottledConnection implements Closeable {
	private static final Logger log = getLogger(ThrottledConnection.class);

	/** The minimum interval between messages, in <em>nanoseconds</em>. */
	public static final long THROTTLE_NS = 35000;

	/** The {@link #receive()} timeout, in milliseconds. */
	private static final int TIMEOUT_MS = 2000;

	private static final ScheduledExecutorService CLOSER;

	static {
		CLOSER = newSingleThreadScheduledExecutor(r -> {
			Thread t = new Thread(r, "ThrottledConnection.Closer");
			t.setDaemon(true);
			return t;
		});
		log.info("inter-message minimum time set to {}us",
				THROTTLE_NS / NSEC_PER_USEC);
	}

	private final ChipLocation location;

	private final InetAddress addr;

	private SCPConnection connection;

	private long lastSend = nanoTime();

	/**
	 * Create a throttled connection for talking to a board and point an IPTag
	 * so that messages sent to it arrive on this connection.
	 *
	 * @param transceiver
	 *            The SCP transceiver.
	 * @param board
	 *            The SpiNNaker board to talk to.
	 * @param iptag
	 *            The tag to reprogram to talk to this connection.
	 * @throws IOException
	 *             If IO fails.
	 * @throws ProcessException
	 *             If SpiNNaker rejects the reprogramming.
	 */
	ThrottledConnection(Transceiver transceiver, Ethernet board, IPTag iptag)
			throws IOException, ProcessException {
		location = board.location;
		addr = getByName(board.ethernetAddress);
		connection = new SCPConnection(location, addr, SCP_SCAMP_PORT);
		log.info(
				"created throttled connection to {} ({}) from {}:{}; "
						+ "reprogramming tag #{} to point to this connection",
				location, addr, connection.getLocalIPAddress(),
				connection.getLocalPort(), iptag.getTag());
		transceiver.setIPTag(iptag, connection);
	}

	/**
	 * Get a message from the connection.
	 *
	 * @return The content of the message.
	 * @throws SocketTimeoutException
	 *             If no message is received by the timeout.
	 * @throws IOException
	 *             If IO fails.
	 */
	public IntBuffer receive() throws SocketTimeoutException, IOException {
		return connection.receive(TIMEOUT_MS).slice().order(LITTLE_ENDIAN)
				.asIntBuffer();
	}

	/**
	 * Throttled send.
	 *
	 * @param message
	 *            The message to send.
	 * @throws IOException
	 *             If IO fails.
	 */
	public void send(SDPMessage message) throws IOException {
		log.debug("about to send {} bytes", message.getData().remaining());
		if (log.isDebugEnabled()) {
			ByteBuffer payload = message.getData();
			log.debug("message payload data: {}", range(0, payload.remaining())
					.mapToObj(i -> hexbyte(payload.get(i))).collect(toList()));
		}
		waitUntil(lastSend + THROTTLE_NS);
		connection.send(message);
		lastSend = nanoTime();
	}

	@Override
	public void close() {
		SCPConnection c = connection;
		connection = null;
		// Prevent reuse of existing socket IDs for other boards
		CLOSER.schedule(() -> {
			try {
				Object name = null;
				if (log.isInfoEnabled()) {
					name = c.toString();
				}
				c.close();
				log.info("closed {}", name);
			} catch (IOException e) {
				log.warn("failed to close connection", e);
			}
		}, 1, SECONDS);
	}
}
