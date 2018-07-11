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
		this.setDaemon(true);
		this.connection = connection;
		this.timeout = timeout;
		this.callbackPool = new ThreadPoolExecutor(1, numProcesses, 1000L,
				MILLISECONDS, new LinkedBlockingQueue<>());
		this.done = false;
		this.callbacks = new HashSet<MessageHandler<MessageType>>();
	}

	@Override
	public final void run() {
		try {
			MessageReceiver<MessageType> handler = connection.getReceiver();
			while (!this.done) {
				try {
					this.runStep(handler);
				} catch (Exception e) {
					if (!this.done) {
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
}
