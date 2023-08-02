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

import static com.fasterxml.jackson.databind.PropertyNamingStrategies.SNAKE_CASE;
import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;

import java.util.List;
import java.util.Map;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;

import org.apache.cxf.jaxrs.client.JAXRSClientFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.jakarta.rs.json.JacksonJsonProvider;

import uk.ac.manchester.spinnaker.nmpi.model.Icinga2CheckResult;

/**
 * Interface to the Icinga2 API for status monitoring.
 */
public interface Icinga2 {

	/**
	 * Update the status of a service or host.
	 *
	 * @param authHeader The authorization header value to authenticate with
	 * @param result The result of a status check to update with.
	 * @return The response from the server as a String.
	 */
	@Produces(APPLICATION_JSON)
	@Consumes(APPLICATION_JSON)
	@POST
	@Path("/v1/actions/process-check-result")
	Map<String, Object> processCheckResult(
			@HeaderParam("Authorization") String authHeader,
			Icinga2CheckResult result);

	/**
	 * Get a client for the API.
	 * @param url The URL to connect to.
	 * @return A proxy of the API.
	 */
	static Icinga2 createClient(String url) {
		ObjectMapper mapper = new ObjectMapper();
		mapper.setPropertyNamingStrategy(SNAKE_CASE);
		return JAXRSClientFactory.create(url, Icinga2.class,
				List.of(new JacksonJsonProvider()));
	}
}
