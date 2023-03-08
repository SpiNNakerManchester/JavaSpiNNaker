/*
 * Copyright (c) 2018 The University of Manchester
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
package uk.ac.manchester.spinnaker.spalloc.messages;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A serialisable request to spalloc. This is modelled on the calling convention
 * used by Python.
 *
 * @param <A>
 *            The type of arguments.
 */
public abstract class Command<A> {
	private final String command;

	private final List<A> args = new ArrayList<>();

	private final Map<String, Object> kwargs = new HashMap<>();

	/**
	 * Add to the keyword arguments part.
	 *
	 * @param key
	 *            The keyword
	 * @param value
	 *            The argument value; will be converted to a string
	 */
	protected final void addKwArg(String key, Object value) {
		kwargs.put(key, value);
	}

	/**
	 * Add to the positional arguments part.
	 *
	 * @param values
	 *            The arguments to add.
	 */
	@SafeVarargs
	protected final void addArg(A... values) {
		for (final A value : values) {
			args.add(value);
		}
	}

	/**
	 * Create a command.
	 *
	 * @param command
	 *            The command token.
	 */
	public Command(String command) {
		this.command = command;
	}

	/** @return The command token. */
	public String getCommand() {
		return command;
	}

	/** @return The positional arguments to the command. */
	public List<A> getArgs() {
		return args;
	}

	/** @return The keyword arguments to the command. */
	public Map<String, Object> getKwargs() {
		return kwargs;
	}
}
