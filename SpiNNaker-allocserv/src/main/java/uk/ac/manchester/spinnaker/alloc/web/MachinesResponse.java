/*
 * Copyright (c) 2021 The University of Manchester
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
