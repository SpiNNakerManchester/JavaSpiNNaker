package uk.ac.manchester.spinnaker.transceiver.processes;

import static uk.ac.manchester.spinnaker.messages.Constants.SCP_TIMEOUT;

import java.io.IOException;
import java.util.function.Consumer;

import uk.ac.manchester.spinnaker.connections.SCPConnection;
import uk.ac.manchester.spinnaker.connections.SCPRequestPipeline;
import uk.ac.manchester.spinnaker.connections.selectors.ConnectionSelector;
import uk.ac.manchester.spinnaker.messages.scp.SCPRequest;
import uk.ac.manchester.spinnaker.messages.scp.SCPResponse;
import uk.ac.manchester.spinnaker.transceiver.RetryTracker;

/**
 * A process that uses a single connection in communication.
 *
 * @param <T>
 *            The type of connection used by this process.
 */
public abstract class SingleConnectionProcess<T extends SCPConnection>
		extends Process {
	private final ConnectionSelector<T> connectionSelector;
	private SCPRequestPipeline requestPipeline;
	private final int timeout;
	private final RetryTracker retryTracker;

	/**
	 * @param connectionSelector
	 *            How to select how to communicate.
	 * @param retryTracker
	 *            Object used to track how many retries were used in an
	 *            operation. May be {@code null} if no suck tracking is
	 *            required.
	 */
	protected SingleConnectionProcess(ConnectionSelector<T> connectionSelector,
			RetryTracker retryTracker) {
		this(connectionSelector, SCP_TIMEOUT, retryTracker);
	}

	/**
	 * @param connectionSelector
	 *            How to select how to communicate.
	 * @param timeout
	 *            How long to take sending the message, in milliseconds.
	 * @param retryTracker
	 *            Object used to track how many retries were used in an
	 *            operation. May be {@code null} if no suck tracking is
	 *            required.
	 */
	protected SingleConnectionProcess(ConnectionSelector<T> connectionSelector,
			int timeout, RetryTracker retryTracker) {
		this.requestPipeline = null;
		this.timeout = timeout;
		this.connectionSelector = connectionSelector;
		this.retryTracker = retryTracker;
	}

	@Override
	protected final <R extends SCPResponse> void sendRequest(
			SCPRequest<R> request, Consumer<R> callback) throws IOException {
		/*
		 * If no pipe line built yet, build one on the connection selected for
		 * it
		 */
		if (requestPipeline == null) {
			requestPipeline = new SCPRequestPipeline(
					connectionSelector.getNextConnection(request), timeout,
					retryTracker);
		}
		requestPipeline.sendRequest(request, callback, this::receiveError);
	}

	@Override
	protected final void finish() throws IOException {
		requestPipeline.finish();
	}
}
