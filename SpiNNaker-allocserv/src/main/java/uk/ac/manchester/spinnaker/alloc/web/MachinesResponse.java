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

import static java.util.stream.Collectors.toUnmodifiableList;
import static uk.ac.manchester.spinnaker.utils.CollectionUtils.copy;

import java.net.URI;
import java.util.List;
import java.util.Map;

import javax.ws.rs.core.UriInfo;

import uk.ac.manchester.spinnaker.alloc.allocator.SpallocAPI.Machine;
import uk.ac.manchester.spinnaker.alloc.model.BoardCoords;
import uk.ac.manchester.spinnaker.alloc.model.DownLink;

/**
 * The description of machines known to the service. A list of
 * {@link BriefMachineDescription}s.
 *
 * @author Donal Fellows
 * @param machines
 *            The list of machines known to the service.
 */
public final record MachinesResponse(List<BriefMachineDescription> machines) {
	/**
	 * A brief, summary description of a machine.
	 *
	 * @author Donal Fellows
	 * @param name
	 *            The name of the machine.
	 * @param tags
	 *            The tags of the machine.
	 * @param uri
	 *            The URI to the machine.
	 * @param width
	 *            The width of the machine, in triads.
	 * @param height
	 *            The height of the machine, in triads.
	 * @param deadBoards
	 *            The dead boards on the machine.
	 * @param deadLinks
	 *            The dead links on the machine.
	 */
	public static record BriefMachineDescription(String name, List<String> tags,
			URI uri, int width, int height, List<BoardCoords> deadBoards,
			List<DownLink> deadLinks) {
	}

	MachinesResponse(Map<String, Machine> machines, UriInfo ui) {
		this(makeBriefDescriptions(machines, ui));
	}

	private static List<BriefMachineDescription> makeBriefDescriptions(
			Map<String, Machine> machines, UriInfo ui) {
		var ub = ui.getAbsolutePathBuilder().path("{name}");
		return machines.entrySet().stream()
				.map(e -> new BriefMachineDescription(e.getKey(),
						List.copyOf(e.getValue().getTags()),
						ub.build(e.getKey()), e.getValue().getWidth(),
						e.getValue().getHeight(),
						copy(e.getValue().getDeadBoards()),
						copy(e.getValue().getDownLinks())))
				.collect(toUnmodifiableList());
	}
}
