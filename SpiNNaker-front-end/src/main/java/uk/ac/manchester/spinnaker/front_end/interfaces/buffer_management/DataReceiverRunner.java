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
import uk.ac.manchester.spinnaker.transceiver.processes.ProcessException;

/**
 * Prototype for early testing.
 *
 * @author Christian-B
 */
public final class DataReceiverRunner {

    private DataReceiverRunner() {
    }

    private static final int THIRD = 3;

    /**
	 * Prototype for early testing.
	 *
	 * @param args
	 *            Arguements as received.
	 * @throws IOException
	 *             If the communications fail
	 * @throws SpinnmanException
	 *             If a BMP is uncontactable
	 * @throws ProcessException
	 *             If SpiNNaker rejects a message
	 * @throws StorageException
	 *             If the database is in an illegal state
	 */
	public static void main(String... args)
            throws IOException, SpinnmanException,
            StorageException, ProcessException {
        //args 0 = instruction to run this
        ObjectMapper mapper = MapperFactory.createMapper();
        FileReader placementReader = new FileReader(args[1]);
        List<Placement> placements = mapper.readValue(
                placementReader, new TypeReference<List<Placement>>() { });

        FileReader machineReader = new FileReader(args[2]);
        MachineBean fromJson = mapper.readValue(
                machineReader, MachineBean.class);
        Machine machine = new Machine(fromJson);

        Transceiver trans = new Transceiver(
                machine.getBootEthernetAddress(), machine.version);

        DataReceiver receiver = new  DataReceiver(trans, args[THIRD]);
        receiver.getDataForPlacements(placements, null);
    }
}
