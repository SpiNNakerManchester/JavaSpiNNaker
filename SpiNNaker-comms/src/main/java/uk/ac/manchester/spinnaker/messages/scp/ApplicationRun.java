package uk.ac.manchester.spinnaker.messages.scp;

import static uk.ac.manchester.spinnaker.machine.MachineDefaults.MAX_NUM_CORES;
import static uk.ac.manchester.spinnaker.messages.scp.Bits.BYTE3;
import static uk.ac.manchester.spinnaker.messages.scp.SCPCommand.CMD_AR;

import java.nio.ByteBuffer;

import uk.ac.manchester.spinnaker.machine.HasChipLocation;

/**
 * A request to run an application.
 */
public class ApplicationRun extends SCPRequest<CheckOKResponse> {
	private static final int WAIT_BIT = 18;

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
		super(chip.getScampCore(), CMD_AR, argument1(appId, processors, wait),
				null, null);
	}

	private static int argument1(int appId, Iterable<Integer> processors,
			boolean wait) {
		int processorMask = 0;
		if (processors != null) {
			for (int p : processors) {
				if (p >= 1 && p < MAX_NUM_CORES) {
					processorMask |= 1 << p;
				}
			}
		}
		processorMask |= appId << BYTE3;
		if (wait) {
			processorMask |= 1 << WAIT_BIT;
		}
		return processorMask;
	}

	@Override
	public CheckOKResponse getSCPResponse(ByteBuffer buffer) throws Exception {
		return new CheckOKResponse("Run Application", CMD_AR, buffer);
	}
}
