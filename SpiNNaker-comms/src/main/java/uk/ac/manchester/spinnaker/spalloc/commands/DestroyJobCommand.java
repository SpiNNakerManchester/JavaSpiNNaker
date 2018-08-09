package uk.ac.manchester.spinnaker.spalloc.commands;

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
}
