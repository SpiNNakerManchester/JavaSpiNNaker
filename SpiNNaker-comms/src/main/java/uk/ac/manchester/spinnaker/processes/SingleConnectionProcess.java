package uk.ac.manchester.spinnaker.processes;

import java.io.IOException;
import java.util.function.Consumer;

import uk.ac.manchester.spinnaker.connections.SCPErrorHandler;
import uk.ac.manchester.spinnaker.connections.SCPRequestPipeline;
import uk.ac.manchester.spinnaker.messages.scp.SCPRequest;
import uk.ac.manchester.spinnaker.messages.scp.SCPResponse;

/** A process that uses a single connection in communication. */
public abstract class SingleConnectionProcess extends Process {
	private final ConnectionSelector connectionSelector;
	private SCPRequestPipeline requestPipeline;

	protected SingleConnectionProcess(ConnectionSelector connectionSelector) {
		this.requestPipeline = null;
		this.connectionSelector = connectionSelector;
	}

	@Override
	protected final <T extends SCPResponse> void sendRequest(
			SCPRequest<T> request, Consumer<T> callback,
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
					connectionSelector.getNextConnection(request));
		}
		requestPipeline.sendRequest(request, callback, errorCallback);
	}

	@Override
	protected final void finish() throws IOException {
		requestPipeline.finish();
	}
}
