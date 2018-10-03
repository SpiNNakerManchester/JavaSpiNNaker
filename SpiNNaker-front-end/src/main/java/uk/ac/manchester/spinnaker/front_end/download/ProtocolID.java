package uk.ac.manchester.spinnaker.front_end.download;

/** The various IDs of messages used in the fast download protocol. */
public enum ProtocolID {
	/** ID of message used to start sending data. */
	START_SENDING_DATA(100),
	/** ID of message used to start sending missing sequence numbers. */
	START_MISSING_SEQS(1000),
	/** ID of message used to send more missing sequence numbers. */
	NEXT_MISSING_SEQS(1001);
	/** The value of the ID. */
	public final int value;

	ProtocolID(int value) {
		this.value = value;
	}
}
