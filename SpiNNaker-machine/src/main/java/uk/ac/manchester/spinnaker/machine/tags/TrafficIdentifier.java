/*
 * Copyright (c) 2018-2023 The University of Manchester
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package uk.ac.manchester.spinnaker.machine.tags;

import java.util.HashMap;
import java.util.Map;

/**
 * The types of traffic an IpTag can handle. Note that this is <em>not</em> a
 * closed set; there are <em>definitely</em> traffic identifiers not listed in
 * here.
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
	public static final TrafficIdentifier DEFAULT =
			new TrafficIdentifier("DEFAULT");

	/**
	 * Used to identify buffered traffic.
	 *
	 * @see <a href=
	 *      "https://github.com/SpiNNakerManchester/SpiNNFrontEndCommon/blob/master/spinn_front_end_common/interface/buffer_management/recording_utilities.py">
	 *      Python Recording Utilities</a>
	 */
	public static final TrafficIdentifier BUFFERED =
			new TrafficIdentifier("BufferTraffic");

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
