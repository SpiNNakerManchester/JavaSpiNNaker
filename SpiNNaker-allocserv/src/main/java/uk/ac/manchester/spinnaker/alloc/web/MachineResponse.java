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

import java.net.URI;
import java.util.List;

import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;

import com.fasterxml.jackson.annotation.JsonInclude;

import uk.ac.manchester.spinnaker.alloc.allocator.SpallocInterface;

public class MachineResponse {
	public String name;

	public List<String> tags;

	public int width;

	public int height;

	// TODO: dead boards, dead links

	@JsonInclude(NON_NULL)
	public URI physicalBoard;

	@JsonInclude(NON_NULL)
	public URI logicalBoard;

	@JsonInclude(NON_NULL)
	public URI chip;

	public MachineResponse() {
	}

	public MachineResponse(SpallocInterface.Machine machine, UriInfo ui) {
		name = machine.getName();
		tags = machine.getTags();
		width = machine.getWidth();
		height = machine.getHeight();
		// FIXME pull info out of machine
		UriBuilder b = ui.getAbsolutePathBuilder().path("{element}");
		physicalBoard = b.build("physical-board");
		logicalBoard = b.build("logical-board");
		chip = b.build("chip");
	}
}
