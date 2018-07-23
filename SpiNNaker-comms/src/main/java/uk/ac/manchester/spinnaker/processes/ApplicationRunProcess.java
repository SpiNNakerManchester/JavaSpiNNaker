package uk.ac.manchester.spinnaker.processes;

import java.io.IOException;

import uk.ac.manchester.spinnaker.connections.SCPConnection;
import uk.ac.manchester.spinnaker.machine.ChipLocation;
import uk.ac.manchester.spinnaker.machine.CoreSubsets;
import uk.ac.manchester.spinnaker.messages.scp.ApplicationRun;
import uk.ac.manchester.spinnaker.selectors.ConnectionSelector;

public class ApplicationRunProcess
		extends MultiConnectionProcess<SCPConnection> {

	public ApplicationRunProcess(
			ConnectionSelector<SCPConnection> connectionSelector) {
		super(connectionSelector, DEFAULT_NUM_RETRIES, DEFAULT_TIMEOUT,
				DEFAULT_NUM_CHANNELS, DEFAULT_INTERMEDIATE_CHANNEL_WAITS);
	}

	public ApplicationRunProcess(
			ConnectionSelector<SCPConnection> connectionSelector,
			int numRetries, int timeout, int numChannels,
			int intermediateChannelWaits) {
		super(connectionSelector, numRetries, timeout, numChannels,
				intermediateChannelWaits);
	}

	public void run(int app_id, CoreSubsets core_subsets, Object wait)
			throws Exception, IOException {
		for (ChipLocation chip : core_subsets.getChips()) {
			sendRequest(new ApplicationRun(app_id, chip,
					core_subsets.pByChip(chip)));
		}
		finish();
		checkForError();
	}
}
