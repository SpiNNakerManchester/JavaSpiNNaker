package uk.ac.manchester.spinnaker.alloc.web;

import java.net.URI;
import java.util.List;

import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;

import uk.ac.manchester.spinnaker.alloc.Machine;

public class MachineResponse {
	public String name;
	public List<String> tags;
	public int width;
	public int height;
	// TODO: dead boards, dead links
	public URI physicalBoard;
	public URI logicalBoard;
	public URI chip;

	public MachineResponse() {
	}

	public MachineResponse(Machine machine, UriInfo ui) {
		name = machine.name;
		tags = machine.tags;
		width = machine.width;
		height = machine.height;
		// FIXME pull info out of machine
		UriBuilder b = ui.getAbsolutePathBuilder().path("{element}");
		physicalBoard = b.build("physical-board");
		logicalBoard = b.build("logical-board");
		chip = b.build("chip");
	}
}
