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
package uk.ac.manchester.spinnaker.transceiver.processes;

import java.io.IOException;

import uk.ac.manchester.spinnaker.connections.SCPConnection;
import uk.ac.manchester.spinnaker.connections.selectors.ConnectionSelector;
import uk.ac.manchester.spinnaker.machine.HasChipLocation;
import uk.ac.manchester.spinnaker.messages.model.RouterDiagnostics;
import uk.ac.manchester.spinnaker.messages.scp.ReadMemory;
import uk.ac.manchester.spinnaker.transceiver.RetryTracker;
import uk.ac.manchester.spinnaker.utils.ValueHolder;

/** A process for reading the diagnostic data block from a SpiNNaker router. */
public class ReadRouterDiagnosticsProcess
		extends MultiConnectionProcess<SCPConnection> {
	private static final int REGISTER = 4;

	private static final int NUM_REGISTERS = 16;

	private static final int ROUTER_CONTROL_REGISTER = 0xe1000000;

	private static final int ROUTER_ERROR_STATUS = 0xe1000014;

	private static final int ROUTER_REGISTERS = 0xe1000300;

	/**
	 * @param connectionSelector
	 *            How to select how to communicate.
	 * @param retryTracker
	 *            Object used to track how many retries were used in an
	 *            operation. May be {@code null} if no suck tracking is
	 *            required.
	 */
	public ReadRouterDiagnosticsProcess(
			ConnectionSelector<SCPConnection> connectionSelector,
			RetryTracker retryTracker) {
		super(connectionSelector, retryTracker);
	}

	/**
	 * Get a chip's router's diagnostics.
	 *
	 * @param chip
	 *            The chip.
	 * @return The diagnostics from the chip's router.
	 * @throws IOException
	 *             If anything goes wrong with networking.
	 * @throws ProcessException
	 *             If SpiNNaker rejects a message.
	 */
	public RouterDiagnostics getRouterDiagnostics(HasChipLocation chip)
			throws IOException, ProcessException {
		ValueHolder<Integer> cr = new ValueHolder<>();
		ValueHolder<Integer> es = new ValueHolder<>();
		int[] reg = new int[NUM_REGISTERS];

		sendRequest(new ReadMemory(chip, ROUTER_CONTROL_REGISTER, REGISTER),
				response -> cr.setValue(response.data.getInt()));
		sendRequest(new ReadMemory(chip, ROUTER_ERROR_STATUS, REGISTER),
				response -> es.setValue(response.data.getInt()));
		sendRequest(
				new ReadMemory(chip, ROUTER_REGISTERS,
						NUM_REGISTERS * REGISTER),
				response -> response.data.asIntBuffer().get(reg));

		finish();
		checkForError();
		return new RouterDiagnostics(cr.getValue(), es.getValue(), reg);
	}
}
