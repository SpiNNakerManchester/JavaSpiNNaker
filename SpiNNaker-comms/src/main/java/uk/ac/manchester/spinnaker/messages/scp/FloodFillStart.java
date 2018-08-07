package uk.ac.manchester.spinnaker.messages.scp;

import static java.lang.Byte.toUnsignedInt;
import static uk.ac.manchester.spinnaker.messages.scp.Bits.BYTE0;
import static uk.ac.manchester.spinnaker.messages.scp.Bits.BYTE1;
import static uk.ac.manchester.spinnaker.messages.scp.Bits.BYTE2;
import static uk.ac.manchester.spinnaker.messages.scp.Bits.BYTE3;
import static uk.ac.manchester.spinnaker.messages.scp.Bits.TOP_BIT;
import static uk.ac.manchester.spinnaker.messages.scp.SCPCommand.CMD_NNP;

import java.nio.ByteBuffer;

import uk.ac.manchester.spinnaker.machine.HasChipLocation;

/** A request to start a flood fill of data. */
public final class FloodFillStart extends SCPRequest<CheckOKResponse> {
	private static final int MAGIC1 = 0x3F;
	private static final int MAGIC2 = 0x18;
	private static final int MAGIC3 = 3;
	private static final int NNP_FLOOD_FILL_START = 6;
	private static final int NNP_FORWARD_RETRY =
			(1 << TOP_BIT) | (MAGIC1 << BYTE1) | (MAGIC2 << BYTE0);
	private static final int NO_CHIP = 0xFFFF;
	private static final int LOW_BITS_MASK = 0b00000011;
	private static final int HIGH_BITS_MASK = 0b11111100;

	/**
	 * Flood fill onto all chips.
	 *
	 * @param nearestNeighbourID
	 *            The ID of the packet, between 0 and 127
	 * @param numBlocks
	 *            The number of blocks of data that will be sent, between 0 and
	 *            255
	 */
	public FloodFillStart(byte nearestNeighbourID, int numBlocks) {
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
	public FloodFillStart(byte nearestNeighbourID, int numBlocks,
			HasChipLocation chip) {
		super(DEFAULT_MONITOR_CORE, CMD_NNP,
				argument1(nearestNeighbourID, numBlocks), argument2(chip),
				NNP_FORWARD_RETRY);
	}

	private static int argument1(byte nearestNeighbourID, int numBlocks) {
		if (numBlocks != toUnsignedInt((byte) numBlocks)) {
			throw new IllegalArgumentException(
					"number of blocks must be representable in 8 bits");
		}
		return (NNP_FLOOD_FILL_START << BYTE3)
				| (toUnsignedInt(nearestNeighbourID) << BYTE2)
				| (numBlocks << BYTE1);
	}

	private static int argument2(HasChipLocation chip) {
		if (chip == null) {
			return NO_CHIP;
		}
		// TODO what is this doing?
		int m = ((chip.getY() & LOW_BITS_MASK) << 2)
				+ (chip.getX() & LOW_BITS_MASK);
		return (((chip.getX() & HIGH_BITS_MASK) << BYTE3)
				+ ((chip.getY() & HIGH_BITS_MASK) << BYTE2) + (MAGIC3 << BYTE2)
				+ (1 << m));
	}

	@Override
	public CheckOKResponse getSCPResponse(ByteBuffer buffer) throws Exception {
		return new CheckOKResponse("Flood Fill", CMD_NNP, buffer);
	}
}
