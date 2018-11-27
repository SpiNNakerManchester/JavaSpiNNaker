/*
 * Copyright (c) 2018 The University of Manchester
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package uk.ac.manchester.spinnaker.front_end.download;

import static uk.ac.manchester.spinnaker.messages.Constants.WORD_SIZE;

/** Various constants used by the implementation. */
abstract class Constants {
	private Constants() {
	}

	/** How many entries should there be in the queue of received packets. */
	static final int QUEUE_CAPACITY = 1024;

	// consts for data and converting between words and bytes

	/** What is the maximum number of <i>words</i> in a packet? */
	static final int DATA_PER_FULL_PACKET = 68;
	/**
	 * What is the maximum number of payload <i>words</i> in a packet that also
	 * has a sequence number?
	 */
	static final int DATA_PER_FULL_PACKET_WITH_SEQUENCE_NUM =
			DATA_PER_FULL_PACKET - 1;
	/** How many bytes for the end-flag? */
	static final int END_FLAG_SIZE_IN_BYTES = WORD_SIZE;
	/** How many bytes for the sequence number? */
	static final int SEQUENCE_NUMBER_SIZE = WORD_SIZE;
	/**
	 * Mask used to pick out the bit tha says whether a sequence number is the
	 * last in a stream.
	 */
	static final int LAST_MESSAGE_FLAG_BIT_MASK = 0x80000000;

	// time out constants

	/** The maximum number of times to retry. */
	static final int TIMEOUT_RETRY_LIMIT = 20;
	/** The time delay between sending each message. */
	static final int TIMEOUT_PER_SENDING_IN_MILLISECONDS = 10;
	/** The timeout when receiving message. */
	static final int TIMEOUT_PER_RECEIVE_IN_MILLISECONDS = 250;
}
