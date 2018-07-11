package uk.ac.manchester.spinnaker.connections;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;

import org.slf4j.Logger;

import uk.ac.manchester.spinnaker.connections.model.Listenable;
import uk.ac.manchester.spinnaker.connections.model.MessageHandler;
import uk.ac.manchester.spinnaker.connections.model.Listenable.MessageReceiver;

/**
 * Thread that listens to a connection and calls callbacks with new messages
 * when they arrive.
 */
public class ConnectionListener<MessageType> extends Thread {
	private Logger log = getLogger(ConnectionListener.class);
	private ThreadPoolExecutor callbackPool;
	private Set<MessageHandler<MessageType>> callbacks;
	private Listenable<MessageType> connection;
	private volatile boolean done;
	private Integer timeout;

	public ConnectionListener(Listenable<MessageType> connection,
			int numProcesses, Integer timeout) {
		super("Connection listener for connection " + connection);
		setDaemon(true);
		this.connection = connection;
		this.timeout = timeout;
		callbackPool = new ThreadPoolExecutor(1, numProcesses, 1000L,
				MILLISECONDS, new LinkedBlockingQueue<>());
		done = false;
		callbacks = new HashSet<MessageHandler<MessageType>>();
	}

	@Override
	public final void run() {
		try {
			MessageReceiver<MessageType> handler = connection.getReceiver();
			while (!done) {
				try {
					runStep(handler);
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

	private void runStep(MessageReceiver<MessageType> handler)
			throws IOException {
		if (connection.isReadyToReceive(timeout)) {
			final MessageType message = handler.receive();
			for (final MessageHandler<MessageType> callback : callbacks) {
				callbackPool.submit(() -> callback.handle(message));
			}
		}
	}

	/** Add a callback to be called when a message is received. */
	public void addCallback(MessageHandler<MessageType> callback) {
		callbacks.add(callback);
	}

	/**
	 * Closes the listener. Note that this does not close the provider of the
	 * messages; this instead marks the listener as closed. The listener will
	 * not truly stop until the get message call returns.
	 *
	 * @throws InterruptedException
	 *             If interrupted while waiting for the thread to terminate
	 */
	public void close() {
		done = true;
		try {
			join();
		} catch (InterruptedException e) {
			log.error("interrupted while waiting for threads to finish", e);
		}
	}
}
