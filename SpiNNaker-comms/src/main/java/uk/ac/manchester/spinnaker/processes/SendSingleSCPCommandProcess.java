package uk.ac.manchester.spinnaker.processes;

import java.io.IOException;

import uk.ac.manchester.spinnaker.connections.SCPConnection;
import uk.ac.manchester.spinnaker.messages.scp.SCPRequest;
import uk.ac.manchester.spinnaker.messages.scp.SCPResponse;
import uk.ac.manchester.spinnaker.selectors.ConnectionSelector;

// TODO refactor this to have the functionality exposed higher up
public class SendSingleSCPCommandProcess
		extends MultiConnectionProcess<SCPConnection> {
	public SendSingleSCPCommandProcess(
			ConnectionSelector<SCPConnection> connectionSelector) {
		this(connectionSelector, DEFAULT_NUM_RETRIES, DEFAULT_TIMEOUT);
	}

	public SendSingleSCPCommandProcess(
			ConnectionSelector<SCPConnection> connectionSelector,
			int numRetries, int timeout) {
		super(connectionSelector, numRetries, timeout, DEFAULT_NUM_CHANNELS,
				DEFAULT_INTERMEDIATE_CHANNEL_WAITS);
	}

	public <T extends SCPResponse> T execute(SCPRequest<T> r)
			throws IOException, Exception {
		return synchronousCall(r);
	}
}
