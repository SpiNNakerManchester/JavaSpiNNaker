package uk.ac.manchester.spinnaker.processes;

import java.io.IOException;

import uk.ac.manchester.spinnaker.connections.SCPConnection;
import uk.ac.manchester.spinnaker.connections.selectors.ConnectionSelector;
import uk.ac.manchester.spinnaker.machine.ChipLocation;
import uk.ac.manchester.spinnaker.machine.CoreSubsets;
import uk.ac.manchester.spinnaker.messages.scp.ApplicationRun;

/** Launch an application. */
public class ApplicationRunProcess
		extends MultiConnectionProcess<SCPConnection> {
	/**
	 * Create.
	 *
	 * @param connectionSelector
	 *            How to choose where to send messages.
	 */
	public ApplicationRunProcess(
			ConnectionSelector<SCPConnection> connectionSelector) {
		super(connectionSelector, DEFAULT_NUM_RETRIES, DEFAULT_TIMEOUT,
				DEFAULT_NUM_CHANNELS, DEFAULT_INTERMEDIATE_CHANNEL_WAITS);
	}

	/**
	 * Create.
	 *
	 * @param connectionSelector
	 *            How to choose where to send messages.
	 * @param numRetries
	 *            The number of retries allowed.
	 * @param timeout
	 *            How long to wait for a reply.
	 * @param numChannels
	 *            The number of parallel channels to use.
	 * @param intermediateChannelWaits
	 *            ???
	 */
	public ApplicationRunProcess(
			ConnectionSelector<SCPConnection> connectionSelector,
			int numRetries, int timeout, int numChannels,
			int intermediateChannelWaits) {
		super(connectionSelector, numRetries, timeout, numChannels,
				intermediateChannelWaits);
	}

	/**
	 * Launch an application (already loaded).
	 *
	 * @param appID
	 *            The application ID to launch.
	 * @param coreSubsets
	 *            Which cores to launch.
	 * @param wait
	 *            Whether to wait for the application launch to fully complete
	 *            before returning.
	 * @throws IOException
	 *             If anything goes wrong with networking.
	 * @throws Exception
	 *             If SpiNNaker rejects a message.
	 */
	public void run(int appID, CoreSubsets coreSubsets, boolean wait)
			throws Exception, IOException {
		for (ChipLocation chip : coreSubsets.getChips()) {
			sendRequest(new ApplicationRun(appID, chip,
					coreSubsets.pByChip(chip), wait));
		}
		finish();
		checkForError();
	}
}
