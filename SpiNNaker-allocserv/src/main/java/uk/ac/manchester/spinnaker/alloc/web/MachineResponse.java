/*
 * Copyright (c) 2021 The University of Manchester
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
