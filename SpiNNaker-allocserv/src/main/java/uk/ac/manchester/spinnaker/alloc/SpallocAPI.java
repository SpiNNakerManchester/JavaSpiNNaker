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
package uk.ac.manchester.spinnaker.alloc;

import static uk.ac.manchester.spinnaker.alloc.Constants.ID;
import static uk.ac.manchester.spinnaker.alloc.Constants.JSON;
import static uk.ac.manchester.spinnaker.alloc.Constants.NAME;
import static uk.ac.manchester.spinnaker.alloc.Constants.TEXT;
import static uk.ac.manchester.spinnaker.alloc.Constants.WAIT;

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

/**
 * The REST API for the SpiNNaker machine allocation service.
 */
@Path("/spalloc")
public interface SpallocAPI {
	@GET
	@Path("version")
	@Produces(JSON)
	VersionResponse getVersion();

	@GET
	@Path("machine")
	@Produces(JSON)
	MachinesResponse getMachines();

	@GET
	@Path("machine/{name}")
	@Produces(JSON)
	MachineResponse getMachine(@PathParam(NAME) String name,
			@QueryParam(WAIT) @DefaultValue("false") boolean wait);

	@GET
	@Path("machine/{name}/physical-board")
	@Produces(JSON)
	WhereIsResponse getPhysicalPosition(@PathParam(NAME) String name,
			@QueryParam("x") int x, @QueryParam("y") int y,
			@QueryParam("z") @DefaultValue("0") int z);

	@GET
	@Path("machine/{name}/logical-board")
	@Produces(JSON)
	WhereIsResponse getLogicalPosition(@PathParam(NAME) String name,
			@QueryParam("cabinet") int cabinet, @QueryParam("frame") int frame,
			@QueryParam("board") int board);

	@GET
	@Path("machine/{name}/chip")
	@Produces(JSON)
	WhereIsResponse getMachineChipLocation(@PathParam(NAME) String name,
			@QueryParam("x") int x, @QueryParam("y") int y);

	@GET
	@Path("job")
	@Produces(JSON)
	ListJobsResponse listJobs(
			@QueryParam(WAIT) @DefaultValue("false") boolean wait);

	@POST
	@Path("job")
	@Consumes(JSON)
	@Produces(JSON)
	CreateJobResponse createJob(CreateJobRequest req);

	@PUT
	@Path("job/{id}/keepalive")
	@Consumes(TEXT)
	@Produces(TEXT)
	String keepAlive(@PathParam(ID) int id, String req);

	@GET
	@Path("job/{id}")
	@Produces(JSON)
	StateResponse getState(@PathParam(ID) int id,
			@QueryParam(WAIT) @DefaultValue("false") boolean wait);

	@DELETE
	@Path("job/{id}")
	@Produces(JSON)
	DeleteResponse deleteJob(@PathParam(ID) int id);

	@GET
	@Path("job/{id}/machine")
	@Produces(JSON)
	MachineResponse getMachine(@PathParam(ID) int id);

	@GET
	@Path("job/{id}/machine/power")
	@Produces(JSON)
	MachinePowerResponse getMachinePower(@PathParam(ID) int id);

	@POST
	@Path("job/{id}/machine/power")
	@Consumes(JSON)
	@Produces(JSON)
	MachinePowerResponse setMachinePower(@PathParam(ID) int id,
			MachinePowerRequest req);

	@GET
	@Path("job/{id}/chip")
	@Produces(JSON)
	WhereIsResponse getJobChipLocation(@PathParam(ID) int id,
			@QueryParam("x") int x, @QueryParam("y") int y);
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
