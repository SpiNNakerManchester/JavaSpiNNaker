/*
 * Copyright (c) 2018 The University of Manchester
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
package uk.ac.manchester.spinnaker.transceiver;

import static java.lang.Math.min;
import static java.util.Arrays.asList;
import static java.util.Collections.unmodifiableMap;
import static org.slf4j.LoggerFactory.getLogger;
import static uk.ac.manchester.spinnaker.messages.model.CPUState.IDLE;
import static uk.ac.manchester.spinnaker.messages.model.P2PTable.getColumnOffset;
import static uk.ac.manchester.spinnaker.messages.model.P2PTable.getNumColumnBytes;
import static uk.ac.manchester.spinnaker.transceiver.CommonMemoryLocations.ROUTER_P2P;
import static uk.ac.manchester.spinnaker.utils.CollectionUtils.range;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;

import uk.ac.manchester.spinnaker.connections.ConnectionSelector;
import uk.ac.manchester.spinnaker.connections.SCPConnection;
import uk.ac.manchester.spinnaker.machine.Chip;
import uk.ac.manchester.spinnaker.machine.ChipLocation;
import uk.ac.manchester.spinnaker.machine.Direction;
import uk.ac.manchester.spinnaker.machine.HasChipLocation;
import uk.ac.manchester.spinnaker.machine.Link;
import uk.ac.manchester.spinnaker.machine.Machine;
import uk.ac.manchester.spinnaker.machine.MachineDimensions;
import uk.ac.manchester.spinnaker.machine.Processor;
import uk.ac.manchester.spinnaker.machine.Router;
import uk.ac.manchester.spinnaker.messages.model.ChipSummaryInfo;
import uk.ac.manchester.spinnaker.messages.model.P2PTable;
import uk.ac.manchester.spinnaker.messages.scp.GetChipInfo;
import uk.ac.manchester.spinnaker.messages.scp.ReadMemory;

/** A process for getting the machine details over a set of connections. */
final class GetMachineProcess extends TxrxProcess {
	private static final Logger log = getLogger(GetMachineProcess.class);

	/** A dictionary of (x, y) &rarr; ChipInfo. */
	private final Map<ChipLocation, ChipSummaryInfo> chipInfo;

	private final Set<ChipLocation> ignoreChips;

	private final Map<ChipLocation, Set<Integer>> ignoreCoresMap;

	private final Map<ChipLocation, EnumSet<Direction>> ignoreLinksMap;

	private final Integer maxSDRAMSize;

	private static <T> Set<T> def(Set<T> c) {
		return c == null ? Set.of() : c;
	}

	private static <K, V> Map<K, V> def(Map<K, V> m) {
		return m == null ? Map.of() : m;
	}

	private static int clamp(int value, Integer limit) {
		if (limit == null) {
			return value;
		}
		return min(value, limit);
	}

	private static final int THROTTLED = 3;

	/**
	 * @param connectionSelector
	 *            How to talk to the machine.
	 * @param ignoreChips
	 *            The chip blacklist. Note that cores on this chip are also
	 *            blacklisted, and links to and from this chip are also
	 *            blacklisted.
	 * @param ignoreCoresMap
	 *            The core blacklist.
	 * @param ignoreLinksMap
	 *            The link blacklist.
	 * @param maxSDRAMSize
	 *            The maximum SDRAM size, or {@code null} for the system's
	 *            standard limit. For debugging.
	 * @param retryTracker
	 *            Object used to track how many retries were used in an
	 *            operation. May be {@code null} if no suck tracking is
	 *            required.
	 */
	GetMachineProcess(
			ConnectionSelector<? extends SCPConnection> connectionSelector,
			Set<ChipLocation> ignoreChips,
			Map<ChipLocation, Set<Integer>> ignoreCoresMap,
			Map<ChipLocation, EnumSet<Direction>> ignoreLinksMap,
			Integer maxSDRAMSize, RetryTracker retryTracker) {
		super(connectionSelector, SCP_RETRIES, SCP_TIMEOUT, THROTTLED,
				THROTTLED - 1, retryTracker);
		this.ignoreChips = def(ignoreChips);
		this.ignoreCoresMap = def(ignoreCoresMap);
		this.ignoreLinksMap = def(ignoreLinksMap);
		this.maxSDRAMSize = maxSDRAMSize;
		this.chipInfo = new HashMap<>();
	}

