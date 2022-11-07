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

import static uk.ac.manchester.spinnaker.messages.scp.SCPCommand.CMD_LINK_WRITE;

import java.nio.ByteBuffer;

import uk.ac.manchester.spinnaker.machine.Direction;
import uk.ac.manchester.spinnaker.machine.HasCoreLocation;
import uk.ac.manchester.spinnaker.machine.MemoryLocation;

/** A request to write memory on a neighbouring chip. */
public class WriteLink extends SimpleRequest {
	/**
	 * @param operation
	 *            The higher-level operation being performed.
	 * @param core
	 *            the core to write via
	 * @param link
	 *            The ID of the link down which to send the write
	 * @param baseAddress
	 *            The positive base address to start the write at
	 * @param data
	 *            The data to write (up to 256 bytes); the <i>position</i> of
	 *            the buffer must be the point where the data starts. The
	 *            position and limit of the buffer will not be updated by this
	 *            constructor.
	 */
	public WriteLink(String operation, HasCoreLocation core, Direction link,
			MemoryLocation baseAddress, ByteBuffer data) {
		super(operation, core, CMD_LINK_WRITE, baseAddress.address,
				data.remaining(), link.id, data);
	}
}
