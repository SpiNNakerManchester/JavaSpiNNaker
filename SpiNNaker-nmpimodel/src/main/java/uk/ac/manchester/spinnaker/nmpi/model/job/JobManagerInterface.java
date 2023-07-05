/*
 * Copyright (c) 2014 The University of Manchester
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
package uk.ac.manchester.spinnaker.nmpi.model.job;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

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
import javax.ws.rs.core.Response;

import uk.ac.manchester.spinnaker.nmpi.model.machine.ChipCoordinates;
import uk.ac.manchester.spinnaker.nmpi.model.machine.SpinnakerMachine;
import uk.ac.manchester.spinnaker.nmpi.model.job.nmpi.Job;

/**
 * JAX-RS interface to a {@link Job} for the purposes of management.
 */
@Path(JobManagerInterface.PATH)
public interface JobManagerInterface {

	/** The path to the interface. **/
	String PATH = "job";

	/** The media type of ZIP files. */
	String APPLICATION_ZIP = "application/zip";

	/** The media type for JSON. */
	String APPLICATION_JSON = "application/json";

	/** The media type for Binary data. */
	String APPLICATION_OCTET_STREAM = "application/octet-stream";

	/** The media type for text. */
	String TEXT_PLAIN = "text/plain";

	/**
	 * The name of the ZIP file we like to serve up when giving people a remote
	 * process manager.
	 */
	String JOB_PROCESS_MANAGER_ZIP = "SpiNNaker-nmpiexec.zip";

	/**
	 * The name of the setup script.
	 */
	String SETUP_SCRIPT = "setup.bash";

	/**
	 * Get the job manager to find out what its next job will be.
	 *
	 * @param executerId
	 *            The executor to talk about.
	 * @return The job discovered.
	 */
	@GET
	@Path("next")
	@Produces(APPLICATION_JSON)
	Job getNextJob(@QueryParam("executerId") String executerId);

	/**
	 * Get the largest machine that could run a job.
	 *
	 * @param id
	 *            The job ID.
	 * @param runTime
	 *            How much resource to allocate. Can be omitted, in which case
	 *            it is set to -1.
	 * @return The machine descriptor.
	 */
	@GET
	@Path("{id}/machine/max")
	@Produces(APPLICATION_JSON)
	SpinnakerMachine getLargestJobMachine(@PathParam("id") int id,
			@QueryParam("runTime") @DefaultValue("-1") double runTime);

	/**
	 * Get a machine for running a job. Typically, only one of {@code nCores},
	 * {@code nChips} and {@code nBoards} will be specified.
	 *
	 * @param id
	 *            The job ID.
	 * @param nCores
	 *            The number of cores wanted. Can be omitted, in which case it
	 *            is set to -1.
	 * @param nChips
	 *            The number of chips wanted. Can be omitted, in which case it
	 *            is set to -1.
	 * @param nBoards
	 *            The number of boards wanted. Can be omitted, in which case it
	 *            is set to -1.
	 * @param runTime
	 *            How much resource to allocate. Can be omitted, in which case
	 *            it is set to -1.
	 * @return The machine descriptor.
	 */
	@GET
	@Path("{id}/machine")
	@Produces(APPLICATION_JSON)
	SpinnakerMachine getJobMachine(@PathParam("id") int id,
			@QueryParam("nCores") @DefaultValue("-1") int nCores,
			@QueryParam("nChips") @DefaultValue("-1") int nChips,
			@QueryParam("nBoards") @DefaultValue("-1") int nBoards,
			@QueryParam("runTime") @DefaultValue("-1") double runTime);

	/**
	 * Check if the job is still allocated to a machine.
	 *
	 * @param id
	 *            The job ID
	 * @param waitTime
	 *            How long should the lease time be. Can be omitted, in which
	 *            case it is set to 1000.
	 * @return Whether the job is allocated.
	 */
	@GET
	@Path("{id}/machine/checkLease")
	@Produces(APPLICATION_JSON)
	JobMachineAllocated checkMachineLease(@PathParam("id") int id,
			@QueryParam("waitTime") @DefaultValue("10000") int waitTime);

	/**
	 * Extend the lease of the job.
	 *
	 * @param id
	 *            The job ID
	 * @param runTime
	 *            How long has the job actually run. Can be omitted, in which
	 *            case it is set to -1.
	 */
	@GET
	@Path("{id}/machine/extendLease")
	void extendJobMachineLease(@PathParam("id") int id,
			@QueryParam("runTime") @DefaultValue("-1") double runTime);

	/**
	 * Drop the allocation of a machine to a job.
	 *
	 * @param id
	 *            The job ID
	 * @param machineName
	 *            The name of the machine to stop using.
	 */
	@DELETE
	@Path("{id}/machine")
	void releaseMachine(@PathParam("id") int id,
			@QueryParam("machineName") String machineName);

