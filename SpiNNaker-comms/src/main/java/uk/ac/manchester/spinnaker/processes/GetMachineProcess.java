package uk.ac.manchester.spinnaker.processes;

import static java.lang.Math.min;
import static java.util.Collections.emptyList;
import static java.util.Collections.sort;
import static java.util.Collections.unmodifiableMap;
import static java.util.stream.Collectors.toList;
import static org.slf4j.LoggerFactory.getLogger;
import static uk.ac.manchester.spinnaker.machine.CPUState.IDLE;
import static uk.ac.manchester.spinnaker.machine.MachineDefaults.N_IPTAGS_PER_CHIP;
import static uk.ac.manchester.spinnaker.machine.MachineDefaults.ROUTER_CLOCK_SPEED;
import static uk.ac.manchester.spinnaker.messages.Constants.ROUTER_REGISTER_P2P_ADDRESS;
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
import uk.ac.manchester.spinnaker.machine.Chip;
import uk.ac.manchester.spinnaker.machine.ChipLocation;
import uk.ac.manchester.spinnaker.machine.ChipSummaryInfo;
import uk.ac.manchester.spinnaker.machine.CoreLocation;
import uk.ac.manchester.spinnaker.machine.HasChipLocation;
import uk.ac.manchester.spinnaker.machine.Link;
import uk.ac.manchester.spinnaker.machine.LinkDescriptor;
import uk.ac.manchester.spinnaker.machine.Machine;
import uk.ac.manchester.spinnaker.machine.MachineDimensions;
import uk.ac.manchester.spinnaker.machine.Processor;
import uk.ac.manchester.spinnaker.machine.Router;
import uk.ac.manchester.spinnaker.messages.model.P2PTable;
import uk.ac.manchester.spinnaker.messages.scp.GetChipInfo;
import uk.ac.manchester.spinnaker.messages.scp.ReadMemory;
import uk.ac.manchester.spinnaker.selectors.ConnectionSelector;

/** A process for getting the machine details over a set of connections. */
public class GetMachineProcess extends MultiConnectionProcess<SCPConnection> {
	private static final Logger log = getLogger(GetMachineProcess.class);
	/** A dictionary of (x, y) -> ChipInfo */
	private final Map<ChipLocation, ChipSummaryInfo> chip_info;

	private final Collection<ChipLocation> ignore_chips;
	private final Collection<CoreLocation> ignore_cores;
	private final Collection<LinkDescriptor> ignore_links;
	private final Integer max_core_id;
	private final Integer max_sdram_size;

	private final <T> Collection<T> def(Collection<T> c) {
		return c == null ? emptyList() : c;
	}

	public GetMachineProcess(
			ConnectionSelector<SCPConnection> connectionSelector,
			Collection<ChipLocation> ignoreChips,
			Collection<CoreLocation> ignoreCores,
			Collection<LinkDescriptor> ignoreLinks, Integer maxCoreID,
			Integer maxSDRAMSize) {
		super(connectionSelector);
		this.ignore_chips = def(ignoreChips);
		this.ignore_cores = def(ignoreCores);
		this.ignore_links = def(ignoreLinks);
		this.max_core_id = maxCoreID;
		this.max_sdram_size = maxSDRAMSize;
		this.chip_info = new HashMap<>();
	}

