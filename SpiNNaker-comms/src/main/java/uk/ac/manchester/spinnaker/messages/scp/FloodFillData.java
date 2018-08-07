package uk.ac.manchester.spinnaker.messages.scp;

import static java.lang.Byte.toUnsignedInt;
import static uk.ac.manchester.spinnaker.messages.Constants.WORD_SIZE;
import static uk.ac.manchester.spinnaker.messages.scp.Bits.BYTE1;
import static uk.ac.manchester.spinnaker.messages.scp.Bits.BYTE2;
import static uk.ac.manchester.spinnaker.messages.scp.Bits.BYTE3;
import static uk.ac.manchester.spinnaker.messages.scp.SCPCommand.CMD_FFD;

import java.nio.ByteBuffer;

/** A request to start a flood fill of data. */
public class FloodFillData extends SCPRequest<CheckOKResponse> {
	private static final int MAGIC1 = 0x3f;
	private static final int MAGIC2 = 0x18;
	private static final int NNP_FORWARD_RETRY =
			(MAGIC1 << BYTE3) | (MAGIC2 << BYTE2);

	/**
	 * @param nearestNeighbourID
	 *            The ID of the packet, between 0 and 127
	 * @param blockNumber
	 *            Which block this block is, between 0 and 255
	 * @param baseAddress
	 *            The base address where the data is to be loaded
	 * @param data
	 *            The data to load, between 4 and 256 bytes and the size must be
	 *            divisible by 4.
	 */
	public FloodFillData(byte nearestNeighbourID, int blockNumber,
			int baseAddress, byte[] data) {
		this(nearestNeighbourID, blockNumber, baseAddress, data, 0,
				data.length);
	}

	/**
	 * @param nearestNeighbourID
	 *            The ID of the packet, between 0 and 127
	 * @param blockNumber
	 *            Which block this block is, between 0 and 255
	 * @param baseAddress
	 *            The base address where the data is to be loaded
	 * @param data
	 *            The data to load, between 4 and 256 bytes and the size must be
	 *            divisible by 4
	 * @param offset
	 *            Where in the array the data starts at.
	 * @param length
	 *            The length of the data; must be divisible by 4.
	 */
	public FloodFillData(byte nearestNeighbourID, int blockNumber,
			int baseAddress, byte[] data, int offset, int length) {
		super(DEFAULT_MONITOR_CORE, CMD_FFD, argument1(nearestNeighbourID),
				argument2(blockNumber, length), baseAddress,
				ByteBuffer.wrap(data, offset, length));
	}

	/**
	 * @param nearestNeighbourID
	 *            The ID of the packet, between 0 and 127
	 * @param blockNumber
	 *            Which block this block is, between 0 and 255
	 * @param baseAddress
	 *            The base address where the data is to be loaded
	 * @param data
	 *            The data to load, starting at the <i>position</i> and going to
	 *            the <i>limit</i>. Must be between 4 and 256 bytes and the size
	 *            must be divisible by 4
	 */
	public FloodFillData(byte nearestNeighbourID, int blockNumber,
			int baseAddress, ByteBuffer data) {
		super(DEFAULT_MONITOR_CORE, CMD_FFD, argument1(nearestNeighbourID),
				argument2(blockNumber, data.remaining()), baseAddress, data);
	}

	private static int argument1(byte nearestNeighbourID) {
		return NNP_FORWARD_RETRY | toUnsignedInt(nearestNeighbourID);
	}

	private static int argument2(int blockNumber, int size) {
		return (blockNumber << BYTE2) | ((size / WORD_SIZE - 1) << BYTE1);
	}

	@Override
	public CheckOKResponse getSCPResponse(ByteBuffer buffer) throws Exception {
		return new CheckOKResponse("Flood Fill", CMD_FFD, buffer);
	}
}
