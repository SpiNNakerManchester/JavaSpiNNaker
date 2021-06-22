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
import static uk.ac.manchester.spinnaker.alloc.web.WebServiceComponentNames.MACH_BOARD_BY_ADDRESS;
import static uk.ac.manchester.spinnaker.alloc.web.WebServiceComponentNames.MACH_BOARD_BY_CHIP;
import static uk.ac.manchester.spinnaker.alloc.web.WebServiceComponentNames.MACH_BOARD_BY_LOGICAL;
import static uk.ac.manchester.spinnaker.alloc.web.WebServiceComponentNames.MACH_BOARD_BY_PHYSICAL;

import java.net.URI;
import java.sql.SQLException;
import java.util.List;

import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;

import com.fasterxml.jackson.annotation.JsonInclude;

import uk.ac.manchester.spinnaker.alloc.allocator.SpallocAPI;
import uk.ac.manchester.spinnaker.alloc.allocator.SpallocAPI.BoardCoords;

/**
 * The description of an individual machine.
 *
 * @author Donal Fellows
 */
public final class MachineResponse {
	/** The name of the machine. */
	public String name;

	/** The tags of the machine. */
	public List<String> tags;

	/** The width of the machine, in triads. */
	public int width;

	/** The height of the machine, in triads. */
	public int height;

	/** The boards of the machine marked as down. */
	public List<BoardCoords> downBoards;

	/** The links of the machine marked as down. */
	public List<SpallocAPI.DownLink> downLinks;

	/** Where to look up a board by physical coordinates. */
	@JsonInclude(NON_NULL)
	public URI lookupByPhysicalBoard;

	/** Where to look up a board by logical coordinates. */
	@JsonInclude(NON_NULL)
	public URI lookupByLogicalBoard;

	/** Where to look up a board by global chip address. */
	@JsonInclude(NON_NULL)
	public URI lookupByChip;

	/** Where to look up a board by its ethernet IP address. */
	@JsonInclude(NON_NULL)
	public URI lookupByAddress;

	public MachineResponse() {
		// TODO remove this
	}

	public MachineResponse(SpallocAPI.Machine machine, UriInfo ui)
			throws SQLException {
		name = machine.getName();
		tags = machine.getTags();
		width = machine.getWidth();
		height = machine.getHeight();
		downBoards = machine.getDeadBoards();
		downLinks = machine.getDownLinks();

		UriBuilder b = ui.getAbsolutePathBuilder().path("{resource}");
		lookupByPhysicalBoard = b.build(MACH_BOARD_BY_PHYSICAL);
		lookupByLogicalBoard = b.build(MACH_BOARD_BY_LOGICAL);
		lookupByChip = b.build(MACH_BOARD_BY_CHIP);
		lookupByAddress = b.build(MACH_BOARD_BY_ADDRESS);
	}
}
