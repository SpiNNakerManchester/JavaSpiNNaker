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
import static uk.ac.manchester.spinnaker.alloc.SecurityConfig.IS_READER;
import static uk.ac.manchester.spinnaker.alloc.SecurityConfig.IS_USER;
import static uk.ac.manchester.spinnaker.alloc.web.DocConstants.T_JOB;
import static uk.ac.manchester.spinnaker.alloc.web.DocConstants.T_MCH;
import static uk.ac.manchester.spinnaker.alloc.web.DocConstants.T_TOP;
import static uk.ac.manchester.spinnaker.alloc.web.WebServiceComponentNames.CHIP_X;
import static uk.ac.manchester.spinnaker.alloc.web.WebServiceComponentNames.CHIP_Y;
import static uk.ac.manchester.spinnaker.alloc.web.WebServiceComponentNames.ID;
import static uk.ac.manchester.spinnaker.alloc.web.WebServiceComponentNames.JOB;
import static uk.ac.manchester.spinnaker.alloc.web.WebServiceComponentNames.JOB_BOARD_BY_CHIP;
import static uk.ac.manchester.spinnaker.alloc.web.WebServiceComponentNames.JOB_KEEPALIVE;
import static uk.ac.manchester.spinnaker.alloc.web.WebServiceComponentNames.JOB_MACHINE;
import static uk.ac.manchester.spinnaker.alloc.web.WebServiceComponentNames.JOB_MACHINE_POWER;
import static uk.ac.manchester.spinnaker.alloc.web.WebServiceComponentNames.MACH;
import static uk.ac.manchester.spinnaker.alloc.web.WebServiceComponentNames.MACH_BOARD_BY_ADDRESS;
import static uk.ac.manchester.spinnaker.alloc.web.WebServiceComponentNames.MACH_BOARD_BY_CHIP;
import static uk.ac.manchester.spinnaker.alloc.web.WebServiceComponentNames.MACH_BOARD_BY_LOGICAL;
import static uk.ac.manchester.spinnaker.alloc.web.WebServiceComponentNames.MACH_BOARD_BY_PHYSICAL;
import static uk.ac.manchester.spinnaker.alloc.web.WebServiceComponentNames.NAME;
import static uk.ac.manchester.spinnaker.alloc.web.WebServiceComponentNames.SERV;
import static uk.ac.manchester.spinnaker.alloc.web.WebServiceComponentNames.WAIT;

import java.sql.SQLException;

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
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriInfo;

import org.apache.cxf.jaxrs.model.wadl.Description;
import org.springframework.security.access.prepost.PreAuthorize;

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
}, servers = @Server(url = SERV))
public interface SpallocServiceAPI {
	/*
	 * When adding to this interface, an operation should be implemented in an
	 * asynchronous manner when it EITHER calls into the waitable epoch system,
	 * OR if it is doing a database write AT ALL because acquiring a write lock
	 * can take a while with SQLite if things are busy.
	 *
	 * That is not an exhaustive list of reasons; anything that causes a request
	 * to wait for more than a millisecond is also a candidate.
	 */

	/**
	 * Get a description of the overall service.
	 *
	 * @param ui
	 *            How to build URIs
	 * @param security
	 *            What are the user's identity and granted permissions?
	 * @return A wrapped {@link ServiceDescription}
	 * @throws SQLException
	 *             If anything goes wrong.
	 */
	@GET
	@Description("Get a description of the overall service.")
	@Operation(
			tags = T_TOP,
			summary = "Describe the overall service",
			description = "Get a description of the overall service.")
	@Produces(APPLICATION_JSON)
	ServiceDescription describeService(@Context UriInfo ui,
			@Context SecurityContext security) throws SQLException;

