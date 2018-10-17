package uk.ac.manchester.spinnaker.transceiver;

import com.fasterxml.jackson.databind.ObjectMapper;
import static java.util.stream.Collectors.toSet;
import static org.junit.jupiter.api.Assertions.*;

import java.net.Inet4Address;
import java.net.URL;
import java.util.ArrayList;
import java.util.Set;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import testconfig.BoardTestConfiguration;
import uk.ac.manchester.spinnaker.machine.ChipLocation;
import uk.ac.manchester.spinnaker.machine.Machine;
import uk.ac.manchester.spinnaker.machine.bean.MachineBean;
import uk.ac.manchester.spinnaker.machine.bean.MapperFactory;
import uk.ac.manchester.spinnaker.utils.InetFactory;

class ReliabilityITCase {
	static BoardTestConfiguration board_config;
    static Machine jsonMachine;

	@BeforeAll
	static void setUpBeforeClass() throws Exception {
		board_config = new BoardTestConfiguration();
        URL url = ReliabilityITCase.class.getResource("/spinn4.json");
        ObjectMapper mapper = MapperFactory.createMapper();
        MachineBean fromJson = mapper.readValue(url, MachineBean.class);
        jsonMachine = new Machine(fromJson);
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

        for (int i = 0 ; i < REPETITIONS ; i++) {
        	try (Transceiver txrx = new Transceiver(host, 5)) {
        		txrx.ensureBoardIsReady();
        		txrx.getMachineDimensions();
        		txrx.getScampVersion();
				Machine machine = txrx.getMachineDetails();
                assertNull(jsonMachine.difference(machine));
            }
		}
	}
}
