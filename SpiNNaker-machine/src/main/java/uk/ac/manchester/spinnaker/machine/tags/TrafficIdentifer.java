/*
 * Copyright (c) 2018 The University of Manchester
 */
package uk.ac.manchester.spinnaker.machine.tags;

/**
 * The types of traffic an IpTag can handle.
 *
 * @author Christian-B
 */
public enum TrafficIdentifer {

    /** Default if not provided.
     *  @see <a href=
     * "https://github.com/SpiNNakerManchester/SpiNNMachine/blob/master/spinn_machine/tags/iptag.py">
     * Python IPTag</a>
     */
    DEFAULT("DEFAULT"),
    /** Used to identify buffered traffic.
     *
     *  @see <a href=
     * "https://github.com/SpiNNakerManchester/SpiNNFrontEndCommon/blob/master/spinn_front_end_common/interface/buffer_management/recording_utilities.py">
     * Python Recording Utilities</a>
     */
    BUFFERED("BufferTraffic");

    /** Label used in Python. */
    public final String label;

    /**
     * Main Constructor.
     *
     * @param label Python label.
     */
    TrafficIdentifer(String label) {
        this.label = label;
    }

    /**
     * Finds a TrafficIdentier based on the label.
     * @param label The label to check for. Currently case sensitive.
     *
     * @return The TrafficIdentifer with this label
     * @throws IllegalArgumentException If this is an unexpected label.
     */
    public static TrafficIdentifer bylabel(String label) {
        for (TrafficIdentifer ti: TrafficIdentifer.values()) {
            if (ti.label.equals(label)) {
                return ti;
            }
        }
        throw new IllegalArgumentException(
                "No TrafficIdentifer with label " + label);
    }
}
