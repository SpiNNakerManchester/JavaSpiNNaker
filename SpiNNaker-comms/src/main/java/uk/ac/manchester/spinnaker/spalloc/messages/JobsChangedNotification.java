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

import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.NON_PRIVATE;
import static uk.ac.manchester.spinnaker.utils.CollectionUtils.copy;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonAutoDetect;

/**
 * A response that describes what jobs have changed state.
 */
@JsonAutoDetect(setterVisibility = NON_PRIVATE)
public class JobsChangedNotification implements Notification {
	private List<Integer> jobsChanged = List.of();

	/** Create a notification response. */
	public JobsChangedNotification() {
	}

	/**
	 * A response that reports a single job has changed.
	 *
	 * @param jobID
	 *            The ID of the job that has changed.
	 */
	public JobsChangedNotification(int jobID) {
		setJobsChanged(List.of(jobID));
	}

	/** @return What jobs have changed. Not accurate. */
	public List<Integer> getJobsChanged() {
		return jobsChanged;
	}

	/** @param jobsChanged What jobs have changed. */
	void setJobsChanged(List<Integer> jobsChanged) {
		this.jobsChanged = copy(jobsChanged);
	}

	@Override
	public boolean equals(Object other) {
		if (!(other instanceof JobsChangedNotification)) {
			return false;
		}
		return jobsChanged
				.equals(((JobsChangedNotification) other).jobsChanged);
	}

	@Override
	public int hashCode() {
		throw new UnsupportedOperationException();
	}

	@Override
	public String toString() {
		return "jobs changed: " + jobsChanged;
	}
}
