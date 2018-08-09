package uk.ac.manchester.spinnaker.spalloc.responses;

import static java.util.Collections.emptyList;
import static java.util.Collections.unmodifiableList;

import java.util.List;

/**
 * A response that describes what jobs have changed state.
 */
public class JobsChangedResponse implements Response {
	private List<Integer> jobsChanged = emptyList();

	public List<Integer> getJobsChanged() {
		return jobsChanged;
	}

	public void setJobsChanged(List<Integer> jobsChanged) {
		this.jobsChanged = jobsChanged == null ? emptyList()
				: unmodifiableList(jobsChanged);
	}
}
