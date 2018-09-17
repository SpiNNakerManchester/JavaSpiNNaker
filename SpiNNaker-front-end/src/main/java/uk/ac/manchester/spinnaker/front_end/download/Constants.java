package uk.ac.manchester.spinnaker.front_end.download;

import static uk.ac.manchester.spinnaker.messages.Constants.WORD_SIZE;

/** Various constants used by the implementation. */
abstract class Constants {
	private Constants() {
	}

	static final int QUEUE_CAPACITY = 1024;
	// consts for data and converting between words and bytes
	static final int DATA_PER_FULL_PACKET = 68;
	static final int DATA_PER_FULL_PACKET_WITH_SEQUENCE_NUM =
			DATA_PER_FULL_PACKET - 1;
	static final int END_FLAG_SIZE_IN_BYTES = WORD_SIZE;
	static final int SEQUENCE_NUMBER_SIZE = WORD_SIZE;
	static final int LAST_MESSAGE_FLAG_BIT_MASK = 0x80000000;
	// time out constants
	public static final int TIMEOUT_RETRY_LIMIT = 20;
	static final int TIMEOUT_PER_SENDING_IN_MILLISECONDS = 10;
	static final int TIMEOUT_PER_RECEIVE_IN_MILLISECONDS = 250;
}
