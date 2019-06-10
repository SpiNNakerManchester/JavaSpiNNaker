package uk.ac.manchester.spinnaker.front_end.dse;

import static java.lang.Integer.toUnsignedLong;
import static java.lang.Math.max;
import static java.lang.Math.min;
import static java.lang.String.format;
import static java.nio.ByteBuffer.allocate;
import static java.nio.ByteOrder.LITTLE_ENDIAN;
import static uk.ac.manchester.spinnaker.front_end.dse.FastDataInCommandID.SEND_DATA_TO_LOCATION;
import static uk.ac.manchester.spinnaker.front_end.dse.FastDataInCommandID.SEND_LAST_DATA_IN;
import static uk.ac.manchester.spinnaker.front_end.dse.FastDataInCommandID.SEND_SEQ_DATA;
import static uk.ac.manchester.spinnaker.messages.Constants.SDP_PAYLOAD_WORDS;
import static uk.ac.manchester.spinnaker.messages.Constants.WORD_SIZE;
import static uk.ac.manchester.spinnaker.messages.sdp.SDPHeader.Flag.REPLY_NOT_EXPECTED;
import static uk.ac.manchester.spinnaker.utils.MathUtils.ceildiv;

import java.nio.ByteBuffer;

import uk.ac.manchester.spinnaker.machine.ChipLocation;
import uk.ac.manchester.spinnaker.machine.HasChipLocation;
import uk.ac.manchester.spinnaker.machine.HasCoreLocation;
import uk.ac.manchester.spinnaker.machine.Machine;
import uk.ac.manchester.spinnaker.messages.sdp.SDPHeader;
import uk.ac.manchester.spinnaker.messages.sdp.SDPMessage;
import uk.ac.manchester.spinnaker.messages.sdp.SDPPort;

/**
 * Manufactures Fast Data In protocol messages.
 *
 * @author Donal Fellows
 */
class FastDataInProtocol {
	/** Items of data a SDP packet can hold when SCP header removed. */
	static final int BYTES_PER_FULL_PACKET = SDP_PAYLOAD_WORDS * WORD_SIZE;
	// 272 bytes as removed SCP header

	/**
	 * Offset where data in starts on first command (command, base_address,
	 * x&amp;y, max_seq_number), in bytes.
	 */
	static final int OFFSET_AFTER_COMMAND_AND_ADDRESS = 4 * WORD_SIZE;

	/**
	 * Offset where data starts after a command ID and seq number, in bytes.
	 */
	static final int OFFSET_AFTER_COMMAND_AND_SEQUENCE = 2 * WORD_SIZE;

	/** Size for data to store when first packet with command and address. */
	static final int DATA_IN_FULL_PACKET_WITH_ADDRESS =
			BYTES_PER_FULL_PACKET - OFFSET_AFTER_COMMAND_AND_ADDRESS;

	/** Size for data in to store when not first packet. */
	static final int DATA_IN_FULL_PACKET_WITHOUT_ADDRESS =
			BYTES_PER_FULL_PACKET - OFFSET_AFTER_COMMAND_AND_SEQUENCE;

	private final HasCoreLocation gathererCore;
	private final HasChipLocation boardLocalDestination;

	/**
	 * Create an instance of the protocol for talking to a particular extra
	 * monitor core on a particular board.
	 *
	 * @param machine
	 *            The machine containing the board.
	 * @param gathererCore
	 *            The gatherer core on the board that messages will be routed
	 *            via.
	 * @param monitorChip
	 *            The extra monitor core on the board that is the destination
	 *            for the messages.
	 */
	FastDataInProtocol(Machine machine, HasCoreLocation gathererCore,
			HasChipLocation monitorChip) {
		this.gathererCore = gathererCore;

		int boardLocalX = monitorChip.getX() - gathererCore.getX();
		if (boardLocalX < 0) {
			boardLocalX += machine.maxChipX() + 1;
		}
		int boardLocalY = monitorChip.getY() - gathererCore.getY();
		if (boardLocalY < 0) {
			boardLocalY += machine.maxChipY() + 1;
		}
		this.boardLocalDestination = new ChipLocation(boardLocalX, boardLocalY);
	}

	private SDPHeader header() {
		return new SDPHeader(REPLY_NOT_EXPECTED, gathererCore,
				SDPPort.GATHERER_DATA_SPEED_UP.value);
	}

	/**
	 * @param baseAddress
	 *            Where the data is to be written.
	 * @param data
	 *            The overall data to be transmitted.
	 * @param numPackets
	 *            How many SDP packets will be sent.
	 * @return The message indicating the start of the data.
	 */
	SDPMessage dataToLocation(int baseAddress, ByteBuffer data,
			int numPackets) {
		ByteBuffer payload =
				allocate(BYTES_PER_FULL_PACKET).order(LITTLE_ENDIAN);
		payload.putInt(SEND_DATA_TO_LOCATION.value);
		payload.putInt(baseAddress);
		payload.putShort((short) boardLocalDestination.getY());
		payload.putShort((short) boardLocalDestination.getX());
		payload.putInt(numPackets - 1);
		putBuffer(data, 0, payload);
		return new SDPMessage(header(), payload);
	}

	/**
	 * @param data
	 *            The overall data to be transmitted.
	 * @param seqNum
	 *            The sequence number of this chunk.
	 * @return The message containing a chunk of the data.
	 */
	SDPMessage seqData(ByteBuffer data, int seqNum) {
		ByteBuffer payload =
				allocate(BYTES_PER_FULL_PACKET).order(LITTLE_ENDIAN);
		int position = calculatePositionFromSequenceNumber(seqNum);
		if (position >= data.limit()) {
			throw new RuntimeException(format(
					"attempt to write off end of buffer due to "
							+ "over-large sequence number (%d) given "
							+ "that only %d bytes are to be sent",
					seqNum, toUnsignedLong(data.limit())));
		}
		payload.putInt(SEND_SEQ_DATA.value);
		payload.putInt(seqNum);
		putBuffer(data, position, payload);
		return new SDPMessage(header(), payload);
	}

	private int putBuffer(ByteBuffer data, int position, ByteBuffer payload) {
		ByteBuffer tmp = data.asReadOnlyBuffer();
		tmp.position(position);
		ByteBuffer slice = tmp.slice();
		slice.limit(min(slice.limit(), payload.remaining()));
		payload.put(slice).flip();
		return slice.position();
	}

	private int calculatePositionFromSequenceNumber(int seqNum) {
		if (seqNum < 1) {
			return 0;
		}
		return DATA_IN_FULL_PACKET_WITH_ADDRESS
				+ DATA_IN_FULL_PACKET_WITHOUT_ADDRESS * (seqNum - 1);
	}

	/**
	 * @return The message indicating the end of the data.
	 */
	SDPMessage lastDataIn() {
		ByteBuffer payload = allocate(WORD_SIZE).order(LITTLE_ENDIAN);
		payload.putInt(SEND_LAST_DATA_IN.value);
		payload.flip();
		return new SDPMessage(header(), payload);
	}

	/**
	 * Computes the number of packets required to send the given data.
	 *
	 * @param data
	 *            The data being sent. (This operation only reads.)
	 * @return The number of packets (i.e. 1 more than the max sequence number).
	 */
	static int computeNumPackets(ByteBuffer data) {
		return ceildiv(
				max(data.remaining() - DATA_IN_FULL_PACKET_WITH_ADDRESS, 0),
				DATA_IN_FULL_PACKET_WITHOUT_ADDRESS) + 1;
	}
}
