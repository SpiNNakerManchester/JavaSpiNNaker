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

import static uk.ac.manchester.spinnaker.utils.UnitConstants.MSEC_PER_SEC;
import static uk.ac.manchester.spinnaker.messages.scp.SCPCommand.CMD_BMP_POWER;

import java.nio.ByteBuffer;
import java.util.Collection;

import uk.ac.manchester.spinnaker.messages.model.PowerCommand;

/**
 * An SCP request for the BMP to power on or power off a rack of boards.
 * <p>
 * <b>Note:</b> There is currently a bug in the BMP that means some boards don't
 * respond to power commands not sent to BMP 0. Because of this, this message
 * should <i>always</i> be sent to BMP 0!
 */
public class SetPower extends BMPRequest<BMPRequest.BMPResponse> {
	private static final int DELAY_SHIFT = 16;

	/**
	 * @param powerCommand
	 *            The power command being sent
	 * @param boards
	 *            The boards on the same backplane to power on or off
	 * @param delay
	 *            Number of seconds delay between power state changes of the
	 *            different boards.
	 */
	public SetPower(PowerCommand powerCommand, Collection<Integer> boards,
			double delay) {
		super(0, CMD_BMP_POWER, argument1(delay, powerCommand),
				argument2(boards));
	}

	private static int argument1(double delay, PowerCommand powerCommand) {
		return ((int) (delay * MSEC_PER_SEC) << DELAY_SHIFT)
				| powerCommand.value;
	}

	private static int argument2(Collection<Integer> boards) {
		return boards.stream().mapToInt(board -> 1 << board).sum();
	}

	@Override
	public BMPResponse getSCPResponse(ByteBuffer buffer) throws Exception {
		return new BMPResponse("powering request", CMD_BMP_POWER, buffer);
	}
}
