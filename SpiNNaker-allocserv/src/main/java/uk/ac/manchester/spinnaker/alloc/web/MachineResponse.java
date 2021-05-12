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

import java.net.URI;
import java.util.List;

import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;

import uk.ac.manchester.spinnaker.alloc.allocator.Machine;

public class MachineResponse {
	public String name;
	public List<String> tags;
	public int width;
	public int height;
	// TODO: dead boards, dead links
	public URI physicalBoard;
	public URI logicalBoard;
	public URI chip;

	public MachineResponse() {
	}

	public MachineResponse(Machine machine, UriInfo ui) {
		name = machine.name;
		tags = machine.tags;
		width = machine.width;
		height = machine.height;
		// FIXME pull info out of machine
		UriBuilder b = ui.getAbsolutePathBuilder().path("{element}");
		physicalBoard = b.build("physical-board");
		logicalBoard = b.build("logical-board");
		chip = b.build("chip");
	}
}
