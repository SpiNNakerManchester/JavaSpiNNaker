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

import static java.util.Collections.unmodifiableCollection;
import static java.util.Collections.unmodifiableList;
import static java.util.Collections.unmodifiableMap;
import static java.util.Collections.unmodifiableSet;
import static java.util.Collections.unmodifiableSortedMap;
import static java.util.EnumSet.noneOf;
import static java.util.Objects.isNull;
import static java.util.stream.Collectors.toUnmodifiableSet;
import static org.slf4j.LoggerFactory.getLogger;
import static uk.ac.manchester.spinnaker.machine.SpiNNakerTriadGeometry.getSpinn5Geometry;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import javax.validation.Valid;

import uk.ac.manchester.spinnaker.machine.bean.MachineBean;
import uk.ac.manchester.spinnaker.machine.datalinks.FPGALinkData;
import uk.ac.manchester.spinnaker.machine.datalinks.FpgaEnum;
import uk.ac.manchester.spinnaker.machine.datalinks.FpgaId;
import uk.ac.manchester.spinnaker.machine.datalinks.InetIdTuple;
import uk.ac.manchester.spinnaker.machine.datalinks.SpinnakerLinkData;
import uk.ac.manchester.spinnaker.utils.DoubleMapIterable;
import uk.ac.manchester.spinnaker.utils.MappableIterable;
import uk.ac.manchester.spinnaker.utils.TripleMapIterable;

/**
 * A representation of a SpiNNaker Machine with a number of Chips.
 * <p>
 * Machine is also iterable, providing the chips within the machine.
 *
 * @see <a href=
 *      "https://github.com/SpiNNakerManchester/SpiNNMachine/blob/master/spinn_machine/machine.py">
 *      Python Version</a>
 *
 * @author Christian-B
 */
public class Machine implements MappableIterable<Chip> {
	/** Size of the machine along the X and Y axes, in chips. */
	@Valid
	public final MachineDimensions machineDimensions;

	// This is not final as will change as processors become monitors.
	private int maxUserProssorsOnAChip;

	private final ArrayList<@Valid Chip> ethernetConnectedChips;

	// This may change to a map of maps
	private final Map<InetIdTuple, @Valid SpinnakerLinkData> spinnakerLinks;

	/** Map of map of map implementation done to allow access to submaps. */
	// If never required this could be changed to single map with tuple key.
	private final Map<InetAddress,
			Map<FpgaId, Map<Integer, @Valid FPGALinkData>>> fpgaLinks;

	/** The coordinates of the chip used to boot the machine. */
	@Valid
	public final ChipLocation boot;

	// Not final as currently could come from a chip added later.
	private InetAddress bootEthernetAddress;

	private final TreeMap<@Valid ChipLocation, @Valid Chip> chips;
	// private final Chip[][] chipArray;

	/** The version of the machine based on its height and width. */
	public final MachineVersion version;

	/**
	 * Creates an empty machine.
	 *
	 * @param machineDimensions
	 *            Size of the machine along the X and Y axes, in chips.
	 * @param boot
	 *            The coordinates of the chip used to boot the machine.
	 */
	public Machine(MachineDimensions machineDimensions, HasChipLocation boot) {
		this.machineDimensions = machineDimensions;
		version = MachineVersion.bySize(machineDimensions);

		maxUserProssorsOnAChip = 0;

		ethernetConnectedChips = new ArrayList<>();
		spinnakerLinks = new HashMap<>();
		fpgaLinks = new HashMap<>();

		this.boot = boot.asChipLocation();
		bootEthernetAddress = null;

		chips = new TreeMap<>();
	}

	/**
	 * Creates a machine starting with the supplied chips.
	 *
	 * @param machineDimensions
	 *            Size of the machine along the X and Y axes, in chips.
	 * @param chips
	 *            The chips in the machine.
	 * @param boot
	 *            The coordinates of the chip used to boot the machine.
	 * @throws IllegalArgumentException
	 *             On an attempt to add a chip with a duplicate location of a
	 *             chip already in the machine.
	 */
	public Machine(MachineDimensions machineDimensions, Iterable<Chip> chips,
			HasChipLocation boot) {
		this(machineDimensions, boot);
		addChips(chips);
	}

	/**
	 * Creates a machine from a (serializable) descriptor.
	 *
	 * @param bean
	 *            Descriptor holding the values to set.
	 */
	public Machine(MachineBean bean) {
		this(bean.getMachineDimensions(), bean.getRoot());
		for (var chipBean : bean.getChips()) {
			chipBean.addDefaults(bean);
			addChip(new Chip(chipBean, this));
		}
		addSpinnakerLinks();
		addFpgaLinks();
	}

