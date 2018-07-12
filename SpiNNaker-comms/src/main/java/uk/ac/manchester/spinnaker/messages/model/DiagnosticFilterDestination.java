package uk.ac.manchester.spinnaker.messages.model;

/**
 * Destination flags for the diagnostic filters. Note that only one has to match
 * for the counter to be incremented
 */
public enum DiagnosticFilterDestination {
	/** Destination is to dump the packet */
	DUMP(0),
	/** Destination is a local core (but not the monitor core) */
	LOCAL(1),
	/** Destination is the local monitor core */
	LOCAL_MONITOR(2),
	/** Destination is link 0 */
	LINK_0(3),
	/** Destination is link 1 */
	LINK_1(4),
	/** Destination is link 2 */
	LINK_2(5),
	/** Destination is link 3 */
	LINK_3(6),
	/** Destination is link 4 */
	LINK_4(7),
	/** Destination is link 5 */
	LINK_5(8);
	public final int value;

	private DiagnosticFilterDestination(int value) {
		this.value = value;
	}
}
