/*
 * Copyright (c) 2021 The University of Manchester
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
package uk.ac.manchester.spinnaker.py2json;

import static java.util.EnumSet.noneOf;
import static uk.ac.manchester.spinnaker.py2json.PythonUtils.getattr;
import static uk.ac.manchester.spinnaker.py2json.PythonUtils.item;
import static uk.ac.manchester.spinnaker.py2json.PythonUtils.toCollectingMap;
import static uk.ac.manchester.spinnaker.py2json.PythonUtils.toMap;
import static uk.ac.manchester.spinnaker.py2json.PythonUtils.toSet;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

import org.python.core.PyObject;

import com.fasterxml.jackson.annotation.JsonProperty;

/** A machine description. JSON-serializable. */
public final class Machine {
	/** The name of the machine. */
	public final String name;

	/** The tags of the machine. */
	public final Set<String> tags;

	/** The width of the machine, in triads. */
	public final int width;

	/** The height of the machine, in triads. */
	public final int height;

	/** The dead boards of the machine. */
	public final Set<XYZ> deadBoards;

	/**
	 * The extra dead links of the machine. Doesn't include links to dead
	 * boards.
	 */
	public final Map<XYZ, EnumSet<Link>> deadLinks;

	/** The logical-to-physical board location map. */
	public final Map<XYZ, CFB> boardLocations;

	/** The IP addresses of the BMPs. */
	@JsonProperty("bmp-ips")
	public final Map<CF, String> bmpIPs;

	/** The IP addresses of the boards. */
	@JsonProperty("spinnaker-ips")
	public final Map<XYZ, String> spinnakerIPs;

	private static final int IDX = 3;

	Machine(PyObject machine) {
		name = getattr(machine, "name").asString();
		tags = toSet(getattr(machine, "tags"), PyObject::asString);
		width = getattr(machine, "width").asInt();
		height = getattr(machine, "height").asInt();
		deadBoards = toSet(getattr(machine, "dead_boards"), XYZ::new);
		deadLinks = toCollectingMap(getattr(machine, "dead_links"), XYZ::new,
				() -> noneOf(Link.class),
				key -> Link.values()[item(key, IDX).asInt()]);
		boardLocations =
				toMap(getattr(machine, "board_locations"), XYZ::new, CFB::new);
		bmpIPs = toMap(getattr(machine, "bmp_ips"), CF::new,
				PyObject::asString);
		spinnakerIPs = toMap(getattr(machine, "spinnaker_ips"), XYZ::new,
				PyObject::asString);
	}

	@Override
	public String toString() {
		return new StringBuilder("Machine(").append("name=").append(name)
				.append(",").append("tags=").append(tags).append(",")
				.append("width=").append(width).append(",").append("height=")
				.append(height).append(",").append("deadBoards=")
				.append(deadBoards).append(",").append("deadLinks=")
				.append(deadLinks).append(",").append("boardLocations=")
				.append(boardLocations).append(",").append("bmpIPs=")
				.append(bmpIPs).append(",").append("spinnakerIPs=")
				.append(spinnakerIPs).append(")").toString();
	}
}
