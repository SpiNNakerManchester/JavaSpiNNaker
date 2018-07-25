package uk.ac.manchester.spinnaker.processes;

import static uk.ac.manchester.spinnaker.messages.Constants.SCP_TIMEOUT;

import java.io.IOException;
import java.util.function.Consumer;

import uk.ac.manchester.spinnaker.connections.SCPConnection;
import uk.ac.manchester.spinnaker.connections.SCPErrorHandler;
import uk.ac.manchester.spinnaker.connections.SCPRequestPipeline;
import uk.ac.manchester.spinnaker.messages.scp.SCPRequest;
import uk.ac.manchester.spinnaker.messages.scp.SCPResponse;
import uk.ac.manchester.spinnaker.selectors.ConnectionSelector;

/** A process that uses a single connection in communication. */
public abstract class SingleConnectionProcess<T extends SCPConnection>
		extends Process {
	private final ConnectionSelector<T> connectionSelector;
	private SCPRequestPipeline requestPipeline;
	private final int timeout;

	protected SingleConnectionProcess(
			ConnectionSelector<T> connectionSelector) {
		this(connectionSelector, SCP_TIMEOUT);
	}

	protected SingleConnectionProcess(ConnectionSelector<T> connectionSelector,
			int timeout) {
		this.requestPipeline = null;
		this.timeout = timeout;
		this.connectionSelector = connectionSelector;
	}

	@Override
	protected final <R extends SCPResponse> void sendRequest(
			SCPRequest<R> request, Consumer<R> callback,
			SCPErrorHandler errorCallback) throws IOException {
		if (errorCallback == null) {
			errorCallback = this::receiveError;
		}
		/*
		 * If no pipe line built yet, build one on the connection selected for
		 * it
		 */
		if (requestPipeline == null) {
			requestPipeline = new SCPRequestPipeline(
					connectionSelector.getNextConnection(request), timeout);
		}
		requestPipeline.sendRequest(request, callback, errorCallback);
	}

	@Override
	protected final void finish() throws IOException {
		requestPipeline.finish();
	}
}
