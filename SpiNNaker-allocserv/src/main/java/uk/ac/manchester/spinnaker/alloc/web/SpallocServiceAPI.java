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
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.MediaType.TEXT_PLAIN;
import static uk.ac.manchester.spinnaker.alloc.web.WebServiceConstants.CHIP_X;
import static uk.ac.manchester.spinnaker.alloc.web.WebServiceConstants.CHIP_Y;
import static uk.ac.manchester.spinnaker.alloc.web.WebServiceConstants.ID;
import static uk.ac.manchester.spinnaker.alloc.web.WebServiceConstants.NAME;
import static uk.ac.manchester.spinnaker.alloc.web.WebServiceConstants.WAIT;
import static uk.ac.manchester.spinnaker.alloc.web.WebServiceConstants.T_TOP;
import static uk.ac.manchester.spinnaker.alloc.web.WebServiceConstants.T_JOB;
import static uk.ac.manchester.spinnaker.alloc.web.WebServiceConstants.T_MCH;

import javax.servlet.http.HttpServletRequest;
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
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.apache.cxf.jaxrs.model.wadl.Description;

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
	@Tag(name = T_TOP, description = "Operations at the service level"),
	@Tag(name = T_MCH, description = "Operations on SpiNNaker machines"),
	@Tag(name = T_JOB, description = "Operations on Spalloc jobs")
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
	@Description("Get a description of the overall service.")
	@Operation(
			tags = T_TOP,
			summary = "Describe the overall service",
			description = "Get a description of the overall service.")
	@Produces(APPLICATION_JSON)
	ServiceDescription describeService(@Context UriInfo ui);

	/**
	 * Get a description of the machines.
	 *
	 * @param ui
	 *            How to build URIs
	 * @return A list of machines
	 */
	@GET
	@Description("Get a description of the machines.")
	@Operation(
			tags = T_MCH,
			summary = "List managed machines",
			description = "Get a description of the machines managed. "
					+ "Does not support paging; "
					+ "number of machines expected to be small.")
	@Path("machines")
	@Produces(APPLICATION_JSON)
	MachinesResponse getMachines(@Context UriInfo ui);
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
	@Path("machines/{name}")
	@Description("Operations on a specific machine.")
	MachineAPI getMachine(
			@Description("The name of the machine.")
			@PathParam(NAME) String name,
			@Context UriInfo ui);

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
	@Description("List the jobs.")
	@Operation(
			tags = T_JOB,
			summary = "List jobs",
			description = "List jobs known to the service. Supports paging. "
					+ "Supports long queries",
			responses = @ApiResponse(content = @Content(schema = @Schema(
				implementation = ListJobsResponse.class))))
	@Path("jobs")
	@Produces(APPLICATION_JSON)
	void listJobs(
			@Description("Whether to wait for a change (for up "
					+ "to 30 seconds) before returning.")
			@Parameter(description = "Whether to wait for a change (for up "
					+ "to 30 seconds) before returning")
			@QueryParam(WAIT) @DefaultValue("false") boolean wait,
			@Description("Whether to also return destroyed jobs.")
			@QueryParam("deleted") @DefaultValue("false") boolean destroyed,
			@Description("Paging support: max records to return.")
			@QueryParam("limit") @DefaultValue("100") int limit,
			@Description("Paging support: offset to start at.")
			@QueryParam("start") @DefaultValue("0") int start,
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
	@Description("Create a new job.")
	@Operation(
			tags = T_JOB,
			summary = "Create job",
			description = "Create a Spalloc job.",
			responses = @ApiResponse(content = @Content(schema = @Schema(
				implementation = CreateJobResponse.class))))
	@Path("jobs")
	@Consumes(APPLICATION_JSON)
	@Produces(APPLICATION_JSON)
	void createJob(
			@Description("What sort of job should be created?")
			CreateJobRequest req,
			@Context UriInfo ui, @Suspended AsyncResponse response);

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
	@Description("Operations on a specific job.")
	@Operation(tags = T_JOB)
	@Path("jobs/{id}")
	@Produces(APPLICATION_JSON)
	JobAPI getJob(@Description("ID of the job.") @PathParam(ID) int id,
			@Context UriInfo ui, @Context HttpServletRequest request);

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
		@Description("Describe basic details of a machine.")
		@Operation(tags = T_MCH,
			summary = "Describe a machine",
			description = "Describes the basic info about a machine.",
			parameters = @Parameter(in = PATH, name = NAME,
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
				@Description("Whether to wait for a change (for up "
						+ "to 30 seconds) before returning.")
				@QueryParam(WAIT) @DefaultValue("false") boolean wait,
				@Suspended AsyncResponse response);

		/**
		 * Get the location description of a board given its logical coords.
		 *
		 * @param x
		 *            Logical X coordinate
		 * @param y
		 *            Logical Y coordinate
		 * @param z
		 *            Logical Z coordinate (deprecated).
		 * @return A board location description
		 */
		@GET
		@Description(
				"Map from logical coordinates to a description of a board.")
		@Operation(tags = T_MCH,
			summary = "Find board by logical coordinates",
			description = "Get the location description of a board given its "
					+ "logical coordinates.",
			parameters = @Parameter(in = PATH, name = NAME,
				description = "Machine name",
				schema = @Schema(implementation = String.class)))
		@Path("logical-board")
		@Produces(APPLICATION_JSON)
		WhereIsResponse whereIsLogicalPosition(
				@Description("Triad X coordinate")
				@QueryParam("x") @DefaultValue("0") int x,
				@Description("Triad Y coordinate")
				@QueryParam("y") @DefaultValue("0") int y,
				@Description("Triad Z (internal member select) coordinate")
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
		 * @return A board location description
		 */
		@GET
		@Description(
				"Map from physical coordinates to a description of a board.")
		@Operation(tags = T_MCH,
			summary = "Find board by physical coordinates",
			parameters = @Parameter(in = PATH, name = NAME,
				description = "Machine name",
				schema = @Schema(implementation = String.class)))
		@Path("physical-board")
		@Produces(APPLICATION_JSON)
		WhereIsResponse whereIsPhysicalPosition(
				@Description("Cabinet number")
				@QueryParam("cabinet") @DefaultValue("0") int cabinet,
				@Description("Frame number")
				@QueryParam("frame") @DefaultValue("0") int frame,
				@Description("Board number")
				@QueryParam("board") @DefaultValue("0") int board);

		/**
		 * Get the location description of a board given the global coordinates
		 * of a chip on the board.
		 *
		 * @param x
		 *            Global chip X coordinate
		 * @param y
		 *            Global chip Y coordinate
		 * @return A board location description
		 */
		@GET
		@Description(
				"Map from global chip coordinates to a description of a board.")
		@Operation(tags = T_MCH,
			summary = "Find board holding a chip",
			parameters = @Parameter(in = PATH, name = NAME,
				description = "Machine name",
				schema = @Schema(implementation = String.class)))
		@Path("chip")
		@Produces(APPLICATION_JSON)
		WhereIsResponse whereIsMachineChipLocation(
				@Description("Global chip X coordinate")
				@QueryParam(CHIP_X) @DefaultValue("0") int x,
				@Description("Global chip Y coordinate")
				@QueryParam(CHIP_Y) @DefaultValue("0") int y);
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
		 *            Filled out with a {@link JobStateResponse}
		 */
		@GET
		@Description("Describe basic details of a machine.")
		@Operation(tags = T_JOB,
			operationId = "describeJob",
			summary = "Describe a job",
			parameters = @Parameter(in = PATH, name = ID,
				description = "Job identifier",
				schema = @Schema(implementation = Integer.class)),
			responses = @ApiResponse(content = @Content(schema = @Schema(
				implementation = JobStateResponse.class))))
		@Path("/")
		@Produces(APPLICATION_JSON)
		void getState(
				@Description("Whether to wait for a change (for up "
						+ "to 30 seconds) before returning.")
				@QueryParam(WAIT) @DefaultValue("false") boolean wait,
				@Suspended AsyncResponse response);

		/**
		 * Keep the job alive.
		 *
		 * @param req
		 *            Arbitrary ignored text
		 * @return A constant response
		 */
		@PUT
		@Description("Keep the job alive. Must be called regularly.")
		@Operation(tags = T_JOB,
			summary = "Keep a job alive",
			parameters = @Parameter(in = PATH, name = ID,
				description = "Job identifier",
				schema = @Schema(implementation = Integer.class)))
		@Path("keepalive")
		@Consumes(TEXT_PLAIN)
		@Produces(TEXT_PLAIN)
		String keepAlive(@Description("Arbitrary string; ignored") String req);

		/**
		 * Delete the job, or at least mark it as destroyed.
		 *
		 * @param reason
		 *            Why the job is destroyed
		 * @return No content
		 */
		@DELETE
		@Description("Delete a job.")
		@Operation(tags = T_JOB,
			summary = "Delete a job",
			parameters = @Parameter(in = PATH, name = ID,
				description = "Job identifier",
				schema = @Schema(implementation = Integer.class)))
		@Path("/")
		@Produces(APPLICATION_JSON)
		Response deleteJob(
				@Description("Some indication of why the delete is being done.")
				@QueryParam("reason") @DefaultValue("") String reason);

		/**
		 * Get a description of the (sub-)machine of the job.
		 *
		 * @return An allocated sub-machine description
		 */
		@GET
		@Description("Describe a job's machine allocation.")
		@Operation(tags = T_JOB,
			summary = "Describe the job's resources",
			parameters = @Parameter(in = PATH, name = ID,
				description = "Job identifier",
				schema = @Schema(implementation = Integer.class)))
		@Path("machine")
		@Produces(APPLICATION_JSON)
		SubMachineResponse getMachine();

		/**
		 * Get the current power state of the job's sub-machine.
		 *
		 * @return A power descriptor
		 */
		@GET
		@Description("Get the power status of a job.")
		@Operation(tags = T_JOB,
			summary = "Get the job's power status",
			parameters = @Parameter(in = PATH, name = ID,
				description = "Job identifier",
				schema = @Schema(implementation = Integer.class)))
		@Path("machine/power")
		@Produces(APPLICATION_JSON)
		MachinePower getMachinePower();

		/**
		 * Set the power state of the job's sub-machine. Note that the actual
		 * change may (will!) take time to process.
		 *
		 * @param req
		 *            What to change to.
		 * @return A wrapped {@link MachinePower}
		 */
		@POST
		@Description("Set the power status of a job.")
		@Operation(tags = T_JOB,
			summary = "Set the job's power status",
			parameters = @Parameter(in = PATH, name = ID,
				description = "Job identifier",
				schema = @Schema(implementation = Integer.class)))
		@Path("machine/power")
		@Consumes(APPLICATION_JSON)
		@Produces(APPLICATION_JSON)
		Response setMachinePower(
				@Description("What to set the power status to.")
				MachinePower req);

		/**
		 * Get the location description of a board given the job-local
		 * coordinates of a chip on that board.
		 *
		 * @param x
		 *            Chip X coordinate
		 * @param y
		 *            Chip Y coordinate
		 * @return A board location description
		 */
		@GET
		@Description(
				"Describe a board in an allocation by a chip on that board.")
		@Operation(tags = T_JOB,
			summary = "Get location info within job's allocation",
			parameters = @Parameter(in = PATH, name = ID,
				description = "Job identifier",
				schema = @Schema(implementation = Integer.class)))
		@Path("chip")
		@Produces(APPLICATION_JSON)
		WhereIsResponse getJobChipLocation(
				@Description("X coordinate of chip within job's allocation.")
				@QueryParam(CHIP_X) @DefaultValue("0") int x,
				@Description("Y coordinate of chip within job's allocation.")
				@QueryParam(CHIP_Y) @DefaultValue("0") int y);
	}
}

abstract class WebServiceConstants {
	private WebServiceConstants() {
	}

	static final String T_TOP = "Spalloc Service Summary";

	static final String T_JOB = "SpiNNaker Jobs";

	static final String T_MCH = "SpiNNaker Machines";

	static final String WAIT = "wait";

	static final String ID = "id";

	static final String NAME = "name";

	static final String CHIP_X = "x";

	static final String CHIP_Y = "y";
}
