/*
 * Copyright (c) 2018 The University of Manchester
 */
package uk.ac.manchester.spinnaker.front_end.interfaces.buffer_management;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;
import uk.ac.manchester.spinnaker.machine.Machine;
import uk.ac.manchester.spinnaker.machine.bean.MachineBean;
import uk.ac.manchester.spinnaker.machine.bean.MapperFactory;
import uk.ac.manchester.spinnaker.storage.StorageException;
import uk.ac.manchester.spinnaker.transceiver.SpinnmanException;
import uk.ac.manchester.spinnaker.transceiver.Transceiver;
import uk.ac.manchester.spinnaker.transceiver.processes.Process;

/**
 *
 * @author Christian-B
 */
public class DataReceiverRunner {

	public static void main(String... args) throws IOException, SpinnmanException, Process.Exception, StorageException {
        //args 0 = instruction to run this
        ObjectMapper mapper = MapperFactory.createMapper();
        FileReader placement_reader = new FileReader(args[1]);

        List<Placement> placements = mapper.readValue(
                placement_reader, new TypeReference<List<Placement>>(){});
        FileReader machine_reader = new FileReader(args[2]);

        MachineBean fromJson = mapper.readValue(
                machine_reader, MachineBean.class);
        Machine machine = new Machine(fromJson);
        Transceiver trans = new Transceiver(machine.getBootEthernetAddress(), 3);
        System.out.println(args[3]);
        DataReceiver receiver = new  DataReceiver(trans, args[3]);
        receiver.getDataForPlacements(placements, null);
        System.out.println("Done");
    }
}
