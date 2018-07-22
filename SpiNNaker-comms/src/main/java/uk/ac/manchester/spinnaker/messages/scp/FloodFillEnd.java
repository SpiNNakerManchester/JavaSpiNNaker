package uk.ac.manchester.spinnaker.messages.scp;

import static java.lang.Byte.toUnsignedInt;
import static uk.ac.manchester.spinnaker.messages.scp.SCPCommand.CMD_NNP;
import static uk.ac.manchester.spinnaker.messages.sdp.SDPFlag.REPLY_EXPECTED;

import java.nio.ByteBuffer;

import uk.ac.manchester.spinnaker.messages.sdp.SDPHeader;

/** A request to start a flood fill of data */
public final class FloodFillEnd extends SCPRequest<CheckOKResponse> {
	private static final int NNP_FORWARD_RETRY = (0x3f << 8) | 0x18;
	private static final int NNP_FLOOD_FILL_END = 15;

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
		super(new SDPHeader(REPLY_EXPECTED, DEFAULT_MONITOR_CORE, 0), CMD_NNP,
				argument1(nearestNeighbourID),
				argument2(appID, processors, wait), NNP_FORWARD_RETRY);
	}

	private static int argument1(byte nearestNeighbourID) {
		return (NNP_FLOOD_FILL_END << 24) | toUnsignedInt(nearestNeighbourID);
	}

	private static int argument2(int appID, Iterable<Integer> processors,
			boolean wait) {
		int processor_mask = 0;
		if (processors != null) {
			for (int p : processors) {
				if (p >= 1 && p <= 17) {
					processor_mask |= 1 << p;
				}
			}
		}
		processor_mask |= appID << 24;
		if (wait) {
			processor_mask |= 1 << 18;
		}
		return processor_mask;
	}

	@Override
	public CheckOKResponse getSCPResponse(ByteBuffer buffer) throws Exception {
		return new CheckOKResponse("Flood Fill", CMD_NNP, buffer);
	}
}
