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

import static java.util.Collections.disjoint;
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
import javax.validation.constraints.AssertTrue;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;

import org.python.core.PyObject;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.errorprone.annotations.Keep;

import uk.ac.manchester.spinnaker.machine.board.TriadCoords;
import uk.ac.manchester.spinnaker.utils.validation.IPAddress;

/** A machine description. JSON-serializable. */
public final class Machine {
	/** The name of the machine. */
	@NotBlank
	public final String name;

	/** The tags of the machine. */
	@NotEmpty
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
	@NotNull
	public final Map<@Valid TriadCoords, @NotEmpty EnumSet<Link>> deadLinks;

	/** The logical-to-physical board location map. */
	@NotNull
	public final Map<@Valid TriadCoords, @Valid CFB> boardLocations;

	/** The IP addresses of the BMPs. */
	@JsonProperty("bmp-ips")
	@NotNull
	public final Map<@Valid CF, @IPAddress String> bmpIPs;

	/** The IP addresses of the boards. */
	@JsonProperty("spinnaker-ips")
	@NotNull
	public final Map<@Valid TriadCoords, @IPAddress String> spinnakerIPs;

	private static final int IDX = 3;

	Machine(PyObject machine) {
		name = getattr(machine, "name").asString();
		tags = toSet(getattr(machine, "tags"), PyObject::asString);
		width = getattr(machine, "width").asInt();
		height = getattr(machine, "height").asInt();
		deadBoards = toSet(getattr(machine, "dead_boards"), Machine::xyz);
		deadLinks = toCollectingMap(getattr(machine, "dead_links"),
				Machine::xyz, () -> noneOf(Link.class),
				key -> Link.values()[item(key, IDX).asInt()]);
		boardLocations = toMap(getattr(machine, "board_locations"),
				Machine::xyz, CFB::new);
		bmpIPs = toMap(getattr(machine, "bmp_ips"), CF::new,
				PyObject::asString);
		spinnakerIPs = toMap(getattr(machine, "spinnaker_ips"), Machine::xyz,
				PyObject::asString);
	}

	private static TriadCoords xyz(PyObject tuple) {
		int index = 0;
		int x = item(tuple, index++).asInt();
		int y = item(tuple, index++).asInt();
		int z = item(tuple, index++).asInt();
		return new TriadCoords(x, y, z);
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

	@Keep
	@AssertTrue(message = "all boards must have a location and an IP address")
	private boolean isEveryBoardWithBMPAndIPaddress() {
		return boardLocations.keySet().equals(spinnakerIPs.keySet());
	}

	@Keep
	@AssertTrue(message = "every board's IP address must be unique")
	private boolean isEveryBoardIPAddressUnique() {
		return spinnakerIPs.size() == Set.copyOf(spinnakerIPs.values()).size();
	}

	@Keep
	@AssertTrue(message = "every BMP's IP address must be unique")
	private boolean isEveryBmpIPAddressUnique() {
		return bmpIPs.size() == Set.copyOf(bmpIPs.values()).size();
	}

	@Keep
	@AssertTrue(message = "IP addresses may not be assigned to "
			+ "both boards and BMPs")
	private boolean isBoardAddressSetDisjointFromBmpAddressSet() {
		return disjoint(spinnakerIPs.values(), bmpIPs.values());
	}

	@Keep
	@AssertTrue(message = "every board's BMP must be addressable")
	private boolean isEveryBoardManaged() {
		return boardLocations.values().stream().map(CFB::asCF)
				.allMatch(bmpIPs::containsKey);
	}
}
