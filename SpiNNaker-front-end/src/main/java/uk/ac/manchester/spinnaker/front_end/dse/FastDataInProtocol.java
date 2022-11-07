/*
 * Copyright (c) 2019 The University of Manchester
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
package uk.ac.manchester.spinnaker.front_end.dse;

import static java.lang.Integer.toUnsignedLong;
import static java.lang.Math.min;
import static java.lang.String.format;
import static java.nio.ByteBuffer.allocate;
import static java.nio.ByteOrder.LITTLE_ENDIAN;
import static uk.ac.manchester.spinnaker.front_end.dse.FastDataInCommandID.SEND_DATA_TO_LOCATION;
import static uk.ac.manchester.spinnaker.front_end.dse.FastDataInCommandID.SEND_SEQ_DATA;
import static uk.ac.manchester.spinnaker.front_end.dse.FastDataInCommandID.SEND_TELL_DATA_IN;
import static uk.ac.manchester.spinnaker.messages.Constants.SDP_PAYLOAD_WORDS;
import static uk.ac.manchester.spinnaker.messages.Constants.WORD_SIZE;
import static uk.ac.manchester.spinnaker.messages.sdp.SDPHeader.Flag.REPLY_NOT_EXPECTED;
import static uk.ac.manchester.spinnaker.messages.sdp.SDPPort.GATHERER_DATA_SPEED_UP;
import static uk.ac.manchester.spinnaker.utils.ByteBufferUtils.slice;
import static uk.ac.manchester.spinnaker.utils.MathUtils.ceildiv;

import java.nio.ByteBuffer;

import uk.ac.manchester.spinnaker.machine.ChipLocation;
import uk.ac.manchester.spinnaker.machine.HasChipLocation;
import uk.ac.manchester.spinnaker.machine.HasCoreLocation;
import uk.ac.manchester.spinnaker.machine.Machine;
import uk.ac.manchester.spinnaker.machine.MemoryLocation;
import uk.ac.manchester.spinnaker.messages.sdp.SDPHeader;
import uk.ac.manchester.spinnaker.messages.sdp.SDPMessage;

/**
 * Manufactures Fast Data In protocol messages.
 *
 * @author Donal Fellows
 * @author Alan Stokes
 */
class FastDataInProtocol {
	/** Items of data a SDP packet can hold when SCP header removed. */
	static final int BYTES_PER_FULL_PACKET = SDP_PAYLOAD_WORDS * WORD_SIZE;

	// 272 bytes as removed SCP header

	/**
	 * size of the location data packet (command, transaction id, start sdram
	 * address, x and y, and max packet number.
	 */
	static final int BYTES_FOR_LOCATION_PACKET = 5 * WORD_SIZE;

	/**
	 * Offset where data in starts on first command (command, transaction id,
	 * seq_number), in bytes.
	 */
	static final int OFFSET_AFTER_COMMAND_AND_KEY = 3 * WORD_SIZE;

	/** Size for data to store when packet with command and key. */
	static final int DATA_IN_FULL_PACKET_WITH_KEY =
			BYTES_PER_FULL_PACKET - OFFSET_AFTER_COMMAND_AND_KEY;

	/**
	 * size for data to store when sending tell packet (command id, transaction
	 * id).
	 */
	static final int BYTES_FOR_TELL_PACKET = 2 * WORD_SIZE;

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
				GATHERER_DATA_SPEED_UP.value);
	}

	/**
	 * @param baseAddress
	 *            Where the data is to be written.
	 * @param numPackets
	 *            How many SDP packets will be sent.
	 * @param transactionId
	 *            The transaction id of this stream.
	 * @return The message indicating the start of the data.
	 */
	SDPMessage dataToLocation(MemoryLocation baseAddress, int numPackets,
			int transactionId) {
		var payload = allocate(BYTES_FOR_LOCATION_PACKET).order(LITTLE_ENDIAN);
		payload.putInt(SEND_DATA_TO_LOCATION.value);
		payload.putInt(transactionId);
		payload.putInt(baseAddress.address);
		payload.putShort((short) boardLocalDestination.getY());
		payload.putShort((short) boardLocalDestination.getX());
		payload.putInt(numPackets - 1);
		payload.flip();
		return new SDPMessage(header(), payload);
	}

	/**
	 * @param data
	 *            The overall data to be transmitted.
	 * @param seqNum
	 *            The sequence number of this chunk.
	 *
	 * @param transactionId
	 *            The transaction id for this stream.
	 * @return The message containing a chunk of the data.
	 * @throws RuntimeException
	 *             If the sequence number is nonsense.
	 */
	SDPMessage seqData(ByteBuffer data, int seqNum, int transactionId) {
		var payload = allocate(BYTES_PER_FULL_PACKET).order(LITTLE_ENDIAN);
		int position = calculatePositionFromSequenceNumber(seqNum);
		if (position >= data.limit()) {
			throw new RuntimeException(format(
					"attempt to write off end of buffer due to "
							+ "over-large sequence number (%d) given "
							+ "that only %d bytes are to be sent",
					seqNum, toUnsignedLong(data.limit())));
		}
		payload.putInt(SEND_SEQ_DATA.value);
		payload.putInt(transactionId);
		payload.putInt(seqNum);
		putBuffer(data, position, payload);
		return new SDPMessage(header(), payload);
	}

	private int putBuffer(ByteBuffer data, int position, ByteBuffer payload) {
		var slice = slice(data, position,
				min(data.remaining(), payload.remaining()));
		payload.put(slice).flip();
		return slice.position();
	}

	private int calculatePositionFromSequenceNumber(int seqNum) {
		return DATA_IN_FULL_PACKET_WITH_KEY * seqNum;
	}

	/**
	 * generates the tell message.
	 *
	 * @param transactionId
	 *            The transaction id for this stream.
	 * @return The message indicating the end of the data.
	 */
	SDPMessage tellDataIn(int transactionId) {
		var payload = allocate(BYTES_FOR_TELL_PACKET).order(LITTLE_ENDIAN);
		payload.putInt(SEND_TELL_DATA_IN.value);
		payload.putInt(transactionId);
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
		return ceildiv(data.remaining(), DATA_IN_FULL_PACKET_WITH_KEY);
	}
}
