package uk.ac.manchester.spinnaker.messages;

/**
 * A SpiNNaker Multicast message. All multicast messages have a key and may also
 * <i>optionally</i> have a payload.
 */
public class MulticastMessage {
	/** The key of the packet. */
	public final int key;
	/**
	 * The payload of the packet if there is one, or <tt>null</tt> otherwise.
	 */
	public final Integer payload;

	/**
	 * Make a multicast message that has a key (determining the target
	 * locations) but which lacks the (optional) payload.
	 *
	 * @param key
	 *            The key of the packet
	 */
	public MulticastMessage(int key) {
		this.key = key;
		this.payload = null;
	}

	/**
	 * Make a multicast message that has a key (determining the target
	 * locations) and a payload.
	 *
	 * @param key
	 *            The key of the packet
	 * @param payload
	 *            The payload of the packet
	 */
	public MulticastMessage(int key, int payload) {
		this.key = key;
		this.payload = payload;
	}
}
