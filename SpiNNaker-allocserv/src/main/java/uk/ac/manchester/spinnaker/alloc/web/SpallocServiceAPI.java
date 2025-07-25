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

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;
import static jakarta.ws.rs.core.MediaType.APPLICATION_OCTET_STREAM;
import static jakarta.ws.rs.core.MediaType.TEXT_PLAIN;
import static uk.ac.manchester.spinnaker.alloc.security.SecurityConfig.IS_READER;
import static uk.ac.manchester.spinnaker.alloc.security.SecurityConfig.IS_USER;
import static uk.ac.manchester.spinnaker.alloc.web.DocConstants.T_JOB;
import static uk.ac.manchester.spinnaker.alloc.web.DocConstants.T_MCH;
import static uk.ac.manchester.spinnaker.alloc.web.DocConstants.T_TOP;
import static uk.ac.manchester.spinnaker.alloc.web.WebServiceComponentNames.ADDRESS;
import static uk.ac.manchester.spinnaker.alloc.web.WebServiceComponentNames.CHIP_X;
import static uk.ac.manchester.spinnaker.alloc.web.WebServiceComponentNames.CHIP_Y;
import static uk.ac.manchester.spinnaker.alloc.web.WebServiceComponentNames.CHIP_P;
import static uk.ac.manchester.spinnaker.alloc.web.WebServiceComponentNames.ETH_ADDRESS;
import static uk.ac.manchester.spinnaker.alloc.web.WebServiceComponentNames.ETH_X;
import static uk.ac.manchester.spinnaker.alloc.web.WebServiceComponentNames.ETH_Y;
import static uk.ac.manchester.spinnaker.alloc.web.WebServiceComponentNames.FAST_DATA_WRITE;
import static uk.ac.manchester.spinnaker.alloc.web.WebServiceComponentNames.GATHER_P;
import static uk.ac.manchester.spinnaker.alloc.web.WebServiceComponentNames.GATHER_X;
import static uk.ac.manchester.spinnaker.alloc.web.WebServiceComponentNames.GATHER_Y;
import static uk.ac.manchester.spinnaker.alloc.web.WebServiceComponentNames.FAST_DATA_READ;
import static uk.ac.manchester.spinnaker.alloc.web.WebServiceComponentNames.ID;
import static uk.ac.manchester.spinnaker.alloc.web.WebServiceComponentNames.IPTAG;
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
import static uk.ac.manchester.spinnaker.alloc.web.WebServiceComponentNames.MEMORY;
import static uk.ac.manchester.spinnaker.alloc.web.WebServiceComponentNames.NAME;
import static uk.ac.manchester.spinnaker.alloc.web.WebServiceComponentNames.REPORT_ISSUE;
import static uk.ac.manchester.spinnaker.alloc.web.WebServiceComponentNames.SERV;
import static uk.ac.manchester.spinnaker.alloc.web.WebServiceComponentNames.SIZE;
import static uk.ac.manchester.spinnaker.alloc.web.WebServiceComponentNames.WAIT;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.container.AsyncResponse;
import jakarta.ws.rs.container.Suspended;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
import jakarta.ws.rs.core.UriInfo;

import org.apache.cxf.jaxrs.model.wadl.Description;
import org.springframework.security.access.prepost.PreAuthorize;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import uk.ac.manchester.spinnaker.machine.ValidP;
import uk.ac.manchester.spinnaker.machine.ValidX;
import uk.ac.manchester.spinnaker.machine.ValidY;
import uk.ac.manchester.spinnaker.machine.board.ValidBoardNumber;
import uk.ac.manchester.spinnaker.machine.board.ValidCabinetNumber;
import uk.ac.manchester.spinnaker.machine.board.ValidFrameNumber;
import uk.ac.manchester.spinnaker.machine.board.ValidTriadX;
import uk.ac.manchester.spinnaker.machine.board.ValidTriadY;
import uk.ac.manchester.spinnaker.machine.board.ValidTriadZ;
import uk.ac.manchester.spinnaker.utils.validation.IPAddress;

/**
 * The REST API for the SpiNNaker machine allocation service.
 */
