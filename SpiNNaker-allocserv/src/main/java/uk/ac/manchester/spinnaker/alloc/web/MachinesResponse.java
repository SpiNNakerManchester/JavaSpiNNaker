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

import static uk.ac.manchester.spinnaker.utils.CollectionUtils.copy;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.ws.rs.core.UriInfo;

import uk.ac.manchester.spinnaker.alloc.allocator.SpallocAPI.Machine;
import uk.ac.manchester.spinnaker.alloc.model.BoardCoords;
import uk.ac.manchester.spinnaker.alloc.model.DownLink;

/**
 * The description of machines known to the service. A list of
 * {@link BriefMachineDescription}s.
 *
 * @author Donal Fellows
 */
public final class MachinesResponse {
	/**
	 * A brief, summary description of a machine.
	 *
	 * @author Donal Fellows
	 */
	public static final class BriefMachineDescription {
		/** The name of the machine. */
		public final String name;

		/** The tags of the machine. */
		public final List<String> tags;

		/** The URI to the machine. */
		public final URI uri;

		/** The width of the machine, in triads. */
		public final int width;

		/** The height of the machine, in triads. */
		public final int height;

		/** The dead boards on the machine. */
		public final List<BoardCoords> deadBoards;

		/** The dead links on the machine. */
		public final List<DownLink> deadLinks;

		private BriefMachineDescription(String name, URI uri, int width,
				int height, Set<String> tags, List<BoardCoords> deadBoards,
				List<DownLink> deadLinks) {
			this.name = name;
			this.uri = uri;
			this.width = width;
			this.height = height;
			this.tags = List.copyOf(tags);
			this.deadBoards = copy(deadBoards);
			this.deadLinks = copy(deadLinks);
		}
	}

	/** The list of machines known to the service. */
	public final List<BriefMachineDescription> machines;

	MachinesResponse(Map<String, Machine> machines, UriInfo ui) {
		var mlist = new ArrayList<BriefMachineDescription>(machines.size());
		var ub = ui.getAbsolutePathBuilder().path("{name}");
		machines.forEach((name,
				machine) -> mlist.add(new BriefMachineDescription(name,
						ub.build(name), machine.getWidth(), machine.getHeight(),
						machine.getTags(), machine.getDeadBoards(),
						machine.getDownLinks())));
		this.machines = copy(mlist);
	}
}
