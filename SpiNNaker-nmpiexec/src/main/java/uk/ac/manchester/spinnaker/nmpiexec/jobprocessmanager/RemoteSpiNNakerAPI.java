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
package uk.ac.manchester.spinnaker.nmpiexec.jobprocessmanager;

import static jakarta.ws.rs.core.HttpHeaders.AUTHORIZATION;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Objects.nonNull;
import static org.apache.commons.codec.binary.Base64.encodeBase64String;

import java.util.concurrent.TimeUnit;

import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.joda.JodaModule;
import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;

import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.ClientRequestFilter;
import uk.ac.manchester.spinnaker.nmpi.model.job.JobManagerInterface;

/**
 * Manufactures communication proxies for the main service web site.
 */
public abstract class RemoteSpiNNakerAPI {

	/**
	 * Makes this impossible to instantiate.
	 */
	private RemoteSpiNNakerAPI() {
	}

	/**
	 * The timeout of socket operation in seconds.
	 */
	private static final long TIMEOUT = 60;

	private static ResteasyClientBuilder clientBuilder() {
		return (ResteasyClientBuilder) ClientBuilder.newBuilder();
	}

	/**
	 * How to talk to the main web site.
	 *
	 * @param url
	 *            Where the main web site is located.
	 * @param authToken
	 *            How to authenticate to the main web site, or {@code null} to
	 *            not provide authorisation. If given, Should be the
	 *            concatenation of the username, a colon ({@code :}), and the
	 *            password.
	 * @return the proxy for the job manager service
	 */
	public static JobManagerInterface createJobManager(final String url,
			final String authToken) {
		final var builder = clientBuilder()
				.connectTimeout(TIMEOUT, TimeUnit.SECONDS)
				.readTimeout(TIMEOUT, TimeUnit.SECONDS);
		// TODO Add HTTPS trust store, etc.
		final var client = builder.build();
		var provider = new JacksonJsonProvider();
		var mapper = new ObjectMapper();
		mapper.registerModule(new JodaModule());
		provider.setMapper(mapper);
		client.register(provider);
		if (nonNull(authToken)) {
			client.register(getBasicAuthFilter(authToken));
		}
		return client.target(url).proxy(JobManagerInterface.class);
	}

	/**
	 * Creates a filter for BASIC authorisation.
	 *
	 * @param authToken The token for authorisation
	 *     (encoded BASE64 username{@code :}password)
	 * @return The filter
	 */
	private static ClientRequestFilter
			getBasicAuthFilter(final String authToken) {
		final var payload = "Basic "
				+ encodeBase64String(authToken.getBytes(UTF_8));
		return requestContext -> {
			requestContext.getHeaders().add(AUTHORIZATION, payload);
		};
	}
}
