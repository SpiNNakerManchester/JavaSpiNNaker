/*
 * Copyright (c) 2021 The University of Manchester
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
package uk.ac.manchester.spinnaker.alloc.allocator;

import uk.ac.manchester.spinnaker.alloc.model.JobState;
import uk.ac.manchester.spinnaker.alloc.model.PowerState;

/**
 * A service that knows how to initiate the setting of the power state of a job.
 *
 * @author Donal Fellows
 */
public interface PowerController {
	/**
	 * Destroy a job. The power controller has the responsibility because it
	 * releases any resources held by the job.
	 *
	 * @param jobId
	 *            The ID of the job.
	 * @param reason
	 *            Why is the job being destroyed.
	 */
	void destroyJob(int jobId, String reason);

	/**
	 * Issue a power change for a job.
	 *
	 * @param jobId
	 *            The ID of the allocated job.
	 * @param power
	 *            What state to change the job's boards' power to.
	 * @param targetState
	 *            What state are we aiming to put the job into once the power
	 *            has been switched. Should be {@link JobState#READY} or
	 *            {@link JobState#DESTROYED}.
	 * @return Whether any change has been requested.
	 */
	boolean setPower(int jobId, PowerState power, JobState targetState);
}
