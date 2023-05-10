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

/**
 * A custom command not part of the standard protocol. <em>These are not
 * guaranteed to be accepted by any spalloc service.</em>
 */
public abstract non-sealed class CustomIntCommand extends Command<Integer> {
	/**
	 * Create a command.
	 *
	 * @param name
	 *            The name of the command.
	 * @param args
	 *            The integer positional arguments.
	 */
	public CustomIntCommand(String name, int... args) {
		super(name);
		for (int arg : args) {
			addArg(arg);
		}
	}
}