	/**
	 * Get a full, booted machine, populated with information directly from the
	 * physical hardware.
	 *
	 * @param bootChip
	 *            Which chip is used to boot the machine.
	 * @param size
	 *            The dimensions of the machine.
	 * @return The machine description.
	 * @throws IOException
	 *             If anything goes wrong with networking.
	 * @throws ProcessException
	 *             If SpiNNaker rejects a message.
	 * @throws InterruptedException
	 *             If the communications were interrupted.
	 */
	Machine getMachineDetails(HasChipLocation bootChip, MachineDimensions size)
			throws IOException, ProcessException, InterruptedException {
		// Get the P2P table; 8 entries are packed into each 32-bit word
		var p2pColumnData = new ByteBuffer[size.width()];
		int byteLength = getNumColumnBytes(size.height());
		for (int col : range(0, size.width())) {
			sendGet(new ReadMemory(bootChip,
					ROUTER_P2P.add(getColumnOffset(col)), byteLength),
					bytes -> p2pColumnData[col] = bytes);
		}
		finishBatch();
		var p2pTable = new P2PTable(size, asList(p2pColumnData));

		// Get the chip information for each chip
		for (var chip : p2pTable.getChips()) {
			sendGet(new GetChipInfo(chip), info -> chipInfo.put(chip, info));
		}
		try {
			finishBatch();
		} catch (ProcessException ignored) {
			/*
			 * Ignore errors from the SpiNNaker side, as any error here just
			 * means that a chip is down that wasn't marked as down
			 */
		}

		// Warn about unexpected missing chips
		for (var chip : p2pTable.getChips()) {
			if (!chipInfo.containsKey(chip)) {
				log.warn("Chip {},{} was expected but didn't reply",
						chip.getX(), chip.getY());
			}
		}

		// Build a Machine
		var machine = new Machine(size, bootChip);
		chipInfo.forEach((chip, data) -> {
			if (!ignoreChips.contains(chip)) {
				machine.addChip(makeChip(size, data));
			}
		});
		return machine;
	}

	/**
	 * Creates a chip from a ChipSummaryInfo structure.
	 *
	 * @param size
	 *            The size of the machine containing the chip.
	 * @param chipInfo
	 *            The ChipSummaryInfo structure to create the chip from.
	 * @return The created chip.
	 */
	private Chip makeChip(MachineDimensions size, ChipSummaryInfo chipInfo) {
		// Create the processor list
		var processors = new ArrayList<Processor>();
		var location = chipInfo.chip.asChipLocation();
		var ignoreCores = ignoreCoresMap.getOrDefault(location, Set.of());
		for (int id = 0; id < chipInfo.numCores; id++) {
			// Add the core provided it is not to be ignored
			if (ignoreCores != null && !ignoreCores.contains(id)) {
				if (id == 0) {
					processors.add(Processor.factory(id, true));
				} else if (chipInfo.coreStates.get(id) == IDLE) {
					processors.add(Processor.factory(id));
				} else {
					log.warn("Not using core {},{},{} in state {}",
							location.getX(), location.getY(), id,
							chipInfo.coreStates.get(id));
				}
			}
		}

		// Create the chip
		List<Link> links;
		if (ignoreLinksMap.containsKey(location)) {
			links = makeLinks(chipInfo, size, ignoreLinksMap.get(location));
		} else {
			links = makeLinks(chipInfo, size);
		}
		return new Chip(location, processors,
				new Router(links, chipInfo.numFreeMulticastRoutingEntries),
				clamp(chipInfo.largestFreeSDRAMBlock, maxSDRAMSize),
				chipInfo.ethernetIPAddress, chipInfo.nearestEthernetChip);
	}

	private List<Link> makeLinks(ChipSummaryInfo chipInfo,
			MachineDimensions size, Set<Direction> ignoreLinks) {
		var chip = chipInfo.chip;
		var links = new ArrayList<Link>();
		for (var link : chipInfo.workingLinks) {
			var dest = getChipOverLink(chip, size, link);
			if (!ignoreLinks.contains(link) && !ignoreChips.contains(dest)
					&& this.chipInfo.containsKey(dest)) {
				links.add(new Link(chip, link, dest));
			}
		}
		return links;
	}

	private List<Link> makeLinks(ChipSummaryInfo chipInfo,
			MachineDimensions size) {
		var chip = chipInfo.chip;
		var links = new ArrayList<Link>();
		for (var link : chipInfo.workingLinks) {
			var dest = getChipOverLink(chip, size, link);
			if (!ignoreChips.contains(dest)
					&& this.chipInfo.containsKey(dest)) {
				links.add(new Link(chip, link, dest));
			}
		}
		return links;
	}

	private static ChipLocation getChipOverLink(HasChipLocation chip,
			MachineDimensions size, Direction link) {
		/// TODO CHECK negative wraparound!
		int x = (chip.getX() + link.xChange + size.width()) % size.width();
		int y = (chip.getY() + link.yChange + size.height()) % size.height();
		return new ChipLocation(x, y);
	}

	/**
	 * Get the chip information for the machine. Note that
	 * {@link #getMachineDetails(HasChipLocation,MachineDimensions)
	 * getMachineDetails(...)} must have been called first.
	 *
	 * @return The description of what the state of each chip is.
	 */
	Map<ChipLocation, ChipSummaryInfo> getChipInfo() {
		return unmodifiableMap(chipInfo);
	}
}
