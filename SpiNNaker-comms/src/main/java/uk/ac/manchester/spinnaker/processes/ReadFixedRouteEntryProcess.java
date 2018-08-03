package uk.ac.manchester.spinnaker.processes;

import java.io.IOException;

import uk.ac.manchester.spinnaker.connections.SCPConnection;
import uk.ac.manchester.spinnaker.connections.selectors.ConnectionSelector;
import uk.ac.manchester.spinnaker.machine.HasChipLocation;
import uk.ac.manchester.spinnaker.machine.RoutingEntry;
import uk.ac.manchester.spinnaker.messages.scp.FixedRouteRead;

/** A process for reading a chip's fixed route routing entry. */
public class ReadFixedRouteEntryProcess
		extends MultiConnectionProcess<SCPConnection> {
	/**
	 * @param connectionSelector
	 *            How to select how to communicate.
	 */
	public ReadFixedRouteEntryProcess(
			ConnectionSelector<SCPConnection> connectionSelector) {
		super(connectionSelector);
	}

	/**
	 * Read the current fixed route from a chip.
	 *
	 * @param chip
	 *            The chip to read from
	 * @return The route.
	 * @throws IOException
	 *             If anything goes wrong with networking.
	 * @throws Exception
	 *             If SpiNNaker rejects the message.
	 */
	public RoutingEntry readFixedRoute(HasChipLocation chip)
			throws IOException, Exception {
		return readFixedRoute(chip, 0);
	}

	/**
	 * Read the current fixed route from a chip.
	 *
	 * @param chip
	 *            The chip to read from
	 * @param appID
	 *            The application ID associated with the route.
	 * @return The route.
	 * @throws IOException
	 *             If anything goes wrong with networking.
	 * @throws Exception
	 *             If SpiNNaker rejects the message.
	 */
	public RoutingEntry readFixedRoute(HasChipLocation chip, int appID)
			throws IOException, Exception {
		return synchronousCall(new FixedRouteRead(chip, appID)).getRoute();
	}
}
