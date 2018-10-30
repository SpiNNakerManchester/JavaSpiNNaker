package uk.ac.manchester.spinnaker.transceiver;

import static java.util.stream.Collectors.toSet;
import static org.junit.jupiter.api.Assertions.*;

import java.net.Inet4Address;
import java.util.ArrayList;
import java.util.Set;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import testconfig.BoardTestConfiguration;
import uk.ac.manchester.spinnaker.machine.ChipLocation;
import uk.ac.manchester.spinnaker.machine.Machine;

class ReliabilityITCase {
	static BoardTestConfiguration board_config;

	@BeforeAll
	static void setUpBeforeClass() throws Exception {
		board_config = new BoardTestConfiguration();
	}

	private static Set<ChipLocation> chips(Machine machine) {
		return machine.chips().stream().map(chip -> chip.asChipLocation())
				.collect(toSet());
	}

	private static final int REPETITIONS = 50;

	@Test
	void testReliableMachine() throws Exception {
		board_config.setUpRemoteBoard();
        Inet4Address host = board_config.remotehost;

        ArrayList<Machine> l = new ArrayList<>();
        for (int i = 0 ; i < REPETITIONS ; i++) {
        	try (Transceiver txrx = new Transceiver(host, 5)) {
        		txrx.ensureBoardIsReady();
        		txrx.getMachineDimensions();
        		txrx.getScampVersion();
				l.add(txrx.getMachineDetails());
			}
		}
		Set<ChipLocation> m = chips(l.remove(0));
		System.out.println(m);
		for (Machine m2 : l) {
			assertEquals(m, chips(m2));
		}
	}
}
