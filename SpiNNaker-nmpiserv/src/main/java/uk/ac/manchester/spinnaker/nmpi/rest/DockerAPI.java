/*
 * Copyright (c) 2020 The University of Manchester
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

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.util.List;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;

import org.apache.cxf.jaxrs.client.JAXRSClientFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.jakarta.rs.json.JacksonJsonProvider;

/**
 * Interface to the Docker API.
 */
public interface DockerAPI {

	/**
	 * The docker output stream format.
	 */
	String APPLICATION_VND_DOCKER_RAW_STREAM =
			"application/vnd.docker.raw-stream";

	/**
	 * The number of bytes in the log header that are unused.
	 */
	int HEADER_UNUSED_BYTES = 4;

	/**
	 * Options for waiting.
	 */
	enum WaitCondition {

		/**
		 * Wait for any not running state.
		 */
		NOT_RUNNING("not-running"),

		/**
		 * Wait for the next exit.
		 */
		NEXT_EXIT("next-exit"),

		/**
		 * Wait for the container to be removed.
		 */
		REMOVED("removed");

		private String value;

		WaitCondition(String value) {
			this.value = value;
		}

		@Override
		public String toString() {
			return value;
		}
	}

	/**
	 * Create a new Docker container.
	 *
	 * @param request The details of the container to create.
	 * @return The response to the container creation request.
	 */
	@Produces(APPLICATION_JSON)
	@Consumes(APPLICATION_JSON)
	@POST
	@Path("/containers/create")
	DockerCreateResponse create(DockerCreateRequest request);

	/**
	 * Start a Docker container.
	 *
	 * @param id The identifier of the container.
	 */
	@POST
	@Path("/containers/{id}/start")
	void start(@PathParam("id") String id);

	@GET
	@Produces(APPLICATION_JSON)
	@Path("/containers/{id}/json")
	DockerInspectResponse inspect(@PathParam("id") String id);

	/**
	 * Wait for a Docker container to exit.
	 *
	 * @param id The identifier of the container.
	 * @param condition What we are waiting for.
	 */
	@POST
	@Path("/containers/{id}/wait")
	void wait(@PathParam("id") String id,
			@QueryParam("condition") WaitCondition condition);

	/**
	 * Delete a Docker container.
	 *
	 * @param id The identifier of the container.
	 */
	@DELETE
	@Path("/containers/{id}")
	void delete(@PathParam("id") String id);

	/**
	 * Get the raw logs of the container.
	 *
	 * @param id The identifier of the container.
	 * @param stdout True if STDOUT should be read.
	 * @param stderr True if STDERR should be read.
	 * @return The raw data stream. See {@link #readLog} for how this is read.
	 */
	@Produces(APPLICATION_VND_DOCKER_RAW_STREAM)
	@GET
	@Path("/containers/{id}/logs")
	byte[] getLog(@PathParam("id") String id,
			@QueryParam("stdout") boolean stdout,
			@QueryParam("stderr") boolean stderr);

	/**
	 * Get a client for the API.
	 * @param url The URL to connect to.
	 * @return A proxy of the API.
	 */
	static DockerAPI createClient(String url) {
		ObjectMapper mapper = new ObjectMapper();
		return JAXRSClientFactory.create(url, DockerAPI.class,
				List.of(new JacksonJsonProvider(mapper)));
	}

	/**
	 * Read docker log raw format.
	 *
	 * @param data The data returned by {@link #getLog}.
	 * @return A decoded String containing all data in order received.
	 * @throws IOException If an error occurs when reading the log.
	 */
	static String readLog(byte[] data) throws IOException {
		DataInputStream input = new DataInputStream(
				new ByteArrayInputStream(data));
		StringBuilder output = new StringBuilder();
		try {
			while (true) {
				input.readFully(new byte[HEADER_UNUSED_BYTES]);
				int size = input.readInt();
				byte[] text = new byte[size];
				input.readFully(text);
				output.append(new String(text));
			}
		} catch (EOFException e) {
			input.close();
		}
		return output.toString();
	}
}
