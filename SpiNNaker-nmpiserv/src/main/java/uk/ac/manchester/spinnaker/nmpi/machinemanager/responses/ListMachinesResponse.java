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
package uk.ac.manchester.spinnaker.nmpi.machinemanager.responses;

import static com.fasterxml.jackson.annotation.JsonFormat.Shape.ARRAY;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonFormat;

/**
 * A response that is the result of listing the machines.
 */
@JsonFormat(shape = ARRAY)
public class ListMachinesResponse {
	/** The list of machines. */
	private List<Machine> machines;

	/**
	 * Get the list of machines.
	 *
	 * @return The list of machines
	 */
	public List<Machine> getMachines() {
		return machines;
	}

	/**
	 * Set the list of machines.
	 *
	 * @param machinesParam The list of machines to set
	 */
	void setMachines(List<Machine> machinesParam) {
		this.machines = machinesParam;
	}
}