	/**
	 * Provides a machine without abnormal chips or links.
	 * <p>
	 * This may be the original machine or it may be a shallow near-copy.
	 * <p>
	 * Once this method is run it is expected that the original {@link Machine}
	 * is no longer used. The original internal objects are reused whenever
	 * possible. Changes made to the original {@link Machine} may or may not
	 * effect the new {@link Machine}. This includes changes made to the chips,
	 * their routers or their processors.
	 * <p>
	 * For what makes up an abnormal link see {@link #findAbnormalLinks()}.
	 * <p>
	 * For what makes up an abnormal chip see {@link #findAbnormalChips()}.
	 *
	 * @return A {@code Machine} (possibly the original one) without the
	 *         abnormal bits.
	 */
	public Machine rebuild() {
		return rebuild(null, null);
	}

	/**
	 * Provides a machine without ignored chips or links.
	 * <p>
	 * This may be the original machine or it may be a shallow near copy.
	 * <p>
	 * Once this method is run it is expected that the original {@link Machine}
	 * is no longer used. The original internal objects are reused whenever
	 * possible. Changes made to the original {@link Machine} may or may not
	 * effect the new {@link Machine}. This includes changes made to the chips,
	 * their routers or their processors.
	 *
	 * @param ignoreChips
	 *            The locations of the chips (if any) that should not be in the
	 *            rebuilt machine. Chips not specified to be ignored are not
	 *            checked in any way. If this parameter is {@code null} the
	 *            result of {@link #findAbnormalLinks()} is used. If this
	 *            parameter is empty it is ignored as are any location that do
	 *            not represent a chip in the machine.
	 * @param ignoreLinks
	 *            A mapping of locations to directions of links that should not
	 *            be in the rebuilt machine. Links not specified to be ignored
	 *            are not checked in any way. If this parameter is {@code null}
	 *            the result of {@link #findAbnormalChips()} is used. If this
	 *            parameter is empty it is ignored as are any location that do
	 *            not represent a chip in the machine, or direction that do not
	 *            represent an existing link.
	 * @return A Machine (possibly the original one) without the ignore/abnormal
	 *         bits.
	 */
	public Machine rebuild(Set<ChipLocation> ignoreChips,
			Map<ChipLocation, EnumSet<Direction>> ignoreLinks) {
		if (ignoreChips == null) {
			ignoreChips = findAbnormalChips();
		}
		if (ignoreLinks == null) {
			ignoreLinks = findAbnormalLinks();
		}
		if (ignoreLinks.isEmpty() && ignoreChips.isEmpty()) {
			return this;
		}
		var log = getLogger(Link.class);
		var rebuilt = new Machine(machineDimensions, boot);
		for (var chip : this) {
			var location = chip.asChipLocation();
			if (ignoreChips.contains(location)) {
				log.info("Rebuilt machine without Chip {}", location);
			} else if (ignoreLinks.containsKey(location)) {
				var downDirections = ignoreLinks.get(location);
				var links = new ArrayList<Link>();
				for (var link : chip.router) {
					if (downDirections.contains(link.sourceLinkDirection)) {
						log.info("Rebuilt machine without Link {} {}",
								location, link.sourceLinkDirection);
					} else {
						links.add(link);
					}
				}
				rebuilt.addChip(new Chip(chip, new Router(chip.router, links)));
			} else {
				rebuilt.addChip(chip);
			}
		}
		// Check that the removals do not cause new abmoral chips.
		return rebuilt.rebuild();
	}

	/**
	 * Add a chip to the machine.
	 *
	 * @param chip
	 *            The chip to add to the machine.
	 * @throws IllegalArgumentException
	 *             On an attempt to add a chip with a duplicate location of a
	 *             chip already in the machine.
	 */
	public final void addChip(Chip chip) {
		var location = chip.asChipLocation();
		if (chips.containsKey(location)) {
			throw new IllegalArgumentException(
					"There is already a Chip at location: " + location);
		}

		if (chip.getX() >= machineDimensions.width) {
			throw new IllegalArgumentException("Chip x: " + chip.getX()
					+ " is too high for a machine with width "
					+ machineDimensions.width);
		}
		if (chip.getY() >= machineDimensions.height) {
			throw new IllegalArgumentException("Chip y: " + chip.getY()
					+ " is too high for a machine with height "
					+ machineDimensions.height + " " + chip);
		}

		chips.put(location, chip);
		if (chip.ipAddress != null) {
			ethernetConnectedChips.add(chip);
			if (boot.onSameChipAs(chip)) {
				bootEthernetAddress = chip.ipAddress;
			}
		}
		if (chip.nUserProcessors() > maxUserProssorsOnAChip) {
			maxUserProssorsOnAChip = chip.nUserProcessors();
		}
	}

