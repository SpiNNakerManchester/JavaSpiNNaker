package uk.ac.manchester.spinnaker.alloc.web;

import static java.util.Collections.unmodifiableList;

import java.net.URI;
import java.util.List;

import javax.ws.rs.core.UriInfo;

import uk.ac.manchester.spinnaker.alloc.SubMachine;
import uk.ac.manchester.spinnaker.spalloc.messages.BoardCoordinates;
import uk.ac.manchester.spinnaker.spalloc.messages.Connection;

public class SubMachineResponse {
	public int width;
	public int height;
	public String machineName;
	public List<Connection> connections;
	public List<BoardCoordinates> boards;
	public URI power;
	public URI machineRef;

	public SubMachineResponse(SubMachine m, UriInfo ui) {
		width = m.width;
		height = m.height;
		machineName = m.machine.name;
		connections = unmodifiableList(m.connections);
		boards = unmodifiableList(m.boards);
		power = ui.getAbsolutePathBuilder().path("power").build();
		machineRef = ui.getBaseUriBuilder().path("machine/{name}")
				.build(m.machine.name);
	}

}
