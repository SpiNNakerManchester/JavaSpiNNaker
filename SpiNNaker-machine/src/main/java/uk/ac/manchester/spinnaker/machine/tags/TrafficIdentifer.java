/*
 * Copyright (c) 2018 The University of Manchester
 */
package uk.ac.manchester.spinnaker.machine.tags;

import java.util.HashMap;
import java.util.Map;

/**
 * The types of traffic an IpTag can handle.
 *
 * @author Christian-B
 */
public final class TrafficIdentifer {

    private static final Map<String, TrafficIdentifer> MAP = new HashMap<>();

    /** Default if not provided.
     *
     * @see <a href=
     * "https://github.com/SpiNNakerManchester/SpiNNMachine/blob/master/spinn_machine/tags/iptag.py">
     * Python IPTag</a>
     */
    public static final TrafficIdentifer DEFAULT = new TrafficIdentifer(
            "DEFAULT");
    /** Used to identify buffered traffic.
     *
     * @see <a href=
     * "https://github.com/SpiNNakerManchester/SpiNNFrontEndCommon/blob/master/spinn_front_end_common/interface/buffer_management/recording_utilities.py">
     * Python Recording Utilities</a>
     */
    public static final TrafficIdentifer BUFFERED = new TrafficIdentifer(
            "BufferTraffic");

    /** Label used in Python. */
    public final String label;

    /**
     * Finds a TrafficIdentier based on the label.
     *
     * @param label
     *            The label to check for. Currently case sensitive.
     * @return The TrafficIdentifer with this label
     * @throws IllegalArgumentException
     *             If this is an unexpected label.
     */
    public static TrafficIdentifer getInstance(String label) {
        if (!MAP.containsKey(label)) {
            new TrafficIdentifer(label);
        }
        return MAP.get(label);
    }

    /**
     * Main Constructor.
     *
     * @param label
     *            Python label.
     */
    private TrafficIdentifer(String label) {
        this.label = label;
        MAP.put(label, this);
    }
}
