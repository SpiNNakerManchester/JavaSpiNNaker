package uk.ac.manchester.spinnaker.spalloc.messages;

/**
 * Request the state of a job.
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
