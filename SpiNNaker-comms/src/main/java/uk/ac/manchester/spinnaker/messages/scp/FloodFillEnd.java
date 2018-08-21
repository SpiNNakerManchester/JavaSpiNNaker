package uk.ac.manchester.spinnaker.messages.scp;

import static java.lang.Byte.toUnsignedInt;
import static uk.ac.manchester.spinnaker.machine.MachineDefaults.MAX_NUM_CORES;
import static uk.ac.manchester.spinnaker.messages.scp.Bits.BYTE0;
import static uk.ac.manchester.spinnaker.messages.scp.Bits.BYTE1;
import static uk.ac.manchester.spinnaker.messages.scp.Bits.BYTE3;
import static uk.ac.manchester.spinnaker.messages.scp.SCPCommand.CMD_NNP;

import java.nio.ByteBuffer;

/** A request to start a flood fill of data. */
public final class FloodFillEnd extends SCPRequest<CheckOKResponse> {
	private static final int MAGIC1 = 0x3f;
	private static final int MAGIC2 = 0x18;
	private static final int NNP_FORWARD_RETRY =
			(MAGIC1 << BYTE1) | (MAGIC2 << BYTE0);
	private static final int NNP_FLOOD_FILL_END = 15;
	private static final int WAIT_BIT = 18;

	/**
	 * @param nearestNeighbourID
	 *            The ID of the packet, between 0 and 127
	 */
	public FloodFillEnd(byte nearestNeighbourID) {
		this(nearestNeighbourID, 0, null, false);
	}

	/**
	 * @param nearestNeighbourID
	 *            The ID of the packet, between 0 and 127
	 * @param appID
	 *            The application ID to start using the data, between 16 and
	 *            255. If not specified, no application is started
	 * @param processors
	 *            A list of processors on which to start the application, each
	 *            between 1 and 17. If not specified, no application is started.
	 */
	public FloodFillEnd(byte nearestNeighbourID, int appID,
			Iterable<Integer> processors) {
		this(nearestNeighbourID, appID, processors, false);
	}

	/**
	 * @param nearestNeighbourID
	 *            The ID of the packet, between 0 and 127
	 * @param appID
	 *            The application ID to start using the data, between 16 and
	 *            255. If not specified, no application is started
	 * @param processors
	 *            A list of processors on which to start the application, each
	 *            between 1 and 17. If not specified, no application is started.
	 * @param wait
	 *            True if the binary should go into a "wait" state before
	 *            executing
	 */
	public FloodFillEnd(byte nearestNeighbourID, int appID,
			Iterable<Integer> processors, boolean wait) {
		super(DEFAULT_MONITOR_CORE, CMD_NNP, argument1(nearestNeighbourID),
				argument2(appID, processors, wait), NNP_FORWARD_RETRY);
	}

	private static int argument1(byte nearestNeighbourID) {
		return (NNP_FLOOD_FILL_END << BYTE3)
				| toUnsignedInt(nearestNeighbourID);
	}

	private static int argument2(int appID, Iterable<Integer> processors,
			boolean wait) {
		int processorMask = 0;
		if (processors != null) {
			for (int p : processors) {
				if (p >= 1 && p < MAX_NUM_CORES) {
					processorMask |= 1 << p;
				}
			}
		}
		processorMask |= appID << BYTE3;
		if (wait) {
			processorMask |= 1 << WAIT_BIT;
		}
		return processorMask;
	}

	@Override
	public CheckOKResponse getSCPResponse(ByteBuffer buffer) throws Exception {
		return new CheckOKResponse("Flood Fill", CMD_NNP, buffer);
	}
}
