package uk.ac.manchester.spinnaker.processes;

import static uk.ac.manchester.spinnaker.messages.Constants.SCP_TIMEOUT;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import uk.ac.manchester.spinnaker.connections.SCPConnection;
import uk.ac.manchester.spinnaker.connections.SCPErrorHandler;
import uk.ac.manchester.spinnaker.connections.SCPRequestPipeline;
import uk.ac.manchester.spinnaker.connections.selectors.ConnectionSelector;
import uk.ac.manchester.spinnaker.messages.scp.SCPRequest;
import uk.ac.manchester.spinnaker.messages.scp.SCPResponse;

/**
 * A process that uses multiple connections in communication.
 *
 * @param <T>
 *            The type of connection used by the process.
 */
public abstract class MultiConnectionProcess<T extends SCPConnection>
		extends Process {
	/** The default for the number of retries. */
	public static final int DEFAULT_NUM_RETRIES = 3;
	/** The default for the timeout (in ms). */
	public static final int DEFAULT_TIMEOUT = SCP_TIMEOUT;
	/** The default for the number of parallel channels. */
	public static final int DEFAULT_NUM_CHANNELS = 8;
	/** The default for the number of instantaneously active channels. */
	public static final int DEFAULT_INTERMEDIATE_CHANNEL_WAITS = 7;

	private final int numWaits;
	private final int numChannels;
	private final int numRetries;
	/**
	 * How to select how to communicate.
	 */
	final ConnectionSelector<T> selector;
	private final Map<T, SCPRequestPipeline> requestPipelines;
	private final int timeout;

	/**
	 * @param connectionSelector
	 *            How to select how to communicate.
	 */
	protected MultiConnectionProcess(ConnectionSelector<T> connectionSelector) {
		this(connectionSelector, DEFAULT_NUM_RETRIES, DEFAULT_TIMEOUT,
				DEFAULT_NUM_CHANNELS, DEFAULT_INTERMEDIATE_CHANNEL_WAITS);
	}

	/**
	 * @param connectionSelector
	 *            How to select how to communicate.
	 * @param numRetries
	 *            The number of times to retry a communication.
	 * @param timeout
	 *            The timeout (in ms) for the communication.
	 * @param numChannels
	 *            The number of parallel communications to support
	 * @param intermediateChannelWaits
	 *            How many parallel communications to launch at once. (??)
	 */
	protected MultiConnectionProcess(ConnectionSelector<T> connectionSelector,
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
	protected <R extends SCPResponse> void sendRequest(SCPRequest<R> request,
			Consumer<R> callback, SCPErrorHandler errorCallback)
			throws IOException {
		if (errorCallback == null) {
			errorCallback = this::receiveError;
		}
		T connection = selector.getNextConnection(request);
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
