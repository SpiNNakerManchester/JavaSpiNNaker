package uk.ac.manchester.spinnaker.messages.model;

/**
 * Source flags for the diagnostic filters. Note that only one has to match for
 * the counter to be incremented.
 */
public enum DiagnosticFilterSource {
	/** Source is a local core */
	LOCAL(0),
	/** Source is not a local core */
	NON_LOCAL(1);
	public final int value;

	private DiagnosticFilterSource(int value) {
		this.value = value;
	}
}
