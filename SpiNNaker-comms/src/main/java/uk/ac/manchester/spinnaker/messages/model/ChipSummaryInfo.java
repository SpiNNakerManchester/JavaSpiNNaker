/*
 * Copyright (c) 2018-2023 The University of Manchester
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package uk.ac.manchester.spinnaker.messages.model;

import static java.lang.Byte.toUnsignedInt;
import static java.net.InetAddress.getByAddress;
import static java.util.Collections.unmodifiableSet;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import uk.ac.manchester.spinnaker.machine.ChipLocation;
import uk.ac.manchester.spinnaker.machine.Direction;
import uk.ac.manchester.spinnaker.machine.HasChipLocation;

/** Represents the chip summary information read via an SCP command. */
public final class ChipSummaryInfo {
	/** The state of the cores on the chip (list of one per core). */
	public final List<CPUState> coreStates;

	/** The IP address of the Ethernet if up, or {@code null} if not. */
	public final InetAddress ethernetIPAddress;

	/** Determines if the Ethernet connection is available on this chip. */
	public final boolean isEthernetAvailable;

	/** The size of the largest block of free SDRAM in bytes. */
	public final int largestFreeSDRAMBlock;

	/** The size of the largest block of free SRAM in bytes. */
	public final int largestFreeSRAMBlock;

	/** The number of cores working on the chip (including monitors). */
	public final int numCores;

	/** The number of multicast routing entries free on this chip. */
	public final int numFreeMulticastRoutingEntries;

	/** The location of the nearest Ethernet chip. */
	public final ChipLocation nearestEthernetChip;

	/** The IDs of the working links outgoing from this chip. */
	public final Set<Direction> workingLinks;

	/** The chip that this data is from. */
	public final HasChipLocation chip;

	private static final int ADDRESS_SIZE = 4;

	private static final byte[] NO_ADDRESS = new byte[] {
		0, 0, 0, 0
	};

	private static final int NUM_CORES = 18;

	private static final int LINKS_FIELD_SHIFT = 8;

	private static final int NUM_CORES_FIELD_MASK = 0b00011111;

	private static final int FREE_ENTRIES_FIELD_SHIFT = 14;

	private static final int FREE_ENTRIES_FIELD_MASK = 0x7FF;

	private static final int ETH_AVAIL_FIELD_BIT = 25;

	private static boolean bitset(int value, int bitnum) {
		return ((value >>> bitnum) & 1) != 0;
	}

	private static Set<Direction> parseWorkingLinks(int flags) {
		var wl = EnumSet.noneOf(Direction.class);
		for (Direction d : Direction.values()) {
			if (bitset(flags, LINKS_FIELD_SHIFT + d.id)) {
				wl.add(d);
			}
		}
		return unmodifiableSet(wl);
	}

	private static List<CPUState> parseStates(byte[] stateBytes) {
		var states = new ArrayList<CPUState>();
		for (byte b : stateBytes) {
			states.add(CPUState.get(b));
		}
		return List.copyOf(states);
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
		int flags = buffer.getInt();
		numCores = flags & NUM_CORES_FIELD_MASK;
		workingLinks = parseWorkingLinks(flags);
		numFreeMulticastRoutingEntries =
				(flags >>> FREE_ENTRIES_FIELD_SHIFT) & FREE_ENTRIES_FIELD_MASK;
		isEthernetAvailable = bitset(flags, ETH_AVAIL_FIELD_BIT);

		largestFreeSDRAMBlock = buffer.getInt();
		largestFreeSRAMBlock = buffer.getInt();

		var states = new byte[NUM_CORES];
		buffer.get(states);
		coreStates = parseStates(states);

		chip = source;
		int neY = toUnsignedInt(buffer.get());
		int neX = toUnsignedInt(buffer.get());
		nearestEthernetChip = new ChipLocation(neX, neY);

		var ia = new byte[ADDRESS_SIZE];
		buffer.get(ia);
		ethernetIPAddress = parseEthernetAddress(ia);
	}
}