	/**
	 * Add some chips to the machine.
	 *
	 * @param chips
	 *           The chips to add.
	 * @throws IllegalArgumentException
	 *             On an attempt to add a chip with a duplicate location of a
	 *             chip already in the machine.
	 */
	public final void addChips(Iterable<Chip> chips) {
		for (var chip : chips) {
			addChip(chip);
		}
	}

	/**
	 * Get the chips in the machine.
	 * The chips will be returned in the natural order of their
	 * {@link ChipLocation}.
	 *
	 * @return An unmodifiable ordered collection of the chips.
	 */
	public final Collection<Chip> chips() {
		return unmodifiableCollection(chips.values());
	}

	/**
	 * Get an iterator over the chips in this machine.
	 * The chips will be returned in the natural order of their
	 * {@link ChipLocation}.
	 *
	 * @return An iterator over the chips in this machine.
	 */
	@Override
	public final Iterator<Chip> iterator() {
		return chips.values().iterator();
	}

	/**
	 * The number of chips on this machine.
	 *
	 * @return The number of chips on this machine.
	 */
	public final int nChips() {
		return chips.size();
	}

	/**
	 * A set of all the locations of the chips in this machine.
	 * This set is guaranteed to iterate in the natural order of the locations.
	 *
	 * @return The (ordered) set of the locations of each chip in the machine.
	 */
	public final Set<ChipLocation> chipCoordinates() {
		return unmodifiableSet(chips.keySet());
	}

	/**
	 * An unmodifiable view over the map from locations to chips.
	 * This map is sorted by the natural order of the locations.
	 *
	 * @return An unmodifiable view over the map from locations to chips.
	 */
	public final SortedMap<ChipLocation, Chip> chipsMap() {
		return unmodifiableSortedMap(chips);
	}

	/**
	 * Get the chip at a specific <em>(x, y)</em> location.
	 * Will return {@code null} if {@link #hasChipAt(ChipLocation) hasChipAt}
	 * for the same location returns {@code false}.
	 *
	 * @param location
	 *            coordinates of the requested chip.
	 * @return A chip, or {@code null} if no chip found at that location.
	 */
	public final Chip getChipAt(ChipLocation location) {
		return chips.get(location);
	}

	/**
	 * Get the chip at a specific <em>(x, y)</em> location.
	 * Will return {@code null} if {@link #hasChipAt(ChipLocation) hasChipAt}
	 * for the same location returns {@code false}.
	 *
	 * @param location
	 *            coordinates of the requested chip.
	 * @return A chip, or {@code null} if no chip found at that location.
	 */
	public final Chip getChipAt(HasChipLocation location) {
		return chips.get(location.asChipLocation());
	}

	/**
	 * Get the chip at a specific <em>(x, y)</em> location.
	 * <p>
	 * Will return {@code null} if {@link #hasChipAt(ChipLocation) hasChipAt}
	 * for the same location returns {@code false}.
	 *
	 * @param x
	 *            The X-coordinate of the requested chip
	 * @param y
	 *            The Y-coordinate of the requested chip
	 * @return A chip, or {@code null} if no Chip found at that location.
	 * @throws IllegalArgumentException
	 *             Thrown is either <em>x</em> or <em>y</em> is negative or too
	 *             big (see
	 *             {@link MachineDefaults#validateChipLocation(int, int)}).
	 */
	public final Chip getChipAt(int x, int y) {
		var location = new ChipLocation(x, y);
		return chips.get(location);
	}

	/**
	 * Determine if a chip exists at the given coordinates.
	 *
	 * @param location
	 *            coordinates of the requested chip.
	 * @return True if and only if the machine has a chip at that location.
	 */
	public final boolean hasChipAt(ChipLocation location) {
		if (location == null) {
			return false;
		}
		return chips.containsKey(location);
	}

	/**
	 * Determine if a chip exists at the given coordinates.
	 *
	 * @param location
	 *            coordinates of the requested chip.
	 * @return True if and only if the machine has a chip at that location.
	 */
	public final boolean hasChipAt(HasChipLocation location) {
		if (location == null) {
			return false;
		}
		return chips.containsKey(location.asChipLocation());
	}

