/*
 * Copyright (c) 2023 The University of Manchester
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
package uk.ac.manchester.spinnaker.alloc.nmpi;

import static com.fasterxml.jackson.databind.PropertyNamingStrategies.SNAKE_CASE;

import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

import org.apache.cxf.jaxrs.client.JAXRSClientFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;

/**
 * The REST API for the NMPI Service.
 */
@Path("/")
public interface NMPIv3API {

	/**
     * Get an NMPI Job.
     *
     * @param apiKey The API key to authenticate with.
     * @param jobId The job id.
     * @return The job.
     */
    @GET
    @Path("jobs/{job_id}")
    @Produces("application/json")
    Job getJob(@HeaderParam("x-api-key") String apiKey,
    		@PathParam("job_id") int jobId);

    /**
     * Update the resources of a Job.
     *
     * @param apiKey The API key to authenticate with.
     * @param jobId The job id.
     * @param resources The resources to set.
     */
    @PUT
    @Path("jobs/{job_id}")
    @Consumes("application/josn")
    void setJobResources(@HeaderParam("x-api-key") String apiKey,
    		@PathParam("job_id") int jobId,
    		JobResourceUpdate resources);

    /**
     * Get a list of projects that match the parameters.
     *
     * @param auth The Authorization header value
     * @param status The status of the project to get.
     * @param collab The name of the collab of the project.
     * @return A list of matching projects.
     */
    @GET
    @Path("projects/")
    List<Project> getProjects(@HeaderParam("x-api-key") String apiKey,
    		@QueryParam("status") String status,
    		@QueryParam("collab") String collab);

    /**
     * Create a new session.
     *
     * @param apiKey The API key to authenticate with.
     * @param session The session details.
     * @return The created session details.
     */
    @POST
    @Path("sessions/")
    @Consumes("application/json")
    @Produces("application/json")
    SessionResponse createSession(@HeaderParam("x-api-key") String apiKey,
    		SessionRequest session);

    /**
     * Update a session.
     *
     * @param apiKey The API key to authenticate with.
     * @param sessionId The identifier of the session.
     * @param resources The resources to set.
     */
    @PUT
    @Path("sessions/{session_id}")
    @Consumes("application/json")
    void setSessionStatusAndResources(@HeaderParam("x-api-key") String apiKey,
    		@PathParam("session_id") int sessionId,
    		SessionResourceUpdate resources);

    /**
     * Get a client for the API.
     * @param url The URL to connect to.
     * @return A proxy of the API.
     */
    static NMPIv3API createClient(String url) {
    	ObjectMapper mapper = new ObjectMapper();
    	mapper.setPropertyNamingStrategy(SNAKE_CASE);
    	return JAXRSClientFactory.create(url, NMPIv3API.class,
    			List.of(new JacksonJsonProvider()));
    }
}
