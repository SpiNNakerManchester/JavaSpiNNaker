/*
 * Copyright (c) 2018-2019 The University of Manchester
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

import static java.lang.Math.min;
import static java.nio.ByteBuffer.allocate;
import static java.nio.ByteOrder.LITTLE_ENDIAN;
import static uk.ac.manchester.spinnaker.front_end.Constants.NEXT_MESSAGES_COUNT;
import static uk.ac.manchester.spinnaker.front_end.download.GatherProtocolMessage.ID.NEXT_MISSING_SEQS;
import static uk.ac.manchester.spinnaker.front_end.download.GatherProtocolMessage.ID.START_MISSING_SEQS;
import static uk.ac.manchester.spinnaker.messages.Constants.SDP_PAYLOAD_WORDS;
import static uk.ac.manchester.spinnaker.messages.Constants.WORD_SIZE;
import static uk.ac.manchester.spinnaker.messages.sdp.SDPPort.EXTRA_MONITOR_CORE_DATA_SPEED_UP;
import static uk.ac.manchester.spinnaker.utils.MathUtils.ceildiv;

import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.List;

import uk.ac.manchester.spinnaker.machine.HasCoreLocation;
import uk.ac.manchester.spinnaker.utils.MappableIterable;

/**
 * A message used to describe what sequence numbers are missing from a fast data
 * transfer stream so that they can be retransmitted.
 */
public final class MissingSequenceNumbersMessage extends GatherProtocolMessage {
	/** Number of words of overhead in a first message. */
	private static final int FIRST_OVERHEAD_WORDS = 3;

	/** Number of words of overhead in a subsequent message. */
	private static final int NEXT_OVERHEAD_WORDS = 2;

	/** How many sequence numbers fit in the first message. */
	private static final int MAX_FIRST_SIZE =
			SDP_PAYLOAD_WORDS - FIRST_OVERHEAD_WORDS;

	/** How many sequence numbers fit in each subsequent message. */
	private static final int MAX_NEXT_SIZE =
			SDP_PAYLOAD_WORDS - NEXT_OVERHEAD_WORDS;

	/**
	 * Max number of sequence numbers to send in one go when asking for
	 * retransmission.
	 */
	private static final int MAX_REQ_LOAD =
			MAX_FIRST_SIZE + NEXT_MESSAGES_COUNT * MAX_NEXT_SIZE;

	/**
	 * Compute the number of packets required to send a given count of sequence
	 * numbers.
	 *
	 * @param numSequenceNumbers
	 *            The number of sequence numbers to move.
	 * @return The number of packets required.
	 */
	private static int computeNumberOfPackets(int numSequenceNumbers) {
		int numPackets = 1;
		int remainingSeqNums = numSequenceNumbers - MAX_FIRST_SIZE;
		if (remainingSeqNums > 0) {
			numPackets += ceildiv(remainingSeqNums, MAX_NEXT_SIZE);
		}
		return numPackets;
	}

	/**
	 * Allocate a buffer of the right size.
	 *
	 * @param numDataWords
	 *            The number of data words that we still want to transmit.
	 * @param overhead
	 *            The header overhead for the type of packet (in words).
	 * @return The allocated little-endian buffer.
	 */
	private static ByteBuffer allocateWords(int numDataWords, int overhead) {
		int numWords = min(SDP_PAYLOAD_WORDS, numDataWords + overhead);
		return allocate(numWords * WORD_SIZE).order(LITTLE_ENDIAN);
	}

	/**
	 * Create the messages describing a bunch of missing sequence numbers.
	 *
	 * @param destination
	 *            Where to send the messages
	 * @param missingSeqs
	 *            The collection of missing sequence numbers.
	 * @param transactionId
	 *            The transaction id of this stream.
	 * @return Iterable of the messages to send.
	 */
	static MappableIterable<MissingSequenceNumbersMessage> createMessages(
			HasCoreLocation destination, List<Integer> missingSeqs,
			int transactionId) {
		var work = reduce(missingSeqs);
		int numPackets = computeNumberOfPackets(work.size());
		var dest = destination.asCoreLocation();
		return () -> new Iterator<MissingSequenceNumbersMessage>() {
			int pktNum = 0;
			int index = 0;

			@Override
			public boolean hasNext() {
				return pktNum < numPackets;
			}

			@Override
			public MissingSequenceNumbersMessage next() {
				ByteBuffer data;
				// Allocate and write header
				int remaining = work.size() - index;
				if (pktNum++ == 0) {
					data = allocateWords(remaining, FIRST_OVERHEAD_WORDS);
					data.putInt(START_MISSING_SEQS.value);
					data.putInt(transactionId);
					data.putInt(numPackets);
				} else {
					data = allocateWords(remaining, NEXT_OVERHEAD_WORDS);
					data.putInt(NEXT_MISSING_SEQS.value);
					data.putInt(transactionId);
				}

				// Write body
				while (data.hasRemaining() && index < work.size()) {
					data.putInt(work.get(index++));
				}
				data.flip();

				// Package the message
				return new MissingSequenceNumbersMessage(dest, data);
			}
		};
	}

	private static List<Integer> reduce(List<Integer> missingSeqs) {
		if (missingSeqs.size() > MAX_REQ_LOAD) {
			return missingSeqs.subList(0, MAX_REQ_LOAD);
		}
		return missingSeqs;
	}

	private MissingSequenceNumbersMessage(HasCoreLocation destination,
			ByteBuffer payload) {
		super(destination, EXTRA_MONITOR_CORE_DATA_SPEED_UP, payload);
	}
}
