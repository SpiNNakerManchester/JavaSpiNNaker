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
