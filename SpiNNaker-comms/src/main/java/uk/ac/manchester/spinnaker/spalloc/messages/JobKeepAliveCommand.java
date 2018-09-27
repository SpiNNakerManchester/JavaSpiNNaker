package uk.ac.manchester.spinnaker.spalloc.messages;

/**
 * Request to keep a job alive.
 */
public class JobKeepAliveCommand extends Command<Integer> {
	/**
	 * Create a request to keep a job alive.
	 *
	 * @param jobId
	 *            The job to ask about.
	 */
	public JobKeepAliveCommand(int jobId) {
		super("job_keepalive");
		addArg(jobId);
	}
}
