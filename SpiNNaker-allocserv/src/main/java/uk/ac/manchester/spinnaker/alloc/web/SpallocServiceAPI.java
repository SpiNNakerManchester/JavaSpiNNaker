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

import static io.swagger.v3.oas.annotations.enums.ParameterIn.PATH;
import static io.swagger.v3.oas.annotations.enums.ParameterIn.QUERY;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.MediaType.TEXT_PLAIN;
import static javax.ws.rs.core.Response.noContent;
import static uk.ac.manchester.spinnaker.alloc.web.Constants.ID;
import static uk.ac.manchester.spinnaker.alloc.web.Constants.NAME;
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
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.servers.Server;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * The REST API for the SpiNNaker machine allocation service.
 */
@OpenAPIDefinition(tags = {
	@Tag(name = "summary"),
	@Tag(name = "machines", description = "Operations on SpiNNaker machines"),
	@Tag(name = "jobs", description = "Operations on Spalloc jobs")
},
servers = @Server(url="spalloc"))
public interface SpallocServiceAPI {
	/**
	 * Get a description of the overall service.
	 *
	 * @param ui
	 *            How to build URIs
	 * @return A wrapped {@link ServiceDescription}
	 */
	@GET
	@Operation(
			tags = "summary",
			summary = "Describe the overall service",
			description = "Get a description of the overall service.",
			responses = {
				@ApiResponse(content = @Content(
						mediaType = APPLICATION_JSON,
						schema = @Schema(
								implementation = ServiceDescription.class)))
			})
	@Produces(APPLICATION_JSON)
	Response describeService(@Context UriInfo ui);

	/**
	 * Describe what HTTP verbs are supported.
	 *
	 * @return HTTP response
	 */
	@OPTIONS
	@Hidden
	default Response optionsService() {
		return noContent().allow("GET").build();
	}

	/**
	 * Get a description of the machines.
	 *
	 * @param ui
	 *            How to build URIs
	 * @return A wrapped {@link MachinesResponse}
	 */
	@GET
	@Operation(
			tags = "machines",
			summary = "List managed machines",
			description = "Get a description of the machines managed. "
					+ "Does not support paging; "
					+ "number of machines expected to be small.",
			responses = {
				@ApiResponse(content = @Content(
						mediaType = APPLICATION_JSON,
						schema = @Schema(
								implementation = MachinesResponse.class)))
			})
	@Path("machines")
	@Produces(APPLICATION_JSON)
	Response getMachines(@Context UriInfo ui);
	// No paging; not expecting very many!

	/**
	 * Get a sub-resource for managing a machine.
	 *
	 * @param name
	 *            The name of the machine
	 * @param ui
	 *            How to build URIs
	 * @return The sub-resource
	 */
	//@Operation(tags = "machines")
	@Path("machines/{name}")
	MachineAPI getMachine(
			@PathParam(NAME) String name,
			@Context UriInfo ui);

	/**
	 * Describe what HTTP verbs are supported.
	 *
	 * @return HTTP response
	 */
	@OPTIONS
	@Hidden
	@Path("machines")
	default Response optionsMachines() {
		return noContent().allow("GET").build();
	}

	/**
	 * List jobs.
	 *
	 * @param wait
	 *            Whether we are waiting for a change with a long query
	 * @param limit
	 *            Paging support: how many values to bring back
	 * @param start
	 *            Paging support: where in the sequence to start
	 * @param destroyed
	 *            Whether to include destroyed jobs
	 * @param ui
	 *            How to build URIs
	 * @param response
	 *            Filled out with a {@link ListJobsResponse}
	 */
	@GET
	@Operation(
			tags = "jobs",
			summary = "List jobs",
			description = "List jobs known to the service. Supports paging. "
					+ "Supports long queries",
			responses = {
				@ApiResponse(content = @Content(
						mediaType = APPLICATION_JSON,
						schema = @Schema(
								implementation = ListJobsResponse.class)))
			})
	@Path("jobs")
	@Produces(APPLICATION_JSON)
	void listJobs(
			@Parameter(description = "Whether to wait for a change (for up "
					+ "to 30 seconds) before returning")
			@QueryParam(WAIT) @DefaultValue("false") boolean wait,
			@Parameter(in = QUERY) @QueryParam("deleted") @DefaultValue("false")
			boolean destroyed,
			@Parameter(in = QUERY) @QueryParam("limit") @DefaultValue("100")
			int limit,
			@Parameter(in = QUERY) @QueryParam("start") @DefaultValue("0")
			int start,
			@Context UriInfo ui, @Suspended AsyncResponse response);

