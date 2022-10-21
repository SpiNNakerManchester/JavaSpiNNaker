/*
 * Copyright (c) 2022 The University of Manchester
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
package uk.ac.manchester.spinnaker.machine.board;

import static com.fasterxml.jackson.core.JsonToken.VALUE_NUMBER_INT;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

/**
 * A helper class for JSON deserializers.
 *
 * @param <T>
 *            The type of values the deserializer deserializes.
 */
@SuppressWarnings("serial")
abstract class DeserializerHelper<T> extends StdDeserializer<T> {
	private final ThreadLocal<DeserializationContext> context =
			new ThreadLocal<>();

	private final ThreadLocal<JsonParser> parser = new ThreadLocal<>();

	protected DeserializerHelper(Class<T> cls) {
		super(cls);
	}

	@Override
	public final T deserialize(JsonParser p, DeserializationContext ctxt)
			throws IOException {
		try {
			this.context.set(ctxt);
			this.parser.set(p);
			switch (p.currentToken()) {
			case START_ARRAY:
				return deserializeArray();
			case START_OBJECT:
				return deserializeObject();
			case VALUE_STRING:
				return deserializeString(p.getValueAsString());
			default:
				ctxt.handleUnexpectedToken(_valueClass, p);
				return null;
			}
		} finally {
			this.context.remove();
			this.parser.remove();
		}
	}

	/**
	 * Deserialize an array to an instance.
	 *
	 * @return The instance
	 * @throws IOException
	 *             On failure
	 */
	abstract T deserializeArray() throws IOException;

	/**
	 * Deserialize an object to an instance.
	 *
	 * @return The instance
	 * @throws IOException
	 *             On failure
	 */
	abstract T deserializeObject() throws IOException;

	/**
	 * Deserialize a string to an instance.
	 *
	 * @param string
	 *            The string, already extracted.
	 * @return The instance
	 * @throws IllegalArgumentException
	 *             On failure
	 */
	abstract T deserializeString(String string);

	int getNextIntOfArray() throws IOException {
		var p = parser.get();
		if (!p.nextToken().isNumeric()) {
			context.get().handleUnexpectedToken(int.class, p);
		}
		return p.getIntValue();
	}

	String getNextFieldName() throws IOException {
		var p = parser.get();
		String name = p.nextFieldName();
		if (isNull(name)) {
			if (!p.currentToken().isStructEnd()) {
				context.get().handleUnexpectedToken(_valueClass, p);
			}
		}
		return name;
	}

	void requireEndOfArray() throws IOException {
		var p = parser.get();
		if (!p.nextToken().isStructEnd()) {
			context.get().handleUnexpectedToken(_valueClass, p);
		}
	}

	int requireSetOnceInt(String name, Integer current) throws IOException {
		if (nonNull(current)) {
			context.get().reportInputMismatch(_valueClass,
					"Duplicate property '%s'", name);
		}
		var p = parser.get();
		if (p.nextToken() != VALUE_NUMBER_INT) {
			context.get().handleUnexpectedToken(int.class, p);
		}
		return p.getIntValue();
	}

	void unknownProperty(String name) throws IOException {
		context.get().handleUnknownProperty(parser.get(), this, _valueClass,
				name);
	}

	<P> void checkMissingProperty(String n1, P v1) throws IOException {
		if (isNull(v1)) {
			context.get().reportInputMismatch(_valueClass,
					"Missing required property '%s'", n1);
		}
	}

	<P> void checkMissingProperty(String n1, P v1, String n2, P v2)
			throws IOException {
		if (isNull(v1)) {
			context.get().reportInputMismatch(_valueClass,
					"Missing required property '%s'", n1);
		} else if (isNull(v2)) {
			context.get().reportInputMismatch(_valueClass,
					"Missing required property '%s'", n2);
		}
	}

	<P> void checkMissingProperty(String n1, P v1, String n2, P v2, String n3,
			P v3) throws IOException {
		if (isNull(v1)) {
			context.get().reportInputMismatch(_valueClass,
					"Missing required property '%s'", n1);
		} else if (isNull(v2)) {
			context.get().reportInputMismatch(_valueClass,
					"Missing required property '%s'", n2);
		} else if (isNull(v3)) {
			context.get().reportInputMismatch(_valueClass,
					"Missing required property '%s'", n3);
		}
	}
}
