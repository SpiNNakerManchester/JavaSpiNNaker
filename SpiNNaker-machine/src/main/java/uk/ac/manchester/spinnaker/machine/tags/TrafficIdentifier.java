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
package uk.ac.manchester.spinnaker.machine.tags;

import java.util.HashMap;
import java.util.Map;

/**
 * The types of traffic an IpTag can handle.
 *
 * @author Christian-B
 */
public final class TrafficIdentifier {
    private static final Map<String, TrafficIdentifier> MAP = new HashMap<>();

    /**
     * Default if not provided.
     *
     * @see <a href=
     *      "https://github.com/SpiNNakerManchester/SpiNNMachine/blob/master/spinn_machine/tags/iptag.py">
     *      Python IPTag</a>
     */
    public static final TrafficIdentifier DEFAULT = new TrafficIdentifier(
            "DEFAULT");

    /**
     * Used to identify buffered traffic.
     *
     * @see <a href=
     *      "https://github.com/SpiNNakerManchester/SpiNNFrontEndCommon/blob/master/spinn_front_end_common/interface/buffer_management/recording_utilities.py">
     *      Python Recording Utilities</a>
     */
    public static final TrafficIdentifier BUFFERED = new TrafficIdentifier(
            "BufferTraffic");

    /** Label used in Python. */
    public final String label;

    /**
     * Finds a TrafficIdentifier based on the label.
     *
     * @param label
     *            The label to check for. Currently case sensitive.
     * @return The TrafficIdentifier with this label
     * @throws IllegalArgumentException
     *             If this is an unexpected label.
     */
    public static TrafficIdentifier getInstance(String label) {
        if (!MAP.containsKey(label)) {
            new TrafficIdentifier(label);
        }
        return MAP.get(label);
    }

    /**
     * Main Constructor.
     *
     * @param label
     *            Python label.
     */
    private TrafficIdentifier(String label) {
        this.label = label;
        MAP.put(label, this);
    }
}
