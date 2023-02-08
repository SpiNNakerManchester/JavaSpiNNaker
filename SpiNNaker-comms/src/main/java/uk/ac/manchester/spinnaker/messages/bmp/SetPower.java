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

import static uk.ac.manchester.spinnaker.utils.UnitConstants.MSEC_PER_SEC;
import static uk.ac.manchester.spinnaker.messages.scp.SCPCommand.CMD_BMP_POWER;

import java.nio.ByteBuffer;
import java.util.Collection;

import uk.ac.manchester.spinnaker.machine.board.BMPBoard;
import uk.ac.manchester.spinnaker.messages.model.PowerCommand;

/**
 * An SCP request for the BMP to power on or power off a rack of boards. There
 * is no response payload.
 * <p>
 * <strong>Note:</strong> There is currently a bug in the BMP that means some
 * boards don't respond to power commands not sent to BMP 0. Because of this,
 * this message should <em>always</em> be sent to BMP 0!
 * <p>
 * Handled by {@code cmd_power()} in {@code bmp_cmd.c}, which in turn calls
 * {@code proc_power()} in the same file.
 */
public class SetPower extends BMPRequest<BMPRequest.BMPResponse> {
	private static final int DELAY_SHIFT = 16;

	private static final BMPBoard FRAME_ROOT = new BMPBoard(0);

	/**
	 * @param powerCommand
	 *            The power command being sent
	 * @param boards
	 *            The boards on the same backplane to power on or off
	 * @param delay
	 *            Number of seconds delay between power state changes of the
	 *            different boards.
	 */
	public SetPower(PowerCommand powerCommand, Collection<BMPBoard> boards,
			double delay) {
		super(FRAME_ROOT, CMD_BMP_POWER, argument1(delay, powerCommand),
				boardMask(boards));
	}

	private static int argument1(double delay, PowerCommand powerCommand) {
		return ((int) (delay * MSEC_PER_SEC) << DELAY_SHIFT)
				| powerCommand.value;
	}

	private static int boardMask(Collection<BMPBoard> boards) {
		return boards.stream().mapToInt(board -> 1 << board.board()).sum();
	}

	@Override
	public BMPResponse getSCPResponse(ByteBuffer buffer) throws Exception {
		return new BMPResponse("powering request", CMD_BMP_POWER, buffer);
	}
}
