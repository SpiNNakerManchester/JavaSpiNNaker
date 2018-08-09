package uk.ac.manchester.spinnaker.spalloc.messages;

import static com.fasterxml.jackson.annotation.JsonFormat.Shape.ARRAY;
import static java.util.Collections.emptyList;
import static java.util.Collections.unmodifiableList;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonFormat;

/**
 * A response that is the result of listing the jobs.
 */
@JsonFormat(shape = ARRAY)
public class ListJobsResponse {
	private List<JobDescription> jobs;

	public List<JobDescription> getJobs() {
		return jobs;
	}

	public void setJobs(List<JobDescription> jobs) {
		this.jobs = jobs == null ? emptyList() : unmodifiableList(jobs);
	}
}
