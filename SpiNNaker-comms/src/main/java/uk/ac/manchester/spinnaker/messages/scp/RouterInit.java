package uk.ac.manchester.spinnaker.messages.scp;

import static uk.ac.manchester.spinnaker.messages.scp.SCPCommand.CMD_RTR;
import static uk.ac.manchester.spinnaker.messages.sdp.SDPFlag.REPLY_EXPECTED;

import java.nio.ByteBuffer;

import uk.ac.manchester.spinnaker.machine.CoreLocation;
import uk.ac.manchester.spinnaker.machine.HasChipLocation;
import uk.ac.manchester.spinnaker.messages.sdp.SDPHeader;

/** A request to initialise the router on a chip */
public class RouterInit extends SCPRequest<CheckOKResponse> {
	/**
	 * @param chip
	 *            The coordinates of the chip to clear the router of
	 * @param numEntries
	 *            The number of entries in the table (more than 0)
	 * @param tableAddress
	 *            The allocated table address
	 * @param baseAddress
	 *            The base address containing the entries
	 * @param appID
	 *            The ID of the application with which to associate the routes.
	 */
	public RouterInit(HasChipLocation chip, int numEntries, int tableAddress,
			int baseAddress, int appID) {
		super(new SDPHeader(REPLY_EXPECTED,
				new CoreLocation(chip.getX(), chip.getY(), 0), 0),
				new SCPRequestHeader(CMD_RTR), argument1(numEntries, appID),
				tableAddress, baseAddress);
		if (numEntries < 1) {
			throw new IllegalArgumentException(
					"numEntries must be more than 0");
		}
		if (baseAddress < 0) {
			throw new IllegalArgumentException(
					"baseAddress must not be negative");
		}
		if (tableAddress < 0) {
			throw new IllegalArgumentException(
					"tableAddress must not be negative");
		}
	}

	private static int argument1(int numEntries, int appID) {
		return (numEntries << 16) | (appID << 8) | 2;
	}

	@Override
	public CheckOKResponse getSCPResponse(ByteBuffer buffer) throws Exception {
		return new CheckOKResponse("Router Init", CMD_RTR, buffer);
	}
}