	/**
	 * Create a job.
	 *
	 * @param req
	 *            Description of what to create
	 * @param ui
	 *            How to build URIs
	 * @param response
	 *            Filled out with a {@link CreateJobResponse}
	 */
	@POST
	@Operation(
			tags = "jobs",
			summary = "Create job",
			description = "Create a Spalloc job.",
			responses = {
				@ApiResponse(content = @Content(
						mediaType = APPLICATION_JSON,
						schema = @Schema(
								implementation = CreateJobResponse.class)))
			})
	@Path("jobs")
	@Consumes(APPLICATION_JSON)
	@Produces(APPLICATION_JSON)
	void createJob(CreateJobRequest req, @Context UriInfo ui,
			@Suspended AsyncResponse response);

	/**
	 * Describe what HTTP verbs are supported.
	 *
	 * @return HTTP response
	 */
	@OPTIONS
	@Hidden
	default Response optionsJobs() {
		return noContent().allow("GET", "POST").build();
	}

	/**
	 * Get a sub-resource for managing a job.
	 *
	 * @param id
	 *            The ID of the job
	 * @param ui
	 *            How to build URIs
	 * @param request
	 *            Information about the request
	 * @return The sub-resource
	 */
	@Operation(tags = "jobs")
	@Path("jobs/{id}")
	@Produces(APPLICATION_JSON)
	JobAPI getJob(@PathParam(ID) int id, @Context UriInfo ui,
			@Context HttpServletRequest request);

	/**
	 * Interface to a particular machine.
	 *
	 * @author Donal Fellows
	 */
	@Path("machines/{name}")
	interface MachineAPI {
		/**
		 * Describe the basic info about a machine.
		 *
		 * @param wait
		 *            Whether to wait for a change.
		 * @param response
		 *            Filled out with a {@link MachineResponse}
		 */
		@GET
		@Operation(tags = "machines",
			summary = "Describe a machine",
			description = "Describes the basic info about a machine.",
			parameters = @Parameter(in = PATH, name = "name",
				description = "Machine name",
				schema = @Schema(implementation = String.class)),
			responses = {
				@ApiResponse(content = @Content(
						schema = @Schema(
								implementation = MachineResponse.class))),
				@ApiResponse(responseCode = "404",
					content = @Content(mediaType = "*/*"))
			})
		@Path("/")
		@Produces(APPLICATION_JSON)
		void describeMachine(
				@QueryParam(WAIT) @DefaultValue("false") boolean wait,
				@Suspended AsyncResponse response);

		/**
		 * Describe what HTTP verbs are supported.
		 *
		 * @param ignored
		 *            Ignored
		 * @return HTTP response
		 */
		@OPTIONS
		@Hidden
		@Path("{path:.*}")
		default Response optionsMachine(@PathParam("path") String ignored) {
			// All paths beneath here are GET-only
			return noContent().allow("GET").build();
		}

