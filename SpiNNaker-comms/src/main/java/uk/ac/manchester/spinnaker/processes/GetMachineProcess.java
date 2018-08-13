package uk.ac.manchester.spinnaker.processes;

import static java.lang.Math.min;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.unmodifiableMap;
import static org.slf4j.LoggerFactory.getLogger;
import static uk.ac.manchester.spinnaker.messages.Constants.ROUTER_REGISTER_P2P_ADDRESS;
import static uk.ac.manchester.spinnaker.messages.model.CPUState.IDLE;
import static uk.ac.manchester.spinnaker.messages.model.P2PTable.getColumnOffset;
import static uk.ac.manchester.spinnaker.messages.model.P2PTable.getNumColumnBytes;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;

import uk.ac.manchester.spinnaker.connections.SCPConnection;
import uk.ac.manchester.spinnaker.connections.selectors.ConnectionSelector;
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
public class GetMachineProcess extends MultiConnectionProcess<SCPConnection> {
	private static final Logger log = getLogger(GetMachineProcess.class);
	/** A dictionary of (x, y) -> ChipInfo. */
	private final Map<ChipLocation, ChipSummaryInfo> chipInfo;
	private final Collection<ChipLocation> ignoreChips;
	private final Map<ChipLocation, Collection<Integer>> ignoreCoresMap;
	private final Map<ChipLocation, Collection<Direction>> ignoreLinksMap;
	private final Integer maxCoreID;
	private final Integer maxSDRAMSize;

	private static <T> Collection<T> def(Collection<T> c) {
		return c == null ? emptyList() : c;
	}

	private static <K, V> Map<K, V> def(Map<K, V> m) {
		return m == null ? emptyMap() : m;
	}

	private static int clamp(int value, Integer limit) {
		if (limit == null) {
			return value;
		}
		return min(value, limit);
	}

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
	 * @param maxCoreID
	 *            The maximum core ID, or <tt>null</tt> for the system's
	 *            standard limit. For debugging.
	 * @param maxSDRAMSize
	 *            The maximum SDRAM size, or <tt>null</tt> for the system's
	 *            standard limit. For debugging.
	 */
    public GetMachineProcess(
            ConnectionSelector<SCPConnection> connectionSelector,
            Collection<ChipLocation> ignoreChips,
            Map<ChipLocation, Collection<Integer>> ignoreCoresMap,
            Map<ChipLocation, Collection<Direction>> ignoreLinksMap,
            Integer maxCoreID, Integer maxSDRAMSize) {
        super(connectionSelector);
        this.ignoreChips = def(ignoreChips);
        this.ignoreCoresMap = def(ignoreCoresMap);
        this.ignoreLinksMap = def(ignoreLinksMap);
        this.maxCoreID = maxCoreID;
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
	 * @throws Exception
	 *             If SpiNNaker rejects a message.
	 */
	public Machine getMachineDetails(HasChipLocation bootChip,
			MachineDimensions size) throws IOException, Exception {
		// Get the P2P table; 8 entries are packed into each 32-bit word
		List<ByteBuffer> p2pColumnData = new ArrayList<>();
		for (int column = 0; column < size.width; column++) {
			sendRequest(
					new ReadMemory(bootChip,
							ROUTER_REGISTER_P2P_ADDRESS
									+ getColumnOffset(column),
							getNumColumnBytes(size.height)),
					response -> p2pColumnData.add(response.data));
		}
		finish();
		checkForError();
		P2PTable p2pTable = new P2PTable(size, p2pColumnData);

		// Get the chip information for each chip
		for (ChipLocation chip : p2pTable.getChips()) {
			sendRequest(new GetChipInfo(chip),
					response -> chipInfo.put(chip, response.chipInfo));
		}
		finish();
		try {
			checkForError();
		} catch (Exception ignored) {
			/*
			 * Ignore errors so far, as any error here just means that a chip is
			 * down that wasn't marked as down
			 */
		}

		// Warn about unexpected missing chips
		for (ChipLocation chip : p2pTable.getChips()) {
			if (!chipInfo.containsKey(chip)) {
				log.warn("Chip {},{} was expected but didn't reply",
						chip.getX(), chip.getY());
			}
		}

		// Build a Machine
		Machine machine = new Machine(size, bootChip);
		for (Map.Entry<ChipLocation, ChipSummaryInfo> entry : chipInfo
				.entrySet()) {
			if (!ignoreChips.contains(entry.getKey())) {
				machine.addChip(makeChip(size, entry.getValue()));
			}
		}
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
		List<Processor> processors = new ArrayList<>();
		int maxCore = clamp(chipInfo.numCores - 1, maxCoreID);
		ChipLocation location = chipInfo.chip.asChipLocation();
		Collection<Integer> ignoreCores;
		if (ignoreCoresMap.containsKey(location)) {
			ignoreCores = ignoreCoresMap.get(location);
		} else {
			ignoreCores = emptyList();
		}
		for (int id = 0; id <= maxCore; id++) {
			// Add the core provided it is not to be ignored
			if (!ignoreCores.contains(id)) {
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
			MachineDimensions size, Collection<Direction> ignoreLinks) {
		HasChipLocation chip = chipInfo.chip;
		List<Link> links = new ArrayList<>();
		for (Direction link : chipInfo.workingLinks) {
			ChipLocation dest = getChipOverLink(chip, size, link);
			if (!ignoreLinks.contains(link) && !ignoreChips.contains(dest)
					&& this.chipInfo.containsKey(dest)) {
				links.add(new Link(chip, link, dest));
			}
		}
		return links;
	}

	private List<Link> makeLinks(ChipSummaryInfo chipInfo,
			MachineDimensions size) {
		HasChipLocation chip = chipInfo.chip;
		List<Link> links = new ArrayList<>();
		for (Direction link : chipInfo.workingLinks) {
			ChipLocation dest = getChipOverLink(chip, size, link);
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
		int x = (chip.getX() + link.xChange + size.width) % size.width;
		int y = (chip.getY() + link.yChange + size.height) % size.height;
		return new ChipLocation(x, y);
	}

	/**
	 * Get the chip information for the machine. Note that
	 * {@link #getMachineDetails(HasChipLocation,MachineDimensions)
	 * getMachineDetails(...)} must have been called first.
	 *
	 * @return The description of what the state of each chip is.
	 */
	public Map<ChipLocation, ChipSummaryInfo> getChipInfo() {
		return unmodifiableMap(chipInfo);
	}
}
