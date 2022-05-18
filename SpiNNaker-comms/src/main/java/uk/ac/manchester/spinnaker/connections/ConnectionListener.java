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

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.Closeable;
import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;

import org.slf4j.Logger;

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

	private Set<MessageHandler<MessageType>> callbacks;

	private MessageReceiver<MessageType> connection;

	private volatile boolean done;

	private int timeout;

	/**
	 * Create a connection listener with the default number of listening threads
	 * and the default OS-level timeout.
	 *
	 * @param connection
	 *            The connection to listen to.
	 */
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
	public ConnectionListener(MessageReceiver<MessageType> connection,
			int numProcesses, int timeout) {
		super("Connection listener for connection " + connection);
		setDaemon(true);
		this.connection = connection;
		this.timeout = timeout;
		callbackPool = new ThreadPoolExecutor(1, numProcesses, POOL_TIMEOUT,
				MILLISECONDS, new LinkedBlockingQueue<>());
		done = false;
		callbacks = new HashSet<MessageHandler<MessageType>>();
	}

	@Override
	public final void run() {
		try {
			while (!done) {
				try {
					runStep();
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

	private void runStep() throws IOException {
		try {
			MessageType message = connection.receiveMessage(timeout);
			for (MessageHandler<MessageType> callback : callbacks) {
				callbackPool.submit(() -> callback.handle(message));
			}
		} catch (SocketTimeoutException e) {
			// Do Nothing; this is expected and can be skipped
		}
	}

	/**
	 * Add a callback to be called when a message is received.
	 *
	 * @param callback
	 *            The callback to add.
	 */
	public void addCallback(MessageHandler<MessageType> callback) {
		callbacks.add(callback);
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
