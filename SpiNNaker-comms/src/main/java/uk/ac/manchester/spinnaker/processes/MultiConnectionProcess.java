package uk.ac.manchester.spinnaker.processes;

import static uk.ac.manchester.spinnaker.messages.Constants.SCP_TIMEOUT;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import uk.ac.manchester.spinnaker.connections.SCPConnection;
import uk.ac.manchester.spinnaker.connections.SCPErrorHandler;
import uk.ac.manchester.spinnaker.connections.SCPRequestPipeline;
import uk.ac.manchester.spinnaker.messages.scp.SCPRequest;
import uk.ac.manchester.spinnaker.messages.scp.SCPResponse;

/** A process that uses multiple connections in communication. */
public abstract class MultiConnectionProcess extends Process {
	public static final int DEFAULT_NUM_RETRIES = 3;
	public static final int DEFAULT_TIMEOUT = SCP_TIMEOUT;
	public static final int DEFAULT_NUM_CHANNELS = 8;
	public static final int DEFAULT_INTERMEDIATE_CHANNEL_WAITS = 7;

	private final int numWaits;
	private final int numChannels;
	private final int numRetries;
	private final ConnectionSelector selector;
	private final Map<SCPConnection, SCPRequestPipeline> requestPipelines;
	private final int timeout;

	protected MultiConnectionProcess(ConnectionSelector connectionSelector,
			int numRetries, int timeout, int numChannels,
			int intermediateChannelWaits) {
		this.requestPipelines = new HashMap<>();
		this.numRetries = numRetries;
		this.timeout = timeout;
		this.numChannels = numChannels;
		this.numWaits = intermediateChannelWaits;
		this.selector = connectionSelector;
	}

	@Override
	protected <T extends SCPResponse> void sendRequest(SCPRequest<T> request,
			Consumer<T> callback, SCPErrorHandler errorCallback)
			throws IOException {
		if (errorCallback == null) {
			errorCallback = this::receiveError;
		}
		SCPConnection connection = selector.getNextConnection(request);
		if (!requestPipelines.containsKey(connection)) {
			SCPRequestPipeline pipeline = new SCPRequestPipeline(connection,
					numChannels, numWaits, numRetries, timeout);
			requestPipelines.put(connection, pipeline);
		}
		requestPipelines.get(connection).sendRequest(request, callback,
				errorCallback);
	}

	@Override
	protected void finish() throws IOException {
		for (SCPRequestPipeline pipe : requestPipelines.values()) {
			pipe.finish();
		}
	}
}
