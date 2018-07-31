package uk.ac.manchester.spinnaker.messages.scp;

import static uk.ac.manchester.spinnaker.messages.scp.SCPCommand.CMD_AR;
import static uk.ac.manchester.spinnaker.messages.sdp.SDPHeader.Flag.REPLY_EXPECTED;

import java.nio.ByteBuffer;

import uk.ac.manchester.spinnaker.machine.HasChipLocation;
import uk.ac.manchester.spinnaker.messages.sdp.SDPHeader;

public class ApplicationRun extends SCPRequest<CheckOKResponse> {
	/**
	 * @param appId
	 *            The ID of the application to run, between 16 and 255
	 * @param chip
	 *            The coordinates of the chip to run on
	 * @param processors
	 *            The processors of the chip to run on, between 1 and 17
	 */
	public ApplicationRun(int appId, HasChipLocation chip,
			Iterable<Integer> processors) {
		this(appId, chip, processors, false);
	}

	/**
	 * @param appId
	 *            The ID of the application to run, between 16 and 255
	 * @param chip
	 *            The coordinates of the chip to run on
	 * @param processors
	 *            The processors of the chip to run on, between 1 and 17
	 * @param wait
	 *            True if the processors should enter a "wait" state on starting
	 */
	public ApplicationRun(int appId, HasChipLocation chip,
			Iterable<Integer> processors, boolean wait) {
		super(new SDPHeader(REPLY_EXPECTED, chip.getScampCore(), 0), CMD_AR,
				argument1(appId, processors, wait), null, null);
	}

	private static int argument1(int appId, Iterable<Integer> processors,
			boolean wait) {
		int processorMask = 0;
		if (processors != null) {
			for (int p : processors) {
				if (p >= 1 && p <= 17) {
					processorMask |= 1 << p;
				}
			}
		}
		processorMask |= appId << 24;
		if (wait) {
			processorMask |= 1 << 18;
		}
		return processorMask;
	}

	@Override
	public CheckOKResponse getSCPResponse(ByteBuffer buffer) throws Exception {
		return new CheckOKResponse("Run Application", CMD_AR, buffer);
	}
}
