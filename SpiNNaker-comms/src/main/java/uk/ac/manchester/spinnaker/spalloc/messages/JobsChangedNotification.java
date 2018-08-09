package uk.ac.manchester.spinnaker.spalloc.messages;

import static java.util.Collections.emptyList;
import static java.util.Collections.unmodifiableList;

import java.util.List;

/**
 * A response that describes what jobs have changed state.
 */
public class JobsChangedNotification implements Notification {
	private List<Integer> jobsChanged = emptyList();

	public List<Integer> getJobsChanged() {
		return jobsChanged;
	}

	public void setJobsChanged(List<Integer> jobsChanged) {
		this.jobsChanged = jobsChanged == null ? emptyList()
				: unmodifiableList(jobsChanged);
	}
}