	/**
	 * Determine if a chip exists at the given coordinates.
	 *
	 * @param x
	 *            The X-coordinate of the requested chip
	 * @param y
	 *            The Y-coordinate of the requested chip
	 * @return True if and only if the machine has a Chip at that location.
	 * @throws IllegalArgumentException
	 *             Thrown is either <em>x</em> or <em>y</em> is negative or too
	 *             big (see
	 *             {@link MachineDefaults#validateChipLocation(int, int)}).
	 */
	public final boolean hasChipAt(int x, int y) {
		var location = new ChipLocation(x, y);
		return chips.containsKey(location);
	}

	/**
	 * Determine if a link exists at the given coordinates.
	 *
	 * @param source
	 *            The coordinates of the source of the link.
	 * @param link
	 *            The direction of the link.
	 * @return True if and only if the Machine/Chip has a link as specified.
	 */
	public final boolean hasLinkAt(ChipLocation source, Direction link) {
		var chip = chips.get(source);
		if (chip == null) {
			return false;
		}
		return chip.router.hasLink(link);
	}

	/**
	 * Get the coordinates of a possible chip over the given link.
	 * This method will take wrap-arounds into account if appropriate.
	 * <p>
	 * This method intentionally <em>does not</em> check if a chip at the
	 * resulting location already exists.
	 *
	 * @param source
	 *            The coordinates of the source of the link.
	 * @param direction
	 *            The direction of the link to traverse
	 * @return Location of a possible chip that would be connected by this link.
	 */
	public final ChipLocation getLocationOverLink(HasChipLocation source,
			Direction direction) {
		return normalizedLocation(source.getX() + direction.xChange,
				source.getY() + direction.yChange);
	}

	/**
	 * Get the existing chip over the given link.
	 * This method returns the destination chip <em>without</em> checking if the
	 * physical link is active.
	 * <p>
	 * This method is just a combination of
	 * {@link #getLocationOverLink(HasChipLocation, Direction)} and
	 * {@link #getChipAt(ChipLocation)}. It therefore takes wrap-around into
	 * account and does check for the existence of the destination chip.
	 *
	 * @param source
	 *            The coordinates of the source of the link.
	 * @param direction
	 *            The direction of the link to traverse
	 * @return The destination chip connected by this (possible) link. or
	 *         {@code null} if it does not exist.
	 */
	public final Chip getChipOverLink(HasChipLocation source,
			Direction direction) {
		return getChipAt(getLocationOverLink(source, direction));
	}

	/**
	 * The maximum possible X-coordinate of any chip in the board.
	 * <p>
	 * Currently no check is carried out to guarantee there is actually a Chip
	 * with this X value.
	 *
	 * @return The maximum possible X-coordinate.
	 */
	public final int maxChipX() {
		return machineDimensions.width - 1;
	}

	/**
	 * The maximum possible Y-coordinate of any chip in the board.
	 * <p>
	 * Currently no check is carried out to guarantee there is actually a Chip
	 * with this Y value.
	 *
	 * @return The maximum possible Y-coordinate.
	 */
	public final int maxChipY() {
		return machineDimensions.height - 1;
	}

	/**
	 * The chips in the machine that have control over an Ethernet connection.
	 * These are defined as the chip that have a non-{@code null} INET address.
	 * There is no guarantee regarding the order of the Chips.
	 * <p>
	 * While these are typically the bottom-left chip of each board, this is not
	 * guaranteed.
	 *
	 * @return An unmodifiable list of the chips with an INET address.
	 */
	public List<Chip> ethernetConnectedChips() {
		return unmodifiableList(ethernetConnectedChips);
	}

	/**
	 * Collection of the SpiNNaker links on this machine.
	 *
	 * @return An unmodifiable unordered collection of all the SpiNNaker links
	 *         on this machine.
	 */
	public final Collection<SpinnakerLinkData> spinnakerLinks() {
		return unmodifiableCollection(spinnakerLinks.values());
	}

	/**
	 * Get a SpiNNaker link with a given ID.
	 *
	 * @param id
	 *            The ID of the link
	 * @param address
	 *            The board address that this SpiNNaker link is associated with.
	 *            If {@code null} the boot INET address will be used.
	 * @return The associated SpiNNaker link information, or {@code null} if not
	 *         found.
	 */
	public final SpinnakerLinkData getSpinnakerLink(int id,
			InetAddress address) {
		InetIdTuple key;
		if (address == null) {
			key = new InetIdTuple(bootEthernetAddress, id);
		} else {
			key = new InetIdTuple(address, id);
		}
		return spinnakerLinks.get(key);
	}

