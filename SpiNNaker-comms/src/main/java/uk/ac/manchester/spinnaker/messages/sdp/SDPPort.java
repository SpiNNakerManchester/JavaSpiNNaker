package uk.ac.manchester.spinnaker.messages.sdp;

/** SDP port handling output buffering data streaming. */
public enum SDPPort {
	/** Default port. */
	DEFAULT_PORT(0),
	/** Command port for the buffered in functionality. */
	INPUT_BUFFERING_SDP_PORT(1),
	/** Command port for the buffered out functionality. */
	OUTPUT_BUFFERING_SDP_PORT(2),
	/** Command port for resetting runtime, etc. */
	RUNNING_COMMAND_SDP_PORT(3),
	/** Extra monitor core re injection functionality. */
	EXTRA_MONITOR_CORE_REINJECTION(4),
	/** Extra monitor core data transfer functionality. */
	EXTRA_MONITOR_CORE_DATA_SPEED_UP(5);
	/** The port ID. */
	public final int value;

	SDPPort(int value) {
		this.value = value;
	}
}
