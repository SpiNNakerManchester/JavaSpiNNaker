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

import static uk.ac.manchester.spinnaker.alloc.web.Constants.ID;
import static uk.ac.manchester.spinnaker.alloc.web.Constants.JSON;
import static uk.ac.manchester.spinnaker.alloc.web.Constants.NAME;
import static uk.ac.manchester.spinnaker.alloc.web.Constants.TEXT;
import static uk.ac.manchester.spinnaker.alloc.web.Constants.WAIT;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

/**
 * The REST API for the SpiNNaker machine allocation service.
 */
@Path("/spalloc")
public interface SpallocAPI {
	@GET
	@Path("version")
	@Produces(JSON)
	Response getVersion();

	@GET
	@Path("machine")
	@Produces(JSON)
	Response getMachines(@Context UriInfo ui);

	@Path("machine/{name}")
	MachineAPI getMachine(@PathParam(NAME) String name);

	@GET
	@Path("job")
	@Produces(JSON)
	Response listJobs(@QueryParam(WAIT) @DefaultValue("false") boolean wait,
			@Context UriInfo ui);

	@POST
	@Path("job")
	@Consumes(JSON)
	@Produces(JSON)
	Response createJob(CreateJobRequest req, @Context UriInfo ui);

	@Path("job/{id}")
	@Produces(JSON)
	JobAPI getJob(@PathParam(ID) int id);

	interface MachineAPI {
		@GET
		@Path("/")
		@Produces(JSON)
		Response describeMachine(
				@QueryParam(WAIT) @DefaultValue("false") boolean wait,
				@Context UriInfo ui);

		@GET
		@Path("physical-board")
		@Produces(JSON)
		Response getPhysicalPosition(@QueryParam("x") int x,
				@QueryParam("y") int y,
				@QueryParam("z") @DefaultValue("0") int z);

		@GET
		@Path("logical-board")
		@Produces(JSON)
		Response getLogicalPosition(@QueryParam("cabinet") int cabinet,
				@QueryParam("frame") int frame, @QueryParam("board") int board);

		@GET
		@Path("chip")
		@Produces(JSON)
		Response getMachineChipLocation(@QueryParam("x") int x,
				@QueryParam("y") int y);
	}

	interface JobAPI {
		@GET
		@Path("/")
		@Produces(JSON)
		Response getState(@QueryParam(WAIT) @DefaultValue("false") boolean wait,
				@Context UriInfo ui);

		@PUT
		@Path("keepalive")
		@Consumes(TEXT)
		@Produces(TEXT)
		Response keepAlive(String req);

		@DELETE
		@Path("/")
		@Produces(JSON)
		Response deleteJob();

		@GET
		@Path("machine")
		@Produces(JSON)
		Response getMachine(@Context UriInfo ui);

		@GET
		@Path("machine/power")
		@Produces(JSON)
		Response getMachinePower();

		@POST
		@Path("machine/power")
		@Consumes(JSON)
		@Produces(JSON)
		Response setMachinePower(MachinePower req);

		@GET
		@Path("chip")
		@Produces(JSON)
		Response getJobChipLocation(@QueryParam("x") int x,
				@QueryParam("y") int y);
	}
}

abstract class Constants {
	private Constants() {
	}

	static final String TEXT = "text/plain";
	static final String JSON = "application/json";
	static final String WAIT = "wait";
	static final String ID = "id";
	static final String NAME = "name";
}
