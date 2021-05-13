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
import java.util.List;

import javax.ws.rs.core.UriInfo;

import uk.ac.manchester.spinnaker.alloc.allocator.SubMachine;
import uk.ac.manchester.spinnaker.spalloc.messages.BoardCoordinates;
import uk.ac.manchester.spinnaker.spalloc.messages.Connection;

public class SubMachineResponse {
	public int width;

	public int height;

	public String machineName;

	public List<Connection> connections;

	public List<BoardCoordinates> boards;

	public URI power;

	public URI machineRef;

	public SubMachineResponse(SubMachine m, UriInfo ui) {
		width = m.width;
		height = m.height;
		machineName = m.machine.name;
		connections = unmodifiableList(m.connections);
		boards = unmodifiableList(m.boards);
		power = ui.getAbsolutePathBuilder().path("power").build();
		machineRef = ui.getBaseUriBuilder().path("machine/{name}")
				.build(m.machine.name);
	}

}
