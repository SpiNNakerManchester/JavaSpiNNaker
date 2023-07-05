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

import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;

import org.apache.cxf.jaxrs.client.JAXRSClientFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;

import uk.ac.manchester.spinnaker.nmpi.model.NMPILog;
import uk.ac.manchester.spinnaker.nmpi.model.job.nmpi.Job;
import uk.ac.manchester.spinnaker.nmpi.model.job.nmpi.QueueEmpty;
import uk.ac.manchester.spinnaker.nmpi.model.job.nmpi.QueueNextResponse;
import uk.ac.manchester.spinnaker.nmpi.rest.utils.CustomJacksonJsonProvider;
import uk.ac.manchester.spinnaker.nmpi.rest.utils.PropertyBasedDeserialiser;

/**
 * The REST API for the HBP Neuromorphic Platform Interface queue.
 */
public interface NMPIQueue {

	/**
	 * Get the next queue item for a specific hardware system.
	 *
	 * @param apiKey
	 *            The API key to use.
	 * @param hardware
	 *            The hardware ID.
	 * @return The queue item.
	 */
	@GET
	@Path("jobs/next/{hardware}")
	@Produces("application/json")
	QueueNextResponse getNextJob(@HeaderParam("x-api-key") String apiKey,
			@PathParam("hardware") String hardware);

	/**
	 * Update the status of a queue item.
	 *
	 * @param apiKey
	 *            The API key to use.
	 * @param id
	 *            The queue ID
	 * @param job
	 *            the Job document.
	 */
	@PUT
	@Path("jobs/{id}")
	@Consumes("application/json")
	void updateJob(@HeaderParam("x-api-key") String apiKey,
			@PathParam("id") int id, Job job);

	/**
	 * Get the queue status.
	 *
	 * @param apiKey
	 *            The API key to use.
	 * @param id
	 *            The queue ID
	 * @return The job on the queue.
	 */
	@GET
	@Path("jobs/{id}")
	@Produces("application/json")
	Job getJob(@HeaderParam("x-api-key") String apiKey,
			@PathParam("id") int id);

	/**
	 * Update the log.
	 *
	 * @param apiKey
	 *            The API key to use.
	 * @param id
	 *            The queue ID
	 * @param log
	 *            The log entry
	 */
	@PUT
	@Path("jobs/{id}/log")
	@Consumes("application/json")
	void updateLog(@HeaderParam("x-api-key") String apiKey,
			@PathParam("id") int id, NMPILog log);

	/**
	 * Get a client for the API.
	 * @param url The URL to connect to.
	 * @return A proxy of the API.
	 */
	static NMPIQueue createClient(String url) {
		ObjectMapper mapper = new ObjectMapper();
		mapper.setPropertyNamingStrategy(SNAKE_CASE);
		return JAXRSClientFactory.create(url, NMPIQueue.class,
				List.of(createProvider()));
	}

	/**
	 * Create a JSON provider capable of handling the messages on the NMPI
	 * queue.
	 *
	 * @return The provider.
	 */
	static JacksonJsonProvider createProvider() {
		var provider = new CustomJacksonJsonProvider();
		provider.addDeserialiser(QueueNextResponse.class,
				new NMPIQueueResponseDeserialiser());
		return provider;
	}
}

/**
 * How to understand messages coming from the queue.
 */
@SuppressWarnings("serial")
class NMPIQueueResponseDeserialiser
		extends PropertyBasedDeserialiser<QueueNextResponse> {
	/**
	 * Make a deserialiser.
	 */
	NMPIQueueResponseDeserialiser() {
		super(QueueNextResponse.class);
		register("id", Job.class);
		register("warning", QueueEmpty.class);
	}
}