	/**
	 * Get a SpiNNaker link with a given ID on the boot chip.
	 *
	 * @param id
	 *            The ID of the link
	 * @return The associated SpiNNaker link information, or {@code null} if not
	 *         found.
	 */
	public final SpinnakerLinkData getBootSpinnakerLink(int id) {
		var key = new InetIdTuple(bootEthernetAddress, id);
		return spinnakerLinks.get(key);
	}

	/**
	 * Add SpiNNaker links that are on a given machine depending on the height
	 * and width and therefore version of the board.
	 * <p>
	 * If a link already exists the original link is retain and that SpiNNaker
	 * link is not added.
	 */
	public final void addSpinnakerLinks() {
		if (version.isFourChip) {
			var chip00 = getChipAt(new ChipLocation(0, 0));
			if (!chip00.router.hasLink(Direction.WEST)) {
				spinnakerLinks.put(new InetIdTuple(chip00.ipAddress, 0),
						new SpinnakerLinkData(0, chip00, Direction.WEST,
								chip00.ipAddress));
			}
			var chip10 = getChipAt(new ChipLocation(1, 0));
			if (!chip10.router.hasLink(Direction.EAST)) {
				// As in Python, the Ethernet address of chip 0 0 is used.
				spinnakerLinks.put(new InetIdTuple(chip00.ipAddress, 1),
						new SpinnakerLinkData(1, chip10, Direction.WEST,
								chip00.ipAddress));
			}
		} else {
			for (var chip : ethernetConnectedChips) {
				if (!chip.router.hasLink(Direction.SOUTHWEST)) {
					spinnakerLinks.put(new InetIdTuple(chip.ipAddress, 0),
							new SpinnakerLinkData(0, chip, Direction.SOUTHWEST,
									chip.ipAddress));
				}
			}
		}
	}

	/**
	 * Converts <em>x</em> and <em>y</em> to a chip location, if required (and
	 * applicable) adjusting for wrap around.
	 * <p>
	 * The only check that the coordinates are valid is to check they are
	 * greater than zero. Otherwise {@code null} is returned.
	 *
	 * @param x
	 *            X coordinate
	 * @param y
	 *            Y coordinate
	 * @return A location based on <em>x</em> and <em>y</em> with possible
	 *         wrap around, or {@code null} if either coordinate is less than
	 *         zero or greater than the dimensions of the machine.
	 */
	public final ChipLocation normalizedLocation(int x, int y) {
		if (version.horizontalWrap) {
			x = (x + machineDimensions.width) % machineDimensions.width;
		} else if (x < 0 || x >= machineDimensions.width) {
			return null;
		}
		if (version.verticalWrap) {
			y = (y + machineDimensions.height) % machineDimensions.height;
		} else if (y < 0 || y >= machineDimensions.height) {
			return null;
		}
		if (x < 0 || y < 0) {
			return null;
		}
		return new ChipLocation(x, y);
	}

	/**
	 * Returns the location you would get to from the source if you move in this
	 * direction, if required (and applicable) adjusting for wrap-around.
	 * <p>
	 * No check is done to see if there is actually a chip at that location.
	 *
	 * @param source
	 *            the original coordinates.
	 * @param direction
	 *            which way to move.
	 * @return A location based on a move in this direction with possible
	 *         wrap around, or {@code null} if either coordinate is less than
	 *         zero or greater than the dimensions of the machine.
	 */
	public final ChipLocation normalizedMove(HasChipLocation source,
			Direction direction) {
		return normalizedLocation(source.getX() + direction.xChange,
				source.getY() + direction.yChange);
	}

	/**
	 * Returns this location adjusted for wrap-arounds.
	 * <p>
	 * No check is done to see if there is actually a chip at that location.
	 *
	 * @param location
	 *            A location which may need to be corrected for wrap-arounds.
	 * @return A location based on a move in this direction with possible
	 *         wrap around, or {@code null} if either coordinate is less than
	 *         zero or greater than the dimensions of the machine.
	 */
	public final ChipLocation normalizedLocation(HasChipLocation location) {
		return normalizedLocation(location.getX(), location.getY());
	}

