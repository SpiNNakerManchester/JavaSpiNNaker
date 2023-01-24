/*
 * Copyright (c) 2019 The University of Manchester
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
package uk.ac.manchester.spinnaker.front_end;

import org.slf4j.MDC;
import org.slf4j.MDC.MDCCloseable;

import uk.ac.manchester.spinnaker.machine.HasChipLocation;
import uk.ac.manchester.spinnaker.machine.Machine;
import uk.ac.manchester.spinnaker.transceiver.TransceiverInterface;

/**
 * A class for making things easier to do on a per-board basis.
 *
 * @author Donal Fellows
 */
public abstract class BoardLocalSupport {
	private static final String BOARD_ROOT = "boardRoot";

	/** The transceiver for talking to the SpiNNaker machine. */
	protected final TransceiverInterface txrx;

	/** The description of the SpiNNaker machine. */
	protected final Machine machine;

	/**
	 * @param transceiver
	 *            How to talk to the SpiNNaker system via SCP. Where the system
	 *            is located.
	 * @param machine
	 *            Which machine is this on? Used for address mapping and
	 *            provided as a general service to subclasses.
	 */
	protected BoardLocalSupport(TransceiverInterface transceiver,
			Machine machine) {
		this.txrx = transceiver;
		this.machine = machine;
	}

	private String root(HasChipLocation chipLoc) {
		var root = machine.getChipAt(chipLoc).nearestEthernet;
		return "(board:" + root.getX() + "," + root.getY() + ")";
	}

	/**
	 * A context that can be pushed to state in logging what board is the root.
	 *
	 * @author Donal Fellows
	 */
	protected final class BoardLocal implements AutoCloseable {
		private MDCCloseable context;

		/**
		 * @param chipLocation
		 *            The location of a chip on its board.
		 */
		public BoardLocal(HasChipLocation chipLocation) {
			context = MDC.putCloseable(BOARD_ROOT, root(chipLocation));
		}

		@Override
		public void close() {
			context.close();
		}
	}
}
