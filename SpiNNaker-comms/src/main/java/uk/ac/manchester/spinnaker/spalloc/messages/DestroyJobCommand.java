/*
 * Copyright (c) 2018 The University of Manchester
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package uk.ac.manchester.spinnaker.spalloc.messages;

/**
 * Request to destroy a job.
 *
 * @see <a href=
 *      "https://spalloc-server.readthedocs.io/en/stable/protocol/#commands.destroy_job"
 *      >Spalloc Server documentation</a>
 */
public class DestroyJobCommand extends Command<Integer> {
	/**
	 * Make a request to destroy a job.
	 *
	 * @param jobId
	 *            The ID of the job.
	 */
	public DestroyJobCommand(int jobId) {
		super("destroy_job");
		addArg(jobId);
	}

	/**
	 * Make a request to destroy a job.
	 *
	 * @param jobId
	 *            The ID of the job.
	 * @param reason
	 *            Why the job is to be destroyed.
	 */
	public DestroyJobCommand(int jobId, String reason) {
		super("destroy_job");
		addArg(jobId);
		if (reason != null) {
			addKwArg("reason", reason);
		}
	}
}
