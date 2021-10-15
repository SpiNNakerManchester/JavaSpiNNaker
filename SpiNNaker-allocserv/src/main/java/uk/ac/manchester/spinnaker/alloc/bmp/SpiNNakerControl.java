/*
 * Copyright (c) 2021 The University of Manchester
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
package uk.ac.manchester.spinnaker.alloc.bmp;

import java.io.IOException;
import java.util.List;

import uk.ac.manchester.spinnaker.transceiver.ProcessException;

/**
 * How to tell a SpiNNaker machine to turn on boards, turn them off, and to turn
 * off links on the perimeter of an allocation.
 */
public interface SpiNNakerControl {
	/**
	 * Switch on a collection of boards on a machine and check that they've come
	 * up correctly.
	 * <p>
	 * Note that this operation can take some time.
	 *
	 * @param boards
	 *            Which boards to switch on.
	 * @throws ProcessException
	 *             If a BMP sends a failure message.
	 * @throws IOException
	 *             If network I/O fails or we reach the limit on retries.
	 * @throws InterruptedException
	 *             If we're interrupted.
	 */
	void powerOnAndCheck(List<Integer> boards)
			throws ProcessException, InterruptedException, IOException;

	/**
	 * Turns a link off. (We never need to explicitly switch a link on; that's
	 * implicit in switching on its board.)
	 *
	 * @param link
	 *            The link to turn off.
	 * @throws ProcessException
	 *             If a BMP rejects a message.
	 * @throws IOException
	 *             If network I/O fails.
	 */
	void setLinkOff(Link link) throws ProcessException, IOException;

	/**
	 * Turn off boards. Turning off a board also turns off its links.
	 *
	 * @param boards
	 *            What boards to turn off.
	 * @throws ProcessException
	 *             If a BMP sends a failure message.
	 * @throws IOException
	 *             If network I/O fails or we reach the limit on retries.
	 * @throws InterruptedException
	 *             If we're interrupted.
	 */
	void powerOff(List<Integer> boards)
			throws ProcessException, InterruptedException, IOException;
}
