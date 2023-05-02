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

import static java.util.Collections.sort;
import static java.util.Collections.unmodifiableCollection;
import static java.util.Objects.isNull;
import static uk.ac.manchester.spinnaker.machine.MachineDefaults.PROCESSORS_PER_CHIP;
import static uk.ac.manchester.spinnaker.machine.MachineDefaults.SDRAM_PER_CHIP;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.TreeMap;

import javax.validation.Valid;

import uk.ac.manchester.spinnaker.machine.bean.ChipBean;
import uk.ac.manchester.spinnaker.machine.tags.TagID;

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
	@Valid
	private final ChipLocation location;

	private TreeMap<@ValidP Integer, @Valid Processor> monitorProcessors;

	private TreeMap<@ValidP Integer, @Valid Processor> userProcessors;

	/** A router for the chip. */
	public final Router router;

	// Changed from an Object to just an int as Object only had a single value
	/** The size of the SDRAM. */
	public final int sdram;

	/** The IP address of the chip or {@code null} if no Ethernet attached. */
	public final InetAddress ipAddress;

	/** List of SDP identifiers available. */
	private final List<@TagID Integer> tagIds;

	/**
	 * The nearest Ethernet-enabled chip coordinates, or {@code null} if none
	 * known. ("Nearest" here means "on the same board".)
	 */
	@Valid
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
	 *            The coordinates of the chip's position in the
	 *            two-dimensional grid of chips.
	 * @param processors
	 *            An iterable of processor objects.
	 * @param router
	 *            A router for the chip.
	 * @param sdram
	 *            The size of the SDRAM.
	 * @param ipAddress
	 *            The IP address of the chip's Ethernet connection, or
	 *            {@code null} if no Ethernet attached.
	 * @param tagIds
	 *            List of SDP identifiers available. Can be empty to force
	 *            empty. If {@code null}, will use the default list for
	 *            Ethernet-enabled chips and empty for non-Ethernet-enabled
	 *            chips
	 * @param nearestEthernet
	 *            The nearest Ethernet-enabled chip coordinates or {@code null}
	 *            if none known.
	 * @throws IllegalArgumentException
	 *             Thrown if multiple chips share the same ID.
	 */
	public Chip(ChipLocation location, Iterable<Processor> processors,
			Router router, int sdram, InetAddress ipAddress,
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
	 *            The coordinates of the chip's position in the
	 *            two-dimensional grid of chips.
	 * @param processors
	 *            An iterable of processor objects.
	 * @param router
	 *            A router for the chip.
	 * @param sdram
	 *            The size of the SDRAM.
	 * @param ipAddress
	 *            The IP address of the chip's attached Ethernet connection, or
	 *            {@code null} if no Ethernet attached.
	 * @param nearestEthernet
	 *            The nearest Ethernet-enabled chip coordinates, or {@code null}
	 *            if none known.
	 * @throws IllegalArgumentException
	 *             Thrown if multiple links share the same
	 *             {@code sourceLinkDirection}, or if multiple chips share
	 *             the same ID.
	 */
	public Chip(ChipLocation location, Iterable<Processor> processors,
			Router router, int sdram, InetAddress ipAddress,
			ChipLocation nearestEthernet) {
		this(location, processors, router, sdram, ipAddress, null,
				nearestEthernet);
	}

	/**
	 * Constructor for a chip with non-default processors.
	 *
	 * @param location
	 *            The coordinates of the chip's position in the
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
		this(location, processors, router, SDRAM_PER_CHIP, ipAddress,
				null, nearestEthernet);
	}

	/**
	 * Constructor for a chip with the default processors.
	 *
	 * @param location
	 *            The coordinates of the chip's position in the two-dimensional
	 *            grid of chips.
	 * @param router
	 *            A router for the chip.
	 * @param ipAddress
	 *            The IP address of the chip or {@code null} if no Ethernet
	 *            attached.
	 * @param nearestEthernet
	 *            The nearest Ethernet-connected chip's coordinates or
	 *            {@code null} if none known.
	 * @throws IllegalArgumentException
	 *             Indicates another link with this {@code sourceLinkDirection}
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

		if (ipAddress == null) {
			tagIds = List.of();
		} else {
			tagIds = DEFAULT_ETHERNET_TAG_IDS;
		}

		this.nearestEthernet = nearestEthernet;
		assert this.nearestEthernet != null;
	}

	Chip(Chip chip, Router newRouter) {
		location = chip.location;
		monitorProcessors = chip.monitorProcessors;
		userProcessors = chip.userProcessors;
		router = newRouter;

		sdram = chip.sdram;
		ipAddress = chip.ipAddress;

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
	 * <strong>Warning:</strong> If a monitor processor exists with this ID,
	 * this method will return {@code false}.
	 *
	 * @param processorId
	 *            ID of the potential processor.
	 * @return True if and only if there is a user processor for this ID.
	 */
	public boolean hasUserProcessor(int processorId) {
		return userProcessors.containsKey(processorId);
	}

	/**
	 * Obtains the user processor with this ID, or returns {@code null}.
	 * <p>
	 * This method will only check user processors so will return {@code null}
	 * even if a monitor processor exists with this ID.
	 *
	 * @param processorId
	 *            ID of the potential processor.
	 * @return The processor, or {@code null} if not is found.
	 */
	public Processor getUserProcessor(int processorId) {
		return userProcessors.get(processorId);
	}

	/**
	 * Return a list off all the processors on this chip.
	 * This method will check both the user and monitor processors.
	 * <p>
	 * The processors will be ordered by their ID, which are guaranteed to all
	 * be different.
	 * <p>
	 * The current implementation builds a new list on the fly so this list is
	 * mutable without affecting the chip. Future implementations could return
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
	 * Return a view over the user processors on this chip.
	 * Monitor processors are not included so every processor in the list is
	 * guaranteed to have the property {@code isMonitor == false}!
	 * <p>
	 * The processors will be ordered by their ID, which are guaranteed to all
	 * be different.
	 *
	 * @return A unmodifiable view over the processors.
	 */
	public Collection<Processor> userProcessors() {
		return unmodifiableCollection(userProcessors.values());
	}

	/**
	 * The total number of processors.
	 *
	 * @return The size of the processor collection.
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
	 * @return A processor
	 * @throws NoSuchElementException
	 *             If all the Processor(s) are monitors.
	 */
	public Processor getFirstUserProcessor() throws NoSuchElementException {
		return userProcessors.get(userProcessors.firstKey());
	}

	/**
	 * Get the IDs of the tags of the chip.
	 *
	 * @return the tag IDs
	 */
	public List<Integer> getTagIds() {
		return tagIds;
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
		return (obj instanceof Chip c) && isNull(difference(c));
	}

	/**
	 * Describes one difference found between this chip and another chip.
	 * This method will always return {@code null} if no difference is found
	 * between the two machines.
	 * <p>
	 * This method returns as soon as it has found a difference; there may be
	 * other unspecified differences.
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
		if (!tagIds.equals(other.tagIds)) {
			return "tagIds " + tagIds + " != " + other.tagIds;
		}
		if (!nearestEthernet.equals(other.nearestEthernet)) {
			return "router";
		}
		return null;
	}
}
