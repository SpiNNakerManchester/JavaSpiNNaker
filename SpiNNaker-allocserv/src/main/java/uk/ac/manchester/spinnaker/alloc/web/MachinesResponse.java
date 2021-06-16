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

import static java.util.stream.Collectors.toList;

import java.net.URI;
import java.util.List;
import java.util.Map;

import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;

public class MachinesResponse {
	/**
	 * The list of URIs to machines known to the service.
	 */
	public final List<URI> machines;

	public MachinesResponse(Map<String, ?> machines, UriInfo ui) {
		UriBuilder ub = ui.getAbsolutePathBuilder().path("{name}");
		this.machines = machines.keySet().stream().map(s -> ub.build(s))
				.collect(toList());
	}
}
