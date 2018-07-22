package uk.ac.manchester.spinnaker.messages.scp;

import static java.lang.Byte.toUnsignedInt;
import static uk.ac.manchester.spinnaker.messages.scp.SCPCommand.CMD_FFD;
import static uk.ac.manchester.spinnaker.messages.sdp.SDPFlag.REPLY_EXPECTED;

import java.nio.ByteBuffer;

import uk.ac.manchester.spinnaker.messages.sdp.SDPHeader;

/** A request to start a flood fill of data */
public class FloodFillData extends SCPRequest<CheckOKResponse> {
	private static final int NNP_FORWARD_RETRY = (0x3f << 24) | (0x18 << 16);

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
		super(new SDPHeader(REPLY_EXPECTED, DEFAULT_MONITOR_CORE, 0), CMD_FFD,
				argument1(nearestNeighbourID), argument2(blockNumber, length),
				baseAddress, ByteBuffer.wrap(data, offset, length));
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
		super(new SDPHeader(REPLY_EXPECTED, DEFAULT_MONITOR_CORE, 0), CMD_FFD,
				argument1(nearestNeighbourID),
				argument2(blockNumber, data.remaining()), baseAddress, data);
	}

	private static int argument1(byte nearestNeighbourID) {
		return NNP_FORWARD_RETRY | toUnsignedInt(nearestNeighbourID);
	}

	private static int argument2(int blockNumber, int size) {
		return (blockNumber << 16) | (((size / 4) - 1) << 8);
	}

	@Override
	public CheckOKResponse getSCPResponse(ByteBuffer buffer) throws Exception {
		return new CheckOKResponse("Flood Fill", CMD_FFD, buffer);
	}
}
