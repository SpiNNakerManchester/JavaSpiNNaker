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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;

import uk.ac.manchester.spinnaker.machine.Chip;
import uk.ac.manchester.spinnaker.machine.ChipLocation;
import uk.ac.manchester.spinnaker.machine.ChipSummaryInfo;
import uk.ac.manchester.spinnaker.machine.HasChipLocation;
import uk.ac.manchester.spinnaker.machine.Link;
import uk.ac.manchester.spinnaker.machine.Machine;
import uk.ac.manchester.spinnaker.machine.Processor;
import uk.ac.manchester.spinnaker.messages.model.MachineDimensions;
import uk.ac.manchester.spinnaker.messages.model.P2PTable;
import uk.ac.manchester.spinnaker.messages.scp.GetChipInfo;
import uk.ac.manchester.spinnaker.messages.scp.ReadMemory;
import uk.ac.manchester.spinnaker.processes.ConnectionSelector;
import uk.ac.manchester.spinnaker.processes.MultiConnectionProcess;

/** A process for getting the machine details over a set of connections. */
public class GetMachineProcess extends MultiConnectionProcess {
	private static final Logger log = getLogger(GetMachineProcess.class);
	private final Map<ChipLocation, ChipSummaryInfo> chip_info = new HashMap<>();
	private final List<ChipLocation> ignore_chips;

	public GetMachineProcess(ConnectionSelector connectionSelector,
			List<Chip> ignoreChips) {
		super(connectionSelector);
		this.ignore_chips = (ignoreChips == null) ? emptyList()
				: ignoreChips.stream().map(chip -> chip.asChipLocation())
						.collect(toList());
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
		List<ChipSummaryInfo> csis = new ArrayList<>(chip_info.values());
		csis.removeIf(ci -> ignore_chips.contains(ci.chip.asChipLocation()));
		sort(csis, (c1, c2) -> c1.chip.asChipLocation()
				.compareTo(c2.chip.asChipLocation()));

		return new Machine(size.width, size.height, bootChip, csis);
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
