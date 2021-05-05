package uk.ac.manchester.spinnaker.alloc.web;

import java.net.URI;

import javax.ws.rs.core.UriInfo;

import uk.ac.manchester.spinnaker.alloc.BoardLocation;
import uk.ac.manchester.spinnaker.machine.ChipLocation;
import uk.ac.manchester.spinnaker.spalloc.messages.BoardCoordinates;
import uk.ac.manchester.spinnaker.spalloc.messages.BoardPhysicalCoordinates;

public class WhereIsResponse {
	public Integer jobId;
	public URI jobRef;
	public ChipLocation jobChip;
	public ChipLocation chip;
	public BoardCoordinates logicalBoardCoordinates;
	public String machine;
	public URI machineRef;
	public ChipLocation boardChip;
	public BoardPhysicalCoordinates physicalBoardCoordinates;

	public WhereIsResponse(BoardLocation location, UriInfo ui) {
		machine = location.machine;
		chip = location.chip;
		boardChip = location.getBoardChip();
		logicalBoardCoordinates = location.logical;
		physicalBoardCoordinates = location.physical;
		if (location.job != null) {
			jobId = location.job.getId();
			jobRef = ui.getBaseUriBuilder().path("jobs/{id}").build(jobId);
			jobChip = location.getChipRelativeTo(location.job.getRootChip());
		}
		// TODO Auto-generated constructor stub
	}

}
