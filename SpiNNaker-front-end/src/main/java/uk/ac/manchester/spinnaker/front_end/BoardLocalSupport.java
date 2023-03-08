/*
 * Copyright (c) 2019 The University of Manchester
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
