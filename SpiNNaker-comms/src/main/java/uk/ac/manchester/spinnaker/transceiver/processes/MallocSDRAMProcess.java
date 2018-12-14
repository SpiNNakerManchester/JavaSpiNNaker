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
import uk.ac.manchester.spinnaker.messages.model.AppID;
import uk.ac.manchester.spinnaker.messages.scp.SDRAMAlloc;
import uk.ac.manchester.spinnaker.transceiver.RetryTracker;

/** A process for allocating a block of SDRAM on a SpiNNaker chip. */
public class MallocSDRAMProcess extends MultiConnectionProcess<SCPConnection> {
	/**
	 * @param connectionSelector
	 *            How to select how to communicate.
	 * @param retryTracker
	 *            Object used to track how many retries were used in an
	 *            operation. May be {@code null} if no suck tracking is
	 *            required.
	 */
	public MallocSDRAMProcess(
			ConnectionSelector<SCPConnection> connectionSelector,
			RetryTracker retryTracker) {
		super(connectionSelector, retryTracker);
	}

	/**
	 * Allocate space in the SDRAM space.
	 *
	 * @param chip
	 *            On what chip to allocate.
	 * @param size
	 *            How many bytes to allocate.
	 * @param appID
	 *            What app will own the allocation.
	 * @return Where the start of the allocated memory is.
	 * @throws IOException
	 *             If anything goes wrong with networking.
	 * @throws ProcessException
	 *             If SpiNNaker rejects the message.
	 */
	public int mallocSDRAM(HasChipLocation chip, int size, AppID appID)
			throws IOException, ProcessException {
		return synchronousCall(new SDRAMAlloc(chip, appID, size)).baseAddress;
	}

	/**
	 * Allocate space in the SDRAM space.
	 *
	 * @param chip
	 *            On what chip to allocate.
	 * @param size
	 *            How many bytes to allocate.
	 * @param appID
	 *            What app will own the allocation.
	 * @param tag
	 *            the tag for the SDRAM, a 8-bit (chip-wide) tag that can be
	 *            looked up by a SpiNNaker application to discover the address
	 *            of the allocated block
	 * @return Where the start of the allocated memory is.
	 * @throws IOException
	 *             If anything goes wrong with networking.
	 * @throws ProcessException
	 *             If SpiNNaker rejects the message.
	 */
	public int mallocSDRAM(HasChipLocation chip, int size, AppID appID, int tag)
			throws IOException, ProcessException {
		return synchronousCall(
				new SDRAMAlloc(chip, appID, size, tag)).baseAddress;
	}
}
