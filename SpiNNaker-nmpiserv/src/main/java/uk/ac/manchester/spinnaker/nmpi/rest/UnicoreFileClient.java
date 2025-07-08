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

import static com.fasterxml.jackson.databind.PropertyNamingStrategies.SNAKE_CASE;

import java.io.InputStream;
import java.util.List;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.WebApplicationException;

import org.apache.cxf.jaxrs.client.JAXRSClientFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.jakarta.rs.json.JacksonJsonProvider;

/**
 * An interface to the UNICORE storage REST API.
 */
@Path("/storages")
public interface UnicoreFileClient {
	/**
	 * Upload a file.
	 *
	 * @param authHeader
	 *            The authorization header to authenticate with.
	 * @param id
	 *            The id of the storage on the server.
	 * @param filePath
	 *            The path at which to store the file (directories are
	 *            automatically created).
	 * @param input
	 *            The input stream containing the file to upload.
	 * @throws WebApplicationException
	 *             If anything goes wrong.
	 */
	@PUT
	@Path("{id}/files/{filePath}")
	@Consumes("application/octet-stream")
	void upload(@HeaderParam("Authorization") String authHeader,
			@PathParam("id") String id,
			@PathParam("filePath") String filePath, InputStream input)
			throws WebApplicationException;

	/**
	 * Get a client for the API.
	 *
	 * @param url
	 *            The URL to connect to.
	 * @return A proxy of the API.
	 */
	static UnicoreFileClient createClient(String url) {
		var mapper = new ObjectMapper();
		mapper.setPropertyNamingStrategy(SNAKE_CASE);
		return JAXRSClientFactory.create(url, UnicoreFileClient.class,
				List.of(new JacksonJsonProvider()));
	}
}
