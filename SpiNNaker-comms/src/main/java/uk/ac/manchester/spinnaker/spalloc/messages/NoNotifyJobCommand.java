package uk.ac.manchester.spinnaker.spalloc.messages;

/**
 * Request to not receive notifications about a job.
 */
public class NoNotifyJobCommand extends Command<Integer> {
	/**
	 * Create a request to not be notified of changes in job state.
	 *
	 * @param jobId
	 *            The job to request about.
	 */
	public NoNotifyJobCommand(int jobId) {
		super("no_notify_job");
		addArg(jobId);
	}

	/**
	 * Create a request to not be notified of changes in all jobs' state.
	 */
	public NoNotifyJobCommand() {
		super("no_notify_job");
	}
}
