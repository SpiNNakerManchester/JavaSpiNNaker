/*
 * Copyright (c) 2018 The University of Manchester
 */
package uk.ac.manchester.spinnaker.front_end.interfaces.buffer_management;

import uk.ac.manchester.spinnaker.connections.SCPConnection;
import uk.ac.manchester.spinnaker.connections.selectors.ConnectionSelector;
import uk.ac.manchester.spinnaker.front_end.utilities.utilityobjs.extramonitorscpprocesses.ReadStatusProcess;
import uk.ac.manchester.spinnaker.transceiver.Transceiver;

/**
 *
 * @author Christian-B
 */
class ExtraMonitorSupportMachineVertex extends Vertex {

    public ExtraMonitorSupportMachineVertex(String label,
            int recordingRegionBaseAddress, int[] recordedRegionIds){
        super(label, recordingRegionBaseAddress, recordedRegionIds);
    }

    Object getReinjectionStatus(Placements placements, Transceiver transceiver) {
        Placement placement = placements.getPlacementOfVertex(this);
        ConnectionSelector<SCPConnection> selector = transceiver.getScampConnectionSelector();
        ReadStatusProcess process = new ReadStatusProcess(selector, transceiver);
        return process.getReinjectionStatus(placement.asCoreLocation());
    }
}
