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

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;
import static java.util.Objects.nonNull;
import static uk.ac.manchester.spinnaker.alloc.web.WebServiceComponentNames.JOB_MACHINE_POWER;
import static uk.ac.manchester.spinnaker.alloc.web.WebServiceComponentNames.MACH;
import static uk.ac.manchester.spinnaker.utils.CollectionUtils.copy;

import java.net.URI;
import java.util.List;

import jakarta.ws.rs.core.UriInfo;

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

	SubMachineResponse(SubMachine m, UriInfo ui) {
		width = m.getWidth();
		height = m.getHeight();
		depth = m.getDepth();
		machineName = m.getMachine().getName();
		connections = copy(m.getConnections());
		boards = copy(m.getBoards());
		if (nonNull(ui)) {
			power = ui.getAbsolutePathBuilder().path(JOB_MACHINE_POWER).build();
			machineRef = ui.getBaseUriBuilder().path(MACH + "/{name}")
					.build(m.getMachine().getName());
		} else {
			power = null;
			machineRef = null;
		}
	}
}
