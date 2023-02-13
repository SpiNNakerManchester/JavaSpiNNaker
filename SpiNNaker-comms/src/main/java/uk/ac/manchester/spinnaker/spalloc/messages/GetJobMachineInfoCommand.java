/*
 * Copyright (c) 2018-2023 The University of Manchester
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
 * Request to get machine information relating to a job.
 *
 * @see <a href=
 *      "https://spalloc-server.readthedocs.io/en/stable/protocol/#commands.get_job_machine_info"
 *      >Spalloc Server documentation</a>
 * @see JobMachineInfo The basic result type associated with the request
 */
public class GetJobMachineInfoCommand extends Command<Integer> {
	/**
	 * Create a request to get information about a job's allocated machine.
	 *
	 * @param jobId
	 *            The job to ask about.
	 */
	public GetJobMachineInfoCommand(int jobId) {
		super("get_job_machine_info");
		addArg(jobId);
	}
}
