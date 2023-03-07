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

/**
 * Request the state of a job.
 *
 * @see <a href=
 *      "https://spalloc-server.readthedocs.io/en/stable/protocol/#commands.get_job_state"
 *      >Spalloc Server documentation</a>
 * @see JobState The basic result type associated with the request
 */
public class GetJobStateCommand extends Command<Integer> {
	/**
	 * Create a request to get the state of a job.
	 *
	 * @param jobId
	 *            The job to get the state of.
	 */
	public GetJobStateCommand(int jobId) {
		super("get_job_state");
		addArg(jobId);
	}
}
