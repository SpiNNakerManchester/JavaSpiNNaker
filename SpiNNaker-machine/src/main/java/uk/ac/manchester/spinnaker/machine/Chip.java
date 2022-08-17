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

import static java.util.Collections.sort;
import static java.util.Collections.unmodifiableCollection;
import static java.util.Collections.unmodifiableList;
import static uk.ac.manchester.spinnaker.machine.MachineDefaults.PROCESSORS_PER_CHIP;
import static uk.ac.manchester.spinnaker.machine.MachineDefaults.SDRAM_PER_CHIP;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.TreeMap;

import uk.ac.manchester.spinnaker.machine.bean.ChipBean;

/**
 * A Description of a Spinnaker Chip including its Router.
 *
 * @see <a href=
 *      "https://github.com/SpiNNakerManchester/SpiNNMachine/blob/master/spinn_machine/chip.py">
 *      Python Chip Version</a>
 * @see <a href=
 *      "https://github.com/SpiNNakerManchester/SpiNNMachine/blob/master/spinn_machine/router.py">
 *      Python Router Version</a>
 *
 * @author Christian-B
 */
public class Chip implements HasChipLocation {

	private final ChipLocation location;

	private TreeMap<Integer, Processor> monitorProcessors;

	private TreeMap<Integer, Processor> userProcessors;

	/** A router for the chip. */
	public final Router router;

	// Changed from an Object to just an int as Object only had a single value
	/** The size of the SDRAM. */
	public final int sdram;

	/** The IP address of the chip or None if no Ethernet attached. */
	public final InetAddress ipAddress;

	/** boolean which defines if this chip is a virtual one. */
	public final boolean virtual;

	/** List of SDP identifiers available. */
	private final List<Integer> tagIds;

	/** The nearest Ethernet coordinates, or {@code null} if none known. */
	public final ChipLocation nearestEthernet;

	private static final TreeMap<Integer, Processor> DEFAULT_USER_PROCESSORS =
			defaultUserProcessors();

	private static final TreeMap<Integer,
			Processor> DEFAULT_MONITOR_PROCESSORS = defaultMonitorProcessors();

	private static final List<Integer> DEFAULT_ETHERNET_TAG_IDS =
			List.of(1, 2, 3, 4, 5, 6, 7);

	// Note: emergency_routing_enabled not implemented as not used
	// TODO convert_routing_table_entry_to_spinnaker_route

	/**
	 * Main constructor which sets all parameters.
	 *
	 * @param location
	 *            The x and y coordinates of the chip's position in the
	 *            two-dimensional grid of chips.
	 * @param processors
	 *            An iterable of processor objects.
	 * @param router
	 *            A router for the chip.
	 * @param sdram
	 *            The size of the SDRAM.
	 * @param ipAddress
	 *            The IP address of the chip or {@code null} if no Ethernet
	 *            attached.
	 * @param virtual
	 *            boolean which defines if this chip is a virtual one
	 * @param tagIds
	 *            List of SDP identifiers available. Can be empty to force
	 *            empty. If {@code null}, will use the default list for Ethernet
	 *            Chips and empty for non-ethernet Chips
	 * @param nearestEthernet
	 *            The nearest Ethernet coordinates or {@code null} if none
	 *            known.
	 * @throws IllegalArgumentException
	 *             Thrown if multiple chips share the same id.
	 */
	public Chip(ChipLocation location, Iterable<Processor> processors,
			Router router, int sdram, InetAddress ipAddress, boolean virtual,
			List<Integer> tagIds, ChipLocation nearestEthernet) {
		this.location = location;
		monitorProcessors = new TreeMap<>();
		userProcessors = new TreeMap<>();
		processors.forEach(processor -> {
			if (monitorProcessors.containsKey(processor.processorId)) {
				throw new IllegalArgumentException("duplicate processor");
			} else if (userProcessors.containsKey(processor.processorId)) {
				throw new IllegalArgumentException("duplicate processor");
			}
			if (processor.isMonitor) {
				monitorProcessors.put(processor.processorId, processor);
			} else {
				userProcessors.put(processor.processorId, processor);
			}
		});
		this.router = router;
		this.sdram = sdram;
		this.ipAddress = ipAddress;
		this.virtual = virtual;
		if (tagIds == null) {
			if (ipAddress == null) {
				this.tagIds = List.of();
			} else {
				this.tagIds = DEFAULT_ETHERNET_TAG_IDS;
			}
		} else {
			this.tagIds = tagIds;
		}

		// previous Router stuff
		this.nearestEthernet = nearestEthernet;
	}

	/**
	 * Constructor which fills in some default values.
	 *
	 * @param location
	 *            The x and y coordinates of the chip's position in the
	 *            two-dimensional grid of chips.
	 * @param processors
	 *            An iterable of processor objects.
	 * @param router
	 *            A router for the chip.
	 * @param sdram
	 *            The size of the SDRAM.
	 * @param ipAddress
	 *            The IP address of the chip or {@code null} if no Ethernet
	 *            attached.
	 * @param nearestEthernet
	 *            The nearest Ethernet coordinates or {@code null} if none
	 *            known.
	 * @throws IllegalArgumentException
	 *             Thrown if multiple Links share the same
	 *             {@code sourceLinkDirection}. Thrown if multiple chips share
	 *             the same id.
	 */
	public Chip(ChipLocation location, Iterable<Processor> processors,
			Router router, int sdram, InetAddress ipAddress,
			ChipLocation nearestEthernet) {
		this(location, processors, router, sdram, ipAddress, false, null,
				nearestEthernet);
	}

