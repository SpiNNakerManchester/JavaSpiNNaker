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
package uk.ac.manchester.spinnaker.messages.scp;

import static uk.ac.manchester.spinnaker.messages.scp.SCPCommand.CMD_LED;

import java.util.Map;
import java.util.Map.Entry;

import uk.ac.manchester.spinnaker.machine.HasCoreLocation;
import uk.ac.manchester.spinnaker.messages.model.LEDAction;

/** A request to change the state of a per-chip LED. */
public class SetLED extends SimpleRequest {
	/**
	 * @param core
	 *            The SpiNNaker core that will set the per-chip LED
	 * @param ledStates
	 *            A mapping of LED index to operation to apply.
	 */
	public SetLED(HasCoreLocation core, Map<Integer, LEDAction> ledStates) {
		super("Set LED", core, CMD_LED, argument1(ledStates));
	}

	private static Integer argument1(Map<Integer, LEDAction> ledStates) {
		int encoded = 0;
		for (Entry<Integer, LEDAction> e : ledStates.entrySet()) {
			encoded |= e.getValue().value << (2 * e.getKey());
		}
		return encoded;
	}
}
