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

import static java.util.Objects.isNull;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.stream.Collectors.toList;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.Closeable;
import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;

import org.slf4j.Logger;

import com.google.errorprone.annotations.MustBeClosed;
import com.google.errorprone.annotations.concurrent.GuardedBy;

import uk.ac.manchester.spinnaker.connections.model.MessageHandler;
import uk.ac.manchester.spinnaker.connections.model.MessageReceiver;

/**
 * Thread that listens to a connection and calls callbacks with new messages
 * when they arrive.
 *
 * @param <MessageType>
 *            The type of message being listened for by the connection.
 */
public class ConnectionListener<MessageType> extends Thread
		implements Closeable {
	/** What size of thread pool to use by default. */
	public static final int POOL_SIZE = 4;

	/**
	 * The default time (in ms) to wait in a system call for a message to
	 * arrive.
	 */
	public static final int TIMEOUT = 1000;

	private static final long POOL_TIMEOUT = 1000L;

	private static final Logger log = getLogger(ConnectionListener.class);

	private ThreadPoolExecutor callbackPool;

	/** The callbacks we make on receiving a message. */
	@GuardedBy("itself")
	private final Set<MessageHandler<MessageType>> callbacks;

	/**
	 * Contents of {@link #callbacks} at the last time
	 * {@link #checkpointCallbacks()} was called.
	 */
	@GuardedBy("callbacks")
	private List<MessageHandler<MessageType>> callbacksCheckpointed;

	private final MessageReceiver<MessageType> connection;

	private volatile boolean done;

	private int timeout;

	/**
	 * Create a connection listener with the default number of listening threads
	 * and the default OS-level timeout.
	 *
	 * @param connection
	 *            The connection to listen to.
	 */
	@MustBeClosed
	public ConnectionListener(MessageReceiver<MessageType> connection) {
		this(connection, POOL_SIZE, TIMEOUT);
	}

	/**
	 * Create a connection listener.
	 *
	 * @param connection
	 *            The connection to listen to.
	 * @param numProcesses
	 *            The maximum number of threads to use to do the listening.
	 * @param timeout
	 *            How long to wait in the OS for a message to arrive; if
	 *            0, wait indefinitely.
	 */
	@MustBeClosed
	public ConnectionListener(MessageReceiver<MessageType> connection,
			int numProcesses, int timeout) {
		super("Connection listener for connection " + connection);
		setDaemon(true);
		this.connection = connection;
		this.timeout = timeout;
		callbackPool = new ThreadPoolExecutor(1, numProcesses, POOL_TIMEOUT,
				MILLISECONDS, new LinkedBlockingQueue<>());
		done = false;
		callbacks = new HashSet<>();
	}

	/**
	 * Receive messages and dispatch them to the registered
	 * {@linkplain MessageHandler handlers}. Stops running when
	 * {@linkplain #close() closed}.
	 */
	@Override
	public final void run() {
		try {
			while (!done) {
				try {
					runStep();
				} catch (SocketTimeoutException e) {
					// Timed out at the base level
					continue;
				} catch (Exception e) {
					if (!done) {
						log.warn("problem when dispatching message", e);
					}
				}
			}
		} finally {
			callbackPool.shutdown();
			callbackPool = null;
		}
	}

	/**
	 * Receive a message and fire it off into the registered callbacks.
	 *
	 * @throws SocketTimeoutException
	 *             If the connection receive times out.
	 * @throws IOException
	 *             If other things go wrong with the comms, or if the callbacks
	 *             throw it.
	 */
	private void runStep() throws IOException {
		var message = connection.receiveMessage(timeout);
		for (var future : checkpointCallbacks().stream().map(
				callback -> callbackPool.submit(() -> callback.handle(message)))
				.collect(toList())) {
			try {
				future.get();
			} catch (InterruptedException e) {
				log.warn("unexpected exception; not waiting for the future", e);
				break;
			} catch (ExecutionException ee) {
				try {
					throw ee.getCause();
				} catch (IOException | RuntimeException | Error e) {
					throw e;
				} catch (Throwable e) {
					log.warn("unexpected exception", e);
				}
			}
		}
	}

	/** @return The current contents of {@link #callbacks}. */
	private List<MessageHandler<MessageType>> checkpointCallbacks() {
		synchronized (callbacks) {
			if (isNull(callbacksCheckpointed)) {
				callbacksCheckpointed = List.copyOf(callbacks);
			}
			return callbacksCheckpointed;
		}
	}

	/**
	 * Add a callback to be called when a message is received.
	 *
	 * @param callback
	 *            The callback to add.
	 */
	public void addCallback(MessageHandler<MessageType> callback) {
		synchronized (callbacks) {
			callbacksCheckpointed = null;
			callbacks.add(callback);
		}
	}

	/**
	 * Closes the listener. Note that this does not close the provider of the
	 * messages; this instead marks the listener as closed. The listener will
	 * not truly stop until the get message call returns, and this call will
	 * block until the callback thread pool has terminated.
	 */
	@Override
	public void close() {
		done = true;
		try {
			join();
		} catch (InterruptedException e) {
			log.error("interrupted while waiting for threads to finish", e);
		}
	}
}
