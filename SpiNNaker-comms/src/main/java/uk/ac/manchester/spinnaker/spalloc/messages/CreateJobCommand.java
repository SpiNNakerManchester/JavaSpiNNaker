package uk.ac.manchester.spinnaker.spalloc.messages;

import java.util.List;
import java.util.Map;

/**
 * Request to create a job.
 */
public class CreateJobCommand extends Command<Integer> {
	/**
	 * Create a request to create a job.
	 *
	 * @param numBoards
	 *            The number of boards to request.
	 * @param owner
	 *            The owner of the job to create.
	 */
	public CreateJobCommand(int numBoards, String owner) {
		super("create_job");
		addArg(numBoards);
		addKwArg("owner", owner);
	}

	/**
	 * Create a request to create a job.
	 *
	 * @param args
	 *            The arguments, describing default (empty), the number of
	 *            boards (one arg), the triad size (two args) or the board
	 *            location (three args).
	 * @param kwargs
	 *            Additional arguments required. Must include the key
	 *            <tt>owner</tt>. Values can be boxed primitive types or
	 *            strings.
	 */
	public CreateJobCommand(List<Integer> args, Map<String, Object> kwargs) {
		super("create_job");
		for (int i : args) {
			addArg(i);
		}
		if (!kwargs.containsKey("owner")) {
			throw new IllegalArgumentException(
					"owner must be specified for all jobs");
		}
		for (String key : kwargs.keySet()) {
			addKwArg(key, kwargs.get(key));
		}
	}
}
