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

import static javax.ws.rs.core.Response.noContent;
import static uk.ac.manchester.spinnaker.alloc.web.Constants.ID;
import static uk.ac.manchester.spinnaker.alloc.web.Constants.JSON;
import static uk.ac.manchester.spinnaker.alloc.web.Constants.NAME;
import static uk.ac.manchester.spinnaker.alloc.web.Constants.TEXT;
import static uk.ac.manchester.spinnaker.alloc.web.Constants.WAIT;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.OPTIONS;
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
	@Produces(JSON)
	Response describeService(@Context UriInfo ui);

	@OPTIONS
	default Response optionsService() {
		return noContent().allow("GET").build();
	}

	@GET
	@Path("machines")
	@Produces(JSON)
	Response getMachines(@Context UriInfo ui);
	// No paging; not expecting very many!

	@Path("machines/{name}")
	MachineAPI getMachine(@PathParam(NAME) String name, @Context UriInfo ui);

	@OPTIONS
	@Path("machines")
	default Response optionsMachines() {
		return noContent().allow("GET").build();
	}

	@GET
	@Path("jobs")
	@Produces(JSON)
	Response listJobs(@QueryParam(WAIT) @DefaultValue("false") boolean wait,
			@QueryParam("limit") @DefaultValue("100") int limit,
			@QueryParam("start") @DefaultValue("0") int start,
			@Context UriInfo ui);

	@POST
	@Path("jobs")
	@Consumes(JSON)
	@Produces(JSON)
	Response createJob(CreateJobRequest req, @Context UriInfo ui);

	@OPTIONS
	default Response optionsJobs() {
		return noContent().allow("GET", "POST").build();
	}

	@Path("jobs/{id}")
	@Produces(JSON)
	JobAPI getJob(@PathParam(ID) int id, @Context UriInfo ui,
			@Context HttpServletRequest request);

	interface MachineAPI {
		@GET
		@Path("/")
		@Produces(JSON)
		Response describeMachine(
				@QueryParam(WAIT) @DefaultValue("false") boolean wait);

		@OPTIONS
		@Path("{path:.*}")
		default Response optionsMachine(@PathParam("path") String ignored) {
			// All paths beneath here are GET-only
			return noContent().allow("GET").build();
		}

		@GET
		@Path("logical-board")
		@Produces(JSON)
		Response whereIsLogicalPosition(
				@QueryParam("x") @DefaultValue("0") int x,
				@QueryParam("y") @DefaultValue("0") int y,
				@QueryParam("z") @DefaultValue("0") int z);

		@GET
		@Path("physical-board")
		@Produces(JSON)
		Response whereIsPhysicalPosition(
				@QueryParam("cabinet") @DefaultValue("0") int cabinet,
				@QueryParam("frame") @DefaultValue("0") int frame,
				@QueryParam("board") @DefaultValue("0") int board);

		@GET
		@Path("chip")
		@Produces(JSON)
		Response whereIsMachineChipLocation(
				@QueryParam("x") @DefaultValue("0") int x,
				@QueryParam("y") @DefaultValue("0") int y);
	}

	interface JobAPI {
		@GET
		@Path("/")
		@Produces(JSON)
		Response getState(
				@QueryParam(WAIT) @DefaultValue("false") boolean wait);

		@OPTIONS
		default Response optionsRoot() {
			return noContent().allow("GET", "DELETE").build();
		}

		@PUT
		@Path("keepalive")
		@Consumes(TEXT)
		@Produces(TEXT)
		Response keepAlive(String req);

		@OPTIONS
		default Response optionsKeepalive() {
			return noContent().allow("PUT").build();
		}

		@DELETE
		@Path("/")
		@Produces(JSON)
		Response deleteJob(
				@QueryParam("reason") @DefaultValue("") String reason);

		@GET
		@Path("machine")
		@Produces(JSON)
		Response getMachine();

		@OPTIONS
		@Path("machine")
		default Response optionsMachine() {
			return noContent().allow("GET").build();
		}

		@GET
		@Path("machine/power")
		@Produces(JSON)
		Response getMachinePower();

		@POST
		@Path("machine/power")
		@Consumes(JSON)
		@Produces(JSON)
		Response setMachinePower(MachinePower req);

		@OPTIONS
		@Path("machine/power")
		default Response optionsPower() {
			return noContent().allow("GET", "POST").build();
		}

		@GET
		@Path("chip")
		@Produces(JSON)
		Response getJobChipLocation(@QueryParam("x") @DefaultValue("0") int x,
				@QueryParam("y") @DefaultValue("0") int y);

		@OPTIONS
		@Path("chip")
		default Response optionsWhereIs() {
			return noContent().allow("GET").build();
		}
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
