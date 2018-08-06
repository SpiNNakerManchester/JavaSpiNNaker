package uk.ac.manchester.spinnaker.transceiver;

import static java.lang.String.format;
import static java.util.stream.Collectors.toList;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.Calendar;
import java.util.Collection;

import org.slf4j.Logger;

import uk.ac.manchester.spinnaker.connections.model.Connection;
import uk.ac.manchester.spinnaker.machine.Chip;
import uk.ac.manchester.spinnaker.machine.ChipLocation;
import uk.ac.manchester.spinnaker.machine.Link;
import uk.ac.manchester.spinnaker.machine.Machine;

// TODO should this move towards the front end?
/**
 * Utility for reporting on the machine.
 */
public abstract class Reports {
	private Reports() {
	}

	private static final Logger log = getLogger(Reports.class);
	private static final String FILENAME = "machine_structure.rpt";

	/**
	 * Generate report on the physical structure of the target SpiNNaker
	 * machine.
	 *
	 * @param reportDirectory
	 *            the directory to which reports are stored
	 * @param machine
	 *            the machine object
	 * @param connections
	 *            the list of connections to the machine
	 * @throws IOException
	 *             when a file cannot be opened for some reason
	 */
	public static void generateMachineReport(File reportDirectory,
			Machine machine, Collection<Connection> connections)
			throws IOException {
		File file = new File(reportDirectory, FILENAME);
		String timestamp = Calendar.getInstance().toString();
		try (Writer f = new BufferedWriter(new FileWriter(file))) {
			writeHeader(f, timestamp, machine, connections);
			for (int x = 0; x <= machine.maxChipX(); x++) {
				for (int y = 0; y <= machine.maxChipY(); y++) {
					writeChipRouterReport(f, machine, x, y);
				}
			}
		} catch (IOException e) {
			log.error("can't open file " + file + " for writing");
			throw e;
		}
	}

	private static void writeHeader(Writer f, String timestamp, Machine machine,
			Collection<Connection> connections) throws IOException {
		f.write("\t\tTarget SpiNNaker Machine Structure\n");
		f.write("\t\t==================================\n");
		f.write(format("\nGenerated: %s for target machine '%s'\n\n", timestamp,
				connections));
		f.write(format("Machine dimensions (in chips) x : %d  y : %d\n\n",
				machine.maxChipX() + 1, machine.maxChipY() + 1));
		f.write("\t\tMachine router information\n");
		f.write("\t\t==========================\n");
	}

	private static void writeChipRouterReport(Writer f, Machine machine, int x,
			int y) throws IOException {
		Chip chip = machine.getChipAt(new ChipLocation(x, y));
		if (chip != null) {
			f.write(format("\nInformation for chip %d:%d\n", chip.getX(),
					chip.getY()));
			f.write("Neighbouring chips:\n\t" + chip.router.neighbouringChipsCoords()
                    + "\n");
			f.write("Router list of links for this chip are:\n");
			for (Link link : chip.router.links()) {
				f.write("\t" + link + "\n");
			}
			f.write("\t\t==========================\n");
		}
	}
}
