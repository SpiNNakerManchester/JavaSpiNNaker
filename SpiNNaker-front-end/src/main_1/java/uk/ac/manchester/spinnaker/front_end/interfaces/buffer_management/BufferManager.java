/*
 * Copyright (c) 2018 The University of Manchester
 */
package uk.ac.manchester.spinnaker.front_end.interfaces.buffer_management;

/**
 *
 * @author Christian-B
 */
public class BufferManager {

    Tags tags;

    public BufferManager(Tags tags) {
        this.tags = tags;
    }

    //def __init__(self, placements, tags, transceiver, extra_monitor_cores,
    //             extra_monitor_cores_to_ethernet_connection_map,
    //             extra_monitor_to_chip_mapping, machine, fixed_routes,
    //             uses_advanced_monitors, store_to_file=False)

    public void addReceivingVertex(Vertex vertex) {
        addBufferListeners(vertex);
    }

    //SpiNNFrontEndCommon/spinn_front_end_common/interface/interface_functions/application_runner.py run_application
    //public void resume() {   }

    ///SpiNNFrontEndCommon/spinn_front_end_common/interface/interface_functions/application_runner.py run_application
    // public void loadInitialBuffers() { }

    private void addBufferListeners(Vertex vertex) {
        for (IPTag tag:tags.getIpTagsForVertex(vertex)) {
            
        }
    }
}
