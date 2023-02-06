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

import java.nio.ByteBuffer;
import java.util.Map;

import uk.ac.manchester.spinnaker.machine.HasCoreLocation;
import uk.ac.manchester.spinnaker.messages.model.LEDAction;
import uk.ac.manchester.spinnaker.messages.model.UnexpectedResponseCodeException;

/**
 * An SCP request to change the state of an LED. There is no response payload.
 * <p>
 * Handled by {@code sark_led_set()} in {@code sark_hw.c}.
 */
public class SetLED extends SCPRequest<EmptyResponse> {
	/**
	 * @param core
	 *            The SpiNNaker core that will set the BMPSetLED
	 * @param ledStates
	 *            A mapping of BMPSetLED index to operation to apply.
	 */
	public SetLED(HasCoreLocation core, Map<Integer, LEDAction> ledStates) {
		super(core, CMD_LED, leds(ledStates));
	}

	private static Integer leds(Map<Integer, LEDAction> ledStates) {
		int encoded = 0;
		for (var e : ledStates.entrySet()) {
			encoded |= e.getValue().value << (2 * e.getKey());
		}
		return encoded;
	}

	@Override
	public EmptyResponse getSCPResponse(ByteBuffer buffer)
			throws UnexpectedResponseCodeException {
		return new EmptyResponse("Set LED", CMD_LED, buffer);
	}
}
