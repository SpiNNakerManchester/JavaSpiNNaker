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
package uk.ac.manchester.spinnaker.nmpi.rest.utils;

import static com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES;
import static com.fasterxml.jackson.databind.PropertyNamingStrategies.SNAKE_CASE;
import static jakarta.ws.rs.core.MediaType.WILDCARD;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.HashSet;
import java.util.Set;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.ext.Provider;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.joda.JodaModule;
import com.fasterxml.jackson.jakarta.rs.json.JacksonJsonProvider;

/**
 * Extended JSON serialisation handler.
 */
@Provider
@Consumes(WILDCARD)
@Produces(WILDCARD)
public class CustomJacksonJsonProvider extends JacksonJsonProvider {
	/** Mapper objects of this provider. */
	private final Set<ObjectMapper> registeredMappers = new HashSet<>();

	/** The module of the provider. */
	private final SimpleModule module = new SimpleModule();

	/** The date-time module of the provider. */
	private final JodaModule jodaModule = new JodaModule();

	/**
	 * Add a deserialiser for a specific type.
	 *
	 * @param <T>
	 *            The type that will be deserialised.
	 * @param type
	 *            The type.
	 * @param deserialiser
	 *            The deserialiser.
	 */
	public <T> void addDeserialiser(final Class<T> type,
			final StdDeserializer<T> deserialiser) {
		module.addDeserializer(type, deserialiser);
	}

	/**
	 * Register a new mapper.
	 *
	 * @param type
	 *            The class of the mapper
	 * @param mediaType
	 *            The media type to handle
	 */
	private void registerMapper(final Class<?> type,
			final MediaType mediaType) {
		final var mapper = locateMapper(type, mediaType);
		if (!registeredMappers.contains(mapper)) {
			mapper.registerModule(module);
			mapper.setPropertyNamingStrategy(SNAKE_CASE);
			mapper.configure(FAIL_ON_UNKNOWN_PROPERTIES, false);
			mapper.registerModule(jodaModule);
			registeredMappers.add(mapper);
		}
	}

	@Override
	public Object readFrom(final Class<Object> type, final Type genericType,
			final Annotation[] annotations, final MediaType mediaType,
			final MultivaluedMap<String, String> httpHeaders,
			final InputStream entityStream) throws IOException {
		registerMapper(type, mediaType);
		return super.readFrom(type, genericType, annotations, mediaType,
				httpHeaders, entityStream);
	}

	@Override
	public void writeTo(final Object value, final Class<?> type,
			final Type genericType, final Annotation[] annotations,
			final MediaType mediaType,
			final MultivaluedMap<String, Object> httpHeaders,
			final OutputStream entityStream) throws IOException {
		registerMapper(type, mediaType);
		super.writeTo(value, type, genericType, annotations, mediaType,
				httpHeaders, entityStream);
	}
}