	/**
	 * Set the power status of a job's machine.
	 *
	 * @param id
	 *            The job ID
	 * @param machineName
	 *            The name of the machine to control the power of.
	 * @param powerOn
	 *            True of the machine is to be switched on; false to switch it
	 *            off.
	 */
	@PUT
	@Path("{id}/machine/power")
	void setMachinePower(@PathParam("id") int id,
			@QueryParam("machineName") String machineName,
			@QueryParam("on") boolean powerOn);

	/**
	 * Get a description of where a chip actually is.
	 *
	 * @param id
	 *            The job ID
	 * @param machineName
	 *            The name of the machine to control the power of.
	 * @param chipX
	 *            The virtual X coordinate of the chip.
	 * @param chipY
	 *            The virtual Y coordinate of the chip.
	 * @return The coordinates of the chip
	 */
	@GET
	@Path("{id}/machine/chipCoordinates")
	@Produces(APPLICATION_JSON)
	ChipCoordinates getChipCoordinates(@PathParam("id") int id,
			@QueryParam("machineName") String machineName,
			@QueryParam("chipX") int chipX, @QueryParam("chipY") int chipY);

	/**
	 * Add to the log of a job.
	 *
	 * @param id
	 *            The job ID
	 * @param logToAppend
	 *            The string to append to the log.
	 */
	@POST
	@Path("{id}/log")
	@Consumes("text/plain")
	void appendLog(@PathParam("id") int id, String logToAppend);

	/**
	 * Add to the provenance of a job.
	 *
	 * @param id
	 *            The job ID
	 * @param path
	 *            The path into the JSON provenance doc.
	 * @param value
	 *            The value to set at that point.
	 */
	@POST
	@Path("{id}/provenance")
	void addProvenance(@PathParam("id") int id,
			@QueryParam("name") List<String> path,
			@QueryParam("value") String value);

	/**
	 * Add to the output files of a job.
	 *
	 * @param projectId
	 *            The ID of the project owning the job.
	 * @param id
	 *            The job ID
	 * @param output
	 *            The name of the file to write to.
	 * @param input
	 *            The contents of the file, streamed.
	 */
	@POST
	@Path("{projectId}/{id}/addoutput")
	@Consumes(APPLICATION_OCTET_STREAM)
	void addOutput(@PathParam("projectId") String projectId,
			@PathParam("id") int id,
			@QueryParam("outputFilename") String output, InputStream input);

	/**
	 * Mark the job as successfully finished.
	 *
	 * @param projectId
	 *            The ID of the project owning the job.
	 * @param id
	 *            The id of the job.
	 * @param logToAppend
	 *            The job log data.
	 * @param baseFilename
	 *            The base of filenames.
	 * @param outputs
	 *            The list of output files.
	 */
	@POST
	@Path("{projectId}/{id}/finished")
	@Consumes(TEXT_PLAIN)
	void setJobFinished(@PathParam("projectId") String projectId,
			@PathParam("id") int id, String logToAppend,
			@QueryParam("baseFilename") String baseFilename,
			@QueryParam("outputFilename") List<String> outputs);

	/**
	 * Mark the job as finished with an error.
	 *
	 * @param projectId
	 *            The project owning the job.
	 * @param id
	 *            The id of the job.
	 * @param error
	 *            The error message.
	 * @param logToAppend
	 *            The job log data.
	 * @param baseFilename
	 *            The base of filenames.
	 * @param outputs
	 *            The list of output files.
	 * @param stackTrace
	 *            The stack trace of the exception that caused the error.
	 */
	@POST
	@Path("{projectId}/{id}/error")
	@Consumes(APPLICATION_JSON)
	void setJobError(@PathParam("projectId") String projectId,
			@PathParam("id") int id, @QueryParam("error") String error,
			@QueryParam("logToAppend") String logToAppend,
			@QueryParam("baseFilename") String baseFilename,
			@QueryParam("outputFilename") List<String> outputs,
			RemoteStackTrace stackTrace);

	/**
	 * Get the implementation code of the Job Process Manager.
	 *
	 * @return a response containing the ZIP file.
	 */
	@GET
	@Path(JOB_PROCESS_MANAGER_ZIP)
	@Produces(APPLICATION_ZIP)
	Response getJobProcessManager();

	/**
	 * Get the setup script to be executed.
	 *
	 * @return a response containing the setup script
	 * @throws IOException if something goes wrong
	 */
	@GET
	@Path(SETUP_SCRIPT)
	@Produces(APPLICATION_OCTET_STREAM)
	Response getSetupScript() throws IOException;
}
