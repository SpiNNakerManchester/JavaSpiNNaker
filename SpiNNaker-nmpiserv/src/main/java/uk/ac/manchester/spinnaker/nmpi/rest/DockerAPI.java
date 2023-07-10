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

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

import org.apache.cxf.jaxrs.client.JAXRSClientFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;

/**
 * Interface to the Docker API.
 */
public interface DockerAPI {

	final String APPLICATION_VND_DOCKER_RAW_STREAM =
			"application/vnd.docker.raw-stream";

	@Produces(APPLICATION_JSON)
	@Consumes(APPLICATION_JSON)
	@POST
	@Path("/containers/create")
	DockerCreateResponse create(DockerCreateRequest request);

	@POST
	@Path("/containers/{id}/start")
	void start(@PathParam("id") String id);

	@POST
	@Path("/containers/{id}/wait")
	void wait(@PathParam("id") String id);

	@DELETE
	@Path("/containers/{id}")
	void delete(@PathParam("id") String id);

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

	static String readLog(byte[] data) throws IOException {
		DataInputStream input = new DataInputStream(new ByteArrayInputStream(data));
		StringBuilder output = new StringBuilder();
		try {
			while (true) {
				input.readFully(new byte[4]);
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
