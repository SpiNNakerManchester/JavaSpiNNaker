package uk.ac.manchester.spinnaker.spalloc.messages;

/**
 * Request the version of the spalloc server.
 */
public class VersionCommand extends Command<Integer> {
	/**
	 * Create a request to get the version of the spalloc server.
	 */
	public VersionCommand() {
		super("version");
	}
}