	/**
	 * Get an FPGA link data item that corresponds to the FPGA and FPGA link for
	 * a given board address.
	 *
	 * @param fpgaId
	 *            The ID of the FPGA that the data is going through.
	 * @param fpgaLinkId
	 *            The link ID of the FPGA.
	 * @param address
	 *            The board address that this FPGA link is associated with.
	 * @return FPGA link data, or {@code null} if no such link has been added.
	 */
	public final FPGALinkData getFpgaLink(FpgaId fpgaId, int fpgaLinkId,
			InetAddress address) {
		var byAddress = fpgaLinks.get(address);
		if (byAddress == null) {
			return null;
		}
		var byId = byAddress.get(fpgaId);
		if (byId == null) {
			return null;
		}
		return byId.get(fpgaLinkId);
	}

	/**
	 * The added FPGA link data items. The iterable may be empty.
	 * <p>
	 * No guarantee of order is provided.
	 *
	 * @return All added FPGA link data items.
	 */
	public final MappableIterable<FPGALinkData> getFpgaLinks() {
		return new TripleMapIterable<>(fpgaLinks);
	}

	/**
	 * The added FPGA link data items for the given address. The
	 * iterable may be empty.
	 * <p>
	 * No guarantee of order is provided.
	 *
	 * @param address
	 *            The board address that this FPGA link is associated with.
	 * @return All added FPGA link data items for this address.
	 */
	public final MappableIterable<FPGALinkData>
			getFpgaLinks(InetAddress address) {
		var byAddress = fpgaLinks.get(address);
		if (byAddress == null) {
			return Collections::emptyIterator;
		}
		return new DoubleMapIterable<>(byAddress);
	}

	private void addFpgaLinks(int rootX, int rootY, InetAddress address) {
		for (var fpgaEnum : FpgaEnum.values()) {
			var location = normalizedLocation(rootX + fpgaEnum.getX(),
					rootY + fpgaEnum.getY());
			if (hasChipAt(location)
					&& !hasLinkAt(location, fpgaEnum.direction)) {
				fpgaLinks.computeIfAbsent(address, __ -> new HashMap<>())
						.computeIfAbsent(fpgaEnum.fpgaId, __ -> new HashMap<>())
						.put(fpgaEnum.id,
								new FPGALinkData(fpgaEnum.id, fpgaEnum.fpgaId,
										location, fpgaEnum.direction, address));
			}
		}
	}

	/**
	 * Add FPGA links that are on a given machine depending on the version of
	 * the board.
	 * <p>
	 * <strong>Note:</strong> This implementation assumes the Ethernet-enabled
	 * chip is the 0, 0 chip on each board.
	 */
	public final void addFpgaLinks() {
		if (version.isFourChip) {
			return; // NO fpga links
		}
		for (var ethernetConnectedChip : ethernetConnectedChips) {
			addFpgaLinks(ethernetConnectedChip.getX(),
					ethernetConnectedChip.getY(),
					ethernetConnectedChip.ipAddress);
		}
	}

	/**
	 * Get a string detailing the number of cores and links.
	 * <p>
	 * <strong>Warning:</strong> the current implementation makes the
	 * simplification assumption that every link exists in both directions.
	 *
	 * @return A quick description of the machine.
	 */
	public final String coresAndLinkOutputString() {
		int cores = 0;
		int everyLink = 0;
		for (var chip : chips.values()) {
			cores += chip.nProcessors();
			everyLink += chip.router.size();
		}
		return cores + " cores and " + (everyLink / 2.0) + " links";
	}

	/**
	 * The coordinates of the chip used to boot the machine. This is typically
	 * the Chip at {@linkplain ChipLocation#ZERO_ZERO <em>(0, 0)</em>}, and will
	 * typically have an associated {@link InetAddress}, but there is no
	 * guarantee of either of these facts.
	 * <p>
	 * If no chip has been added to the machine at the boot, location this
	 * method returns {@code null}.
	 *
	 * @return The chip at the location specified as boot, or {@code null}.
	 */
	public final Chip bootChip() {
		return chips.get(boot);
	}

	/**
	 * The chips on the same board as the given chip.
	 *
	 * @param chip
	 *            The exemplar chip from the board of interest
	 * @return Iterable of chips on the board. Not expected to be empty; the
	 *         exemplar should be present.
	 */
	public final MappableIterable<Chip> iterChipsOnBoard(Chip chip) {
		return () -> new ChipOnBoardIterator(chip.nearestEthernet);
	}

	/**
	 * The maximum number of user cores on any chip.
	 * <p>
	 * A user core is defined as one that has not been reserved as a monitor.
	 *
	 * @return Maximum for at at least one core.
	 */
	public final int maximumUserCoresOnChip() {
		return maxUserProssorsOnAChip;
	}

