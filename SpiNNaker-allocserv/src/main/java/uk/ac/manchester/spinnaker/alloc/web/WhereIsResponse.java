/*
 * Copyright (c) 2021 The University of Manchester
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package uk.ac.manchester.spinnaker.alloc.web;

import java.net.URI;

import javax.ws.rs.core.UriInfo;

import uk.ac.manchester.spinnaker.alloc.allocator.BoardLocation;
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