	public Machine getMachineDetails(HasChipLocation bootChip,
			MachineDimensions size) throws IOException, Exception {
		// Get the P2P table; 8 entries are packed into each 32-bit word
		List<ByteBuffer> p2p_column_data = new ArrayList<>();
		for (int column = 0; column < size.width; column++) {
			sendRequest(
					new ReadMemory(bootChip,
							ROUTER_REGISTER_P2P_ADDRESS
									+ getColumnOffset(column),
							getNumColumnBytes(size.height)),
					response -> p2p_column_data.add(response.data));
		}
		finish();
		checkForError();
		P2PTable p2p_table = new P2PTable(size, p2p_column_data);

		// Get the chip information for each chip
		for (ChipLocation chip : p2p_table.getChips()) {
			sendRequest(new GetChipInfo(chip),
					response -> chip_info.put(chip, response.chipInfo));
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
		for (ChipLocation chip : p2p_table.getChips()) {
			if (!chip_info.containsKey(chip)) {
				log.warn("Chip %d,%d was expected but didn't reply",
						chip.getX(), chip.getY());
			}
		}

		// Build a Machine
		List<ChipSummaryInfo> chips = new ArrayList<>(chip_info.values());
		chips.removeIf(ci -> ignore_chips.contains(ci.chip.asChipLocation()));
		sort(chips, (c1, c2) -> c1.chip.asChipLocation()
				.compareTo(c2.chip.asChipLocation()));
		return new Machine(size.width, size.height,
				chips.stream().map(ci -> make_chip(size, ci)).collect(toList()),
				ignore_cores, ignore_links, bootChip);
	}

	private static int clamp(int value, Integer limit) {
		if (limit == null) {
			return value;
		}
		return min(value, limit);
	}

	/**
	 * Creates a chip from a ChipSummaryInfo structure.
	 *
	 * @param size
	 *            The size of the machine containing the chip.
	 * @param chip_info
	 *            The ChipSummaryInfo structure to create the chip from.
	 * @return The created chip.
	 */
	private Chip make_chip(MachineDimensions size, ChipSummaryInfo chip_info) {
		// Create the processor list
		List<Processor> processors = new ArrayList<>();
		int max_core = clamp(chip_info.numCores - 1, max_core_id);
		HasChipLocation chip = chip_info.chip;
		for (int virtual_core_id = 0; virtual_core_id <= max_core; virtual_core_id++) {
			// Add the core provided it is not to be ignored
			if (!ignore_cores.contains(new CoreLocation(chip.getX(),
					chip.getY(), virtual_core_id))) {
				if (virtual_core_id == 0) {
					processors.add(Processor.factory(virtual_core_id, true));
				} else if (chip_info.coreStates.get(virtual_core_id) == IDLE) {
					processors.add(Processor.factory(virtual_core_id));
				} else {
					log.warn("Not using core %d,%d,%d in state %s", chip.getX(),
							chip.getY(), virtual_core_id,
							chip_info.coreStates.get(virtual_core_id));
				}
			}
		}

		// Create the chip
		return new Chip(chip.getX(), chip.getY(), processors,
				makeRouter(chip_info, size),
				clamp(chip_info.largestFreeSDRAMBlock, max_sdram_size),
				chip_info.ethernetIPAddress, false, N_IPTAGS_PER_CHIP,
				chip_info.nearestEthernetChip);
	}

	private Router makeRouter(ChipSummaryInfo chip_info,
			MachineDimensions size) {
		HasChipLocation chip = chip_info.chip;
		List<Link> links = new ArrayList<>();
		for (int link : chip_info.workingLinks) {
			ChipLocation dest = getChipOverLink(chip, size, link);
			if (!this.ignore_links.contains(new LinkDescriptor(chip.getX(), chip.getY(), link))
					&& !this.ignore_chips.contains(dest)
					&& this.chip_info.containsKey(dest)) {
				links.add(new Link(chip, link, dest));
			}
		}
		return new Router(links, ROUTER_CLOCK_SPEED,
				chip_info.numFreeMulticastRoutingEntries);
	}

	private static final int[][] LINK_ADD_TABLE = { { 1, 0 }, { 1, 1 },
			{ 0, 1 }, { -1, 0 }, { -1, -1 }, { 0, -1 } };

	private static ChipLocation getChipOverLink(HasChipLocation chip,
			MachineDimensions size, int link) {
		/// TODO CHECK negative wraparound!
		int deltaX = LINK_ADD_TABLE[link][0];
		int deltaY = LINK_ADD_TABLE[link][1];
		int x = (chip.getX() + deltaX + size.width) % size.width;
		int y = (chip.getY() + deltaY + size.height) % size.height;
		return new ChipLocation(x, y);
	}

	/**
	 * Get the chip information for the machine. Note that
	 * {@link #getMachineDetails(HasChipLocation,MachineDimensions)} must have
	 * been called first.
	 */
	public Map<ChipLocation, ChipSummaryInfo> getChipInfo() {
		return unmodifiableMap(chip_info);
	}
}
