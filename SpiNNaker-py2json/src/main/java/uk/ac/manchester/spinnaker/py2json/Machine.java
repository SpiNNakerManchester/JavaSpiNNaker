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

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Positive;

import org.python.core.PyObject;

import com.fasterxml.jackson.annotation.JsonProperty;

import uk.ac.manchester.spinnaker.machine.board.BMPCoords;
import uk.ac.manchester.spinnaker.machine.board.CFB;
import uk.ac.manchester.spinnaker.machine.board.Link;
import uk.ac.manchester.spinnaker.machine.board.TriadCoords;
import uk.ac.manchester.spinnaker.machine.tags.IPAddress;

/** A machine description. JSON-serializable. */
public final class Machine {
	/** The name of the machine. */
	@NotBlank
	public final String name;

	/** The tags of the machine. */
	public final Set<@NotBlank String> tags;

	/** The width of the machine, in triads. */
	@Positive
	public final int width;

	/** The height of the machine, in triads. */
	@Positive
	public final int height;

	/** The dead boards of the machine. */
	public final Set<@Valid TriadCoords> deadBoards;

	/**
	 * The extra dead links of the machine. Doesn't include links to dead
	 * boards.
	 */
	public final Map<@Valid TriadCoords, EnumSet<Link>> deadLinks;

	/** The logical-to-physical board location map. */
	public final Map<@Valid TriadCoords, @Valid CFB> boardLocations;

	/** The IP addresses of the BMPs. */
	@JsonProperty("bmp-ips")
	public final Map<@Valid BMPCoords, @IPAddress String> bmpIPs;

	/** The IP addresses of the boards. */
	@JsonProperty("spinnaker-ips")
	public final Map<@Valid TriadCoords, @IPAddress String> spinnakerIPs;

	private static final int IDX = 3;

	Machine(PyObject machine) {
		name = getattr(machine, "name").asString();
		tags = toSet(getattr(machine, "tags"), PyObject::asString);
		width = getattr(machine, "width").asInt();
		height = getattr(machine, "height").asInt();
		deadBoards = toSet(getattr(machine, "dead_boards"), Machine::newXYZ);
		deadLinks = toCollectingMap(getattr(machine, "dead_links"),
				Machine::newXYZ, () -> noneOf(Link.class),
				key -> Link.values()[item(key, IDX).asInt()]);
		boardLocations = toMap(getattr(machine, "board_locations"),
				Machine::newXYZ, Machine::newCFB);
		bmpIPs = toMap(getattr(machine, "bmp_ips"), Machine::newCF,
				PyObject::asString);
		spinnakerIPs = toMap(getattr(machine, "spinnaker_ips"), Machine::newXYZ,
				PyObject::asString);
	}

	private static BMPCoords newCF(PyObject tuple) {
		int index = 0;
		var c = item(tuple, index++);
		var f = item(tuple, index++);
		return new BMPCoords(c.asInt(), f.asInt());
	}

	private static CFB newCFB(PyObject tuple) {
		int index = 0;
		var c = item(tuple, index++);
		var f = item(tuple, index++);
		var b = item(tuple, index++);
		return new CFB(c.asInt(), f.asInt(), b.asInt());
	}

	private static TriadCoords newXYZ(PyObject tuple) {
		int index = 0;
		var x = item(tuple, index++);
		var y = item(tuple, index++);
		var z = item(tuple, index++);
		return new TriadCoords(x.asInt(), y.asInt(), z.asInt());
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
