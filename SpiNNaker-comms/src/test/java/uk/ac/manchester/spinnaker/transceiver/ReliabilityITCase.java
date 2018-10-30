package uk.ac.manchester.spinnaker.transceiver;

import com.fasterxml.jackson.databind.ObjectMapper;
import static org.junit.jupiter.api.Assertions.*;

import java.net.InetAddress;
import java.net.URL;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import uk.ac.manchester.spinnaker.machine.Machine;
import uk.ac.manchester.spinnaker.machine.bean.MachineBean;
import uk.ac.manchester.spinnaker.machine.bean.MapperFactory;
import static uk.ac.manchester.spinnaker.utils.Ping.ping;

class ReliabilityITCase {
    static Machine jsonMachine;

	@BeforeAll
	static void setUpBeforeClass() throws Exception {
        URL url = ReliabilityITCase.class.getResource("/spinn4.json");
        ObjectMapper mapper = MapperFactory.createMapper();
        MachineBean fromJson = mapper.readValue(url, MachineBean.class);
        jsonMachine = new Machine(fromJson);
	}

	private static final int REPETITIONS = 50;

	@Test
	void testReliableMachine() throws Exception {
        InetAddress host = InetAddress.getByName("spinn-4.cs.man.ac.uk");
        assumeTrue(ping(host) == 0);

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