	/**
	 * Constructor for a virtual Chip with the non-default processors.
	 * <p>
	 * Creates the Router on the fly based on the links.
	 *
	 * @param location
	 *            The x and y coordinates of the chip's position in the
	 *            two-dimensional grid of chips.
	 * @param processors
	 *            An iterable of processor objects.
	 * @param router
	 *            A router for the chip.
	 * @param ipAddress
	 *            The IP address of the chip or {@code null} if no Ethernet
	 *            attached.
	 * @param nearestEthernet
	 *            The nearest Ethernet coordinates or {@code null} if none
	 *            known.
	 * @throws IllegalArgumentException
	 *             Indicates another Link with this {@code sourceLinkDirection}
	 *             has already been added.
	 */
	public Chip(ChipLocation location, Iterable<Processor> processors,
			Router router, InetAddress ipAddress,
			ChipLocation nearestEthernet) {
		this(location, processors, router, SDRAM_PER_CHIP, ipAddress, false,
				null, nearestEthernet);
	}

	/**
	 * Constructor for a virtual Chip with the default processors.
	 * <p>
	 * Creates the Router on the fly based on the links.
	 *
	 * @param location
	 *            The x and y coordinates of the chip's position in the
	 *            two-dimensional grid of chips.
	 * @param router
	 *            A router for the chip.
	 * @param ipAddress
	 *            The IP address of the chip or {@code null} if no Ethernet
	 *            attached.
	 * @param nearestEthernet
	 *            The nearest Ethernet coordinates or {@code null} if none
	 *            known.
	 * @throws IllegalArgumentException
	 *             Indicates another Link with this {@code sourceLinkDirection}
	 *             has already been added.
	 */
	public Chip(ChipLocation location, Router router, InetAddress ipAddress,
			ChipLocation nearestEthernet) {
		this.location = location;
		monitorProcessors = DEFAULT_MONITOR_PROCESSORS;
		userProcessors = DEFAULT_USER_PROCESSORS;
		this.router = router;

		sdram = SDRAM_PER_CHIP;
		this.ipAddress = ipAddress;

		virtual = false;
		if (ipAddress == null) {
			tagIds = List.of();
		} else {
			tagIds = DEFAULT_ETHERNET_TAG_IDS;
		}

		this.nearestEthernet = nearestEthernet;
		if (virtual) {
			assert this.nearestEthernet == null;
		} else {
			assert this.nearestEthernet != null;
		}
	}

	Chip(Chip chip, Router newRouter) {
		location = chip.location;
		monitorProcessors = chip.monitorProcessors;
		userProcessors = chip.userProcessors;
		router = newRouter;

		sdram = chip.sdram;
		ipAddress = chip.ipAddress;

		virtual = chip.virtual;
		tagIds = chip.tagIds;

		nearestEthernet = chip.nearestEthernet;
	}

	Chip(ChipBean bean, Machine machine) {
		var details = bean.getDetails();
		var resources = bean.getResources();

		location = bean.getLocation();
		monitorProcessors = provideMonitors(resources.getMonitors());
		userProcessors =
				provideUserProcesses(resources.getMonitors(), details.cores);

		router = new Router(location, resources.getRouterEntries(), details,
				machine);

		sdram = resources.getSdram();
		ipAddress = details.getIpAddress();
		virtual = resources.getVirtual();
		tagIds = resources.getTags();

		nearestEthernet = details.getEthernet(); // chip.nearestEthernet;
	}

	private static TreeMap<Integer, Processor> defaultUserProcessors() {
		var processors = new TreeMap<Integer, Processor>();
		for (int i = 1; i < PROCESSORS_PER_CHIP; i++) {
			processors.put(i, Processor.factory(i, false));
		}
		return processors;
	}

	private static TreeMap<Integer, Processor> defaultMonitorProcessors() {
		var processors = new TreeMap<Integer, Processor>();
		processors.put(0, Processor.factory(0, true));
		return processors;
	}

	private static TreeMap<Integer, Processor> provideMonitors(int monitors) {
		var processors = new TreeMap<Integer, Processor>();
		for (int i = 0; i < monitors; i++) {
			processors.put(i, Processor.factory(i, true));
		}
		return processors;
	}

	private TreeMap<Integer, Processor> provideUserProcesses(int monitors,
			int cores) {
		var processors = new TreeMap<Integer, Processor>();
		for (int i = monitors; i < cores; i++) {
			processors.put(i, Processor.factory(i, false));
		}
		return processors;
	}

	@Override
	public int getX() {
		return location.getX();
	}

	@Override
	public int getY() {
		return location.getY();
	}