@OpenAPIDefinition(tags = {
	@Tag(name = T_TOP, description = "Operations at the service level"),
	@Tag(name = T_MCH, description = "Operations on SpiNNaker machines"),
	@Tag(name = T_JOB, description = "Operations on Spalloc jobs")
})
@Path(SERV)
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
	 * @param request
	 *            Details about the request, used to extract the CSRF token.
	 * @return A wrapped {@link ServiceDescription}
	 */
	@GET
	@Description("Get a description of the overall service.")
	@Operation(tags = T_TOP, summary = "Describe the overall service",
			description = "Get a description of the overall service.")
	@Produces(APPLICATION_JSON)
	@Path("")
	ServiceDescription describeService(@Context UriInfo ui,
			@Context SecurityContext security,
			@Context HttpServletRequest request);

	/**
	 * Get a description of the machines.
	 *
	 * @param ui
	 *            How to build URIs
	 * @return A list of machines
	 */
	@GET
	@Description("Get a description of the machines.")
	@Operation(tags = T_MCH, summary = "List managed machines",
			description = "Get a description of the machines managed. "
					+ "Does not support paging; "
					+ "number of machines expected to be small.")
	@Path(MACH)
	@Produces(APPLICATION_JSON)
	@PreAuthorize(IS_READER)
	MachinesResponse getMachines(@Context UriInfo ui);
	// No paging; not expecting very many!

	/**
	 * Get a sub-resource for managing a machine.
	 *
	 * @param name
	 *            The name of the machine
	 * @param ui
	 *            How to build URIs
	 * @param security
	 *            What are the user's identity and granted permissions?
	 * @return The sub-resource
	 */
	@Path(MACH + "/{name}")
	@Description("Operations on a specific machine.")
	@PreAuthorize(IS_READER)
	MachineAPI getMachine(
			@Description("The name of the machine.") @PathParam(NAME)
			@Parameter(description = "The name of the machine.")
			@NotBlank(message = "machine name must not be blank") String name,
			@Context UriInfo ui, @Context SecurityContext security);

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
	@Operation(tags = T_JOB, summary = "List the jobs.",
			description = "List jobs known to the service. Supports paging. "
					+ "Supports long queries")
	@ApiResponse(content = @Content(schema = @Schema(
			implementation = ListJobsResponse.class)))
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
			@Parameter(description = "Whether to also return destroyed jobs.")
			@QueryParam("deleted") @DefaultValue("false") boolean destroyed,
			@Description("Paging support: max records to return.")
			@Parameter(description = "Paging support: max records to return.")
			@QueryParam("limit") @DefaultValue("100")
			@Positive(message = "limit must be at least 1") int limit,
			@Description("Paging support: offset to start at.")
			@Parameter(description = "Paging support: offset to start at.")
			@QueryParam("start") @DefaultValue("0")
			@PositiveOrZero(message = "start must be at least 0") int start,
			@Context UriInfo ui, @Suspended AsyncResponse response);

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
	 */
	@POST
	@Description("Create a new job.")
	@Operation(tags = T_JOB, summary = "Create job",
			description = "Create a Spalloc job.")
	@ApiResponse(content = @Content(schema = @Schema(
			implementation = CreateJobResponse.class)))
	@Path(JOB)
	@Consumes(APPLICATION_JSON)
	@Produces(APPLICATION_JSON)
	@PreAuthorize(IS_USER)
	void createJob(
			@Description("What sort of job should be created?") @NotNull
			@Parameter(description = "What sort of job should be created?")
			@Valid CreateJobRequest req, @Context UriInfo ui,
			@Context SecurityContext security,
			@Suspended AsyncResponse response);

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
	 */
	@Description("Operations on a specific job.")
	@Path(JOB + "/{id}")
	@Produces(APPLICATION_JSON)
	@PreAuthorize(IS_USER)
	JobAPI getJob(
			@Description("ID of the job.") @PathParam(ID)
			@Parameter(description = "ID of the job.")
			@Positive(message = "job ID must be positive") int id,
			@Context UriInfo ui, @Context HttpServletRequest request,
			@Context SecurityContext security);

	/**
	 * Immediately stop all jobs and power down all boards.
	 *
	 * @param commandCode
	 * @param response
	 */
	@GET
	@Description("Immediately stop all jobs and power down all boards,"
			+ " and stop accepting new jobs.")
	@Operation(tags = T_TOP, summary = "Emergency stop",
				description = "Immediately stop all jobs and power down"
					+ " all boards, and stop accepting new jobs.")
	@Path("/emergencyStop")
	void emergencyStop(@Description("Code to validate the request.")
			@Parameter(description = "Code to validate the request.")
			@QueryParam("commandCode") String commandCode,
			@Suspended AsyncResponse response);

	/**
	 * Interface to a particular machine.
	 *
	 * @author Donal Fellows
	 */
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
		@Operation(tags = T_MCH, summary = "Describe a machine",
				description = "Describes the basic info about a machine.")
		@ApiResponse(content = @Content(schema = @Schema(
				implementation = MachineResponse.class)))
		@Path("/")
		@Produces(APPLICATION_JSON)
		void describeMachine(
				@Description("Whether to wait for a change (for up "
						+ "to 30 seconds) before returning.")
				@Parameter(description = "Whether to wait for a change (for up "
						+ "to 30 seconds) before returning")
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
		@Description("Map from logical coordinates to "
				+ "a description of a board.")
		@Operation(tags = T_MCH, summary = "Find board by logical coordinates",
				description = "Get the location description of a board given "
						+ "its logical coordinates.")
		@Path(MACH_BOARD_BY_LOGICAL)
		@Produces(APPLICATION_JSON)
		WhereIsResponse whereIsLogicalPosition(
				@Description("Triad X coordinate") @QueryParam("x")
				@Parameter(description = "Triad X coordinate")
				@ValidTriadX int x,
				@Description("Triad Y coordinate") @QueryParam("y")
				@Parameter(description = "Triad Y coordinate")
				@ValidTriadY int y,
				@Description("Triad Z (internal member select) coordinate")
				@Parameter(description =
						"Triad Z (internal member select) coordinate")
				@QueryParam("z") @DefaultValue("0")
				@ValidTriadZ int z);

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
		@Description("Map from physical coordinates to "
				+ "a description of a board.")
		@Operation(tags = T_MCH, summary = "Find board by physical coordinates")
		@Path(MACH_BOARD_BY_PHYSICAL)
		@Produces(APPLICATION_JSON)
		WhereIsResponse whereIsPhysicalPosition(
				@Description("Cabinet number") @QueryParam("cabinet")
				@Parameter(description = "Cabinet number")
				@DefaultValue("0") @ValidCabinetNumber int cabinet,
				@Description("Frame number") @QueryParam("frame")
				@Parameter(description = "Frame number")
				@DefaultValue("0") @ValidFrameNumber int frame,
				@Description("Board number") @QueryParam("board")
				@Parameter(description = "Board number")
				@ValidBoardNumber int board);

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
		@Description("Map from global chip coordinates to "
				+ "a description of a board.")
		@Operation(tags = T_MCH, summary = "Find board holding a chip")
		@Path(MACH_BOARD_BY_CHIP)
		@Produces(APPLICATION_JSON)
		WhereIsResponse whereIsMachineChipLocation(
				@Description("Global chip X coordinate") @QueryParam(CHIP_X)
				@Parameter(description = "Global chip X coordinate")
				@ValidX int x,
				@Description("Global chip Y coordinate") @QueryParam(CHIP_Y)
				@Parameter(description = "Global chip Y coordinate")
				@ValidY int y);

		/**
		 * Get the location description of a board given its ethernet chip's IP
		 * address.
		 *
		 * @param address
		 *            IP address
		 * @return A board location description
		 */
		@GET
		@Description("Map from IP address to a description of a board.")
		@Operation(tags = T_MCH, summary = "Find board by IP address",
				description = "Get the location description of a board given "
						+ "its ethernet chip's IP address.")
		@Path(MACH_BOARD_BY_ADDRESS)
		@Produces(APPLICATION_JSON)
		WhereIsResponse
				whereIsIPAddress(@Description("Ethernet chip IP address")
				@Parameter(description = "Ethernet chip IP address")
				@QueryParam("address") @IPAddress String address);
	}

	/**
	 * Interface to a particular job.
	 *
	 * @author Donal Fellows
	 */
	interface JobAPI {
		/**
		 * Describe the basic info about a job.
		 *
		 * @param wait
		 *            Whether to wait for a change.
		 * @param response
		 *            Filled out with a {@link JobStateResponse}.
		 */
		@GET
		@Description("Describe basic details of a machine.")
		@Operation(tags = T_JOB, operationId = "describeJob",
				summary = "Describe a job")
		@ApiResponse(content = @Content(schema = @Schema(
				implementation = JobStateResponse.class)))
		@Path("/")
		@Produces(APPLICATION_JSON)
		void getState(
				@Description("Whether to wait for a change (for up "
						+ "to 30 seconds) before returning.")
				@Parameter(description = "Whether to wait for a change (for up "
						+ "to 30 seconds) before returning")
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
		@Operation(tags = T_JOB, summary = "Keep a job alive")
		@Path(JOB_KEEPALIVE)
		@Consumes(TEXT_PLAIN)
		@Produces(TEXT_PLAIN)
		String keepAlive(@Description("Arbitrary string; ignored")
				@Parameter(description = "Arbitrary string; ignored")
				String req);

		/**
		 * Delete the job, or at least mark it as destroyed.
		 *
		 * @param reason
		 *            Why the job is destroyed
		 * @return No content
		 */
		@DELETE
		@Description("Delete a job.")
		@Operation(tags = T_JOB, summary = "Delete a job")
		@Path("/")
		@Produces(APPLICATION_JSON)
		Response deleteJob(
				@Description("Some indication of why the delete is being done.")
				@Parameter(description =
						"Some indication of why the delete is being done.")
				@QueryParam("reason") @DefaultValue("") String reason);

		/**
		 * Get a description of the (sub-)machine of the job.
		 *
		 * @return An allocated sub-machine description
		 */
		@GET
		@Description("Describe a job's machine allocation.")
		@Operation(tags = T_JOB, summary = "Describe the job's resources")
		@Path(JOB_MACHINE)
		@Produces(APPLICATION_JSON)
		SubMachineResponse getMachine();

		/**
		 * Get the current power state of the job's sub-machine.
		 *
		 * @return A power descriptor
		 */
		@GET
		@Description("Get the power status of a job.")
		@Operation(tags = T_JOB, summary = "Get the job's power status")
		@Path(JOB_MACHINE + "/" + JOB_MACHINE_POWER)
		@Produces(APPLICATION_JSON)
		MachinePower getMachinePower();

		/**
		 * Set the power state of the job's sub-machine. Note that the actual
		 * change may (will!) take time to process.
		 *
		 * @param req
		 *            What to change to.
		 * @param response
		 *            Filled out with a {@link MachinePower}.
		 */
		@POST
		@Description("Set the power status of a job.")
		@Operation(tags = T_JOB, summary = "Set the job's power status")
		@ApiResponse(content = @Content(schema = @Schema(
				implementation = MachinePower.class)))
		@Path(JOB_MACHINE + "/" + JOB_MACHINE_POWER)
		@Consumes(APPLICATION_JSON)
		@Produces(APPLICATION_JSON)
		void setMachinePower(//
				@Description("What to set the power status to.") @NotNull
				@Parameter(description = "What to set the power status to.")
				@Valid MachinePower req, @Suspended AsyncResponse response);

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
		@Description("Describe a board in an allocation by "
				+ "a chip on that board.")
		@Operation(tags = T_JOB,
				summary = "Get location info within job's allocation")
		@Path(JOB_BOARD_BY_CHIP)
		@Produces(APPLICATION_JSON)
		WhereIsResponse getJobChipLocation(
				@Description("X coordinate of chip within job's allocation.")
				@Parameter(description =
						"X coordinate of chip within job's allocation.")
				@QueryParam(CHIP_X) @DefaultValue("0") @ValidX int x,
				@Description("Y coordinate of chip within job's allocation.")
				@Parameter(description =
						"Y coordinate of chip within job's allocation.")
				@QueryParam(CHIP_Y) @DefaultValue("0") @ValidY int y);

		/**
		 * Report an issue with some boards.
		 *
		 * @param report
		 *            The problem description.
		 * @param response
		 *            Filled out with an {@link IssueReportResponse}.
		 */
		@POST
		@Description("Report an issue with some boards.")
		@Operation(tags = T_JOB, summary = "Report an issue with some boards.")
		@ApiResponse(content = @Content(schema = @Schema(
				implementation = IssueReportResponse.class)))
		@Path(REPORT_ISSUE)
		@Consumes(APPLICATION_JSON)
		@Produces(APPLICATION_JSON)
		void reportBoardIssue(IssueReportRequest report,
				@Suspended AsyncResponse response);

		/**
		 * Write data to job boards.
		 *
		 * @param x Chip X coordinate
		 * @param y Chip Y coordinate
		 * @param address Address to write to
		 * @param bytes Data to write
		 * @param response Eventual response once request is complete
		 */
		@POST
		@Description("Write data to job boards.")
		@Operation(tags = T_JOB, summary = "Write data to job boards.")
		@Path(MEMORY)
		@Consumes(APPLICATION_OCTET_STREAM)
		void writeDataToJob(
				@Description("X coordinate of chip within job's allocation.")
				@Parameter(description =
						"X coordinate of chip within job's allocation.")
				@QueryParam(CHIP_X) @DefaultValue("0") @ValidX int x,
				@Description("Y coordinate of chip within job's allocation.")
				@Parameter(description =
						"Y coordinate of chip within job's allocation.")
				@QueryParam(CHIP_Y) @DefaultValue("0") @ValidY int y,
				@Description("The address to write the data to")
				@Parameter(description = "The address to write the data to")
				@QueryParam(ADDRESS) long address,
				byte[] bytes,
				@Suspended AsyncResponse response);

		/**
		 * Read data from job boards.
		 *
		 * @param x Chip X coordinate
		 * @param y Chip Y coordinate
		 * @param address Address to read from
		 * @param size Number of bytes to read
		 * @param response Filled out with bytes read.
		 */
		@GET
		@Description("Read data from job boards.")
		@Operation(tags = T_JOB, summary = "Read data from job boards.")
		@ApiResponse(content = @Content(schema = @Schema(
				implementation = byte[].class)))
		@Path(MEMORY)
		@Produces(APPLICATION_OCTET_STREAM)
		void readDataFromJob(
				@Description("X coordinate of chip within job's allocation.")
				@Parameter(description =
						"X coordinate of chip within job's allocation.")
				@QueryParam(CHIP_X) @DefaultValue("0") @ValidX int x,
				@Description("Y coordinate of chip within job's allocation.")
				@Parameter(description =
						"Y coordinate of chip within job's allocation.")
				@QueryParam(CHIP_Y) @DefaultValue("0") @ValidY int y,
				@Description("The address to write the data to")
				@Parameter(description =
						"The address to write the data to")
				@QueryParam(ADDRESS) long address,
				@Description("Number of bytes to read")
				@Parameter(description = "Number of bytes to read")
				@QueryParam(SIZE) int size,
				@Suspended AsyncResponse response);

		/**
		 * Write data to job boards using the fast data protocol.
		 * Note: it is assumed that the board has been set up before this is
		 * called.
		 *
		 * @param gatherX X coordinate of the gather core
		 * @param gatherY Y coordinate of the gather core
		 * @param gatherP Processor ID of the gather core
		 * @param ethX X coordinate of the Ethernet chip of x, y
		 * @param ethY Y coordinate of the Ethernet chip of x, y
		 * @param ethAddress The Ethernet address of the Ethernet chip
		 * @param iptag The IPTag to use for the write
		 * @param x The X coordinate of the chip within the job's allocation.
		 * @param y The Y coordinate of the chip within the job's allocation.
		 * @param address The address to write the data to.
		 * @param bytes The data to write.
		 * @param response Eventual response once request is complete
		 */
		@POST
		@Description("Write data to job boards using the fast data protocol.")
		@Operation(tags = T_JOB, summary = "Write data to job boards.")
		@Path(FAST_DATA_WRITE)
		@Consumes(APPLICATION_OCTET_STREAM)
		@SuppressWarnings("checkstyle:ParameterNumber")
		void fastDataWrite(
				@Description("X coordinate of the gather core")
				@Parameter(description = "X coordinate of the gather core")
				@QueryParam(GATHER_X) @ValidX int gatherX,
				@Description("Y coordinate of the gather core")
				@Parameter(description = "Y coordinate of the gather core")
				@QueryParam(GATHER_Y) @ValidY int gatherY,
				@Description("Processor ID of the gather core")
				@Parameter(description = "Processor ID of the gather core")
				@QueryParam(GATHER_P) @Positive int gatherP,
				@Description("X coordinate of the Ethernet chip of x, y")
				@Parameter(description =
						"X coordinate of the Ethernet chip of x, y")
				@QueryParam(ETH_X) @DefaultValue("0") @ValidX int ethX,
				@Description("Y coordinate of the Ethernet chip of x, y")
				@Parameter(description =
						"Y coordinate of the Ethernet chip of x, y")
				@QueryParam(ETH_Y) @DefaultValue("0") @ValidY int ethY,
				@Description("The Ethernet address of the Ethernet chip")
				@Parameter(description =
						"The Ethernet address of the Ethernet chip")
				@QueryParam(ETH_ADDRESS) @IPAddress String ethAddress,
				@Description("The IPTag to use for the write")
				@Parameter(description = "The IPTag to use for the write")
				@QueryParam(IPTAG) int iptag,
				@Description("X coordinate of chip within job's allocation.")
				@Parameter(description =
						"X coordinate of chip within job's allocation.")
				@QueryParam(CHIP_X) @DefaultValue("0") @ValidX int x,
				@Description("Y coordinate of chip within job's allocation.")
				@Parameter(description =
						"Y coordinate of chip within job's allocation.")
				@QueryParam(CHIP_Y) @DefaultValue("0") @ValidY int y,
				@Description("The address to write the data to")
				@Parameter(description = "The address to write the data to")
				@QueryParam(ADDRESS) long address, byte[] bytes,
				@Suspended AsyncResponse response);

		/**
		 * Read data from job boards using the fast data download protocol.
		 * Note: it is assumed that the board has been set up before this is
		 * called.
		 *
		 * @param gatherX X coordinate of the gather core
		 * @param gatherY Y coordinate of the gather core
		 * @param ethX X coordinate of the Ethernet chip of x, y
		 * @param ethY Y coordinate of the Ethernet chip of x, y
		 * @param ethAddress The Ethernet address of the Ethernet chip
		 * @param iptag The IPTag to use for the write
		 * @param x Chip X coordinate
		 * @param y Chip Y coordinate
		 * @param p Core processor
		 * @param address Address to read from
		 * @param size Number of bytes to read
		 * @param response Filled out with bytes read.
		 */
		@GET
		@Description("Read data from job boards using the fast data protocol.")
		@Operation(tags = T_JOB, summary = "Read data from job boards.")
		@ApiResponse(content = @Content(schema = @Schema(
				implementation = byte[].class)))
		@Path(FAST_DATA_READ)
		@Produces(APPLICATION_OCTET_STREAM)
		@SuppressWarnings("checkstyle:ParameterNumber")
		void fastDataRead(
				@Description("X coordinate of the gather core")
				@Parameter(description = "X coordinate of the gather core")
				@QueryParam(GATHER_X) @ValidX int gatherX,
				@Description("Y coordinate of the gather core")
				@Parameter(description = "Y coordinate of the gather core")
				@QueryParam(GATHER_Y) @ValidY int gatherY,
				@Description("X coordinate of the Ethernet chip of x, y")
				@Parameter(description =
						"X coordinate of the Ethernet chip of x, y")
				@QueryParam(ETH_X) @DefaultValue("0") @ValidX int ethX,
				@Description("Y coordinate of the Ethernet chip of x, y")
				@Parameter(description =
						"Y coordinate of the Ethernet chip of x, y")
				@QueryParam(ETH_Y) @DefaultValue("0") @ValidY int ethY,
				@Description("The Ethernet address of the Ethernet chip")
				@Parameter(description =
						"The Ethernet address of the Ethernet chip")
				@QueryParam(ETH_ADDRESS) @IPAddress String ethAddress,
				@Description("The IPTag to use for the write")
				@Parameter(description = "The IPTag to use for the write")
				@QueryParam(IPTAG) int iptag,
				@Description("X coordinate of chip within job's allocation.")
				@Parameter(description =
						"X coordinate of chip within job's allocation.")
				@QueryParam(CHIP_X) @DefaultValue("0") @ValidX int x,
				@Description("Y coordinate of chip within job's allocation.")
				@Parameter(description =
						"Y coordinate of chip within job's allocation.")
				@QueryParam(CHIP_Y) @DefaultValue("0") @ValidY int y,
				@Description("Processor id of monitor core on chip.")
				@Parameter(description =
						"Processor id of monitor core on chip.")
				@QueryParam(CHIP_P) @DefaultValue("0") @ValidP int p,
				@Description("The address to write the data to")
				@Parameter(description = "The address to write the data to")
				@QueryParam(ADDRESS) long address,
				@Description("Number of bytes to read")
				@Parameter(description = "Number of bytes to read")
				@QueryParam(SIZE) int size, @Suspended AsyncResponse response);

		/**
		 * Clear routes, reset counters and install counting filters needed.
		 *
		 * @param uriInfo
		 *         The info containing the query parameters of the request,
		 *         to include filters to be set as n=value where n is the
		 *         index of the filter to be set and value is the integer word
		 *         made of the combined flags of the filter.
		 * @param response The response to answer with
		 */
		@DELETE
		@Description("Clear routes, reset counters and install counters needed")
		@Operation(tags = T_JOB, summary = "Prepare routing tables",
				description = "Clear routes, reset counters and install "
						+ "counters needed.")
		@Path("/router")
		void prepareRoutingTables(@Context UriInfo uriInfo,
				@Suspended AsyncResponse response);
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
