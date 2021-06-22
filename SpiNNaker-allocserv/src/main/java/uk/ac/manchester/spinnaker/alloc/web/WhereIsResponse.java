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

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;
import static uk.ac.manchester.spinnaker.alloc.web.WebServiceComponentNames.JOB;
import static uk.ac.manchester.spinnaker.alloc.web.WebServiceComponentNames.MACH;

import java.net.URI;
import java.sql.SQLException;

import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;

import com.fasterxml.jackson.annotation.JsonInclude;

import uk.ac.manchester.spinnaker.alloc.allocator.SpallocAPI.BoardLocation;
import uk.ac.manchester.spinnaker.alloc.allocator.SpallocAPI.Job;
import uk.ac.manchester.spinnaker.machine.ChipLocation;
import uk.ac.manchester.spinnaker.spalloc.messages.BoardCoordinates;
import uk.ac.manchester.spinnaker.spalloc.messages.BoardPhysicalCoordinates;

/**
 * Describes the result of a where-is style request.
 *
 * @author Donal Fellows
 */
@JsonInclude(NON_NULL)
public class WhereIsResponse {
	/** The ID of the job using the board, or {@code null}/absent if none. */
	public final Integer jobId;

	/** The URI to the job using the board, or {@code null}/absent if none. */
	public final URI jobRef;

	/**
	 * The location of the chip in the job using the board, or
	 * {@code null}/absent if none.
	 */
	public final ChipLocation jobChip;

	/**
	 * The location of the chip.
	 */
	public final ChipLocation chip;

	/** The logical (triad) coordinates of the board. */
	public final BoardCoordinates logicalBoardCoordinates;

	/** The name of the machine. */
	public final String machine;

	/** The URI to the machine. */
	public final URI machineRef;

	/** The chip location of the board's root. */
	public final ChipLocation boardChip;

	/** The physical (BMP+board) coordinates of the board. */
	public final BoardPhysicalCoordinates physicalBoardCoordinates;

	WhereIsResponse(BoardLocation location, UriInfo ui) throws SQLException {
		UriBuilder minter = ui.getBaseUriBuilder().path("{major}/{minor}");
		machine = location.getMachine();
		machineRef = minter.build(MACH, machine);
		chip = location.getChip();
		boardChip = location.getBoardChip();
		logicalBoardCoordinates = location.getLogical();
		physicalBoardCoordinates = location.getPhysical();
		Job j = location.getJob();
		if (j != null) {
			jobId = j.getId();
			jobRef = minter.build(JOB, jobId);
			jobChip = j.getRootChip().map(rc -> location.getChipRelativeTo(rc))
					.orElse(null);
		} else {
			jobId = null;
			jobRef = null;
			jobChip = null;
		}
	}
}
