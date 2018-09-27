package uk.ac.manchester.spinnaker.spalloc.messages;

/**
 * Request to get notifications about a job.
 */
public class NotifyJobCommand extends Command<Integer> {
	/**
	 * Create a request to be notified of changes in job state.
	 *
	 * @param jobId
	 *            The job to request about.
	 */
	public NotifyJobCommand(int jobId) {
		super("notify_job");
		addArg(jobId);
	}

	/**
	 * Create a request to be notified of changes in all jobs' state.
	 */
	public NotifyJobCommand() {
		super("notify_job");
	}
}
