/*
 * Copyright (c) 2018 The University of Manchester
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package uk.ac.manchester.spinnaker.machine.reports;

import static org.slf4j.LoggerFactory.getLogger;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Calendar;
import java.util.Collection;

import org.slf4j.Logger;

import uk.ac.manchester.spinnaker.machine.Chip;
import uk.ac.manchester.spinnaker.machine.ChipLocation;
import uk.ac.manchester.spinnaker.machine.Link;
import uk.ac.manchester.spinnaker.machine.Machine;

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
	 *            the list of connections to the machine; the elements in the
	 *            list <i>should</i> have {@code toString()} methods that
	 *            produce human-readable output.
	 * @throws IOException
	 *             when a file cannot be opened for some reason
	 */
	public static void generateMachineReport(File reportDirectory,
			Machine machine, Collection<?> connections) throws IOException {
		var file = new File(reportDirectory, FILENAME);
		var timestamp = Calendar.getInstance();
		try (var f = new PrintWriter(
				new BufferedWriter(new FileWriter(file)))) {
			writeHeader(f, timestamp, machine, connections);
			for (int x = 0; x <= machine.maxChipX(); x++) {
				for (int y = 0; y <= machine.maxChipY(); y++) {
					writeChipRouterReport(f, machine, x, y);
				}
			}
		} catch (IOException e) {
			log.error("can't open file {} for writing", file);
			throw e;
		}
	}

	private static void writeHeader(PrintWriter f, Calendar timestamp,
			Machine machine, Collection<?> connections) {
		f.println("\t\tTarget SpiNNaker Machine Structure");
		f.println("\t\t==================================");
		f.printf("\nGenerated: %s for target machine '%s'\n\n", timestamp,
				connections);
		f.printf("Machine dimensions (in chips) x : %d  y : %d\n\n",
				machine.maxChipX() + 1, machine.maxChipY() + 1);
		f.println("\t\tMachine router information");
		f.println("\t\t==========================");
	}

	private static void writeChipRouterReport(PrintWriter f, Machine machine,
			int x, int y) {
		Chip chip = machine.getChipAt(new ChipLocation(x, y));
		if (chip != null) {
			f.printf("\nInformation for chip %d:%d\n", chip.getX(),
					chip.getY());
			f.printf("Neighbouring chips:\n\t%s\n",
					chip.router.neighbouringChipsCoords());
			f.println("Router list of links for this chip are:");
			for (Link link : chip.router.links()) {
				f.printf("\t%s\n", link);
			}
			f.println("\t\t==========================");
		}
	}
}
