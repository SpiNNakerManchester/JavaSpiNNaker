/*
 * Copyright (c) 2018 The University of Manchester
 */
package uk.ac.manchester.spinnaker.transceiver;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.InetAddress;
import java.net.URL;
import java.net.UnknownHostException;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import uk.ac.manchester.spinnaker.machine.Machine;
import uk.ac.manchester.spinnaker.machine.MachineDimensions;
import uk.ac.manchester.spinnaker.machine.bean.MachineBean;
import uk.ac.manchester.spinnaker.machine.bean.MapperFactory;
import uk.ac.manchester.spinnaker.messages.model.VersionInfo;
import uk.ac.manchester.spinnaker.processes.Process;
import static uk.ac.manchester.spinnaker.transceiver.TestTransceiver.board_config;
import uk.ac.manchester.spinnaker.utils.InetFactory;

/**
 *
 * @author Christian-B
 */
public class IntegrationTester1 {

    public static void main(String[] callargs) throws UnknownHostException, IOException, SpinnmanException, Process.Exception, Exception {

        InetAddress remoteHost = InetFactory.getByName("spinn-4.cs.man.ac.uk");

            URL url = IntegrationTester1.class.getResource("/spinn4.json");
            ObjectMapper mapper = MapperFactory.createMapper();
            MachineBean fromJson = mapper.readValue(url, MachineBean.class);
            System.out.println(fromJson);

            Machine machine2 = new Machine(fromJson);

        for (int i = 0; i < 1; i++) {
    		try (Transceiver trans = Transceiver.createTransceiver(remoteHost, 5)) {
        		// self.assertFalse(trans.is_connected())
            	trans.ensureBoardIsReady();
                MachineDimensions dimensions = trans.getMachineDimensions();
                System.out.println(dimensions);
                VersionInfo version = trans.getScampVersion();
                System.out.println(version);
                Machine machine = trans.getMachineDetails();
                System.out.println(machine);

                if (! machine.equals(machine2)) {
                    throw new Exception ("POP");
                }
            }
		}
	}
}
