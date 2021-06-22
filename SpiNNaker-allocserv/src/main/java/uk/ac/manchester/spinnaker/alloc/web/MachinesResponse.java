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

import static java.util.Collections.unmodifiableList;

import java.net.URI;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;

import uk.ac.manchester.spinnaker.alloc.allocator.SpallocAPI.BoardCoords;
import uk.ac.manchester.spinnaker.alloc.allocator.SpallocAPI.DownLink;
import uk.ac.manchester.spinnaker.alloc.allocator.SpallocAPI.Machine;

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
	public final class BriefMachineDescription {
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
				int height, List<String> tags, List<BoardCoords> deadBoards,
				List<DownLink> deadLinks) {
			this.name = name;
			this.uri = uri;
			this.width = width;
			this.height = height;
			this.tags = unmodifiableList(tags);
			this.deadBoards = unmodifiableList(deadBoards);
			this.deadLinks = unmodifiableList(deadLinks);
		}
	}

	/** The list of machines known to the service. */
	public final List<BriefMachineDescription> machines;

	MachinesResponse(Map<String, Machine> machines, UriInfo ui)
			throws SQLException {
		List<BriefMachineDescription> mlist = new ArrayList<>();
		UriBuilder ub = ui.getAbsolutePathBuilder().path("{name}");
		for (Entry<String, Machine> ment : machines.entrySet()) {
			String name = ment.getKey();
			URI uri = ub.build(name);
			Machine m = ment.getValue();

			mlist.add(new BriefMachineDescription(name, uri, m.getWidth(),
					m.getHeight(), m.getTags(), m.getDeadBoards(),
					m.getDownLinks()));
		}
		this.machines = unmodifiableList(mlist);
	}
}
