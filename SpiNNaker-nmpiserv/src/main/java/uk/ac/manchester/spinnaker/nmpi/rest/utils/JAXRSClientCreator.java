/*
 * Copyright (c) 2026 The University of Manchester
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
package uk.ac.manchester.spinnaker.nmpi.rest.utils;

import static org.apache.cxf.transports.http.configuration.ConnectionType.CLOSE;
import static com.fasterxml.jackson.databind.PropertyNamingStrategies.SNAKE_CASE;

import java.util.List;

import org.apache.cxf.jaxrs.client.ClientConfiguration;
import org.apache.cxf.jaxrs.client.JAXRSClientFactory;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.transport.http.HTTPConduit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.jakarta.rs.json.JacksonJsonProvider;

/**
 * Creates JAX-RS clients with default settings.
 */
public abstract class JAXRSClientCreator {

	/**
	 * Creates a JAX-RS client for the given interface and URL.
	 *
	 * @param url
	 *            The base URL of the REST service.
	 * @param clazz
	 *            The interface class of the REST service.
	 * @param <T>
	 *            The type of the REST service interface.
	 * @return A client proxy for the REST service.
	 */
	public static <T> T createClient(String url, Class<T> clazz,
			JacksonJsonProvider provider) {
		var proxy = JAXRSClientFactory.create(url, clazz,
				List.of(provider), true);
		ClientConfiguration config = WebClient.getConfig(proxy);
		HTTPConduit conduit = config.getHttpConduit();
		conduit.getClient().setConnection(CLOSE);
		return proxy;
	}

	public static <T> T createClient(String url, Class<T> clazz) {
		return createClient(url, clazz, new JacksonJsonProvider(
				new ObjectMapper().setPropertyNamingStrategy(SNAKE_CASE)));
	}
}
