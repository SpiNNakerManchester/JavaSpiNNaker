package uk.ac.manchester.spinnaker.processes;

import java.io.IOException;

import uk.ac.manchester.spinnaker.connections.SCPConnection;
import uk.ac.manchester.spinnaker.connections.selectors.ConnectionSelector;
import uk.ac.manchester.spinnaker.messages.scp.SCPRequest;
import uk.ac.manchester.spinnaker.messages.scp.SCPResponse;

// TODO refactor this to have the functionality exposed higher up
/**
 * A simple wrapper round the basic underlying connection process system.
 *
 * @author Donal Fellows
 */
public class SendSingleSCPCommandProcess
		extends MultiConnectionProcess<SCPConnection> {
	/**
	 * @param connectionSelector
	 *            How to select which connection to use for communication.
	 */
	public SendSingleSCPCommandProcess(
			ConnectionSelector<SCPConnection> connectionSelector) {
		this(connectionSelector, DEFAULT_NUM_RETRIES, DEFAULT_TIMEOUT);
	}

	/**
	 * @param connectionSelector
	 *            How to select which connection to use for communication.
	 * @param numRetries
	 *            The number of retries to use.
	 * @param timeout
	 *            The timeout on the communications, in milliseconds.
	 */
	public SendSingleSCPCommandProcess(
			ConnectionSelector<SCPConnection> connectionSelector,
			int numRetries, int timeout) {
		super(connectionSelector, numRetries, timeout, DEFAULT_NUM_CHANNELS,
				DEFAULT_INTERMEDIATE_CHANNEL_WAITS);
	}

	/**
	 * Execute a call of a request and get a response from it.
	 *
	 * @param <T>
	 *            The type of the response.
	 * @param request
	 *            The request to make
	 * @return The response to the request
	 * @throws IOException
	 *             If communications fail.
	 * @throws Exception
	 *             If SCAMP on SpiNNaker reports a failure.
	 */
	public <T extends SCPResponse> T execute(SCPRequest<T> request)
			throws IOException, Exception {
		return synchronousCall(request);
	}
}
