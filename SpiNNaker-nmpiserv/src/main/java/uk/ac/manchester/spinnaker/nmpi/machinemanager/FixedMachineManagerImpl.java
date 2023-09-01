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

import static java.util.Objects.nonNull;
import static uk.ac.manchester.spinnaker.utils.ThreadUtils.waitfor;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.annotation.Value;

import uk.ac.manchester.spinnaker.nmpi.model.machine.ChipCoordinates;
import uk.ac.manchester.spinnaker.nmpi.model.machine.SpinnakerMachine;

/**
 * A manager of directly-connected SpiNNaker machines.
 */
public class FixedMachineManagerImpl implements MachineManager {
	/** The queue of available machines. */
	private final Set<SpinnakerMachine> machinesAvailable = new HashSet<>();

	/** The set of machine allocated. */
	private final Set<SpinnakerMachine> machinesAllocated = new HashSet<>();

	/** Lock to avoid concurrent modification in different threads. */
	private final Object lock = new Object();

	/** True when the manager is finished. */
	private boolean done = false;

	/**
	 * Sets the initial set of machines that are available.
	 *
	 * @param machines
	 *            the collection of machines to use
	 */
	@Value("${machines}")
	void setInitialMachines(final List<SpinnakerMachine> machines) {
		machinesAvailable.addAll(machines);
	}

	@Override
	public List<SpinnakerMachine> getMachines() {
		final var machines = new ArrayList<SpinnakerMachine>();
		synchronized (lock) {
			machines.addAll(machinesAvailable);
			machines.addAll(machinesAllocated);
		}
		return machines;
	}

	@Override
	public SpinnakerMachine getNextAvailableMachine(final int nBoards,
			final String owner, final int jobId) {
		synchronized (lock) {
			while (!done) {
				final var machine = getLargeEnoughMachine(nBoards);
				if (nonNull(machine)) {
					// Move the machine from available to allocated
					machinesAvailable.remove(machine);
					machinesAllocated.add(machine);
					return machine;
				}
				// If no machine was found, wait for something to change
				if (waitfor(lock)) {
					break;
				}
			}
			return null;
		}
	}

	/**
	 * Get a machine with at least the given number of boards.
	 *
	 * @param nBoards
	 *            The number of boards required.
	 * @return A machine big enough, or null of none.
	 */
	private SpinnakerMachine getLargeEnoughMachine(final int nBoards) {
		return machinesAvailable.stream()
				.filter(machine -> machine.getnBoards() >= nBoards)
				.findFirst().orElse(null);
	}

	@Override
	public void releaseMachine(final SpinnakerMachine machine) {
		synchronized (lock) {
			machinesAllocated.remove(machine);
			machinesAvailable.add(machine);
			lock.notifyAll();
		}
	}

	@Override
	public void close() {
		synchronized (lock) {
			done = true;
			lock.notifyAll();
		}
	}

	@Override
	public boolean isMachineAvailable(final SpinnakerMachine machine) {
		synchronized (lock) {
			return !machinesAvailable.contains(machine);
		}
	}

	@Override
	public boolean waitForMachineStateChange(final SpinnakerMachine machine,
			final int waitTime) {
		synchronized (lock) {
			final boolean isAvailable = machinesAvailable.contains(machine);
			waitfor(lock, waitTime);
			return machinesAvailable.contains(machine) != isAvailable;
		}
	}

	@Override
	public void setMachinePower(
			final SpinnakerMachine machine, final boolean powerOn) {
		// Does Nothing in this implementation
	}

	@Override
	public ChipCoordinates getChipCoordinates(final SpinnakerMachine machine,
			final int x, final int y) {
		return new ChipCoordinates(0, 0, 0);
	}
}
