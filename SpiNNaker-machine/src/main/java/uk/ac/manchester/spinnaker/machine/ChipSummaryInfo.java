package uk.ac.manchester.spinnaker.machine;

import static java.net.InetAddress.getByAddress;
import static java.util.Collections.unmodifiableList;
import static java.util.Collections.unmodifiableSet;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Represents the chip summary information read via an SCP command. */
public final class ChipSummaryInfo {
	/** The state of the cores on the chip (list of one per core) */
	public final List<CPUState> coreStates;
	/** The IP address of the Ethernet if up, or <tt>null</tt> if not */
	public final InetAddress ethernetIPAddress;
	/** Determines if the Ethernet connection is available on this chip */
	public final boolean isEthernetAvailable;
	/** The size of the largest block of free SDRAM in bytes */
	public final int largestFreeSDRAMBlock;
	/** The size of the largest block of free SRAM in bytes */
	public final int largestFreeSRAMBlock;
	/** The number of cores working on the chip (including monitors) */
	public final int numCores;
	/** The number of multicast routing entries free on this chip */
	public final int numFreeMulticastRoutingEntries;
	/** The location of the nearest Ethernet chip */
	public final ChipLocation nearestEthernetChip;
	/** The IDs of the working links outgoing from this chip */
	public final Set<Integer> workingLinks;
	/** The chip that this data is from */
	public final HasChipLocation chip;

	private static final int ADDRESS_SIZE = 4;
	private static final byte[] NO_ADDRESS = new byte[ADDRESS_SIZE];
	private static final int NUM_CORES = 18;

	private static Set<Integer> parseWorkingLinks(int summaryFlags) {
		Set<Integer> wl = new LinkedHashSet<>();
		for (int link = 0; link < 6; link++) {
			if (((summaryFlags >> (8 + link)) & 1) != 0) {
				wl.add(link);
			}
		}
		return unmodifiableSet(wl);
	}

	private static List<CPUState> parseStates(byte[] stateBytes) {
		List<CPUState> states = new ArrayList<>();
		for (byte b : stateBytes) {
			states.add(CPUState.get(b));
		}
		return unmodifiableList(states);
	}

	private static InetAddress parseEthernetAddress(byte[] addr) {
		try {
			if (!Arrays.equals(addr, NO_ADDRESS)) {
				return getByAddress(addr);
			}
		} catch (UnknownHostException e) {
			// should be unreachable
		}
		return null;
	}

	/**
	 * @param buffer
	 *            The data from the SCP response
	 * @param source
	 *            The coordinates of the chip that this data is from
	 */
	public ChipSummaryInfo(ByteBuffer buffer, HasChipLocation source) {
		int summaryFlags = buffer.getInt();
		numCores = summaryFlags & 0x1F;
		workingLinks = parseWorkingLinks(summaryFlags);
		numFreeMulticastRoutingEntries = (summaryFlags >> 14) & 0x7FF;
		isEthernetAvailable = (summaryFlags & (1 << 25)) != 0;

		largestFreeSDRAMBlock = buffer.getInt();
		largestFreeSRAMBlock = buffer.getInt();

		byte[] states = new byte[NUM_CORES];
		buffer.get(states);
		coreStates = parseStates(states);

		chip = source;
		int neY = buffer.get();
		nearestEthernetChip = new ChipLocation(buffer.get(), neY);

		byte[] ia = new byte[ADDRESS_SIZE];
		buffer.get(ia);
		ethernetIPAddress = parseEthernetAddress(ia);
	}
}