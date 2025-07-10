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
package uk.ac.manchester.spinnaker.nmpi.rest;

import static jakarta.ws.rs.core.MediaType.MEDIA_TYPE_WILDCARD;
import static jakarta.ws.rs.core.MediaType.TEXT_PLAIN;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.List;

import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Response;

import uk.ac.manchester.spinnaker.nmpi.model.job.nmpi.DataItem;

/**
 * The REST API for the output manager.
 */
@Path("/output")
public interface OutputManager {
	/**
	 * Adds outputs to be hosted for a given id, returning a matching list of
	 * URLs on which the files are hosted.
	 *
	 * @param projectId
	 *            The id of the project
	 * @param id
	 *            The id of the job
	 * @param rootFile
	 *            The root directory containing all the files
	 * @param outputs
	 *            The files to add
	 * @return A list of DataItem instances for adding to the job
	 * @throws IOException
	 *            If anything goes wrong.
	 */
	List<DataItem> addOutputs(String projectId, int id, File rootFile,
			Collection<File> outputs) throws IOException;

	/**
	 * Gets a results file.
	 *
	 * @param projectId
	 *            The id of the project which owns the file.
	 * @param id
	 *            The id of the job which produced the file.
	 * @param filename
	 *            The name of the file.
	 * @param download
	 *            Whether to mark the retrieval as a download to the browser.
	 * @return A response containing the file, or a "NOT FOUND" response if the
	 *         file does not exist.
	 */
	// TODO: Enable authentication based on collab id
	// @PreAuthorize("@collabSecurityService.canRead(#projectId)")
	@GET
	@Path("{projectId}/{id}/{filename:.*}")
	@Produces(MEDIA_TYPE_WILDCARD)
	Response getResultFile(@PathParam("projectId") String projectId,
			@PathParam("id") int id, @PathParam("filename") String filename,
			@QueryParam("download") @DefaultValue("true") boolean download);

	/**
	 * Gets a results file.
	 *
	 * @param id
	 *            The id of the job which produced the file.
	 * @param filename
	 *            The name of the file.
	 * @param download
	 *            Whether to mark the retrieval as a download to the browser.
	 * @return A response containing the file, or a "NOT FOUND" response if the
	 *         file does not exist.
	 */
	@GET
	@Path("{id}/{filename:.*}")
	@Produces(MEDIA_TYPE_WILDCARD)
	Response getResultFile(@PathParam("id") int id,
			@PathParam("filename") String filename,
			@QueryParam("download") @DefaultValue("true") boolean download);

	/**
	 * Upload a file to the HPC store.
	 *
	 * @param projectId
	 *            The project ID
	 * @param id
	 *            The job ID
	 * @param serverUrl
	 *            The HPC storage service
	 * @param storageId
	 *            The ID for the storage on the HPC service
	 * @param filePath
	 *            The path within the storage
	 * @param userId
	 *            The HPC user ID
	 * @param token
	 *            The auth token
	 * @return Description of whether the upload was successful.
	 */
	@POST
	@Produces(TEXT_PLAIN)
	@Path("{projectId}/{id}/uploadToHPC")
	// TODO: Enable authentication based on collab id
	// @PreAuthorize("@collabSecurityService.canWrite(#projectId)")
	Response uploadResultsToHPCServer(@PathParam("projectId") String projectId,
			@PathParam("id") int id, @QueryParam("url") String serverUrl,
			@QueryParam("storageId") String storageId,
			@QueryParam("filePath") String filePath,
			@QueryParam("userId") String userId,
			@QueryParam("token") String token);
	// What is body of the POST? What is the type of the response?
}
