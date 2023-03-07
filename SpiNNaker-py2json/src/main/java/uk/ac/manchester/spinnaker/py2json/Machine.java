/*
 * Copyright (c) 2021 The University of Manchester
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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

import org.python.core.PyObject;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.errorprone.annotations.Keep;

import uk.ac.manchester.spinnaker.machine.board.BMPCoords;
import uk.ac.manchester.spinnaker.machine.board.PhysicalCoords;
import uk.ac.manchester.spinnaker.machine.board.TriadCoords;
import uk.ac.manchester.spinnaker.machine.board.ValidTriadHeight;
import uk.ac.manchester.spinnaker.machine.board.ValidTriadWidth;
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
	@ValidTriadWidth
	public final int width;

	/** The height of the machine, in triads. */
	@ValidTriadHeight
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
	public final Map<@Valid TriadCoords, @Valid PhysicalCoords> boardLocations;

	/** The IP addresses of the BMPs. */
	@JsonProperty("bmp-ips")
	@NotNull
	public final Map<@Valid BMPCoords, @IPAddress String> bmpIPs;

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
				Machine::xyz, Machine::cfb);
		bmpIPs = toMap(getattr(machine, "bmp_ips"), Machine::cf,
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

	private static PhysicalCoords cfb(PyObject tuple) {
		int index = 0;
		int c = item(tuple, index++).asInt();
		int f = item(tuple, index++).asInt();
		int b = item(tuple, index++).asInt();
		return new PhysicalCoords(c, f, b);
	}

	private static BMPCoords cf(PyObject tuple) {
		int index = 0;
		int c = item(tuple, index++).asInt();
		int f = item(tuple, index++).asInt();
		return new BMPCoords(c, f);
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
		return boardLocations.values().stream()
				.map(PhysicalCoords::getBmpCoords)
				.allMatch(bmpIPs::containsKey);
	}
}
