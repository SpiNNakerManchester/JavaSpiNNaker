/*
 * Copyright (c) 2018 The University of Manchester
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package uk.ac.manchester.spinnaker.spalloc.messages;

import static java.util.Collections.unmodifiableList;
import static java.util.Objects.isNull;

import java.util.List;

/**
 * A response that describes what jobs have changed state.
 */
public final class JobsChangedNotification implements Notification {
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
	public void setJobsChanged(List<Integer> jobsChanged) {
		this.jobsChanged = isNull(jobsChanged) ? List.of()
				: unmodifiableList(jobsChanged);
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
