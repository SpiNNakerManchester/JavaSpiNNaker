/*
 * Copyright (c) 2014 The University of Manchester
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
package uk.ac.manchester.spinnaker.nmpi.machinemanager;

import java.io.IOException;
import java.util.List;

import uk.ac.manchester.spinnaker.nmpi.model.machine.ChipCoordinates;
import uk.ac.manchester.spinnaker.nmpi.model.machine.SpinnakerMachine;

/**
 * A service for managing SpiNNaker boards in a machine.
 */
public sealed interface MachineManager extends AutoCloseable
		permits FixedMachineManagerImpl, SpallocJavaMachineManagerImpl,
		SpallocMachineManagerImpl {
	/**
	 * Gets the machines that this manager allocates from.
	 *
	 * @return collection of machines
	 * @throws IOException
	 *             if there is communication error.
	 */
	List<SpinnakerMachine> getMachines() throws IOException;

	/**
	 * Get the next available machine of a given size.
	 *
	 * @param nBoards
	 *            The (minimum) number of boards that the machine needs to have.
	 * @param owner
	 *            The owner of the job.
	 * @param jobId
	 *            The ID of the NMPI job.
	 * @return a machine.
	 * @throws IOException
	 *             if there is communication error.
	 */
	SpinnakerMachine getNextAvailableMachine(int nBoards, String owner,
			int jobId) throws IOException;

	/**
	 * Test if a specific machine is available.
	 *
	 * @param machine
	 *            The machine handle
	 * @return true if the machine is available.
	 * @throws IOException
	 *             if there is communication error.
	 */
	boolean isMachineAvailable(SpinnakerMachine machine) throws IOException;

	/**
	 * Wait for the machine's availability to change.
	 *
	 * @param machine
	 *            The machine handle
	 * @param waitTime
	 *            Maximum wait time (in milliseconds)
	 * @return Whether the machine state has changed.
	 * @throws IOException
	 *             if there is communication error.
	 */
	boolean waitForMachineStateChange(SpinnakerMachine machine, int waitTime)
			throws IOException;

	/**
	 * Release an allocated machine.
	 *
	 * @param machine
	 *            The machine handle
	 * @throws IOException
	 *             if there is communication error.
	 */
	void releaseMachine(SpinnakerMachine machine) throws IOException;

	/**
	 * Turn a machine on or off.
	 *
	 * @param machine
	 *            The machine handle
	 * @param powerOn
	 *            True to power a machine on, false to turn it off.
	 * @throws IOException
	 *             if there is communication error.
	 */
	void setMachinePower(SpinnakerMachine machine, boolean powerOn)
			throws IOException;

	/**
	 * Find a chip on a machine.
	 *
	 * @param machine
	 *            The machine handle
	 * @param x
	 *            The virtual X coordinate of the chip
	 * @param y
	 *            The virtual Y coordinate of the chip
	 * @return The chip location description
	 * @throws IOException
	 *             if there is communication error.
	 */
	ChipCoordinates getChipCoordinates(SpinnakerMachine machine, int x, int y)
			throws IOException;
}
