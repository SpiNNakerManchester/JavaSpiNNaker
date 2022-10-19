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
		if (!parser.get().nextToken().isNumeric()) {
			context.get().handleUnexpectedToken(int.class, parser.get());
		}
		return parser.get().getIntValue();
	}

	String getNextFieldName() throws IOException {
		String name = parser.get().nextFieldName();
		if (name == null) {
			requireEndOfStruct();
		}
		return name;
	}

	void requireEndOfStruct() throws IOException {
		if (!parser.get().nextToken().isStructEnd()) {
			context.get().handleUnexpectedToken(_valueClass, parser.get());
		}
	}

	int requireSetOnceInt(String name, Integer current) throws IOException {
		if (current != null) {
			context.get().handleUnknownProperty(parser.get(), this, _valueClass,
					name);
		}
		return parser.get().nextIntValue(0);
	}

	void unknownProperty(String name) throws IOException {
		context.get().handleUnknownProperty(parser.get(), this, _valueClass,
				name);
	}

	void missingProperty() throws IOException {
		context.get().handleUnexpectedToken(_valueClass, parser.get());
	}
}
