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
import uk.ac.manchester.spinnaker.machine.MachineVersion;
import uk.ac.manchester.spinnaker.machine.bean.MachineBean;
import uk.ac.manchester.spinnaker.machine.bean.MapperFactory;
import uk.ac.manchester.spinnaker.storage.StorageException;
import uk.ac.manchester.spinnaker.transceiver.SpinnmanException;
import uk.ac.manchester.spinnaker.transceiver.Transceiver;
import uk.ac.manchester.spinnaker.transceiver.processes.Process;

/**
 * Prototype for early testing.
 *
 * @author Christian-B
 */
public final class DataReceiverRunner {

    private DataReceiverRunner() {
    }

    /** TEMP VARIABLE NEEDS MOVING. */
    public static final int DEFAULT_HARDWARE = 5;

    private static final int THIRD = 3;

    /**
     * Prototype for early testing.
     * @param args Arguements as received.
     * @throws IOException
     * @throws SpinnmanException
     * @throws
     *      uk.ac.manchester.spinnaker.transceiver.processes.Process.Exception
     * @throws StorageException
     */
	public static void main(String... args)
            throws IOException, SpinnmanException, Process.Exception,
            StorageException {
        //args 0 = instruction to run this
        ObjectMapper mapper = MapperFactory.createMapper();
        FileReader placementReader = new FileReader(args[1]);
        List<Placement> placements = mapper.readValue(
                placementReader, new TypeReference<List<Placement>>() { });

        FileReader machineReader = new FileReader(args[2]);
        MachineBean fromJson = mapper.readValue(
                machineReader, MachineBean.class);
        Machine machine = new Machine(fromJson);

        // TODO: MachineVersion needs pushing down!
        MachineVersion version = machine.version;
        int hardwareVersion;
        if (version.id == null) {
            hardwareVersion = DEFAULT_HARDWARE;
        } else {
            hardwareVersion = version.id;
        }
        Transceiver trans = new Transceiver(
                machine.getBootEthernetAddress(), hardwareVersion);

        DataReceiver receiver = new  DataReceiver(trans, args[THIRD]);
        receiver.getDataForPlacements(placements, null);
    }
}
