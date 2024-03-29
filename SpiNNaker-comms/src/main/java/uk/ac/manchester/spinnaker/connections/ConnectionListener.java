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
package uk.ac.manchester.spinnaker.connections;

import static java.util.Objects.isNull;
import static java.util.Objects.requireNonNull;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.stream.Collectors.toList;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.Closeable;
import java.io.EOFException;
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

/**
 * Thread that listens to a connection and calls callbacks with new messages
 * when they arrive.
 *
 * @param <MessageType>
 *            The type of message being listened for by the connection.
 */
public final class ConnectionListener<MessageType> extends Thread
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

	private final UDPConnection<MessageType> connection;

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
	public ConnectionListener(UDPConnection<MessageType> connection) {
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
	public ConnectionListener(UDPConnection<MessageType> connection,
			int numProcesses, int timeout) {
		super("Connection listener for connection " + connection);
		setDaemon(true);
		setUncaughtExceptionHandler(this::logExn);
		this.connection = connection;
		this.timeout = timeout;
		callbackPool = new ThreadPoolExecutor(1, numProcesses, POOL_TIMEOUT,
				MILLISECONDS, new LinkedBlockingQueue<>());
		done = false;
		callbacks = new HashSet<>();
	}

	private void logExn(Thread thread, Throwable ex) {
		log.warn("unexpected exception in {}", thread, ex);
	}

	/**
	 * Receive messages and dispatch them to the registered
	 * {@linkplain MessageHandler handlers}. Stops running when
	 * {@linkplain #close() closed}.
	 */
	@Override
	public void run() {
		try {
			while (!done) {
				try {
					runStep();
				} catch (SocketTimeoutException e) {
					// Timed out at the base level
					continue;
				} catch (EOFException e) {
					// Definitely at EOF; time to stop
					break;
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
	 * @throws InterruptedException
	 *             If communications are interrupted.
	 */
	private void runStep() throws IOException, InterruptedException {
		var message = connection.receiveMessage(timeout);
		for (var future : checkpointCallbacks().stream().map(
				callback -> callbackPool.submit(() -> callback.handle(message)))
				.collect(toList())) {
			try {
				future.get();
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
			callbacks.add(requireNonNull(callback));
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
