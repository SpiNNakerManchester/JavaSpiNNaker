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

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;

import org.apache.cxf.jaxrs.client.JAXRSClientFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.jakarta.rs.json.JacksonJsonProvider;

import uk.ac.manchester.spinnaker.nmpi.model.QueueEmpty;
import uk.ac.manchester.spinnaker.nmpi.model.QueueJob;
import uk.ac.manchester.spinnaker.nmpi.model.QueueJobCompat;
import uk.ac.manchester.spinnaker.nmpi.model.QueueNextResponse;
import uk.ac.manchester.spinnaker.nmpi.rest.utils.CustomJacksonJsonProvider;
import uk.ac.manchester.spinnaker.nmpi.rest.utils.PropertyBasedDeserialiser;

/**
 * The REST API for the HBP Neuromorphic Platform Interface queue.
 */
public interface NMPIQueue {
	/**
	 * Gets all jobs in the queue.
	 *
	 * @param apiKey
	 *            The API key to use.
	 * @param hardware
	 *            The hardware to request the jobs for.
	 * @param status
	 *            List of accepted statuses.
	 * @return The list of jobs that meet the criteria.
	 */
	@GET
	@Path("jobs/")
	@Produces("application/json")
	List<QueueJobCompat> getJobs(@HeaderParam("x-api-key") String apiKey,
			@QueryParam("hardware") String hardware,
			@QueryParam("status") List<String> status);

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
	 * Update the log of a job.
	 *
	 * @param apiKey
	 *            The API key to use.
	 * @param id
	 *            The queue ID
	 * @param log
	 *            the Job Log.
	 */
	@PUT
	@Path("jobs/{id}")
	@Consumes("application/json")
	void updateJobLog(@HeaderParam("x-api-key") String apiKey,
			@PathParam("id") int id, JobLogOnly log);

	/**
	 * Update the status of a job.
	 *
	 * @param apiKey
	 *            The API key to use.
	 * @param id
	 *            The queue ID
	 * @param status
	 *            the Job Status.
	 */
	@PUT
	@Path("jobs/{id}")
	@Consumes("application/json")
	void updateJobStatus(@HeaderParam("x-api-key") String apiKey,
			@PathParam("id") int id, JobStatusOnly status);

	/**
	 * Set a job when done.
	 *
	 * @param apiKey
	 *            The API key to use.
	 * @param id
	 *            The queue ID
	 * @param job
	 *            The details to update
	 */
	@PUT
	@Path("jobs/{id}")
	@Consumes("application/json")
	void finishJob(@HeaderParam("x-api-key") String apiKey,
			@PathParam("id") int id, JobDone job);

	/**
	 * Get a client for the API.
	 *
	 * @param url
	 *            The URL to connect to.
	 * @return A proxy of the API.
	 */
	static NMPIQueue createClient(String url) {
		var mapper = new ObjectMapper();
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
		register("id", QueueJob.class);
		register("warning", QueueEmpty.class);
	}
}
