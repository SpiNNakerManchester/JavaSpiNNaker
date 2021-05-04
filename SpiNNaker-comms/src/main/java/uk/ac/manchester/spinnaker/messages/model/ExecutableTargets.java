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
package uk.ac.manchester.spinnaker.messages.model;

import static java.lang.String.format;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import uk.ac.manchester.spinnaker.machine.CoreLocation;
import uk.ac.manchester.spinnaker.machine.CoreSubsets;

/** Encapsulate the binaries and cores on which to execute them. */
public class ExecutableTargets {
	private final Map<String, CoreSubsets> targets;

	private int totalProcessors;

	private final CoreSubsets allCoreSubsets;

	/** Create. */
	public ExecutableTargets() {
		targets = new HashMap<>();
		totalProcessors = 0;
		allCoreSubsets = new CoreSubsets();
	}

	/**
	 * Add core subsets to a binary.
	 *
	 * @param binary
	 *            the path to the binary needed to be executed
	 * @param subsets
	 *            the subset of cores that the binary needs to be loaded on
	 */
	public void addSubsets(String binary, CoreSubsets subsets) {
		for (CoreLocation core : subsets) {
			addProcessor(binary, core);
		}
	}

	/**
	 * Add a processor to the executable targets.
	 *
	 * @param binary
	 *            the binary path for executable
	 * @param core
	 *            the coordinates on the machine where the binary will run
	 */
	public void addProcessor(String binary, CoreLocation core) {
		if (known(binary, core)) {
			return;
		}
		if (!targets.containsKey(binary)) {
			targets.put(binary, new CoreSubsets());
		}
		targets.get(binary).addCore(core);
		allCoreSubsets.addCore(core);
		totalProcessors++;
	}

	/**
	 * Get the cores that a binary is to run on.
	 *
	 * @param binary
	 *            The binary to find the cores for
	 * @return Which cores are to run the binary.
	 */
	public CoreSubsets getCoresForBinary(String binary) {
		return targets.get(binary);
	}

	/** @return The binaries of the executables. */
	public Set<String> getBinaries() {
		return targets.keySet();
	}

	/** @return The total number of cores to be loaded. */
	public int getTotalProcessors() {
		return totalProcessors;
	}

	/** @return All the core subsets for all the binaries. */
	public CoreSubsets getAllCoreSubsets() {
		return allCoreSubsets;
	}

	private boolean known(String binary, CoreLocation core) {
		if (!allCoreSubsets.isCore(core)) {
			return false;
		}
		// OK if and only if the chip is in this binary already
		if (targets.containsKey(binary) && targets.get(binary).isCore(core)) {
			return true;
		}
		throw new IllegalArgumentException(format(
				"Setting parameter x:%d y:%d p:%d to value %s is invalid: %s",
				core.getX(), core.getY(), core.getP(), binary,
				"Already associated with a different binary"));
	}
}
