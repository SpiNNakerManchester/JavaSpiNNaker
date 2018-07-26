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
	public ReadFixedRouteEntryProcess(
			ConnectionSelector<SCPConnection> connectionSelector) {
		super(connectionSelector);
	}

	public RoutingEntry readFixedRoute(HasChipLocation chip)
			throws IOException, Exception {
		return readFixedRoute(chip, 0);
	}

	public RoutingEntry readFixedRoute(HasChipLocation chip, int appID)
			throws IOException, Exception {
		return synchronousCall(new FixedRouteRead(chip, appID)).getRoute();
	}
}
