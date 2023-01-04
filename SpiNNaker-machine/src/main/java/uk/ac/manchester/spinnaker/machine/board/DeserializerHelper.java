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
import com.fasterxml.jackson.databind.DeserializationConfig;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.google.errorprone.annotations.ForOverride;
import com.google.errorprone.annotations.FormatMethod;

import uk.ac.manchester.spinnaker.utils.ValueHolder;

/**
 * A helper class for JSON deserializers.
 *
 * @param <T>
 *            The type of values the deserializer deserializes.
 */
@SuppressWarnings("serial")
abstract class DeserializerHelper<T> extends StdDeserializer<T> {
	private static final ThreadLocal<DeserializationContext> CONTEXT =
			new ThreadLocal<>();

	private static final ThreadLocal<JsonParser> PARSER = new ThreadLocal<>();

	protected DeserializerHelper(Class<T> cls) {
		super(cls);
	}

	@Override
	public final T deserialize(JsonParser p, DeserializationContext ctxt)
			throws IOException {
		try {
			CONTEXT.set(ctxt);
			PARSER.set(p);
			return switch (p.currentToken()) {
			case START_ARRAY -> deserializeArray();
			case START_OBJECT -> deserializeObject();
			case VALUE_STRING -> deserializeString(p.getValueAsString());
			default -> {
				ctxt.handleUnexpectedToken(_valueClass, p);
				yield null;
			}
			};
		} finally {
			CONTEXT.remove();
			PARSER.remove();
		}
	}

	/**
	 * Deserialize an array to an instance.
	 *
	 * @return The instance
	 * @throws IOException
	 *             On failure
	 */
	@ForOverride
	abstract T deserializeArray() throws IOException;

	/**
	 * Deserialize an object to an instance.
	 *
	 * @return The instance
	 * @throws IOException
	 *             On failure
	 */
	@ForOverride
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
	@ForOverride
	abstract T deserializeString(String string);

	/**
	 * Throw an exception because of an unexpected token.
	 *
	 * @param cls
	 *            What we were trying to instantiate.
	 * @throws IOException
	 *             The exception <em>always</em> thrown.
	 */
	private void unexpectedToken(Class<?> cls) throws IOException {
		CONTEXT.get().handleUnexpectedToken(cls, PARSER.get());
	}

	/**
	 * Throw an exception because of an input mismatch.
	 *
	 * @param msg
	 *            Used to describe the problem.
	 * @param args
	 *            Values to substitute in.
	 * @throws IOException
	 *             The exception <em>always</em> thrown.
	 */
	@FormatMethod
	private void inputMismatch(String msg, Object... args)
			throws IOException {
		CONTEXT.get().reportInputMismatch(_valueClass, msg, args);
	}

	/**
	 * Throw an exception because of an unknown property.
	 *
	 * @param name
	 *            The unknown property name.
	 * @throws IOException
	 *             The exception <em>always</em> thrown.
	 */
	void unknownProperty(String name) throws IOException {
		CONTEXT.get().handleUnknownProperty(PARSER.get(), this, _valueClass,
				name);
	}

	int getNextIntOfArray() throws IOException {
		var p = PARSER.get();
		if (!p.nextToken().isNumeric()) {
			unexpectedToken(int.class);
		}
		return p.getIntValue();
	}

	String getNextFieldName() throws IOException {
		var p = PARSER.get();
		String name = p.nextFieldName();
		if (name == null) {
			if (!p.currentToken().isStructEnd()) {
				unexpectedToken(_valueClass);
			}
		}
		return name;
	}

	void requireEndOfArray() throws IOException {
		var p = PARSER.get();
		if (!p.nextToken().isStructEnd()) {
			unexpectedToken(_valueClass);
		}
	}

	int requireSetOnceInt(String name, Integer current) throws IOException {
		if (current != null) {
			inputMismatch("Duplicate property '%s'", name);
		}
		return PARSER.get().nextIntValue(0);
	}

	void requireSetOnceInt(String name, ValueHolder<Integer> holder)
			throws IOException {
		if (!holder.isEmpty()) {
			inputMismatch("Duplicate property '%s'", name);
		}
		holder.setValue(PARSER.get().nextIntValue(0));
	}

	void missingProperty(String n1, Object v1) throws IOException {
		if (v1 == null) {
			inputMismatch("Missing required property '%s'", n1);
		}
	}

	void missingProperty(String n1, Object v1, String n2, Object v2)
			throws IOException {
		if (v1 == null) {
			inputMismatch("Missing required property '%s'", n1);
		} else if (v2 == null) {
			inputMismatch("Missing required property '%s'", n2);
		}
	}

	void missingProperty(String n1, Object v1, String n2, Object v2, String n3,
			Object v3) throws IOException {
		if (v1 == null) {
			inputMismatch("Missing required property '%s'", n1);
		} else if (v2 == null) {
			inputMismatch("Missing required property '%s'", n2);
		} else if (v3 == null) {
			inputMismatch("Missing required property '%s'", n3);
		}
	}

	@Override
	public final Boolean supportsUpdate(DeserializationConfig config) {
		return Boolean.FALSE;
	}
}
