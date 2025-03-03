/*
 * Copyright (c) 2025 The University of Manchester
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package uk.ac.manchester.spinnaker.protocols;

import static java.lang.Integer.toUnsignedLong;
import static java.lang.Math.min;
import static java.lang.String.format;
import static java.nio.ByteBuffer.allocate;
import static java.nio.ByteOrder.LITTLE_ENDIAN;
import static java.util.stream.Collectors.toList;
import static java.util.stream.IntStream.range;
import static org.slf4j.LoggerFactory.getLogger;
import static uk.ac.manchester.spinnaker.protocols.FastDataInCommandID.SEND_DATA_TO_LOCATION;
import static uk.ac.manchester.spinnaker.protocols.FastDataInCommandID.SEND_SEQ_DATA;
import static uk.ac.manchester.spinnaker.protocols.FastDataInCommandID.SEND_TELL_DATA_IN;
import static uk.ac.manchester.spinnaker.messages.Constants.SDP_PAYLOAD_WORDS;
import static uk.ac.manchester.spinnaker.messages.Constants.WORD_SIZE;
import static uk.ac.manchester.spinnaker.messages.sdp.SDPHeader.Flag.REPLY_NOT_EXPECTED;
import static uk.ac.manchester.spinnaker.messages.sdp.SDPPort.GATHERER_DATA_SPEED_UP;
import static uk.ac.manchester.spinnaker.utils.ByteBufferUtils.slice;
import static uk.ac.manchester.spinnaker.utils.MathUtils.ceildiv;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.BitSet;

import org.slf4j.Logger;

import com.google.errorprone.annotations.CheckReturnValue;
import com.google.errorprone.annotations.MustBeClosed;

import uk.ac.manchester.spinnaker.connections.ThrottledConnection;
import uk.ac.manchester.spinnaker.machine.ChipLocation;
import uk.ac.manchester.spinnaker.machine.CoreLocation;
import uk.ac.manchester.spinnaker.machine.HasChipLocation;
import uk.ac.manchester.spinnaker.machine.HasCoreLocation;
import uk.ac.manchester.spinnaker.machine.MemoryLocation;
import uk.ac.manchester.spinnaker.machine.tags.IPTag;
import uk.ac.manchester.spinnaker.messages.sdp.SDPHeader;
import uk.ac.manchester.spinnaker.messages.sdp.SDPLocation;
import uk.ac.manchester.spinnaker.messages.sdp.SDPMessage;
import uk.ac.manchester.spinnaker.transceiver.ProcessException;
import uk.ac.manchester.spinnaker.transceiver.TransceiverInterface;

public class FastDataIn implements AutoCloseable {

	private static final Logger log = getLogger(FastDataIn.class);

	/** Items of data a SDP packet can hold when SCP header removed. */
	private static final int BYTES_PER_FULL_PACKET =
			SDP_PAYLOAD_WORDS * WORD_SIZE;

	// 272 bytes as removed SCP header

	/**
	 * size of the location data packet (command, transaction id, start sdram
	 * address, x and y, and max packet number.
	 */
	private static final int BYTES_FOR_LOCATION_PACKET = 5 * WORD_SIZE;

	/**
	 * Offset where data in starts on first command (command, transaction id,
	 * seq_number), in bytes.
	 */
	private static final int OFFSET_AFTER_COMMAND_AND_KEY = 3 * WORD_SIZE;

	/** Size for data to store when packet with command and key. */
	private static final int DATA_IN_FULL_PACKET_WITH_KEY =
			BYTES_PER_FULL_PACKET - OFFSET_AFTER_COMMAND_AND_KEY;

	/**
	 * size for data to store when sending tell packet (command id, transaction
	 * id).
	 */
	private static final int BYTES_FOR_TELL_PACKET = 2 * WORD_SIZE;

	private static final int TIMEOUT_RETRY_LIMIT = 100;

	/** flag for saying missing all SEQ numbers. */
	private static final int FLAG_FOR_MISSING_ALL_SEQUENCES = 0xFFFFFFFE;

	/** Sequence number that marks the end of a sequence number stream. */
	private static final int MISSING_SEQS_END = -1;

	private final int maxChipX;

	private final int maxChipY;

	private final HasCoreLocation gathererCore;

	private final ThrottledConnection connection;

	/** The current transaction id for the board. */
	private int transactionId = 0;

	/**
	 * Create an instance of the protocol for talking to a particular extra
	 * monitor core on a particular board.
	 *
	 * @param maxChipX The maximum X coordinate of the machine to speak to.
	 * @param maxChipY The maximum Y coordinate of the machine to speak to.
	 * @param gathererCore
	 *            The gatherer core on the board that messages will be routed
	 *            via.
	 * @param transceiver A transceiver to use for communications.
	 * @param ethernetChip The Ethernet chip to talk to the chip via.
	 * @param ethernetAddress The Ethernet address of the chip to talk via.
	 * @param iptag The IPTag to use for communications.
	 * @throws IOException
	 *             If IO fails.
	 * @throws ProcessException
	 *             If SpiNNaker rejects the reprogramming of the tag.
	 * @throws InterruptedException
	 *             If communications are interrupted.
	 */
	@MustBeClosed
	@SuppressWarnings("MustBeClosed")
	public FastDataIn(int maxChipX, int maxChipY, CoreLocation gathererCore,
			TransceiverInterface transceiver, ChipLocation ethernetChip,
			String ethernetAddress, IPTag iptag)
					throws ProcessException, IOException, InterruptedException {
		this.gathererCore = gathererCore;
		this.maxChipX = maxChipX;
		this.maxChipY = maxChipY;
		this.connection = new ThrottledConnection(transceiver, ethernetChip,
				ethernetAddress, iptag);
	}

	@Override
	public void close() throws IOException {
		connection.close();
	}

	private HasChipLocation getBoardLocalDestination(
			HasChipLocation monitorChip) {
		int boardLocalX = monitorChip.getX() - gathererCore.getX();
		if (boardLocalX < 0) {
			boardLocalX += maxChipX + 1;
		}
		int boardLocalY = monitorChip.getY() - gathererCore.getY();
		if (boardLocalY < 0) {
			boardLocalY += maxChipY + 1;
		}
		return new ChipLocation(boardLocalX, boardLocalY);
	}

	/**
	 * Write data to a given memory location.
	 *
	 * @param destination
	 *            The target chip of the write.
	 * @param baseAddress
	 *            Whether the data will be written.
	 * @param data
	 *            The data to be written.
	 * @throws IOException
	 *             If IO fails.
	 * @throws InterruptedException
	 *            If communications are interrupted.
	 */
	public void fastWrite(HasChipLocation destination,
			MemoryLocation baseAddress, ByteBuffer data)
					throws IOException, InterruptedException {
		int timeoutCount = 0;
		int numPackets = computeNumPackets(data);
		int transactionId = ++this.transactionId;

		outerLoop: while (true) {
			// Do the initial blast of data
			sendInitialPackets(getBoardLocalDestination(destination),
					baseAddress, data, transactionId, numPackets);
			/*
			 * Don't create a missing buffer until at least one packet has
			 * come back.
			 */
			BitSet missing = null;

			// Wait for confirmation and do required retransmits
			innerLoop: while (true) {
				try {
					var buf = connection.receive();
					var received = buf.order(LITTLE_ENDIAN).asIntBuffer();
					timeoutCount = 0; // Reset the timeout counter
					int command = received.get();
					try {
						// read transaction id
						var commandCode = FastDataInCommandID.forValue(command);
						int thisTransactionId = received.get();

						// if wrong transaction id, ignore packet
						if (thisTransactionId != transactionId) {
							continue innerLoop;
						}

						// Decide what to do with the packet
						switch (commandCode) {
						case RECEIVE_FINISHED_DATA_IN:
							// We're done!
							break outerLoop;

						case RECEIVE_MISSING_SEQ_DATA_IN:
							if (!received.hasRemaining()) {
								throw new BadDataInMessageException(
										received.get(0), received);
							}
							log.debug(
									"another packet (#{}) of missing "
											+ "sequence numbers;",
									received.get(1));
							break;
						default:
							throw new BadDataInMessageException(
									received.get(0), received);
						}

						/*
						 * The currently received packet has missing
						 * sequence numbers. Accumulate and dispatch
						 * transactionId when we've got them all.
						 */
						if (missing == null) {
							missing = new BitSet(numPackets);
						}
						var flags = addMissedSeqNums(
								received, missing, numPackets);

						/*
						 * Check that you've seen something that implies
						 * ready to retransmit.
						 */
						if (flags.seenAll || flags.seenEnd) {
							retransmitMissingPackets(data, missing,
									transactionId);
							missing.clear();
						}
					} catch (IllegalArgumentException e) {
						log.error("Unexpected command code " + command
								+ " received from "
								+ connection.getLocation());
					}
				} catch (SocketTimeoutException e) {
					if (timeoutCount++ > TIMEOUT_RETRY_LIMIT) {
						log.error(
								"ran out of attempts on transaction {}"
										+ " due to timeouts.",
								transactionId);
						throw e;
					}
					/*
					 * If we never received a packet, we will never have
					 * created the buffer, so send everything again
					 */
					if (missing == null) {
						log.debug("full timeout; resending initial "
								+ "packets for stream with transaction "
								+ "id {}", transactionId);
						continue outerLoop;
					}
					log.warn(
							"timeout {} on transaction {} writing to {}"
									+ " via {}",
							timeoutCount, transactionId, baseAddress,
							gathererCore);
					retransmitMissingPackets(data, missing,
							transactionId);
					missing.clear();
				}
			}
		}
	}

	@CheckReturnValue
	private SeenFlags addMissedSeqNums(IntBuffer received, BitSet seqNums,
			int expectedMax) {
		var flags = new SeenFlags();
		var addedEnd = "";
		var addedAll = "";
		int actuallyAdded = 0;
		while (received.hasRemaining()) {
			int num = received.get();

			if (num == MISSING_SEQS_END) {
				addedEnd = "and saw END marker";
				flags.seenEnd = true;
				break;
			}
			if (num == FLAG_FOR_MISSING_ALL_SEQUENCES) {
				addedAll = "by finding ALL missing marker";
				flags.seenAll = true;
				for (int seqNum = 0; seqNum < expectedMax; seqNum++) {
					seqNums.set(seqNum);
					actuallyAdded++;
				}
				break;
			}

			seqNums.set(num);
			actuallyAdded++;
			if (num < 0 || num > expectedMax) {
				throw new CrazySequenceNumberException(num, received);
			}
		}
		log.debug("added {} missed packets, {}{}", actuallyAdded, addedEnd,
				addedAll);
		return flags;
	}

	private int sendInitialPackets(HasChipLocation boardLocalDestination,
			MemoryLocation baseAddress, ByteBuffer data, int transactionId,
			int numPackets) throws IOException {
		log.debug("streaming {} bytes in {} packets using transaction {}",
				data.remaining(), numPackets, transactionId);
		log.debug("sending packet #{}", 0);
		connection.send(dataToLocation(boardLocalDestination, baseAddress,
				numPackets, transactionId));
		for (int seqNum = 0; seqNum < numPackets; seqNum++) {
			log.debug("sending packet #{}", seqNum);
			connection.send(seqData(data, seqNum, transactionId));
		}
		log.debug("sending terminating packet");
		connection.send(tellDataIn(transactionId));
		return numPackets;
	}

	private void retransmitMissingPackets(ByteBuffer dataToSend,
			BitSet missingSeqNums, int transactionId)
			throws IOException {
		log.debug("retransmitting {} packets", missingSeqNums.cardinality());

		missingSeqNums.stream().forEach(seqNum -> {
			log.debug("resending packet #{}", seqNum);
			try {
				connection.send(seqData(dataToSend, seqNum, transactionId));
			} catch (IOException e) {
				log.error(
						"missing sequence packet with id {}-{} "
								+ "failed to transmit",
						seqNum, transactionId, e);
			}
		});
		log.debug("sending terminating packet");
		connection.send(tellDataIn(transactionId));
	}

	/**
	 * Contains flags for seen missing sequence numbers.
	 *
	 * @author Alan Stokes
	 */
	private static final class SeenFlags {
		boolean seenEnd;

		boolean seenAll;
	}

	private SDPHeader header() {
		return new SDPHeader(REPLY_NOT_EXPECTED, new SDPLocation(gathererCore),
				GATHERER_DATA_SPEED_UP.value);
	}

	/**
	 * @param boardLocalDestination
	 *            The destination for the data on the board being used.
	 * @param baseAddress
	 *            Where the data is to be written.
	 * @param numPackets
	 *            How many SDP packets will be sent.
	 * @param transactionId
	 *            The transaction id of this stream.
	 * @return The message indicating the start of the data.
	 */
	private SDPMessage dataToLocation(HasChipLocation boardLocalDestination,
			MemoryLocation baseAddress, int numPackets, int transactionId) {
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
				min(data.remaining() - position, payload.remaining()));
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

	/**
	 * Exception thrown when something mad comes back off SpiNNaker.
	 *
	 * @author Donal Fellows
	 */
	static class BadDataInMessageException extends RuntimeException {
		private static final long serialVersionUID = 1L;

		BadDataInMessageException(int code, IntBuffer message) {
			super("unexpected response code: " + toUnsignedLong(code));
			log.warn("bad message payload: {}", range(0, message.limit())
					.map(i -> message.get(i)).boxed().collect(toList()));
		}
	}

	/**
	 * Exception thrown when something mad comes back off SpiNNaker.
	 *
	 * @author Donal Fellows
	 * @author Alan Stokes
	 */
	static class CrazySequenceNumberException extends RuntimeException {
		private static final long serialVersionUID = 1L;

		CrazySequenceNumberException(int remaining, IntBuffer message) {
			super("crazy number of missing packets: "
					+ toUnsignedLong(remaining));
			log.warn("bad message payload: {}", range(0, message.limit())
					.map(i -> message.get(i)).boxed().collect(toList()));
		}
	}
}