		/**
		 * Get the location description of a board given its logical coords.
		 *
		 * @param x
		 *            Logical X coordinate
		 * @param y
		 *            Logical Y coordinate
		 * @param z
		 *            Logical Z coordinate (deprecated).
		 * @return A wrapped {@link WhereIsResponse}
		 */
		@GET
		@Operation(tags = "machines",
			summary = "Find board by logical coordinates",
			description = "Get the location description of a board given its "
					+ "logical coordinates.",
			parameters = @Parameter(in = PATH, name = "name",
				description = "Machine name",
				schema = @Schema(implementation = String.class)),
			responses = @ApiResponse(content = @Content(
					schema = @Schema(implementation = WhereIsResponse.class))))
		@Path("logical-board")
		@Produces(APPLICATION_JSON)
		Response whereIsLogicalPosition(
				@QueryParam("x") @DefaultValue("0") int x,
				@QueryParam("y") @DefaultValue("0") int y,
				@QueryParam("z") @DefaultValue("0") int z);

		/**
		 * Get the location description of a board given its physical coords.
		 *
		 * @param cabinet
		 *            Cabinet number
		 * @param frame
		 *            Frame number
		 * @param board
		 *            Board number
		 * @return A wrapped {@link WhereIsResponse}
		 */
		@GET
		@Operation(tags = "machines",
			summary = "Find board by physical coordinates",
			parameters = @Parameter(in = PATH, name = "name",
				description = "Machine name",
				schema = @Schema(implementation = String.class)),
			responses = @ApiResponse(content = @Content(
					schema = @Schema(implementation = WhereIsResponse.class))))
		@Path("physical-board")
		@Produces(APPLICATION_JSON)
		Response whereIsPhysicalPosition(
				@QueryParam("cabinet") @DefaultValue("0") int cabinet,
				@QueryParam("frame") @DefaultValue("0") int frame,
				@QueryParam("board") @DefaultValue("0") int board);

		/**
		 * Get the location description of a board given the global coordinates
		 * of a chip on the board.
		 *
		 * @param x
		 *            Global chip X coordinate
		 * @param y
		 *            Global chip Y coordinate
		 * @return A wrapped {@link WhereIsResponse}
		 */
		@GET
		@Operation(tags = "machines",
			summary = "Find board holding a chip",
			parameters = @Parameter(in = PATH, name = "name",
				description = "Machine name",
				schema = @Schema(implementation = String.class)),
			responses = @ApiResponse(content = @Content(
					schema = @Schema(implementation = WhereIsResponse.class))))
		@Path("chip")
		@Produces(APPLICATION_JSON)
		Response whereIsMachineChipLocation(
				@QueryParam("x") @DefaultValue("0") int x,
				@QueryParam("y") @DefaultValue("0") int y);
	}

	/**
	 * Interface to a particular job.
	 *
	 * @author Donal Fellows
	 */
	@Path("jobs/{id}")
	interface JobAPI {
		/**
		 * Describe the basic info about a job.
		 *
		 * @param wait
		 *            Whether to wait for a change.
		 * @param response
		 *            Filled out with a {@link StateResponse}
		 */
		@GET
		@Operation(tags = "jobs",
			operationId = "describeJob",
			summary = "Describe a job",
			parameters = @Parameter(in = PATH, name = "id",
				description = "Job identifier",
				schema = @Schema(implementation = Integer.class)))
		@Path("/")
		@Produces(APPLICATION_JSON)
		void getState(@QueryParam(WAIT) @DefaultValue("false") boolean wait,
				@Suspended AsyncResponse response);

		/**
		 * Describe what HTTP verbs are supported.
		 *
		 * @return HTTP response
		 */
		@OPTIONS
		@Hidden
		default Response optionsRoot() {
			return noContent().allow("GET", "DELETE").build();
		}

		/**
		 * Keep the job alive.
		 *
		 * @param req
		 *            Arbitrary ignored text
		 * @return A constant response
		 */
		@PUT
		@Operation(tags = "jobs",
			summary = "Keep a job alive",
			parameters = @Parameter(in = PATH, name = "id",
				description = "Job identifier",
				schema = @Schema(implementation = Integer.class)))
		@Path("keepalive")
		@Consumes(TEXT_PLAIN)
		@Produces(TEXT_PLAIN)
		Response keepAlive(String req);

