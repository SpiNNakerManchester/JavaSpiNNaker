/*
 * Copyright (c) 2018 The University of Manchester
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
package uk.ac.manchester.spinnaker.machine;

import static uk.ac.manchester.spinnaker.machine.ChipLocation.ZERO_ZERO;
import static uk.ac.manchester.spinnaker.machine.MachineDefaults.FOUR_CHIP_DOWN_LINKS;
import static uk.ac.manchester.spinnaker.machine.MachineDefaults.PROCESSORS_PER_CHIP;
import static uk.ac.manchester.spinnaker.machine.SpiNNakerTriadGeometry.getSpinn5Geometry;
import static uk.ac.manchester.spinnaker.utils.InetFactory.getByAddress;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import uk.ac.manchester.spinnaker.utils.MappableIterable;

/**
 * A representation of a SpiNNaker Machine with a number of {@link Chip}s. This
 * machine is one that has been constructed to model the real world, but which
 * is not actually based truly on real-world data.
 *
 * @see <a href=
 *      "https://github.com/SpiNNakerManchester/SpiNNMachine/blob/master/spinn_machine/machine.py">
 *      Python Version</a>
 * @author Christian-B
 */
public class VirtualMachine extends Machine {
	/**
	 * Creates a virtual machine to fill the machine dimensions.
	 *
	 * @param machineDimensions
	 *            Size of the machine along the X and Y axes, in chips.
	 * @param ignoreChips
	 *            A set of chips to ignore in the machine. Requests for a
	 *            "machine" will have these chips excluded, as if they never
	 *            existed. The processor IDs of the specified chips are ignored.
	 *            May be {@code null}.
	 * @param ignoreCores
	 *            A map of cores to ignore in the machine. Requests for a
	 *            "machine" will have these cores excluded, as if they never
	 *            existed. May be {@code null}.
	 * @param ignoreLinks
	 *            A set of links to ignore in the machine. Requests for a
	 *            "machine" will have these links excluded, as if they never
	 *            existed. May be {@code null}.
	 */
	public VirtualMachine(MachineDimensions machineDimensions,
			Set<ChipLocation> ignoreChips,
			Map<ChipLocation, Set<Integer>> ignoreCores,
			Map<ChipLocation, EnumSet<Direction>> ignoreLinks) {
		super(machineDimensions, ZERO_ZERO);

		if (ignoreChips == null) {
			ignoreChips = Set.of();
		}
		if (ignoreCores == null) {
			ignoreCores = Map.of();
		}
		if (ignoreLinks == null) {
			ignoreLinks = new HashMap<>();
		} else {
			// Copy because we want to modify this below
			ignoreLinks = new HashMap<>(ignoreLinks);
		}

		addVersionIgnores(ignoreLinks);

		var geometry = getSpinn5Geometry();

		// Get all the root and therefore ethernet locations
		var roots = geometry.getPotentialRootChips(machineDimensions);

		// Get all the valid locations
		var allChips = new HashMap<ChipLocation, ChipLocation>();
		for (var root : roots) {
			for (var local : geometry.singleBoard()) {
				var normalized = normalizedLocation(
						root.getX() + local.getX(), root.getY() + local.getY());
				if (!ignoreChips.contains(normalized)) {
					allChips.put(normalized, root);
				}
			}
		}
		for (var location : allChips.keySet()) {
			var router = getRouter(location, allChips, ignoreLinks);
			var ipAddress = getIpaddress(location, roots);
			addChip(getChip(location, router, ipAddress, allChips.get(location),
					ignoreCores));
		}
	}

	/**
	 * Creates a virtual machine to fill the machine dimensions with no ignores.
	 *
	 * @param machineDimensions
	 *            Size of the machine along the X and Y axes, in chips.
	 */
	public VirtualMachine(MachineDimensions machineDimensions) {
		this(machineDimensions, null, null, null);
	}

	/**
	 * Creates a virtual machine based on the given machine version.
	 *
	 * @param version
	 *            A version which specifies fixed size.
	 */
	public VirtualMachine(MachineVersion version) {
		this(version.machineDimensions, null, null, null);
	}

	private void addVersionIgnores(
			Map<ChipLocation, EnumSet<Direction>> ignoreLinks) {
		if (version.isFourChip) {
			ignoreLinks.putAll(FOUR_CHIP_DOWN_LINKS);
		}
	}

	private Router getRouter(ChipLocation location,
			Map<ChipLocation, ChipLocation> allChips,
			Map<ChipLocation, EnumSet<Direction>> ignoreLinks) {
		MappableIterable<Link> links;
		if (ignoreLinks.containsKey(location)) {
			links = getLinks(location, allChips, ignoreLinks.get(location));
		} else {
			links = getLinks(location, allChips);
		}
		return new Router(links);
	}

	private MappableIterable<Link> getLinks(ChipLocation location,
			Map<ChipLocation, ChipLocation> allChips) {
		var links = new ArrayList<Link>();
		for (var direction : Direction.values()) {
			var destination = normalizedMove(location, direction);
			if (allChips.containsKey(destination)) {
				links.add(new Link(location, direction, destination));
			}
		}
		return links::iterator;
	}

	private MappableIterable<Link> getLinks(ChipLocation location,
			Map<ChipLocation, ChipLocation> allChips,
			EnumSet<Direction> ignoreLinks) {
		var links = new ArrayList<Link>();
		for (var direction : Direction.values()) {
			if (!ignoreLinks.contains(direction)) {
				var destination = normalizedMove(location, direction);
				if (allChips.containsKey(destination)) {
					links.add(new Link(location, direction, destination));
				}
			}
		}
		return links::iterator;
	}

	private Chip getChip(ChipLocation location, Router router,
			InetAddress ipAddress, ChipLocation ethernet,
			Map<ChipLocation, Set<Integer>> ignoreCores) {
		if (ignoreCores.containsKey(location)) {
			var ignoreProcessors = ignoreCores.get(location);
			var processors = new ArrayList<Processor>();
			if (!ignoreProcessors.contains(0)) {
				processors.add(Processor.factory(0, true));
			}
			for (int i = 1; i < PROCESSORS_PER_CHIP; i++) {
				if (!ignoreProcessors.contains(i)) {
					processors.add(Processor.factory(i, false));
				}
			}
			return new Chip(location, processors, router, ipAddress, ethernet);
		} else {
			return new Chip(location, router, ipAddress, ethernet);
		}
	}

	// Hide magic numbers
	private static final int LOCAL_HOST_ONE = 127;

	private Inet4Address getIpaddress(ChipLocation location,
			Set<ChipLocation> roots) {
		if (!roots.contains(location)) {
			return null;
		}

		// Addr is 127.0.X.Y
		byte[] bytes = {
			// Fixed prefix
			LOCAL_HOST_ONE, 0,
			// Variable suffix
			(byte) location.getX(), (byte) location.getY()
		};
		try {
			return getByAddress(bytes);
		} catch (UnknownHostException ex) {
			// Should never happen so convert to non-catchable
			throw new Error(ex);
		}
	}
}
