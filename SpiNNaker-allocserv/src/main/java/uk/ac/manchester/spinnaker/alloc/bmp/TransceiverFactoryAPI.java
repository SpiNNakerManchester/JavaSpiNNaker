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

import uk.ac.manchester.spinnaker.alloc.allocator.SpallocAPI.Machine;
import uk.ac.manchester.spinnaker.transceiver.BMPTransceiverInterface;
import uk.ac.manchester.spinnaker.transceiver.SpinnmanException;

/**
 * Creates transceivers for talking to the BMPs of machines. Note that each
 * machine only has the one BMP that is talked to, and only ever one transceiver
 * that is used to do it.
 *
 * @param <T>
 *            The actual type of transceiver produced.
 * @author Donal Fellows
 */
public interface TransceiverFactoryAPI<T extends BMPTransceiverInterface> {
	/**
	 * Get the transceiver for talking to a given machine's BMPs.
	 *
	 * @param machineDescription
	 *            The machine we're talking about.
	 * @return The transceiver. Only operations relating to BMPs are required to
	 *         be supported.
	 * @throws IOException
	 *             If low-level things go wrong.
	 * @throws SpinnmanException
	 *             If the transceiver can't be built.
	 */
	T getTransciever(Machine machineDescription)
			throws IOException, SpinnmanException;
}
