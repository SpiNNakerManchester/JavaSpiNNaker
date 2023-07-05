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
 * Request to not receive notifications about a job.
 */
public class NoNotifyJobCommand extends Command<Integer> {
	/**
	 * Create a request to not be notified of changes in job state.
	 *
	 * @param jobId
	 *            The job to request about.
	 */
	public NoNotifyJobCommand(final int jobId) {
		super("no_notify_job");
		addArg(jobId);
	}
}
