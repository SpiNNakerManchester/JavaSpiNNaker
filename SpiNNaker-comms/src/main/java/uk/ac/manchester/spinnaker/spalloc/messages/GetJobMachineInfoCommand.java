package uk.ac.manchester.spinnaker.spalloc.messages;

/**
 * Request to get machine information relating to a job.
 */
public class GetJobMachineInfoCommand extends Command<Integer> {
	/**
	 * Create a request to get information about a job's allocated machine.
	 *
	 * @param jobId
	 *            The job to ask about.
	 */
	public GetJobMachineInfoCommand(int jobId) {
		super("get_job_machine_info");
		addArg(jobId);
	}
}
