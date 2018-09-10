/*
 * Copyright (c) 2018 The University of Manchester
 */
package uk.ac.manchester.spinnaker.front_end.interfaces.buffer_management;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;
import uk.ac.manchester.spinnaker.machine.CoreLocation;
import uk.ac.manchester.spinnaker.transceiver.Transceiver;

/**
 *
 * @author Christian-B
 */
class DataSpeedUpPacketGatherMachineVertex {

    void setCoresForDataExtraction(Transceiver transceiver, Placements placements, List<ExtraMonitorSupportMachineVertex> extraMonitorCores) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    ByteBuffer getData(Transceiver transceiver, Placement senderPlacement, int address, int length, Map<CoreLocation, FixedRouteEntry> fixedRoutes) {
        // read thingie needs to be beginning of the data
        //ByteBuffer is little Endian
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

}
