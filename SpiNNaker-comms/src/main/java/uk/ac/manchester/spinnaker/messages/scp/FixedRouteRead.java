package uk.ac.manchester.spinnaker.messages.scp;

import static uk.ac.manchester.spinnaker.messages.scp.Bits.BYTE0;
import static uk.ac.manchester.spinnaker.messages.scp.Bits.BYTE1;
import static uk.ac.manchester.spinnaker.messages.scp.Bits.TOP_BIT;
import static uk.ac.manchester.spinnaker.messages.scp.SCPCommand.CMD_RTR;

import java.nio.ByteBuffer;

import uk.ac.manchester.spinnaker.machine.HasChipLocation;
import uk.ac.manchester.spinnaker.machine.RoutingEntry;
import uk.ac.manchester.spinnaker.messages.model.UnexpectedResponseCodeException;

/** Gets a fixed route entry. */
public final class FixedRouteRead extends SCPRequest<FixedRouteRead.Response> {
	private static final int MAGIC = 3;

	private static int argument1(int appID) {
		return (appID << BYTE1) | (MAGIC << BYTE0);
	}

	private static int argument2() {
		return 1 << TOP_BIT;
	}

	/**
	 * @param chip
	 *            The chip to get the route from.
	 * @param appID
	 *            The ID of the application associated with the route, between 0
	 *            and 255
	 */
	public FixedRouteRead(HasChipLocation chip, int appID) {
		super(chip.getScampCore(), CMD_RTR, argument1(appID), argument2(),
				null);
	}

	@Override
	public Response getSCPResponse(ByteBuffer buffer) throws Exception {
		return new Response(buffer);
	}

	/** Response for the fixed route read. */
	public static final class Response extends CheckOKResponse {
		private final int route;

		private Response(ByteBuffer buffer)
				throws UnexpectedResponseCodeException {
			super("Read Fixed RoutingEntry route", CMD_RTR, buffer);
			route = buffer.getInt();
		}

		/** @return the fixed route router route. */
		public RoutingEntry getRoute() {
			return new RoutingEntry(route);
		}
	}
}
