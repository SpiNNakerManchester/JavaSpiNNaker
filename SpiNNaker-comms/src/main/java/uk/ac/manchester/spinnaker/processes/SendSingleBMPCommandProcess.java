package uk.ac.manchester.spinnaker.processes;

import static uk.ac.manchester.spinnaker.messages.Constants.BMP_TIMEOUT;

import java.io.IOException;

import uk.ac.manchester.spinnaker.connections.BMPConnection;
import uk.ac.manchester.spinnaker.messages.Constants;
import uk.ac.manchester.spinnaker.messages.bmp.BMPRequest;
import uk.ac.manchester.spinnaker.messages.bmp.BMPRequest.BMPResponse;
import uk.ac.manchester.spinnaker.selectors.ConnectionSelector;

// TODO refactor this to have the functionality exposed higher up
public class SendSingleBMPCommandProcess
		extends SingleConnectionProcess<BMPConnection> {
	public SendSingleBMPCommandProcess(
			ConnectionSelector<BMPConnection> connectionSelector) {
		this(connectionSelector, (int) (1000 * BMP_TIMEOUT));
	}

	public SendSingleBMPCommandProcess(
			ConnectionSelector<BMPConnection> connectionSelector,
			int timeout) {
		super(connectionSelector, timeout);
	}

	public <T extends BMPResponse> T execute(BMPRequest<T> r)
			throws IOException, Exception {
		return synchronousCall(r);
	}
}
