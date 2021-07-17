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
import static java.util.Collections.unmodifiableList;
import static uk.ac.manchester.spinnaker.alloc.web.WebServiceComponentNames.JOB_MACHINE_POWER;
import static uk.ac.manchester.spinnaker.alloc.web.WebServiceComponentNames.MACH;

import java.net.URI;
import java.sql.SQLException;
import java.util.List;

import javax.ws.rs.core.UriInfo;

import com.fasterxml.jackson.annotation.JsonInclude;

import uk.ac.manchester.spinnaker.alloc.allocator.SpallocAPI.SubMachine;
import uk.ac.manchester.spinnaker.alloc.model.ConnectionInfo;
import uk.ac.manchester.spinnaker.spalloc.messages.BoardCoordinates;

/**
 * Describes an allocation of part of a machine to a job.
 *
 * @author Donal Fellows
 */
public class SubMachineResponse {
	/** Rectangle width. */
	public final int width;

	/** Rectangle height. */
	public final int height;

	/** Depth of rectangle. 1 or 3. */
	public final int depth;

	/** On what machine. */
	public final String machineName;

	/** How to talk to boards. */
	public final List<ConnectionInfo> connections;

	/** Where the boards are. */
	public final List<BoardCoordinates> boards;

	/** Where to get machine power information. */
	@JsonInclude(NON_NULL)
	public final URI power;

	/** Where to get the full machine description. */
	@JsonInclude(NON_NULL)
	public final URI machineRef;

	public SubMachineResponse(SubMachine m, UriInfo ui) throws SQLException {
		width = m.getWidth();
		height = m.getHeight();
		depth = m.getDepth();
		machineName = m.getMachine().getName();
		connections = unmodifiableList(m.getConnections());
		boards = unmodifiableList(m.getBoards());
		power = ui.getAbsolutePathBuilder().path(JOB_MACHINE_POWER).build();
		machineRef = ui.getBaseUriBuilder().path(MACH + "/{name}")
				.build(m.getMachine().getName());
	}
}