	/**
	 * The total number of cores on the machine which are not monitor cores.
	 *
	 * @return The number of user cores over all chips.
	 */
	public final int totalAvailableUserCores() {
		int count = 0;
		for (var chip : chips.values()) {
			count += chip.nUserProcessors();
		}
		return count;
	}

	/**
	 * The total number of cores on the machine including monitor cores.
	 *
	 * @return The number of cores over all Chips.
	 */
	public final int totalCores() {
		int count = 0;
		for (var chip : chips.values()) {
			count += chip.nProcessors();
		}
		return count;
	}

	/**
	 * Check if the inverse link to the one described exists. This check is in
	 * two stages.
	 * <ol>
	 * <li>Check that there is actually a second chip in the given direction
	 * from the input chip. There need not be an actual working link.
	 * <li>Check that the second chip does have a working link back. (Inverse
	 * direction)
	 * </ol>
	 *
	 * @param chip
	 *            Starting Chip which will be the Target of the actual link
	 *            checked.
	 * @param direction
	 *            Original direction. This is the inverse of the direction in
	 *            the link actually checked.
	 * @return True if and only if there is a active inverse link
	 */
	public boolean hasInverseLinkAt(ChipLocation chip, Direction direction) {
		var source = getChipOverLink(chip, direction);
		if (source == null) {
			return false;
		}
		return source.router.hasLink(direction.inverse());
	}

	@Override
	public String toString() {
		return "[Machine: max_x=" + maxChipX() + ", max_y=" + maxChipY()
				+ ", n_chips=" + nChips() + "]";
	}

	/**
	 * Returns a list of the abnormal links that are recommended for removal.
	 * <p>
	 * The current implementation identifies the Links where there is no
	 * matching reverse link as abnormal. This includes case where the whole
	 * destination chip is missing.
	 * <p>
	 * Future implementations may add other tests for abnormal Chips.
	 *
	 * @return A map of locations to sets of directions (hopefully empty) which
	 *         identify links to remove
	 */
	public Map<ChipLocation, EnumSet<Direction>> findAbnormalLinks() {
		var abnormalLinks = new HashMap<ChipLocation, EnumSet<Direction>>();
		for (var chip : chips.values()) {
			for (var link : chip.router) {
				/*
				 * If a link has both directions existing according to standard
				 * rules, it's considered to be normal. Everything else is
				 * abnormal.
				 */
				if (hasChipAt(link.destination)
						&& getChipAt(link.destination).router
								.hasLink(link.sourceLinkDirection.inverse())) {
					continue;
				}
				abnormalLinks
						.computeIfAbsent(link.source,
								__ -> noneOf(Direction.class))
						.add(link.sourceLinkDirection);
			}
		}
		return unmodifiableMap(abnormalLinks);
	}

	/**
	 * Returns a list of the abnormal chips that are recommended for removal.
	 * <p>
	 * The current implementation identifies chips with no outgoing links as
	 * abnormal.
	 * Future implementations may add other tests for abnormal chips.
	 *
	 * @return A set (hopefully empty) of locations of chips to remove.
	 */
	public Set<ChipLocation> findAbnormalChips() {
		return chips.values().stream().filter(chip -> chip.router.size() == 0)
				.map(Chip::asChipLocation)
				.collect(toUnmodifiableSet());
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
		return (obj instanceof Machine) && isNull(difference((Machine) obj));
	}

