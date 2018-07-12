package uk.ac.manchester.spinnaker.messages.scp;

import static uk.ac.manchester.spinnaker.messages.scp.SCPCommand.CMD_RTR;
import static uk.ac.manchester.spinnaker.messages.sdp.SDPFlag.REPLY_EXPECTED;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import uk.ac.manchester.spinnaker.machine.HasChipLocation;
import uk.ac.manchester.spinnaker.messages.model.UnexpectedResponseCodeException;
import uk.ac.manchester.spinnaker.messages.sdp.SDPHeader;

/** Gets a fixed route entry */
public final class FixedRouteRead extends SCPRequest<FixedRouteRead.Response> {
	private static int argument1(int appID) {
		return (appID << 8) | 3;
	}

	private static int argument2() {
		return 1 << 31;
	}

	/**
	 * @param chip
	 *            The chip to get the route from.
	 * @param appID
	 *            The ID of the application associated with the route, between 0
	 *            and 255
	 */
	public FixedRouteRead(HasChipLocation chip, int entry, int appID) {
		super(new SDPHeader(REPLY_EXPECTED, chip.getScampCore(), 0),
				new SCPRequestHeader(CMD_RTR), argument1(appID), argument2(),
				null);
	}

	@Override
	public Response getSCPResponse(ByteBuffer buffer) throws Exception {
		return new Response(buffer);
	}

	/** response for the fixed route read */
	public static class Response extends CheckOKResponse {
		private final int route;

		Response(ByteBuffer buffer) throws UnexpectedResponseCodeException {
			super("Read Fixed Route route", CMD_RTR, buffer);
			route = buffer.getInt();
		}

		/** @return the fixed route router route */
		public Object getRoute() {
			List<Integer> processorIDs = new ArrayList<Integer>();
			for (int i = 0; i < 25; i++) {
				if ((route & (1 << (6 + i))) != 0) {
					processorIDs.add(i);
				}
			}
			List<Integer> linkIDs = new ArrayList<Integer>();
			for (int i = 0; i < 6; i++) {
				if ((route & (1 << i)) != 0) {
					linkIDs.add(i);
				}
			}
			// return new FixedRouteEntry(processorIDs, linkIDs);// FIXME
			return route;
		}
	}
}
