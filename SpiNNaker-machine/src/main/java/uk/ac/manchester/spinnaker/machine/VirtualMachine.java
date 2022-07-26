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
package uk.ac.manchester.spinnaker.machine;

import static java.util.Collections.emptyMap;
import static java.util.Collections.emptySet;
import static java.util.Objects.isNull;
import static uk.ac.manchester.spinnaker.machine.ChipLocation.ZERO_ZERO;
import static uk.ac.manchester.spinnaker.machine.MachineDefaults.FOUR_CHIP_DOWN_LINKS;
import static uk.ac.manchester.spinnaker.machine.MachineDefaults.PROCESSORS_PER_CHIP;
import static uk.ac.manchester.spinnaker.machine.SpiNNakerTriadGeometry.getSpinn5Geometry;
import static uk.ac.manchester.spinnaker.utils.InetFactory.getByAddress;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import uk.ac.manchester.spinnaker.utils.MappableIterable;

/**
 * A representation of a SpiNNaker Machine with a number of {@link Chip}s. This
 * machine is one that has been constructed to model the real world, but which
 * is not actually based truly on real-world data.
 * <p>
 * Machine is also iterable, providing {@code ((x, y), chip)} where: {@code x}
 * is the x-coordinate of a chip. {@code y} is the y-coordinate of a chip, and
 * {@code chip} is the chip with the given {@code x, y} coordinates.
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
	 *            Size of the machine along the x and y axes in Chips.
	 * @param ignoreChips
	 *            A set of chips to ignore in the machine. Requests for a
	 *            "machine" will have these chips excluded, as if they never
	 *            existed. The processor IDs of the specified chips are ignored.
	 * @param ignoreCores
	 *            A map of cores to ignore in the machine. Requests for a
	 *            "machine" will have these cores excluded, as if they never
	 *            existed.
	 * @param ignoreLinks
	 *            A set of links to ignore in the machine. Requests for a
	 *            "machine" will have these links excluded, as if they never
	 *            existed.
	 */
	public VirtualMachine(MachineDimensions machineDimensions,
			Set<ChipLocation> ignoreChips,
			Map<ChipLocation, Set<Integer>> ignoreCores,
			Map<ChipLocation, Set<Direction>> ignoreLinks) {
		super(machineDimensions, ZERO_ZERO);

		if (isNull(ignoreChips)) {
			ignoreChips = emptySet();
		}
		if (isNull(ignoreCores)) {
			ignoreCores = emptyMap();
		}
		if (isNull(ignoreLinks)) {
			ignoreLinks = new HashMap<>();
		}

		addVersionIgnores(ignoreLinks);

		SpiNNakerTriadGeometry geometry = getSpinn5Geometry();

		// Get all the root and therefore ethernet locations
		Set<ChipLocation> roots =
				geometry.getPotentialRootChips(machineDimensions);

		// Get all the valid locations
		Map<ChipLocation, ChipLocation> allChips = new HashMap<>();
		for (ChipLocation root : roots) {
			for (ChipLocation local : geometry.singleBoard()) {
				ChipLocation normalized = normalizedLocation(
						root.getX() + local.getX(), root.getY() + local.getY());
				if (!ignoreChips.contains(normalized)) {
					allChips.put(normalized, root);
				}
			}
		}
		for (ChipLocation location : allChips.keySet()) {
			Router router = getRouter(location, allChips, ignoreLinks);
			InetAddress ipAddress = getIpaddress(location, roots);
			addChip(getChip(location, router, ipAddress, allChips.get(location),
					ignoreCores));
		}
	}

	/**
	 * Creates a virtual machine to fill the machine dimensions with no ignores.
	 *
	 * @param machineDimensions
	 *            Size of the machine along the x and y axes in Chips.
	 */
	public VirtualMachine(MachineDimensions machineDimensions) {
		this(machineDimensions, null, null, null);
	}

	/**
	 * Creates a virtual machine based on the MachineVersion.
	 *
	 * @param version
	 *            A version which specifies fixed size.
	 */
	public VirtualMachine(MachineVersion version) {
		this(version.machineDimensions, null, null, null);
	}

	private void addVersionIgnores(
			Map<ChipLocation, Set<Direction>> ignoreLinks) {
		if (version.isFourChip) {
			ignoreLinks.putAll(FOUR_CHIP_DOWN_LINKS);
		}
	}

	private Router getRouter(ChipLocation location,
			Map<ChipLocation, ChipLocation> allChips,
			Map<ChipLocation, Set<Direction>> ignoreLinks) {
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
		List<Link> links = new ArrayList<>();
		for (Direction direction : Direction.values()) {
			ChipLocation destination = normalizedMove(location, direction);
			if (allChips.containsKey(destination)) {
				links.add(new Link(location, direction, destination));
			}
		}
		return links::iterator;
	}

	private MappableIterable<Link> getLinks(ChipLocation location,
			Map<ChipLocation, ChipLocation> allChips,
			Set<Direction> ignoreLinks) {
		List<Link> links = new ArrayList<>();
		for (Direction direction : Direction.values()) {
			if (!ignoreLinks.contains(direction)) {
				ChipLocation destination = normalizedMove(location, direction);
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
			Set<Integer> ignoreProcessors = ignoreCores.get(location);
			Collection<Processor> processors = new ArrayList<>();
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
		if (roots.contains(location)) {
			// Addr is 127.0.X.Y
			byte[] bytes = new byte[] {
				// Fixed prefix
				LOCAL_HOST_ONE, 0,
				// Variable suffix
				(byte) location.getX(), (byte) location.getY()
			};
			try {
				return getByAddress(bytes);
			} catch (UnknownHostException ex) {
				// Should never happen so convert to none catchable
				throw new Error(ex);
			}
		} else {
			return null;
		}
	}
}
