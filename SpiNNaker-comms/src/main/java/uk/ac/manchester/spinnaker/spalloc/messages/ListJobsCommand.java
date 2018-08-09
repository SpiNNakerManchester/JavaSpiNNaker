package uk.ac.manchester.spinnaker.spalloc.messages;

/**
 * Request the list of all jobs from the spalloc server.
 */
public class ListJobsCommand extends Command<Integer> {
	/**
	 * Create a request to list the jobs.
	 */
	public ListJobsCommand() {
		super("list_jobs");
	}
}
