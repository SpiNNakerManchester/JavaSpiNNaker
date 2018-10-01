package uk.ac.manchester.spinnaker.spalloc.messages;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.unmodifiableList;

import java.util.List;

/**
 * A response that describes what jobs have changed state.
 */
public class JobsChangedNotification implements Notification {
	private List<Integer> jobsChanged = emptyList();

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
		setJobsChanged(asList(jobID));
	}

	public List<Integer> getJobsChanged() {
		return jobsChanged;
	}

	public void setJobsChanged(List<Integer> jobsChanged) {
		this.jobsChanged = jobsChanged == null ? emptyList()
				: unmodifiableList(jobsChanged);
	}

	@Override
	public boolean equals(Object other) {
		if (other == null || !(other instanceof JobsChangedNotification)) {
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