	@Override
	public ChipLocation asChipLocation() {
		return location;
	}

	/**
	 * Determines if a user processor with the given ID exists in the chip.
	 * <p>
	 * Warning: If a Monitor processor exists with this ID this method will
	 * return false.
	 *
	 * @param processorId
	 *            Id of the potential processor.
	 * @return True if and only if there is a user processor for this ID.
	 */
	public boolean hasUserProcessor(int processorId) {
		return userProcessors.containsKey(processorId);
	}

	/**
	 * Obtains the User Processor with this ID, or returns {@code null}.
	 * <p>
	 * This method will only check user processors so will return {@code null}
	 * even if a monitor processor exists with this id.
	 *
	 * @param processorId
	 *            Id of the potential processor.
	 * @return The Processor or {@code null} if not is found.
	 */
	public Processor getUserProcessor(int processorId) {
		return userProcessors.get(processorId);
	}

	/**
	 * Return a list off all the Processors on this Chip
	 * <p>
	 * This method will check both the user and monitor processors.
	 * <p>
	 * The Processors will be ordered by ProcessorID which are guaranteed to all
	 * be different.
	 * <p>
	 * The current implementation builds a new list on the fly so this list is
	 * mutable without affecting the Chip. Future implementations could return
	 * an unmodifiable list.
	 *
	 * @return A list of all the processors including both monitor and user.
	 */
	public List<Processor> allProcessors() {
		var all = new ArrayList<>(monitorProcessors.values());
		all.addAll(userProcessors.values());
		sort(all);
		return all;
	}

	/**
	 * Return a view over the User Processors on this Chip
	 * <p>
	 * Monitor processors are not included so every Processor in the list is
	 * guaranteed to have the property {@code isMonitor == false}!
	 * <p>
	 * The Processors will be ordered by ProcessorID which are guaranteed to all
	 * be different.
	 *
	 * @return A unmodifiable View over the processors.
	 */
	public Collection<Processor> userProcessors() {
		return unmodifiableCollection(userProcessors.values());
	}

	/**
	 * The total number of processors.
	 *
	 * @return The size of the Processor Collection
	 */
	public int nProcessors() {
		return userProcessors.size() + monitorProcessors.size();
	}

	/**
	 * The total number of user processors.
	 * <p>
	 * For just the user processors so ignores monitor processors.
	 *
	 * @return The size of the Processor Collection
	 */
	public int nUserProcessors() {
		return userProcessors.size();
	}

	/**
	 * Get the first processor in the list which is not a monitor core.
	 *
	 * @return A Processor
	 * @throws NoSuchElementException
	 *             If all the Processor(s) are monitors.
	 */
	public Processor getFirstUserProcessor() throws NoSuchElementException {
		return userProcessors.get(userProcessors.firstKey());
	}

	/**
	 * @return the tagIds
	 */
	public List<Integer> getTagIds() {
		return unmodifiableList(tagIds);
	}

	@Override
	public String toString() {
		return "[Chip: x=" + getX() + ", y=" + getY() + ", sdram=" + sdram
				+ ", ip_address=" + ipAddress + ", router=" + router
				+ ", monitors=" + monitorProcessors.keySet() + ", users="
				+ userProcessors.keySet() + ", nearest_ethernet="
				+ nearestEthernet + "]";
	}

	@Override
	public int hashCode() {
		throw new UnsupportedOperationException(
				"hashCode not supported as equals implemented.");
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof Chip)) {
			return false;
		}
		return difference((Chip) obj) == null;
	}

	/**
	 * Describes one difference found between this machine and another machine.
	 * <p>
	 * This method will always return {@code null} if no difference is found
	 * between the two machines. So semantically is the same as Equals except
	 * that this works if other is a super class of machine in which case only
	 * the share variables are compared.
	 * <p>
	 * This method returns as soon as it has found a difference so there may be
	 * other not specified differences.
	 * <p>
	 * <strong>Warning:</strong> This method could change over time, so there is
	 * no implied guarantee to the order that variables are checked or to the
	 * message that is returned.
	 * <p>
	 * The only guarantee is that {@code null} is returned if no difference is
	 * detected.
	 *
	 * @param other
	 *            Another chip to check if it has the same variables.
	 * @return {@code null} if no difference is detected, otherwise a string
	 *         describing the difference.
	 */
	public String difference(Chip other) {
		if (!location.equals(other.location)) {
			return "Location";
		}
		if (!monitorProcessors.equals(other.monitorProcessors)) {
			return "Monitors";
		}
		if (!userProcessors.equals(other.userProcessors)) {
			return "userProcessors";
		}
		if (!router.equals(other.router)) {
			return "router";
		}
		if (sdram != other.sdram) {
			return "sdram";
		}
		if (!Objects.equals(ipAddress, other.ipAddress)) {
			return "ipAddress";
		}
		if (virtual != other.virtual) {
			return "virtual";
		}
		if (!tagIds.equals(other.tagIds)) {
			return "tagIds " + tagIds + " != " + other.tagIds;
		}
		if (!nearestEthernet.equals(other.nearestEthernet)) {
			return "router";
		}
		return null;
	}
}
