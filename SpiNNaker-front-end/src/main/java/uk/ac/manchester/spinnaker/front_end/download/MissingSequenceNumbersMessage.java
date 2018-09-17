package uk.ac.manchester.spinnaker.front_end.download;

import static java.lang.Math.min;
import static java.nio.ByteBuffer.allocate;
import static java.nio.ByteOrder.LITTLE_ENDIAN;
import static uk.ac.manchester.spinnaker.front_end.download.Constants.DATA_PER_FULL_PACKET;
import static uk.ac.manchester.spinnaker.front_end.download.HostDataReceiver.ceildiv;
import static uk.ac.manchester.spinnaker.front_end.download.ProtocolID.NEXT_MISSING_SEQS;
import static uk.ac.manchester.spinnaker.front_end.download.ProtocolID.START_MISSING_SEQS;
import static uk.ac.manchester.spinnaker.messages.Constants.WORD_SIZE;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import uk.ac.manchester.spinnaker.machine.HasCoreLocation;

/**
 * A message used to describe what sequence numbers are missing from a fast data
 * transfer stream so that they can be retransmitted.
 */
final class MissingSequenceNumbersMessage extends ProtocolMessage {
	/** Number of words of overhead in a first message. */
	private static final int FIRST_OVERHEAD_WORDS = 2;
	/** Number of words of overhead in a subsequent message. */
	private static final int NEXT_OVERHEAD_WORDS = 1;
	/** How many sequence numbers fit in the first message. */
	static final int MAX_FIRST_SIZE =
			DATA_PER_FULL_PACKET - FIRST_OVERHEAD_WORDS;
	/** How many sequence numbers fit in each subsequent message. */
	static final int MAX_NEXT_SIZE = DATA_PER_FULL_PACKET - NEXT_OVERHEAD_WORDS;

	/**
	 * Compute the number of packets required to send a given count of sequence
	 * numbers.
	 *
	 * @param numSequenceNumbers
	 *            The number of sequence numbers to move.
	 * @return The number of packets required.
	 */
	static int computeNumberOfPackets(int numSequenceNumbers) {
		int numPackets = 1;
		int remainingSeqNums = numSequenceNumbers - MAX_FIRST_SIZE;
		if (remainingSeqNums > 0) {
			numPackets += ceildiv(remainingSeqNums, MAX_NEXT_SIZE);
		}
		return numPackets;
	}

	/**
	 * Create the first message describing a bunch of missing sequence numbers.
	 *
	 * @param destination
	 *            Where to send the message
	 * @param destPort
	 *            Which port to send the message to
	 * @param missingSeqs
	 *            The collection of missing sequence numbers. <i>Modified by
	 *            this method.</i>
	 * @param numPackets
	 *            The total number of packets that will describe the collection.
	 * @return First message to send.
	 */
	static MissingSequenceNumbersMessage createFirst(
			HasCoreLocation destination, int destPort, IntBuffer missingSeqs,
			int numPackets) {
		ByteBuffer data = allocate(min(DATA_PER_FULL_PACKET,
				missingSeqs.remaining() + FIRST_OVERHEAD_WORDS) * WORD_SIZE)
						.order(LITTLE_ENDIAN);
		data.putInt(START_MISSING_SEQS.value);
		data.putInt(numPackets);
		return create(destination, destPort, missingSeqs, data);
	}

	/**
	 * Create a subsequent message describing a bunch of missing sequence
	 * numbers.
	 *
	 * @param destination
	 *            Where to send the message
	 * @param destPort
	 *            Which port to send the message to
	 * @param missingSeqs
	 *            The collection of missing sequence numbers. <i>Modified by
	 *            this method.</i>
	 * @return Subsequent message to send.
	 */
	static MissingSequenceNumbersMessage createNext(HasCoreLocation destination,
			int destPort, IntBuffer missingSeqs) {
		ByteBuffer data = allocate(min(DATA_PER_FULL_PACKET,
				missingSeqs.remaining() + NEXT_OVERHEAD_WORDS) * WORD_SIZE)
						.order(LITTLE_ENDIAN);
		data.putInt(NEXT_MISSING_SEQS.value);
		return create(destination, destPort, missingSeqs, data);
	}

	private static MissingSequenceNumbersMessage create(
			HasCoreLocation destination, int destPort, IntBuffer missingSeqs,
			ByteBuffer data) {
		while (data.hasRemaining() || missingSeqs.hasRemaining()) {
			data.putInt(missingSeqs.get());
		}
		data.flip();
		return new MissingSequenceNumbersMessage(destination, destPort, data);
	}

	private MissingSequenceNumbersMessage(HasCoreLocation destination,
			int destPort, ByteBuffer payload) {
		super(destination, destPort, payload);
	}
}
