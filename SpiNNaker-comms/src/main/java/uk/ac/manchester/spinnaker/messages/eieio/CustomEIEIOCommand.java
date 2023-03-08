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
package uk.ac.manchester.spinnaker.messages.eieio;

/**
 * A non-standard EIEIO command. Note that its constructor is not exposed.
 *
 * @see EIEIOCommandID#get(int)
 * @author Donal Fellows
 */
public class CustomEIEIOCommand implements EIEIOCommand {
	// Must be power of 2 (minus 1)
	private static final int MAX_COMMAND = 0x3FFF;

	private final int command;

	/**
	 * @param command
	 *            The ID value of the command.
	 */
	CustomEIEIOCommand(int command) {
		if (command < 0 || command > MAX_COMMAND) {
			throw new IllegalArgumentException(
					"parameter command is outside the allowed range (0 to "
							+ MAX_COMMAND + ")");
		}
		this.command = command;
	}

	@Override
	public int getValue() {
		return command;
	}
}
