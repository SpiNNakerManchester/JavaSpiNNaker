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
     *            list <i>should</i> have <tt>toString()</tt> methods that
     *            produce human-readable output.
     * @throws IOException
     *             when a file cannot be opened for some reason
     */
    public static void generateMachineReport(File reportDirectory,
            Machine machine, Collection<?> connections) throws IOException {
        File file = new File(reportDirectory, FILENAME);
        String timestamp = Calendar.getInstance().toString();
        try (PrintWriter f = new PrintWriter(
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

    private static void writeHeader(PrintWriter f, String timestamp,
            Machine machine, Collection<?> connections) throws IOException {
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
            int x, int y) throws IOException {
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
