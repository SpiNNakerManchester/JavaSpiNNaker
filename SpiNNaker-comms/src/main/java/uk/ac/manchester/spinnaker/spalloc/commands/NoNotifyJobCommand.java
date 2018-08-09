package uk.ac.manchester.spinnaker.spalloc.commands;

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
}
