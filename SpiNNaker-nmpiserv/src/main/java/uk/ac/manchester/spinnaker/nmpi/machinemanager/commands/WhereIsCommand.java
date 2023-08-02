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
package uk.ac.manchester.spinnaker.nmpi.machinemanager.commands;

/**
 * Request to get the location of a chip in a job's allocation relative to a
 * machine.
 */
public class WhereIsCommand extends Command<Integer> {
	/**
	 * Create a request to locate a chip within a job's allocation.
	 *
	 * @param jobId
	 *            The job to request about.
	 * @param chipX
	 *            The X coordinate of the chip to ask about.
	 * @param chipY
	 *            The Y coordinate of the chip to ask about.
	 */
	public WhereIsCommand(final int jobId, final int chipX, final int chipY) {
		super("where_is");
		addKwArg("job_id", jobId);
		addKwArg("chip_x", chipX);
		addKwArg("chip_y", chipY);
	}
}
