/*
 * Copyright (c) 2022 The University of Manchester
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
		var name = p.nextFieldName();
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
