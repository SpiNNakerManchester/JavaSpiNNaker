/*
 * Copyright (c) 2019 The University of Manchester
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
package uk.ac.manchester.spinnaker.connections;

import static java.lang.System.nanoTime;
import static java.nio.ByteOrder.LITTLE_ENDIAN;
import static java.util.stream.Collectors.toList;
import static java.util.stream.IntStream.range;
import static org.slf4j.LoggerFactory.getLogger;
import static uk.ac.manchester.spinnaker.utils.MathUtils.hexbyte;
import static uk.ac.manchester.spinnaker.utils.UnitConstants.NSEC_PER_USEC;
import static uk.ac.manchester.spinnaker.utils.WaitUtils.waitUntil;

import java.io.Closeable;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;

import com.google.errorprone.annotations.MustBeClosed;

import uk.ac.manchester.spinnaker.machine.ChipLocation;
import uk.ac.manchester.spinnaker.machine.tags.IPTag;
import uk.ac.manchester.spinnaker.messages.scp.IPTagSet;
import uk.ac.manchester.spinnaker.messages.sdp.SDPMessage;
import uk.ac.manchester.spinnaker.transceiver.ProcessException;
import uk.ac.manchester.spinnaker.transceiver.TxrxProcess;

/**
 * An SDP connection that uses a throttle to stop SCAMP from overloading. Note
 * that this does not bother to implement the full connection API.
 *
 * @author Donal Fellows
 */
public class ThrottledConnection implements Closeable {
	private static final Logger log = getLogger(ThrottledConnection.class);

	/** The minimum interval between messages, in <em>nanoseconds</em>. */
	public static final long THROTTLE_NS = 50000;

	/** The {@link #receive()} timeout, in milliseconds. */
	private static final int TIMEOUT_MS = 2000;

	static {
		log.info("inter-message minimum time set to {}us",
				THROTTLE_NS / NSEC_PER_USEC);
	}

	private final SCPConnection connection;

	private final AtomicBoolean closed = new AtomicBoolean();

	private long lastSend = nanoTime();

	/**
	 * Create a throttled connection for talking to a board and point an IPTag
	 * so that messages sent to it arrive on this connection.
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
	public ThrottledConnection(IPTag iptag)
			throws IOException, ProcessException, InterruptedException {
		connection = new SCPConnection(iptag.getDestination(), null, null,
				iptag.getBoardAddress());
		log.debug("created throttled connection to {} ({}) from {}:{}; "
						+ "reprogramming tag #{} to point to this connection",
				iptag.getDestination(), iptag.getBoardAddress(),
				connection.getLocalIPAddress(),
				connection.getLocalPort(), iptag.getTag());
		var process = new TxrxProcess(new SingletonConnectionSelector<>(
				connection), null);
		process.call(new IPTagSet(connection.getChip(), null, 0,
					iptag.getTag(), iptag.isStripSDP(), true));
	}

	/**
	 * Get a message from the connection.
	 *
	 * @return The content of the message.
	 * @throws SocketTimeoutException
	 *             If no message is received by the timeout.
	 * @throws IOException
	 *             If IO fails.
	 * @throws InterruptedException
	 *             If communications are interrupted.
	 */
	public ByteBuffer receive()
			throws SocketTimeoutException, IOException, InterruptedException {
		return connection.receive(TIMEOUT_MS).slice().order(LITTLE_ENDIAN);
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
			var payload = message.getData();
			log.debug("message payload data: {}", range(0, payload.remaining())
					.mapToObj(i -> hexbyte(payload.get(i))).collect(toList()));
		}
		throttledSend(message);
	}

	private void throttledSend(SDPMessage message) throws IOException {
		if (waitUntil(lastSend + THROTTLE_NS)) {
			throw new InterruptedIOException(
					"interrupted while sending message");
		}
		connection.send(message);
		lastSend = nanoTime();
	}

	@Override
	public void close() {
		if (closed.compareAndSet(false, true)) {
			connection.closeEventually();
		}
	}

	public ChipLocation getLocation() {
		return connection.getChip();
	}

	@Override
	public String toString() {
		return "Throttled: " + connection.toString();
	}
}