	/**
	 * Get a description of the machines.
	 *
	 * @param ui
	 *            How to build URIs
	 * @return A list of machines
	 * @throws SQLException
	 *             If anything goes wrong.
	 */
	@GET
	@Description("Get a description of the machines.")
	@Operation(
			tags = T_MCH,
			summary = "List managed machines",
			description = "Get a description of the machines managed. "
					+ "Does not support paging; "
					+ "number of machines expected to be small.")
	@Path(MACH)
	@Produces(APPLICATION_JSON)
	@PreAuthorize(IS_READER)
	MachinesResponse getMachines(@Context UriInfo ui) throws SQLException;
	// No paging; not expecting very many!

	/**
	 * Get a sub-resource for managing a machine.
	 *
	 * @param name
	 *            The name of the machine
	 * @param ui
	 *            How to build URIs
	 * @return The sub-resource
	 * @throws SQLException
	 *             If anything goes wrong.
	 */
	@Path(MACH + "/{name}")
	@Description("Operations on a specific machine.")
	@PreAuthorize(IS_READER)
	MachineAPI getMachine(
			@Description("The name of the machine.")
			@PathParam(NAME) String name,
			@Context UriInfo ui) throws SQLException;

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
	 * @throws SQLException
	 *             If anything goes wrong.
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
	@Path(JOB)
	@Produces(APPLICATION_JSON)
	@PreAuthorize(IS_READER)
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
			@Context UriInfo ui, @Suspended AsyncResponse response)
					throws SQLException;

	/**
	 * Create a job.
	 *
	 * @param req
	 *            Description of what to create
	 * @param ui
	 *            How to build URIs
	 * @param security
	 *            Information about the user
	 * @param response
	 *            Filled out with a {@link CreateJobResponse}
	 * @throws SQLException
	 *             If anything goes wrong.
	 */
	@POST
	@Description("Create a new job.")
	@Operation(
			tags = T_JOB,
			summary = "Create job",
			description = "Create a Spalloc job.",
			responses = @ApiResponse(content = @Content(schema = @Schema(
				implementation = CreateJobResponse.class))))
	@Path(JOB)
	@Consumes(APPLICATION_JSON)
	@Produces(APPLICATION_JSON)
	@PreAuthorize(IS_USER)
	void createJob(
			@Description("What sort of job should be created?")
			CreateJobRequest req,
			@Context UriInfo ui, @Context SecurityContext security,
			@Suspended AsyncResponse response) throws SQLException;

	/**
	 * Get a sub-resource for managing a job.
	 *
	 * @param id
	 *            The ID of the job
	 * @param ui
	 *            How to build URIs
	 * @param request
	 *            Information about the request
	 * @param security
	 *            Information about the user
	 * @return The sub-resource
	 * @throws SQLException
	 *             If anything goes wrong.
	 */
	@Description("Operations on a specific job.")
	@Operation(tags = T_JOB)
	@Path(JOB + "/{id}")
	@Produces(APPLICATION_JSON)
	@PreAuthorize(IS_USER)
	JobAPI getJob(@Description("ID of the job.") @PathParam(ID) int id,
			@Context UriInfo ui, @Context HttpServletRequest request,
			@Context SecurityContext security) throws SQLException;

	/**
	 * Interface to a particular machine.
	 *
	 * @author Donal Fellows
	 */
	@Path(MACH + "/{name}")
	interface MachineAPI {
		/**
		 * Describe the basic info about a machine.
		 *
		 * @param wait
		 *            Whether to wait for a change.
		 * @param response
		 *            Filled out with a {@link MachineResponse}
		 * @throws SQLException
		 *             If anything goes wrong.
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
				@Suspended AsyncResponse response) throws SQLException;

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
		 * @throws SQLException
		 *             If anything goes wrong.
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
		@Path(MACH_BOARD_BY_LOGICAL)
		@Produces(APPLICATION_JSON)
		WhereIsResponse whereIsLogicalPosition(
				@Description("Triad X coordinate")
				@QueryParam("x") @DefaultValue("0") int x,
				@Description("Triad Y coordinate")
				@QueryParam("y") @DefaultValue("0") int y,
				@Description("Triad Z (internal member select) coordinate")
				@QueryParam("z") @DefaultValue("0") int z) throws SQLException;

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
		 * @throws SQLException
		 *             If anything goes wrong.
		 */
		@GET
		@Description(
				"Map from physical coordinates to a description of a board.")
		@Operation(tags = T_MCH,
			summary = "Find board by physical coordinates",
			parameters = @Parameter(in = PATH, name = NAME,
				description = "Machine name",
				schema = @Schema(implementation = String.class)))
		@Path(MACH_BOARD_BY_PHYSICAL)
		@Produces(APPLICATION_JSON)
		WhereIsResponse whereIsPhysicalPosition(
				@Description("Cabinet number")
				@QueryParam("cabinet") @DefaultValue("0") int cabinet,
				@Description("Frame number")
				@QueryParam("frame") @DefaultValue("0") int frame,
				@Description("Board number")
				@QueryParam("board") @DefaultValue("0") int board)
						throws SQLException;

		/**
		 * Get the location description of a board given the global coordinates
		 * of a chip on the board.
		 *
		 * @param x
		 *            Global chip X coordinate
		 * @param y
		 *            Global chip Y coordinate
		 * @return A board location description
		 * @throws SQLException
		 *             If anything goes wrong.
		 */
		@GET
		@Description(
				"Map from global chip coordinates to a description of a board.")
		@Operation(tags = T_MCH,
			summary = "Find board holding a chip",
			parameters = @Parameter(in = PATH, name = NAME,
				description = "Machine name",
				schema = @Schema(implementation = String.class)))
		@Path(MACH_BOARD_BY_CHIP)
		@Produces(APPLICATION_JSON)
		WhereIsResponse whereIsMachineChipLocation(
				@Description("Global chip X coordinate")
				@QueryParam(CHIP_X) @DefaultValue("0") int x,
				@Description("Global chip Y coordinate")
				@QueryParam(CHIP_Y) @DefaultValue("0") int y)
						throws SQLException;

		/**
		 * Get the location description of a board given its ethernet chip's IP
		 * address.
		 *
		 * @param address
		 *            IP address
		 * @return A board location description
		 * @throws SQLException
		 *             If anything goes wrong.
		 */
		@GET
		@Description("Map from IP address to a description of a board.")
		@Operation(tags = T_MCH,
			summary = "Find board by IP address",
			description = "Get the location description of a board given its "
					+ "ethernet chip's IP address.",
			parameters = @Parameter(in = PATH, name = NAME,
				description = "Machine name",
				schema = @Schema(implementation = String.class)))
		@Path(MACH_BOARD_BY_ADDRESS)
		@Produces(APPLICATION_JSON)
		WhereIsResponse whereIsIPAddress(
				@Description("Ethernet chip IP address")
				@QueryParam("address") @DefaultValue("0.0.0.0") String address)
						throws SQLException;
	}

	/**
	 * Interface to a particular job.
	 *
	 * @author Donal Fellows
	 */
	@Path(JOB + "/{id}")
	interface JobAPI {
		/**
		 * Describe the basic info about a job.
		 *
		 * @param wait
		 *            Whether to wait for a change.
		 * @param response
		 *            Filled out with a {@link JobStateResponse}
		 * @throws SQLException
		 *             If anything goes wrong.
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
				@Suspended AsyncResponse response) throws SQLException;

		/**
		 * Keep the job alive.
		 *
		 * @param req
		 *            Arbitrary ignored text
		 * @return A constant response
		 * @throws SQLException
		 *             If anything goes wrong.
		 */
		@PUT
		@Description("Keep the job alive. Must be called regularly.")
		@Operation(tags = T_JOB,
			summary = "Keep a job alive",
			parameters = @Parameter(in = PATH, name = ID,
				description = "Job identifier",
				schema = @Schema(implementation = Integer.class)))
		@Path(JOB_KEEPALIVE)
		@Consumes(TEXT_PLAIN)
		@Produces(TEXT_PLAIN)
		String keepAlive(@Description("Arbitrary string; ignored") String req)
				throws SQLException;

		/**
		 * Delete the job, or at least mark it as destroyed.
		 *
		 * @param reason
		 *            Why the job is destroyed
		 * @return No content
		 * @throws SQLException
		 *             If anything goes wrong.
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
				@QueryParam("reason") @DefaultValue("") String reason)
						throws SQLException;

		/**
		 * Get a description of the (sub-)machine of the job.
		 *
		 * @return An allocated sub-machine description
		 * @throws SQLException
		 *             If anything goes wrong.
		 */
		@GET
		@Description("Describe a job's machine allocation.")
		@Operation(tags = T_JOB,
			summary = "Describe the job's resources",
			parameters = @Parameter(in = PATH, name = ID,
				description = "Job identifier",
				schema = @Schema(implementation = Integer.class)))
		@Path(JOB_MACHINE)
		@Produces(APPLICATION_JSON)
		SubMachineResponse getMachine() throws SQLException;

		/**
		 * Get the current power state of the job's sub-machine.
		 *
		 * @return A power descriptor
		 * @throws SQLException
		 *             If anything goes wrong.
		 */
		@GET
		@Description("Get the power status of a job.")
		@Operation(tags = T_JOB,
			summary = "Get the job's power status",
			parameters = @Parameter(in = PATH, name = ID,
				description = "Job identifier",
				schema = @Schema(implementation = Integer.class)))
		@Path(JOB_MACHINE + "/" + JOB_MACHINE_POWER)
		@Produces(APPLICATION_JSON)
		MachinePower getMachinePower() throws SQLException;

		/**
		 * Set the power state of the job's sub-machine. Note that the actual
		 * change may (will!) take time to process.
		 *
		 * @param req
		 *            What to change to.
		 * @param response
		 *            Filled out with a {@link MachinePower}.
		 * @throws SQLException
		 *             If anything goes wrong.
		 */
		@POST
		@Description("Set the power status of a job.")
		@Operation(tags = T_JOB,
			summary = "Set the job's power status",
			parameters = @Parameter(in = PATH, name = ID,
				description = "Job identifier",
				schema = @Schema(implementation = Integer.class)),
			responses = @ApiResponse(content = @Content(schema = @Schema(
					implementation = MachinePower.class))))
		@Path(JOB_MACHINE + "/" + JOB_MACHINE_POWER)
		@Consumes(APPLICATION_JSON)
		@Produces(APPLICATION_JSON)
		void setMachinePower(
				@Description("What to set the power status to.")
				MachinePower req, @Suspended AsyncResponse response)
						throws SQLException;

		/**
		 * Get the location description of a board given the job-local
		 * coordinates of a chip on that board.
		 *
		 * @param x
		 *            Chip X coordinate
		 * @param y
		 *            Chip Y coordinate
		 * @return A board location description
		 * @throws SQLException
		 *             If anything goes wrong.
		 */
		@GET
		@Description(
				"Describe a board in an allocation by a chip on that board.")
		@Operation(tags = T_JOB,
			summary = "Get location info within job's allocation",
			parameters = @Parameter(in = PATH, name = ID,
				description = "Job identifier",
				schema = @Schema(implementation = Integer.class)))
		@Path(JOB_BOARD_BY_CHIP)
		@Produces(APPLICATION_JSON)
		WhereIsResponse getJobChipLocation(
				@Description("X coordinate of chip within job's allocation.")
				@QueryParam(CHIP_X) @DefaultValue("0") int x,
				@Description("Y coordinate of chip within job's allocation.")
				@QueryParam(CHIP_Y) @DefaultValue("0") int y)
						throws SQLException;
	}
}

/**
 * Names of things used on Swagger documentation.
 *
 * @author Donal Fellows
 */
abstract class DocConstants {
	private DocConstants() {
	}

	/** Summary section tag. */
	static final String T_TOP = "Spalloc Service Summary";

	/** Jobs section tag. */
	static final String T_JOB = "SpiNNaker Jobs";

	/** Machines section tag. */
	static final String T_MCH = "SpiNNaker Machines";
}
