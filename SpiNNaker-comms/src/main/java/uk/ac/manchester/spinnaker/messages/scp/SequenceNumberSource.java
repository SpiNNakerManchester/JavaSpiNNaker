package uk.ac.manchester.spinnaker.messages.scp;

/** Where to get sequence numbers from. */
public abstract class SequenceNumberSource {
	/** The number of items in the sequence. */
	public static final int SEQUENCE_LENGTH = 65536;

	private SequenceNumberSource() {
	}

	/** Keep a global track of the sequence numbers used. */
	private static int nextSequence = 0;

	/**
	 * Get the next number from the global sequence, applying appropriate
	 * wrapping rules as the sequence numbers have a fixed number of bits.
	 *
	 * @return the next sequence number; these loop between 0 and 65535
	 *         (unsigned).
	 */
	static synchronized short getNextSequenceNumber() {
		int seq = nextSequence;
		nextSequence = (nextSequence + 1) % SEQUENCE_LENGTH;
		return (short) seq;
	}
}
