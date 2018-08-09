package uk.ac.manchester.spinnaker.spalloc.commands;

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
}
