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
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

import java.util.List;
import java.util.Map;

import javax.ws.rs.Consumes;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

import org.apache.cxf.jaxrs.client.JAXRSClientFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;

import uk.ac.manchester.spinnaker.nmpi.model.Icinga2CheckResult;

/**
 * Interface to the Icinga2 API for status monitoring.
 */
public interface Icinga2 {
	/**
	 * Update the status of a service or host.
	 *
	 * @param authHeader
	 *            The authorization header value to authenticate with
	 * @param result
	 *            The result of a status check to update with.
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
	 *
	 * @param url
	 *            The URL to connect to.
	 * @return A proxy of the API.
	 */
	static Icinga2 createClient(String url) {
		var mapper = new ObjectMapper();
		mapper.setPropertyNamingStrategy(SNAKE_CASE);
		return JAXRSClientFactory.create(url, Icinga2.class,
				List.of(new JacksonJsonProvider()));
	}
}
