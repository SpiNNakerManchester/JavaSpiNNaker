package uk.ac.manchester.spinnaker.messages.scp;

import static uk.ac.manchester.spinnaker.messages.scp.Bits.BYTE1;
import static uk.ac.manchester.spinnaker.messages.scp.SCPCommand.CMD_RTR;

import java.nio.ByteBuffer;

import uk.ac.manchester.spinnaker.machine.HasChipLocation;

/** Sets a fixed route entry. */
public final class FixedRouteInitialise extends SCPRequest<CheckOKResponse> {
	private static int argument1(int appID) {
		return appID << BYTE1;
	}

	/**
	 * @param chip
	 *            The chip to set the route on.
	 * @param entry
	 *            the fixed route entry (converted for writing)
	 * @param appID
	 *            The ID of the application, between 0 and 255
	 */
	public FixedRouteInitialise(HasChipLocation chip, int entry, int appID) {
		super(chip.getScampCore(), CMD_RTR, argument1(appID), entry, null);
	}

	@Override
	public CheckOKResponse getSCPResponse(ByteBuffer buffer) throws Exception {
		return new CheckOKResponse("Fixed Route Initialise", CMD_RTR, buffer);
	}
}
