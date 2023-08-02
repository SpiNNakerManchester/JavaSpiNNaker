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
 * Request to create a job.
 */
public class CreateJobCommand extends Command<Integer> {
	/**
	 * Create a request to create a job.
	 *
	 * @param numBoards
	 *            The number of boards to request.
	 * @param owner
	 *            The owner of the job to create.
	 */
	public CreateJobCommand(final int numBoards, final String owner) {
		super("create_job");
		addArg(numBoards);
		addKwArg("owner", owner);
	}
}
