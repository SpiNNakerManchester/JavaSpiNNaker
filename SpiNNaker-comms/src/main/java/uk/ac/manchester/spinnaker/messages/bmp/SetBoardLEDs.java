/*
 * Copyright (c) 2018 The University of Manchester
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
package uk.ac.manchester.spinnaker.messages.bmp;

import static uk.ac.manchester.spinnaker.messages.scp.SCPCommand.CMD_LED;

import java.nio.ByteBuffer;
import java.util.Collection;

import uk.ac.manchester.spinnaker.machine.board.BMPBoard;
import uk.ac.manchester.spinnaker.messages.model.LEDAction;
import uk.ac.manchester.spinnaker.messages.scp.SetLED;
import uk.ac.manchester.spinnaker.utils.UsedInJavadocOnly;

/**
 * A request to alter the LEDs on a board managed by a BMP. There is no response
 * payload. Note that these LEDs are different to those managed by sending
 * {@linkplain SetLED a request to SCAMP}.
 * <p>
 * Handled by {@code cmp_led()} in {@code bmp_cmd.c}.
 */
@UsedInJavadocOnly(SetLED.class)
public class SetBoardLEDs extends BMPRequest<BMPRequest.BMPResponse> {
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
	public SetBoardLEDs(Collection<Integer> leds, LEDAction action,
			Collection<BMPBoard> boards) {
		super(boards, CMD_LED, argument1(action, leds), argument2(boards));
	}

	private static int argument1(LEDAction action, Collection<Integer> leds) {
		return leds.stream().mapToInt(led -> action.value << (led * 2)).sum();
	}

	private static int argument2(Collection<BMPBoard> boards) {
		return boards.stream().mapToInt(board -> 1 << board.board()).sum();
	}

	@Override
	public BMPResponse getSCPResponse(ByteBuffer buffer) throws Exception {
		return new BMPResponse("Set the LEDs of a board", CMD_LED, buffer);
	}
}
