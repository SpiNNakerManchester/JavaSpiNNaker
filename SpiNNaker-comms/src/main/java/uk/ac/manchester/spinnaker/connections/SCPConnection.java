/*
 * Copyright (c) 2018 The University of Manchester
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
package uk.ac.manchester.spinnaker.connections;

import static java.lang.Thread.currentThread;
import static java.util.Collections.synchronizedMap;
import static java.util.Objects.requireNonNull;
import static java.util.concurrent.Executors.newSingleThreadScheduledExecutor;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.slf4j.LoggerFactory.getLogger;
import static uk.ac.manchester.spinnaker.messages.Constants.SCP_SCAMP_PORT;

import java.io.IOException;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ScheduledExecutorService;

import org.slf4j.Logger;

import com.google.errorprone.annotations.ForOverride;

import uk.ac.manchester.spinnaker.connections.model.SCPSenderReceiver;
import uk.ac.manchester.spinnaker.machine.HasChipLocation;
import uk.ac.manchester.spinnaker.messages.scp.SCPResultMessage;
import uk.ac.manchester.spinnaker.utils.Daemon;

/** A UDP connection to SC&amp;MP on the board. */
public class SCPConnection extends SDPConnection implements SCPSenderReceiver {
	private static final Logger log = getLogger(SCPConnection.class);

	private static final ScheduledExecutorService CLOSER;

	private final Map<Integer, Thread> receiverMap =
			synchronizedMap(new HashMap<>());

	private final Map<Thread, Deque<SCPResultMessage>> receiverQueues =
			synchronizedMap(new WeakHashMap<>());

	static {
		CLOSER = newSingleThreadScheduledExecutor(
				r -> new Daemon(r, "SCPConnection.Closer"));
	}

	/**
	 * Create a connection to a particular instance of SCAMP.
	 *
	 * @param chip
	 *            The location of the chip on the board with this remoteHost
	 * @param remoteHost
	 *            The remote host to send messages to.
	 * @throws IOException
	 *             If anything goes wrong with socket setup.
	 */
	public SCPConnection(HasChipLocation chip, InetAddress remoteHost)
			throws IOException {
		this(chip, null, null, remoteHost, SCP_SCAMP_PORT);
	}

	/**
	 * Create a connection to a particular instance of SCAMP. Can use a
	 * specified local network interface.
	 *
	 * @param chip
	 *            The location of the chip on the board with this remoteHost
	 * @param localHost
	 *            The optional host of the local interface to listen on; use
	 *            {@code null} to listen on all local interfaces.
	 * @param localPort
	 *            The optional local port to listen on; use {@code null} to pick
	 *            a random port.
	 * @param remoteHost
	 *            The remote host to send messages to.
	 * @param remotePort
	 *            The optional remote port number to send messages to. If
	 *            {@code null}, the default remote port is used.
	 * @throws IOException
	 *             If anything goes wrong with socket setup.
	 */
	protected SCPConnection(HasChipLocation chip, InetAddress localHost,
			Integer localPort, InetAddress remoteHost, int remotePort)
			throws IOException {
		super(chip, localHost, localPort, requireNonNull(remoteHost,
				"SCPConnection only meaningful with a real remote host"),
				remotePort);
	}

	/**
	 * Create a connection where the mechanism for sending and receiving
	 * messages is being overridden by a subclass.
	 *
	 * @param chip
	 *            The location of the target chip on the board.
	 * @throws IOException
	 *             If anything goes wrong with socket manipulation.
	 */
	protected SCPConnection(HasChipLocation chip) throws IOException {
		super(chip, true);
	}

	/**
	 * Do the basic reception of a message.
	 *
	 * @param timeout
	 *            The time in milliseconds to wait for the message to arrive,
	 *            oruntil the connection is closed.
	 * @return The SCP result, the sequence number, and the data of theresponse.
	 *         The buffer pointer will be positioned at the pointwhere the
	 *         payload starts.
	 * @throws IOException If there is an error receiving the message
	 * @throws InterruptedException If communications are interrupted.
	 */
	@ForOverride
	protected SCPResultMessage receiveSCPResultMessage(int timeout)
			throws IOException, InterruptedException {
		return new SCPResultMessage(receive(timeout));
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * This is tricky code because there can be multiple threads using a
	 * connection at once and we need to direct responses to the threads that
	 * originated the requests that the responses match to.
	 */
	@Override
	public final SCPResultMessage receiveSCPResponse(int timeout)
			throws IOException, InterruptedException {
		var currentThread = currentThread();
		var myQueue = receiverQueues.get(currentThread);
		while (true) {
			// Prefer to take a queued message; it's already been received...
			if (myQueue == null) {
				myQueue = receiverQueues.get(currentThread);
			}
			if (myQueue != null) {
				var queuedMsg = myQueue.pollFirst();
				if (queuedMsg != null) {
					return queuedMsg;
				}
			}
			// Need to talk to the network
			var receivedMsg = receiveSCPResultMessage(timeout);
			var targetThread =
					receiverMap.remove(receivedMsg.getSequenceNumber());
			// If message is ours or unrecognized, pass to caller
			if (targetThread == currentThread || targetThread == null) {
				return receivedMsg;
			}
			// Queue for correct thread
			receiverQueues.computeIfAbsent(targetThread,
					__ -> new ConcurrentLinkedDeque<>()).addLast(receivedMsg);
		}
	}

	/**
	 * Close this connection eventually. The close might not happen immediately.
	 */
	@SuppressWarnings("FutureReturnValueIgnored")
	public void closeEventually() {
		CLOSER.schedule(this::closeAndLogNoExcept, 1, SECONDS);
	}

	/**
	 * Close this connection, logging failures instead of throwing.
	 * <p>
	 * Core of implementation of {@link #closeEventually()}.
	 */
	protected final void closeAndLogNoExcept() {
		try {
			var name = "";
			if (log.isInfoEnabled()) {
				name = toString();
			}
			close();
			log.info("closed {}", name);
		} catch (IOException e) {
			log.warn("failed to close connection", e);
		}
	}

	@Override
	public void send(ByteBuffer requestData, int seq) throws IOException {
		send(requestData);
		receiverMap.put(seq, currentThread());
	}
}
