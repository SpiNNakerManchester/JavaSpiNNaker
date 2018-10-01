package uk.ac.manchester.spinnaker.front_end.download;

import static java.nio.ByteBuffer.allocate;
import static java.nio.ByteOrder.LITTLE_ENDIAN;
import static uk.ac.manchester.spinnaker.front_end.download.ProtocolID.START_SENDING_DATA;
import static uk.ac.manchester.spinnaker.messages.Constants.WORD_SIZE;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import uk.ac.manchester.spinnaker.machine.HasCoreLocation;

/**
 * A message used to request fast data transfer from SpiNNaker to Host.
 */
final class StartSendingMessage extends ProtocolMessage {
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
	static StartSendingMessage create(HasCoreLocation destination, int destPort,
			int address, int length) {
		ByteBuffer payload =
				allocate(NUM_WORDS * WORD_SIZE).order(LITTLE_ENDIAN);
		IntBuffer msgPayload = payload.asIntBuffer();
		msgPayload.put(START_SENDING_DATA.value);
		msgPayload.put(address);
		msgPayload.put(length);
		return new StartSendingMessage(destination, destPort, payload);
	}

	private StartSendingMessage(HasCoreLocation destination, int destPort,
			ByteBuffer payload) {
		super(destination, destPort, payload);
	}
}
