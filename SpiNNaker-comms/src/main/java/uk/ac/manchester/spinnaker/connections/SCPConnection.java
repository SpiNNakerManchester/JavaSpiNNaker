/*
 * Copyright (c) 2018 The University of Manchester
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
package uk.ac.manchester.spinnaker.connections;

import static java.lang.Thread.currentThread;
import static java.lang.String.format;
import static java.util.Collections.synchronizedMap;
import static java.util.Objects.requireNonNull;
import static java.util.concurrent.Executors.newSingleThreadScheduledExecutor;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.slf4j.LoggerFactory.getLogger;
import static uk.ac.manchester.spinnaker.messages.Constants.SCP_SCAMP_PORT;

import java.io.IOException;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.Queue;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ScheduledExecutorService;

import org.slf4j.Logger;

import uk.ac.manchester.spinnaker.connections.model.SCPSenderReceiver;
import uk.ac.manchester.spinnaker.machine.HasChipLocation;
import uk.ac.manchester.spinnaker.messages.scp.SCPResultMessage;
import uk.ac.manchester.spinnaker.utils.Daemon;

/** A UDP connection to SC&amp;MP on the board. */
public class SCPConnection extends SDPConnection implements SCPSenderReceiver {
	private static final Logger log = getLogger(SCPConnection.class);

	/**
	 * Used to postpone actual closing of a connection, getting better
	 * connection ID distribution.
	 *
	 * @see #closeEventually()
	 */
	private static final ScheduledExecutorService CLOSER;

	/**
	 * Mapping from sequence numbers of requests actively in flight on this
	 * connection to the thread that is interested in the response. One-way
	 * requests do not get an entry in this table.
	 */
	private final Map<Integer, Thread> receiverMap = new ConcurrentHashMap<>();

	/**
	 * A <em>weak map</em> holding a concurrent queue of messages for each
	 * interested receiving thread. Only used to transfer messages from one
	 * thread to another when a message is received by the wrong thread.
	 * <p>
	 * Not a {@link ThreadLocal} because we need to look up queues from other
	 * threads too.
	 */
	private final Map<Thread, Queue<SCPResultMessage>> receiverQueues =
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
	 * @param localHost
	 *            The optional host of the local interface to listen on; use
	 *            {@code null} to listen on all local interfaces.
	 * @param localPort
	 *            The optional local port to listen on; use {@code null} to pick
	 *            a random port.
	 * @param remoteHost
	 *            The remote host to send messages to.
	 * @throws IOException
	 *             If anything goes wrong with socket setup.
	 */
	public SCPConnection(HasChipLocation chip, InetAddress localHost,
			Integer localPort, InetAddress remoteHost)
			throws IOException {
		this(chip, localHost, localPort, remoteHost, SCP_SCAMP_PORT);
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
	 * @param remoteHost
	 *            The remote host name or IP address to send packets to. If
	 *            {@code null}, the socket will be available for listening only,
	 *            and will throw an exception if used for sending.
	 *
	 * @throws IOException
	 *             If anything goes wrong with socket manipulation.
	 */
	protected SCPConnection(HasChipLocation chip, InetAddress remoteHost)
			throws IOException {
		super(chip, remoteHost, SCP_SCAMP_PORT);
	}

	private SCPResultMessage getMessageFromQueue() {
		var myQueue = receiverQueues.get(currentThread());
		if (myQueue == null) {
			return null;
		}
		return myQueue.poll();
	}

	private void putMessageOnQueue(Thread targetThread,
			SCPResultMessage receivedMsg) {
		receiverQueues
				.computeIfAbsent(targetThread,
						__ -> new ConcurrentLinkedQueue<>())
				.add(receivedMsg);
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * This is tricky code because there can be multiple threads using a
	 * connection at once and we need to direct responses to the threads that
	 * originated the requests that the responses match to. A consequence of
	 * this is that it is possible for the wait for a message to be longer than
	 * the timeout given; that will occur when the current thread keeps
	 * receiving messages for other threads.
	 */
	@Override
	public final SCPResultMessage receiveSCPResponse(int timeout)
			throws IOException, InterruptedException {
		while (true) {
			/*
			 * Prefer to take a queued message; it's already been received...
			 */
			var queuedMsg = getMessageFromQueue();
			if (queuedMsg != null) {
				return queuedMsg;
			}
			// Need to talk to the network
			SCPResultMessage receivedMsg;
			try {
				receivedMsg = new SCPResultMessage(receive(timeout));
			} catch (SocketTimeoutException e) {
				/*
				 * If we are timing out, but another thread received for us,
				 * that's OK too
				 */
				var qm = getMessageFromQueue();
				if (qm != null) {
					return qm;
				}
				throw e;
			}
			var targetThread =
					receiverMap.remove(receivedMsg.getSequenceNumber());
			// If message is ours or unrecognized, pass to caller
			if (targetThread == currentThread() || targetThread == null) {
				return receivedMsg;
			}
			// Queue for correct thread
			putMessageOnQueue(targetThread, receivedMsg);
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
		var prev = receiverMap.put(seq, currentThread());
		if (prev != null && prev != currentThread()) {
			log.warn("response for message now awaited by different thread");
		}
		send(requestData);
	}

	@Override
	public String toString() {
		return format("%s(%s <-%s-> %s)",
				getClass().getSimpleName().replaceAll("^.*\\.", ""),
				getChip(), isClosed() ? "|" : "", getRemoteAddress());
	}
}
