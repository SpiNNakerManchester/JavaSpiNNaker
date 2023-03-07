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
package uk.ac.manchester.spinnaker.spalloc.messages;

import uk.ac.manchester.spinnaker.machine.HasChipLocation;

/**
 * Request to get the location of a chip in a job's allocation relative to a
 * machine.
 *
 * @see <a href=
 *      "https://spalloc-server.readthedocs.io/en/stable/protocol/#commands.where_is"
 *      >Spalloc Server documentation</a>
 * @see WhereIs The basic result type associated with the request
 */
public class WhereIsJobChipCommand extends Command<Integer> {
	/**
	 * Create a request to locate a chip within a job's allocation.
	 *
	 * @param jobId
	 *            The job to request about.
	 * @param chip
	 *            The coordinates of the chip to ask about.
	 */
	public WhereIsJobChipCommand(int jobId, HasChipLocation chip) {
		super("where_is");
		addKwArg("job_id", jobId);
		addKwArg("chip_x", chip.getX());
		addKwArg("chip_y", chip.getY());
	}
}
