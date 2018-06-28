/*
 * Copyright (c) 2018 The University of Manchester
 */
package uk.ac.manchester.spinnaker.machine;

/**
 * A Description of a Spinnaker Chip.
 *
 * @see <a
 * href="https://github.com/SpiNNakerManchester/SpiNNMachine/blob/master/spinn_machine/chip.py">
 * Python Version</a>
 *
 * @author Christian-B
 */
public class Chip {

    public final int x;
    public final int y;
    public final Processor[] processors;
    //"_router", "_sdram", "_ip_address", "_virtual",
    //    "_tag_ids", "_nearest_ethernet_x", "_nearest_ethernet_y",
    //    "_n_user_processors"

    /**
     * TEMP.
     */
    public Chip() {
        x = -1;
        y = -1;
        processors = null;
    }
}
