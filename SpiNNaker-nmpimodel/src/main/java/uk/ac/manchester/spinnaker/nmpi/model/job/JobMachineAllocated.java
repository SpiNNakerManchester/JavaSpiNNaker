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
package uk.ac.manchester.spinnaker.nmpi.model.job;

/**
 * Indicates whether a machine was allocated for a job.
 */
public class JobMachineAllocated {
	/** True if the machine has been allocated. */
	private boolean allocated = false;

	/**
	 * Empty for serialisation.
	 */
	public JobMachineAllocated() {
		// Does Nothing
	}

	/**
	 * Create an instance.
	 *
	 * @param allocatedParam
	 *            Whether the job was allocated.
	 */
	public JobMachineAllocated(boolean allocatedParam) {
		this.allocated = allocatedParam;
	}

	/**
	 * Determine if the job is allocated.
	 *
	 * @return True if allocated
	 */
	public boolean isAllocated() {
		return this.allocated;
	}

	/**
	 * Set the job allocation status.
	 *
	 * @param allocatedParam The allocation status
	 */
	public void setAllocated(boolean allocatedParam) {
		this.allocated = allocatedParam;
	}
}
