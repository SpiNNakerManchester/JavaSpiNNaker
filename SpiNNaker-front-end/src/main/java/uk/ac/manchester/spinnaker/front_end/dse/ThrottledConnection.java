/*
 * Copyright (c) 2019 The University of Manchester
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
import static java.lang.Thread.yield;
import static java.net.InetAddress.getByName;
import static java.nio.ByteOrder.LITTLE_ENDIAN;
import static java.util.Collections.emptySet;
import static java.util.stream.Collectors.toList;
import static java.util.stream.IntStream.range;
import static org.slf4j.LoggerFactory.getLogger;
import static uk.ac.manchester.spinnaker.messages.Constants.SCP_SCAMP_PORT;
import static uk.ac.manchester.spinnaker.utils.MathUtils.hexbyte;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import org.slf4j.Logger;

import uk.ac.manchester.spinnaker.connections.SCPConnection;
import uk.ac.manchester.spinnaker.machine.ChipLocation;
import uk.ac.manchester.spinnaker.machine.tags.IPTag;
import uk.ac.manchester.spinnaker.messages.model.UnexpectedResponseCodeException;
import uk.ac.manchester.spinnaker.messages.scp.IPTagSet;
import uk.ac.manchester.spinnaker.messages.sdp.SDPMessage;
import uk.ac.manchester.spinnaker.storage.DSEStorage.Ethernet;

/**
 * An SDP connection that uses a throttle to stop SCAMP from overloading. Note
 * that this does not bother to implement the full connection API.
 *
 * @author Donal Fellows
 */
public class ThrottledConnection implements Closeable {
	private static final Logger log = getLogger(ThrottledConnection.class);
	/** The minimum interval between messages, in <em>nanoseconds</em>. */
	public static final int THROTTLE_NS = 35000;
	/** The {@link #receive()} timeout, in milliseconds. */
	private static final int TIMEOUT_MS = 1000;
	private static final int IPTAG_REPROGRAM_TIMEOUT = 1;
	private static final int IPTAG_REPROGRAM_ATTEMPTS = 3;

	private final ChipLocation location;
	private final InetAddress addr;
	private SCPConnection connection;
	private long lastSend;

	/**
	 * Create a throttled connection for talking to a board.
	 *
	 * @param board
	 *            The board to talk to.
	 * @throws IOException
	 *             If IO fails.
	 */
	public ThrottledConnection(Ethernet board) throws IOException {
		location = board.location;
		addr = getByName(board.ethernetAddress);
		connection = new SCPConnection(location, addr, SCP_SCAMP_PORT);
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

	private static final byte[] INADDR_ANY = new byte[] {
		0, 0, 0, 0
	};
	private static final int ANY_PORT = 0;

	/**
	 * Reprogram a SpiNNaker IPTag to direct messages to this connection. It's
	 * up to the caller to ensure that the tag is allocated in the first place.
	 *
	 * @param iptag
	 *            The tag to reprogram.
	 * @throws IOException
	 *             If IO fails.
	 * @throws UnexpectedResponseCodeException
	 *             If a weird message is received.
	 */
	public void reprogramTag(IPTag iptag)
			throws IOException, UnexpectedResponseCodeException {
		log.debug("reprogramming tag {} to point to {}:{}", iptag.getTag(),
				connection.getLocalIPAddress(), connection.getLocalPort());
		IPTagSet tagSet = new IPTagSet(location, INADDR_ANY, ANY_PORT,
				iptag.getTag(), true, true);
		tagSet.scpRequestHeader.issueSequenceNumber(emptySet());
		ByteBuffer data = connection.getSCPData(tagSet);
		SocketTimeoutException e = null;
		for (int i = 0; i < IPTAG_REPROGRAM_ATTEMPTS; i++) {
			try {
				connection.send(data.duplicate());
				lastSend = nanoTime();
				connection.receiveSCPResponse(IPTAG_REPROGRAM_TIMEOUT)
						.parsePayload(tagSet);
				return;
			} catch (SocketTimeoutException timeout) {
				e = timeout;
			} catch (IOException | RuntimeException
					| UnexpectedResponseCodeException ex) {
				throw ex;
			} catch (Exception ex) {
				throw new RuntimeException("unexpected exception", e);
			}
		}
		if (e != null) {
			throw e;
		}
	}

	/**
	 * Shut down and reopen the connection. It sometimes unsticks things
	 * apparently.
	 *
	 * @throws IOException
	 *             If IO fails
	 */
	public void restart() throws IOException {
		log.info("restarting UDP connection");
		InetAddress localAddr = connection.getLocalIPAddress();
		int localPort = connection.getLocalPort();
		connection.close();
		connection = new SCPConnection(location, localAddr, localPort, addr,
				SCP_SCAMP_PORT);
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
		long waited = nanoTime() - lastSend;
		if (waited < THROTTLE_NS) {
			// BUSY LOOP! https://stackoverflow.com/q/11498585/301832
			while (nanoTime() - lastSend < THROTTLE_NS) {
				// Make the loop slightly less heavy
				yield();
			}
		}
		connection.sendSDPMessage(message);
		lastSend = nanoTime();
	}

	@Override
	public void close() throws IOException {
		connection.close();
	}
}
