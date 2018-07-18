package uk.ac.manchester.spinnaker.messages.scp;

import static uk.ac.manchester.spinnaker.messages.scp.SCPCommand.CMD_NNP;
import static uk.ac.manchester.spinnaker.messages.sdp.SDPFlag.REPLY_EXPECTED;

import java.nio.ByteBuffer;

import uk.ac.manchester.spinnaker.machine.HasChipLocation;
import uk.ac.manchester.spinnaker.messages.sdp.SDPHeader;

/** A request to start a flood fill of data */
public final class FloodFillStart extends SCPRequest<CheckOKResponse> {
	private static final int NNP_FLOOD_FILL_START = 6;
	private static final int NNP_FORWARD_RETRY = (1 << 31) | (0x3f << 8) | 0x18;

	/**
	 * Flood fill onto all chips.
	 *
	 * @param nearestNeighbourID
	 *            The ID of the packet, between 0 and 127
	 * @param numBlocks
	 *            The number of blocks of data that will be sent, between 0 and
	 *            255
	 */
	public FloodFillStart(int nearestNeighbourID, int numBlocks) {
		this(nearestNeighbourID, numBlocks, null);
	}

	/**
	 * Flood fill on a specific chip.
	 *
	 * @param nearestNeighbourID
	 *            The ID of the packet, between 0 and 127
	 * @param numBlocks
	 *            The number of blocks of data that will be sent, between 0 and
	 *            255
	 * @param chip
	 *            The chip to load the data on to, or <tt>null</tt> to load data
	 *            onto all chips.
	 */
	public FloodFillStart(int nearestNeighbourID, int numBlocks,
			HasChipLocation chip) {
		super(new SDPHeader(REPLY_EXPECTED, DEFAULT_MONITOR_CORE, 0), CMD_NNP,
				argument1(nearestNeighbourID, numBlocks), argument2(chip),
				NNP_FORWARD_RETRY);
	}

	private static int argument1(int nearestNeighbourID, int numBlocks) {
		return (NNP_FLOOD_FILL_START << 24) | (nearestNeighbourID << 16)
				| (numBlocks << 8);
	}

	private static int argument2(HasChipLocation chip) {
		int data = 0xFFFF;
		if (chip != null) {
			// TODO what is this doing?
			int m = ((chip.getY() & 3) * 4) + (chip.getX() & 3);
			data = (((chip.getX() & 0xfc) << 24) + ((chip.getY() & 0xfc) << 16)
					+ (3 << 16) + (1 << m));
		}
		return data;
	}

	@Override
	public CheckOKResponse getSCPResponse(ByteBuffer buffer) throws Exception {
		return new CheckOKResponse("Flood Fill", CMD_NNP, buffer);
	}
}
