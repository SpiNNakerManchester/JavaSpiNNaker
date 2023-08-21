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

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

/**
 * A deserialiser which deserialises classes based on unique properties that
 * they have. The classes to be deserialised need to be registered with a unique
 * property using the "register" function.
 *
 * @param <T>
 *      The type of values being deserialised.
 */
public class PropertyBasedDeserialiser<T> extends StdDeserializer<T> {
	private static final long serialVersionUID = 1L;

	/**
	 * The registry of known elements.
	 */
	private final Map<String, Class<? extends T>> registry = new HashMap<>();

	/**
	 * Creates a new deserialiser.
	 *
	 * @param type
	 *          The (super)class of the values that will be produced.
	 */
	public PropertyBasedDeserialiser(final Class<T> type) {
		super(type);
	}

	/**
	 * Registers a type against a property in the deserialiser.
	 *
	 * @param propertyName
	 *            The name of the unique property that identifies the class.
	 *            This is the JSON name.
	 * @param type
	 *            The class to register against the property.
	 * @throws IllegalArgumentException
	 *             if one of the parameters is null.
	 */
	public void register(final String propertyName,
			final Class<? extends T> type) {
		if (isNull(propertyName)) {
			throw new IllegalArgumentException("propertyName must be non-null");
		}
		if (isNull(type)) {
			throw new IllegalArgumentException("type must be non-null");
		}

		registry.put(propertyName, type);
	}

	@Override
	public T deserialize(final JsonParser parser,
			final DeserializationContext context)
			throws IOException, JsonProcessingException {
		final var root = parser.readValueAsTree();
		final var elementsIterator = root.fieldNames();
		while (elementsIterator.hasNext()) {
			var c = registry.get(elementsIterator.next());
			if (nonNull(c)) {
				return parser.getCodec().treeToValue(root, c);
			}
		}
		return null;
	}
}
