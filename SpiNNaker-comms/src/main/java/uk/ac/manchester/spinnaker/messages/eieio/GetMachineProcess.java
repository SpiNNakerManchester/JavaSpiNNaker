package uk.ac.manchester.spinnaker.messages.eieio;

import static java.util.Collections.emptyList;
import static java.util.Collections.sort;
import static java.util.Collections.unmodifiableMap;
import static java.util.stream.Collectors.toList;
import static org.slf4j.LoggerFactory.getLogger;
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
import uk.ac.manchester.spinnaker.machine.HasChipLocation;
import uk.ac.manchester.spinnaker.machine.Link;
import uk.ac.manchester.spinnaker.machine.Machine;
import uk.ac.manchester.spinnaker.machine.MachineDimensions;
import uk.ac.manchester.spinnaker.machine.Processor;
import uk.ac.manchester.spinnaker.messages.model.P2PTable;
import uk.ac.manchester.spinnaker.messages.scp.GetChipInfo;
import uk.ac.manchester.spinnaker.messages.scp.ReadMemory;
import uk.ac.manchester.spinnaker.processes.MultiConnectionProcess;
import uk.ac.manchester.spinnaker.selectors.ConnectionSelector;

/** A process for getting the machine details over a set of connections. */
public class GetMachineProcess extends MultiConnectionProcess<SCPConnection> {
	private static final Logger log = getLogger(GetMachineProcess.class);
	private final Map<ChipLocation, ChipSummaryInfo> chip_info = new HashMap<>();
	private final List<ChipLocation> ignore_chips;
	private final Iterable<Processor> ignore_cores;
	private final Iterable<Link> ignore_links;

	public GetMachineProcess(
			ConnectionSelector<SCPConnection> connectionSelector,
			Collection<Chip> ignoreChips, Collection<Processor> ignoreCores,
			Collection<Link> ignoreLinks) {
		super(connectionSelector);
		this.ignore_chips = (ignoreChips == null) ? emptyList()
				: ignoreChips.stream().map(chip -> chip.asChipLocation())
						.collect(toList());
		this.ignore_cores = (ignoreCores == null) ? emptyList()
				: new ArrayList<>(ignoreCores);
		this.ignore_links = (ignoreLinks == null) ? emptyList()
				: new ArrayList<>(ignoreLinks);
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
		return new Machine(size.width, size.height, chips, ignore_cores,
				ignore_links, bootChip);
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
