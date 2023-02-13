/*
 * Copyright (c) 2021-2023 The University of Manchester
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

import uk.ac.manchester.spinnaker.alloc.allocator.SpallocAPI.Machine;
import uk.ac.manchester.spinnaker.machine.board.BMPCoords;
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
	 * @param bmp
	 *            Which BMP on the machine are we talking to.
	 * @return The transceiver. Only operations relating to BMPs are required to
	 *         be supported.
	 * @throws IOException
	 *             If low-level things go wrong.
	 * @throws SpinnmanException
	 *             If the transceiver can't be built.
	 * @throws InterruptedException
	 *             If the communications were interrupted.
	 */
	T getTransciever(Machine machineDescription, BMPCoords bmp)
			throws IOException, SpinnmanException, InterruptedException;
}
