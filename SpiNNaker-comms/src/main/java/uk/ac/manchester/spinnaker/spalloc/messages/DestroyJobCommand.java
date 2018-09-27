package uk.ac.manchester.spinnaker.spalloc.messages;

/**
 * Request to destroy a job.
 */
public class DestroyJobCommand extends Command<Integer> {
	/**
	 * Make a request to destroy a job.
	 *
	 * @param jobId
	 *            The ID of the job.
	 */
	public DestroyJobCommand(int jobId) {
		super("destroy_job");
		addArg(jobId);
	}

	/**
	 * Make a request to destroy a job.
	 *
	 * @param jobId
	 *            The ID of the job.
	 * @param reason
	 *            Why the job is to be destroyed.
	 */
	public DestroyJobCommand(int jobId, String reason) {
		super("destroy_job");
		addArg(jobId);
		if (reason != null) {
			addKwArg("reason", reason);
		}
	}
}