		/**
		 * Describe what HTTP verbs are supported.
		 *
		 * @return HTTP response
		 */
		@OPTIONS
		@Hidden
		default Response optionsKeepalive() {
			return noContent().allow("PUT").build();
		}

		/**
		 * Delete the job, or at least mark it as destroyed.
		 *
		 * @param reason
		 *            Why the job is destroyed
		 * @return No content
		 */
		@DELETE
		@Operation(tags = "jobs",
			summary = "Delete a job",
			parameters = @Parameter(in = PATH, name = "id",
				description = "Job identifier",
				schema = @Schema(implementation = Integer.class)))
		@Path("/")
		@Produces(APPLICATION_JSON)
		Response deleteJob(
				@QueryParam("reason") @DefaultValue("") String reason);

		/**
		 * Get a description of the (sub-)machine of the job.
		 *
		 * @return A wrapped {@link SubMachineResponse}
		 */
		@GET
		@Operation(tags = "jobs",
			summary = "Describe the job's resources",
			parameters = @Parameter(in = PATH, name = "id",
				description = "Job identifier",
				schema = @Schema(implementation = Integer.class)))
		@Path("machine")
		@Produces(APPLICATION_JSON)
		Response getMachine();

		/**
		 * Describe what HTTP verbs are supported.
		 *
		 * @return HTTP response
		 */
		@OPTIONS
		@Hidden
		@Path("machine")
		default Response optionsMachine() {
			return noContent().allow("GET").build();
		}

		/**
		 * Get the current power state of the job's sub-machine.
		 *
		 * @return A wrapped {@link MachinePower}
		 */
		@GET
		@Operation(tags = "jobs",
			summary = "Get the job's power status",
			parameters = @Parameter(in = PATH, name = "id",
				description = "Job identifier",
				schema = @Schema(implementation = Integer.class)))
		@Path("machine/power")
		@Produces(APPLICATION_JSON)
		Response getMachinePower();

		/**
		 * Set the power state of the job's sub-machine. Note that the actual
		 * change may (will!) take time to process.
		 *
		 * @param req
		 *            What to change to.
		 * @return A wrapped {@link MachinePower}
		 */
		@POST
		@Operation(tags = "jobs",
			summary = "Set the job's power status",
			parameters = @Parameter(in = PATH, name = "id",
				description = "Job identifier",
				schema = @Schema(implementation = Integer.class)))
		@Path("machine/power")
		@Consumes(APPLICATION_JSON)
		@Produces(APPLICATION_JSON)
		Response setMachinePower(MachinePower req);

		/**
		 * Describe what HTTP verbs are supported.
		 *
		 * @return HTTP response
		 */
		@OPTIONS
		@Hidden
		@Path("machine/power")
		default Response optionsPower() {
			return noContent().allow("GET", "POST").build();
		}

		/**
		 * Get the location description of a chip given its job-local
		 * coordinates.
		 *
		 * @param x
		 *            Chip X coordinate
		 * @param y
		 *            Chip Y coordinate
		 * @return A wrapped {@link WhereIsResponse}
		 */
		@GET
		@Operation(tags = "jobs",
			summary = "Get location within job's allocation",
			parameters = @Parameter(in = PATH, name = "id",
				description = "Job identifier",
				schema = @Schema(implementation = Integer.class)))
		@Path("chip")
		@Produces(APPLICATION_JSON)
		Response getJobChipLocation(@QueryParam("x") @DefaultValue("0") int x,
				@QueryParam("y") @DefaultValue("0") int y);

		/**
		 * Describe what HTTP verbs are supported.
		 *
		 * @return HTTP response
		 */
		@OPTIONS
		@Hidden
		@Path("chip")
		default Response optionsWhereIs() {
			return noContent().allow("GET").build();
		}
	}
}

abstract class Constants {
	private Constants() {
	}

	static final String WAIT = "wait";

	static final String ID = "id";

	static final String NAME = "name";
}
