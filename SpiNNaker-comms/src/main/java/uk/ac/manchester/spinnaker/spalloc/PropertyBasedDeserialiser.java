/*
 * Copyright (c) 2018 The University of Manchester
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package uk.ac.manchester.spinnaker.spalloc;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

/**
 * A deserialiser which deserialises classes based on unique properties that
 * they have. The classes to be deserialised need to be registered with a unique
 * property using the "register" function.
 *
 * @param <T>
 *            The type of values being deserialised.
 */
class PropertyBasedDeserialiser<T> extends StdDeserializer<T> {
	private static final long serialVersionUID = 1L;

	private final Map<String, Class<? extends T>> registry = new HashMap<>();

	/**
	 * Creates a new deserialiser.
	 *
	 * @param type
	 *            The (super)class of the values that will be produced.
	 */
	PropertyBasedDeserialiser(Class<T> type) {
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
	 *             if given bad arguments
	 */
	protected void register(String propertyName, Class<? extends T> type) {
		if (isNull(propertyName)) {
			throw new IllegalArgumentException("propertyName must be non-null");
		}
		if (isNull(type)) {
			throw new IllegalArgumentException("type must be non-null");
		}

		registry.put(propertyName, type);
	}

	/**
	 * Look up what class we might deserialize as.
	 *
	 * @param elementNames
	 *            The element names available to us.
	 * @return The class, or {@code null} if nothing matches (i.e., if we've
	 *         gone through the iterator and had no matches).
	 */
	private Class<? extends T> getTargetClass(Iterator<String> elementNames) {
		while (elementNames.hasNext()) {
			var cls = registry.get(elementNames.next());
			if (nonNull(cls)) {
				return cls;
			}
		}
		return null;
	}

	/**
	 * Deserialize a JSON object as a Java object, using the names of the
	 * properties of the JSON object to work out what sort of Java object to
	 * return.
	 *
	 * @param parser
	 *            The parser to get the JSON object from.
	 * @param context
	 *            ignored
	 * @return A Java object (probably a POJO), or {@code null} if the
	 *         properties of the JSON object don't identify which Java class we
	 *         should use for deserialization.
	 * @throws IOException
	 *             If the deserialization fails.
	 */
	@Override
	public T deserialize(JsonParser parser, DeserializationContext context)
			throws IOException {
		var root = parser.readValueAsTree();
		var cls = getTargetClass(root.fieldNames());
		if (isNull(cls)) {
			return null;
		}
		return parser.getCodec().treeToValue(root, cls);
	}
}
