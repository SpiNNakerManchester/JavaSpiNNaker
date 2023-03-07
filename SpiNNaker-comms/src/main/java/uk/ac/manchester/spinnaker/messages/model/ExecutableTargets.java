/*
 * Copyright (c) 2018 The University of Manchester
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
package uk.ac.manchester.spinnaker.messages.model;

import static java.lang.String.format;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.PositiveOrZero;

import uk.ac.manchester.spinnaker.machine.CoreLocation;
import uk.ac.manchester.spinnaker.machine.CoreSubsets;

/** Encapsulate the binaries and cores on which to execute them. */
public class ExecutableTargets {
	private final Map<@NotBlank String, @Valid CoreSubsets> targets;

	@PositiveOrZero
	private int totalProcessors;

	@Valid
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
		for (var core : subsets) {
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
