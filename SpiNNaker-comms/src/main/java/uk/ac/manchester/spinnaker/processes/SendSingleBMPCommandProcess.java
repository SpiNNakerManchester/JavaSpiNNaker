package uk.ac.manchester.spinnaker.processes;

import java.io.IOException;

import uk.ac.manchester.spinnaker.connections.BMPConnection;
import uk.ac.manchester.spinnaker.messages.bmp.BMPRequest;
import uk.ac.manchester.spinnaker.messages.bmp.BMPRequest.BMPResponse;
import uk.ac.manchester.spinnaker.selectors.ConnectionSelector;

// TODO refactor this to have the functionality exposed higher up
public class SendSingleBMPCommandProcess
		extends MultiConnectionProcess<BMPConnection> {
	public SendSingleBMPCommandProcess(
			ConnectionSelector<BMPConnection> connectionSelector) {
		this(connectionSelector, DEFAULT_NUM_RETRIES, DEFAULT_TIMEOUT);
	}

	public SendSingleBMPCommandProcess(
			ConnectionSelector<BMPConnection> connectionSelector,
			int numRetries, int timeout) {
		super(connectionSelector, numRetries, timeout, DEFAULT_NUM_CHANNELS,
				DEFAULT_INTERMEDIATE_CHANNEL_WAITS);
	}

	public <T extends BMPResponse> T execute(BMPRequest<T> r)
			throws IOException, Exception {
		return synchronousCall(r);
	}
}