	/**
	 * Describes one difference found between this machine and another machine.
	 * <p>
	 * This method will always return {@code null} if no difference is found
	 * between the two machines. So semantically is the same as equals except
	 * that this works if other is a superclass of machine in which case only
	 * the shared variables are compared.
	 * <p>
	 * This method returns as soon as it has found a difference so there may be
	 * other not specified differences.
	 * <p>
	 * <strong>Warning:</strong> This method could change over time, so there is
	 * no implied guarantee to the order that variables are checked or to the
	 * message that is returned.
	 * The only guarantee is that {@code null} is returned if no difference is
	 * detected.
	 *
	 * @param other
	 *            Another machine to check if it has the same variables.
	 * @return {@code null} if no difference is detected, otherwise a string.
	 */
	public String difference(Machine other) {
		if (!machineDimensions.equals(other.machineDimensions)) {
			return "machineDimensions " + machineDimensions + " != "
					+ other.machineDimensions;
		}
		if (!chips.keySet().equals(other.chips.keySet())) {
			return chipLocationDifference(other);
		}
		if (!version.equals(other.version)) {
			return "version " + version + " != " + version;
		}
		if (maxUserProssorsOnAChip != other.maxUserProssorsOnAChip) {
			return "maxUserProcessorsOnAChip " + maxUserProssorsOnAChip + " != "
					+ other.maxUserProssorsOnAChip;
		}
		if (!boot.equals(other.boot)) {
			return "boot " + boot + " != " + other.boot;
		}
		if (!bootEthernetAddress.equals(other.bootEthernetAddress)) {
			return "bootEthernetAddress " + bootEthernetAddress + " != "
					+ other.bootEthernetAddress;
		}
		for (var loc : chips.keySet()) {
			var c1 = chips.get(loc);
			var c2 = other.chips.get(loc);
			if (c1.equals(c2)) {
				return null;
			}
			var diff = c1.difference(c2);
			if (!"userProcessors".equals(diff)) {
				return c1 + " != " + c2 + "(diff = " + diff + ")";
			}
			if (c1.userProcessors().size() == c2.userProcessors().size() + 1) {
				return null;
			}
			if (c1.userProcessors().size() == c2.userProcessors().size() - 1) {
				return null;
			}
			return c1 + " != " + c2 + "(diff = userProcessors)";
		}
		if (!ethernetConnectedChips.equals(other.ethernetConnectedChips)) {
			return "ethernetConnectedChips " + ethernetConnectedChips + " != "
					+ other.ethernetConnectedChips;
		}
		if (!spinnakerLinks.equals(other.spinnakerLinks)) {
			return " spinnakerLinks " + spinnakerLinks + " != "
					+ other.spinnakerLinks;
		}
		if (!fpgaLinks.equals(other.fpgaLinks)) {
			return "fpgaLinks " + fpgaLinks + " != " + fpgaLinks;
		}
		return null;
	}

	/**
	 * Describes the difference between chip location between two machines.
	 * <p>
	 * This method is expected to only be called then there is a detected or
	 * expected difference between the two chip locations so will always return
	 * a message
	 * <p>
	 * <strong>Warning:</strong> As this method is mainly a support method for
	 * {@link #difference(Machine)}, the returned result can be changed at any
	 * time.
	 *
	 * @param that
	 *            Another machine with suspected difference in the location of
	 *            chips.
	 * @return Some useful human readable information.
	 */
	public String chipLocationDifference(Machine that) {
		var setThis = chips.keySet();
		var setThat = that.chips.keySet();
		if (setThis.size() < setThat.size()) {
			var temp = new HashSet<>(setThat);
			temp.removeAll(setThis);
			return "other has extra Chips at " + temp;
		} else if (setThis.size() > setThat.size()) {
			var temp = new HashSet<>(setThis);
			temp.removeAll(setThat);
			return "other has missing Chips at " + temp;
		} else {
			var temp1 = new HashSet<>(setThis);
			temp1.removeAll(setThat);
			if (temp1.isEmpty()) {
				return "No difference between chip key sets found.";
			}
			var temp2 = new HashSet<>(setThat);
			temp2.removeAll(setThis);
			return "other has missing Chips at " + temp1 + "and extra Chips at "
					+ temp2;
		}
	}

	/**
	 * Obtains the Boot Ethernet IP Address.
	 *
	 * @return The IPv4 Address of the boot chip (typically 0, 0)
	 */
	public InetAddress getBootEthernetAddress() {
		return bootEthernetAddress;
	}

	private class ChipOnBoardIterator implements Iterator<Chip> {
		private HasChipLocation root;

		private Chip nextChip;

		private Iterator<ChipLocation> singleBoardIterator;

		ChipOnBoardIterator(HasChipLocation root) {
			this.root = root;
			var geometry = getSpinn5Geometry();
			singleBoardIterator = geometry.singleBoardIterator();
			prepareNextChip();
		}

		@Override
		public boolean hasNext() {
			return nextChip != null;
		}

		@Override
		public Chip next() {
			if (nextChip == null) {
				throw new NoSuchElementException("No more chips available.");
			}
			var result = nextChip;
			prepareNextChip();
			return result;
		}

		private void prepareNextChip() {
			while (singleBoardIterator.hasNext()) {
				var local = singleBoardIterator.next();
				var global = normalizedLocation(
						root.getX() + local.getX(), root.getY() + local.getY());
				nextChip = getChipAt(global);
				if (nextChip != null) {
					return;
				}
			}
			nextChip = null;
		}
	}
}
