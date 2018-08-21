package uk.ac.manchester.spinnaker.processes;

import java.io.IOException;

import uk.ac.manchester.spinnaker.connections.SCPConnection;
import uk.ac.manchester.spinnaker.connections.selectors.ConnectionSelector;
import uk.ac.manchester.spinnaker.machine.HasChipLocation;
import uk.ac.manchester.spinnaker.machine.RoutingEntry;
import uk.ac.manchester.spinnaker.messages.scp.FixedRouteInitialise;

/** Load a fixed route routing entry onto a chip. */
public class LoadFixedRouteEntryProcess
		extends MultiConnectionProcess<SCPConnection> {
	/**
	 * @param connectionSelector
	 *            How to select how to communicate.
	 */
	public LoadFixedRouteEntryProcess(
			ConnectionSelector<SCPConnection> connectionSelector) {
		super(connectionSelector);
	}

	/**
	 * Load a fixed route routing entry onto a chip with a default application
	 * ID.
	 *
	 * @param chip
	 *            The coordinates of the chip.
	 * @param fixedRoute
	 *            the fixed route entry
	 * @throws IOException
	 *             If anything goes wrong with networking.
	 * @throws Exception
	 *             If SpiNNaker rejects the message.
	 */
	public void loadFixedRoute(HasChipLocation chip, RoutingEntry fixedRoute)
			throws IOException, Exception {
		loadFixedRoute(chip, fixedRoute, 0);
	}

	/**
	 * Load a fixed route routing entry onto a chip.
	 *
	 * @param chip
	 *            The coordinates of the chip.
	 * @param fixedRoute
	 *            the fixed route entry
	 * @param appID
	 *            The ID of the application with which to associate the routes.
	 * @throws IOException
	 *             If anything goes wrong with networking.
	 * @throws Exception
	 *             If SpiNNaker rejects the message.
	 */
	public void loadFixedRoute(HasChipLocation chip, RoutingEntry fixedRoute,
			int appID) throws IOException, Exception {
		int entry = fixedRoute.encode();
		synchronousCall(new FixedRouteInitialise(chip, entry, appID));
	}
}
