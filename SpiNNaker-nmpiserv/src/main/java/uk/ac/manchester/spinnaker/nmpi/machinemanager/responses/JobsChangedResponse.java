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

import static java.util.Collections.emptyList;

import java.util.List;

/**
 * A response that describes what jobs have changed state.
 */
public class JobsChangedResponse implements Response {
	/** The list of jobs that have changed. */
	private List<Integer> jobsChanged = emptyList();

	/**
	 * Get the jobs that have changed.
	 *
	 * @return The list of job ids
	 */
	public List<Integer> getJobsChanged() {
		return jobsChanged;
	}

	/**
	 * Set the jobs that have changed.
	 *
	 * @param jobsChanged
	 *            The list of job ids
	 */
	public void setJobsChanged(final List<Integer> jobsChanged) {
		this.jobsChanged = jobsChanged;
	}
}
