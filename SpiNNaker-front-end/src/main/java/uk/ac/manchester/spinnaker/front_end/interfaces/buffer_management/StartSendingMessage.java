package uk.ac.manchester.spinnaker.front_end.interfaces.buffer_management;

import static java.nio.ByteBuffer.allocate;
import static java.nio.ByteOrder.LITTLE_ENDIAN;
import static uk.ac.manchester.spinnaker.front_end.interfaces.buffer_management.GatherProtocolMessage.ID.START_SENDING_DATA;
import static uk.ac.manchester.spinnaker.messages.Constants.WORD_SIZE;
import static uk.ac.manchester.spinnaker.messages.sdp.SDPPort.EXTRA_MONITOR_CORE_DATA_SPEED_UP;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import uk.ac.manchester.spinnaker.machine.HasCoreLocation;

/**
 * A message used to request fast data transfer from SpiNNaker to Host.
 */
public final class StartSendingMessage extends GatherProtocolMessage {
	private static final int NUM_WORDS = 3;

	/**
	 * Create a message used to request fast data transfer from SpiNNaker to
	 * Host.
	 *
	 * @param destination
	 *            Where to send the message
	 * @param destPort
	 *            Which port to send the message to
	 * @param address
	 *            Where to start reading from
	 * @param length
	 *            How many bytes to read
	 * @return The created message.
	 */
	static StartSendingMessage create(HasCoreLocation destination, int address,
			int length) {
		ByteBuffer payload =
				allocate(NUM_WORDS * WORD_SIZE).order(LITTLE_ENDIAN);
		IntBuffer msgPayload = payload.asIntBuffer();
		msgPayload.put(START_SENDING_DATA.value);
		msgPayload.put(address);
		msgPayload.put(length);
		return new StartSendingMessage(destination, payload);
	}

	private StartSendingMessage(HasCoreLocation destination,
			ByteBuffer payload) {
		super(destination, EXTRA_MONITOR_CORE_DATA_SPEED_UP, payload);
	}
}
