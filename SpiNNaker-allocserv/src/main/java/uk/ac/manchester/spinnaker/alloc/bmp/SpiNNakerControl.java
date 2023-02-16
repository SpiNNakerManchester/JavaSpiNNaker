/*
 * Copyright (c) 2021 The University of Manchester
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
package uk.ac.manchester.spinnaker.alloc.bmp;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import uk.ac.manchester.spinnaker.alloc.allocator.SpallocAPI.Machine;
import uk.ac.manchester.spinnaker.machine.board.BMPBoard;
import uk.ac.manchester.spinnaker.machine.board.BMPCoords;
import uk.ac.manchester.spinnaker.messages.model.Blacklist;
import uk.ac.manchester.spinnaker.transceiver.ProcessException;

/**
 * How to tell a SpiNNaker BMP (specifically one that manages a frame) to turn
 * on boards, turn them off, and to turn off links on the perimeter of an
 * allocation.
 * <p>
 * Implementations of this are expected to be <em>prototype beans</em> that have
 * a constructor that takes two arguments: <br>
 * <blockquote> {@code TheCls(}{@link Machine} {@code machine,}
 * {@link BMPCoords} {@code bmp)} </blockquote>
 * <p>
 * Note that SpiNNaker-2 will have a different concrete implementation of this,
 * and functionality implemented by this might eventually move into the
 * transceiver.
 */
public interface SpiNNakerControl {
	/**
	 * Switch on a collection of boards managed by a BMP on a machine and check
	 * that they've come up correctly.
	 * <p>
	 * Note that this operation can take some time.
	 *
	 * @param boards
	 *            The <em>database IDs</em> of the boards to switch on.
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
	 * @throws InterruptedException
	 *             If the communications were interrupted.
	 */
	void setLinkOff(Link link)
			throws ProcessException, IOException, InterruptedException;

	/**
	 * Turn off boards managed by a BMP. Turning off a board also turns off its
	 * links.
	 *
	 * @param boards
	 *            The <em>database IDs</em> of the boards to turn off.
	 * @throws ProcessException
	 *             If a BMP sends a failure message.
	 * @throws IOException
	 *             If network I/O fails or we reach the limit on retries.
	 * @throws InterruptedException
	 *             If we're interrupted.
	 */
	void powerOff(List<Integer> boards)
			throws ProcessException, InterruptedException, IOException;

	/**
	 * Set how to map from database IDs for a board to what to use when talking
	 * to the BMP.
	 *
	 * @param idToBoard
	 *            How to get a physical board number from a database ID of the
	 *            board.
	 */
	void setIdToBoardMap(Map<Integer, BMPBoard> idToBoard);

	/**
	 * Read a BMP serial number from the given board.
	 *
	 * @param board
	 *            The board to read from.
	 * @return The serial number.
	 * @throws ProcessException
	 *             If a BMP sends a failure message.
	 * @throws IOException
	 *             If network I/O fails or we reach the limit on retries.
	 * @throws InterruptedException
	 *             If we're interrupted.
	 */
	String readSerial(BMPBoard board)
			throws ProcessException, InterruptedException, IOException;

	/**
	 * Read a blacklist from the given board.
	 *
	 * @param board
	 *            The board to read the blacklist from.
	 * @return The blacklist.
	 * @throws ProcessException
	 *             If a BMP sends a failure message.
	 * @throws IOException
	 *             If network I/O fails or we reach the limit on retries.
	 * @throws InterruptedException
	 *             If we're interrupted.
	 */
	Blacklist readBlacklist(BMPBoard board)
			throws ProcessException, InterruptedException, IOException;

	/**
	 * Write a blacklist to the given board.
	 *
	 * @param board
	 *            The board to write the blacklist to.
	 * @param blacklist
	 *            The blacklist to write.
	 * @throws ProcessException
	 *             If a BMP sends a failure message.
	 * @throws IOException
	 *             If network I/O fails or we reach the limit on retries.
	 * @throws InterruptedException
	 *             If we're interrupted.
	 */
	void writeBlacklist(BMPBoard board, Blacklist blacklist)
			throws ProcessException, InterruptedException, IOException;

	/**
	 * A guide for how to make a BMP controller.
	 *
	 * @author Donal Fellows
	 */
	@FunctionalInterface
	interface Factory {
		/**
		 * Create a SpiNNaker board controller. Note that this is only provided
		 * for reference. It is up to the caller to ensure that the controller
		 * is the only controller that talks to a particular BMP; <em>it is
		 * <strong>essential</strong> that only one controller and one thread
		 * accesses a BMP in order to prevent the BMP from crashing!</em>
		 *
		 * @param machine
		 *            The machine that this will be managing.
		 * @param coords
		 *            The coordinates of the BMP that this will manage.
		 * @return The board controller.
		 */
		SpiNNakerControl create(Machine machine, BMPCoords coords);
	}
}
