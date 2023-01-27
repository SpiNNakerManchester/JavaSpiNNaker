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

import static java.lang.Byte.toUnsignedInt;
import static uk.ac.manchester.spinnaker.machine.MachineDefaults.MAX_NUM_CORES;
import static uk.ac.manchester.spinnaker.messages.model.AppID.DEFAULT;
import static uk.ac.manchester.spinnaker.messages.scp.Bits.BYTE0;
import static uk.ac.manchester.spinnaker.messages.scp.Bits.BYTE1;
import static uk.ac.manchester.spinnaker.messages.scp.Bits.BYTE3;
import static uk.ac.manchester.spinnaker.messages.scp.FloodFillConstants.DELAY;
import static uk.ac.manchester.spinnaker.messages.scp.FloodFillConstants.FORWARD_LINKS;
import static uk.ac.manchester.spinnaker.messages.scp.SCPCommand.CMD_NNP;

import java.nio.ByteBuffer;
import java.util.List;

import uk.ac.manchester.spinnaker.machine.ValidP;
import uk.ac.manchester.spinnaker.messages.model.AppID;

/**
 * An SCP request to finish a flood fill of data across all cores and launch the
 * application. There is no response payload.
 * <p>
 * Handled ultimately by {@code nn_cmd_ffe()} in {@code scamp-nn.c}.
 */
public final class FloodFillEnd extends SCPRequest<CheckOKResponse> {
	// Send on all links, std inter-message delay, no message resends
	private static final int NNP_FORWARD_RETRY =
			(FORWARD_LINKS << BYTE1) | (DELAY << BYTE0);

	// See nn_rcv_pkt()
	private static final int NN_CMD_FFE = 15;

	private static final int WAIT_BIT = 18;

	/**
	 * @param nearestNeighbourID
	 *            The ID of the packet, between 0 and 127
	 */
	public FloodFillEnd(byte nearestNeighbourID) {
		this(nearestNeighbourID, DEFAULT, List.of(), false);
	}

	/**
	 * @param nearestNeighbourID
	 *            The ID of the packet, between 0 and 127
	 * @param appID
	 *            The application ID to start using the data
	 * @param processors
	 *            A list of processors on which to start the application, each
	 *            between 1 and 17. If not specified, no application is started.
	 * @param wait
	 *            True if the binary should go into a "wait" state before
	 *            executing
	 */
	public FloodFillEnd(byte nearestNeighbourID, AppID appID,
			Iterable<@ValidP Integer> processors, boolean wait) {
		super(BOOT_MONITOR_CORE, CMD_NNP, key(nearestNeighbourID),
				data(appID, processors, wait), NNP_FORWARD_RETRY);
	}

	private static int key(byte nearestNeighbourID) {
		return (NN_CMD_FFE << BYTE3) | toUnsignedInt(nearestNeighbourID);
	}

	/**
	 * The value to pass to {@code proc_start_app()} to say what cores to start
	 * on and what app ID to use.
	 *
	 * @param appID
	 *            The application ID.
	 * @param processors
	 *            What to launch on.
	 * @param wait
	 *            Whether to start in the {@code wait} state.
	 * @return The packed word.
	 */
	private static int data(AppID appID, Iterable<Integer> processors,
			boolean wait) {
		int processorMask = 0;
		for (int p : processors) {
			if (p >= 1 && p < MAX_NUM_CORES) {
				processorMask |= 1 << p;
			}
		}
		processorMask |= appID.appID << BYTE3;
		if (wait) {
			processorMask |= 1 << WAIT_BIT;
		}
		return processorMask;
	}

	@Override
	public CheckOKResponse getSCPResponse(ByteBuffer buffer) throws Exception {
		return new CheckOKResponse("Flood Fill", CMD_NNP, buffer);
	}
}
