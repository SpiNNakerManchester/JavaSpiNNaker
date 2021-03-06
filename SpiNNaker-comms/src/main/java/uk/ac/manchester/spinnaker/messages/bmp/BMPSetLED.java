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
package uk.ac.manchester.spinnaker.messages.bmp;

import static uk.ac.manchester.spinnaker.messages.scp.SCPCommand.CMD_LED;

import java.nio.ByteBuffer;
import java.util.Collection;

import uk.ac.manchester.spinnaker.messages.model.LEDAction;

/**
 * A request to alter the LEDs on a board.
 */
public class BMPSetLED extends BMPRequest<BMPRequest.BMPResponse> {
	/**
	 * Make a request.
	 *
	 * @param leds
	 *            What LEDs to alter
	 * @param action
	 *            What operation to apply to them
	 * @param boards
	 *            The boards to talk to
	 */
	public BMPSetLED(Collection<Integer> leds, LEDAction action,
			Collection<Integer> boards) {
		super(boards, CMD_LED, argument1(action, leds), argument2(boards));
	}

	private static int argument1(LEDAction action, Collection<Integer> leds) {
		return leds.stream().mapToInt(led -> action.value << (led * 2)).sum();
	}

	private static int argument2(Collection<Integer> boards) {
		return boards.stream().mapToInt(board -> 1 << board).sum();
	}

	@Override
	public BMPResponse getSCPResponse(ByteBuffer buffer) throws Exception {
		return new BMPResponse("Set the LEDs of a board", CMD_LED, buffer);
	}
}
