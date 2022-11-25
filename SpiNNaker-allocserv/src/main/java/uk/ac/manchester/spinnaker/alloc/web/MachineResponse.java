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
import static java.util.Objects.nonNull;
import static uk.ac.manchester.spinnaker.alloc.web.WebServiceComponentNames.MACH_BOARD_BY_ADDRESS;
import static uk.ac.manchester.spinnaker.alloc.web.WebServiceComponentNames.MACH_BOARD_BY_CHIP;
import static uk.ac.manchester.spinnaker.alloc.web.WebServiceComponentNames.MACH_BOARD_BY_LOGICAL;
import static uk.ac.manchester.spinnaker.alloc.web.WebServiceComponentNames.MACH_BOARD_BY_PHYSICAL;

import java.net.URI;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

import jakarta.ws.rs.core.UriInfo;
import uk.ac.manchester.spinnaker.alloc.allocator.SpallocAPI;
import uk.ac.manchester.spinnaker.alloc.model.BoardCoords;
import uk.ac.manchester.spinnaker.alloc.model.DownLink;

/**
 * The description of an individual machine.
 *
 * @author Donal Fellows
 */
public final class MachineResponse {
	/** The name of the machine. */
	public final String name;

	/** The tags of the machine. */
	public final List<String> tags;

	/** The width of the machine, in triads. */
	public final int width;

	/** The height of the machine, in triads. */
	public final int height;

	/** The boards of the machine marked as down. */
	public final List<BoardCoords> downBoards;

	/** The links of the machine marked as down. */
	public final List<DownLink> downLinks;

	/** Where to look up a board by physical coordinates. */
	@JsonInclude(NON_NULL)
	public final URI lookupByPhysicalBoard;

	/** Where to look up a board by logical coordinates. */
	@JsonInclude(NON_NULL)
	public final URI lookupByLogicalBoard;

	/** Where to look up a board by global chip address. */
	@JsonInclude(NON_NULL)
	public final URI lookupByChip;

	/** Where to look up a board by its ethernet IP address. */
	@JsonInclude(NON_NULL)
	public final URI lookupByAddress;

	/**
	 * Create a machine response.
	 *
	 * @param machine
	 *            The machine being described.
	 * @param ui
	 *            How to manufacture URIs. May be {@code null}.
	 */
	public MachineResponse(SpallocAPI.Machine machine, UriInfo ui) {
		name = machine.getName();
		tags = List.copyOf(machine.getTags());
		width = machine.getWidth();
		height = machine.getHeight();
		downBoards = machine.getDeadBoards();
		downLinks = machine.getDownLinks();

		if (nonNull(ui)) {
			var b = ui.getAbsolutePathBuilder().path("{resource}");
			lookupByPhysicalBoard = b.build(MACH_BOARD_BY_PHYSICAL);
			lookupByLogicalBoard = b.build(MACH_BOARD_BY_LOGICAL);
			lookupByChip = b.build(MACH_BOARD_BY_CHIP);
			lookupByAddress = b.build(MACH_BOARD_BY_ADDRESS);
		} else {
			lookupByPhysicalBoard = null;
			lookupByLogicalBoard = null;
			lookupByChip = null;
			lookupByAddress = null;
		}
	}
}
