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

import static java.util.Collections.emptyList;
import static org.slf4j.LoggerFactory.getLogger;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import org.slf4j.Logger;

import uk.ac.manchester.spinnaker.machine.bean.ChipBean;
import uk.ac.manchester.spinnaker.machine.bean.MachineBean;
import uk.ac.manchester.spinnaker.machine.datalinks.FPGALinkData;
import uk.ac.manchester.spinnaker.machine.datalinks.FpgaEnum;
import uk.ac.manchester.spinnaker.machine.datalinks.FpgaId;
import uk.ac.manchester.spinnaker.machine.datalinks.InetIdTuple;
import uk.ac.manchester.spinnaker.machine.datalinks.SpinnakerLinkData;
import uk.ac.manchester.spinnaker.utils.DefaultMap;
import uk.ac.manchester.spinnaker.utils.DoubleMapIterable;
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
public class Machine implements Iterable<Chip> {
	private static final Logger log = getLogger(Link.class);

	/** Size of the machine along the x and y axes in Chips. */
	public final MachineDimensions machineDimensions;

	// This is not final as will change as processors become monitors.
	private int maxUserProssorsOnAChip;

	private final ArrayList<Chip> ethernetConnectedChips;

	// This may change to a map of maps
	private final HashMap<InetIdTuple, SpinnakerLinkData> spinnakerLinks;

	/** Map of map of map implementation done to allow access to submaps. */
	// If never required this could be changed to single map with tuple key.
	private final HashMap<InetAddress,
			Map<FpgaId, Map<Integer, FPGALinkData>>> fpgaLinks;

	/** The coordinates of the chip used to boot the machine. */
	public final ChipLocation boot;

	// Not final as currently could come from a chip added later.
	private InetAddress bootEthernetAddress;

	private final TreeMap<ChipLocation, Chip> chips;
	// private final Chip[][] chipArray;

	/** The version of the Machine based on its height and Width. */
	public final MachineVersion version;

	/**
	 * Creates an empty machine.
	 *
	 * @param machineDimensions
	 *            Size of the machine along the x and y axes, in Chips.
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

		this.chips = new TreeMap<>();
	}

	/**
	 * Creates a machine starting with the supplied chips.
	 *
	 * @param machineDimensions
	 *            Size of the machine along the x and y axes, in Chips.
	 * @param chips
	 *            An iterable of chips in the machine.
	 * @param boot
	 *            The coordinates of the chip used to boot the machine.
	 * @throws IllegalArgumentException
	 *             On an attempt to add a second Chip with the same location.
	 */
	public Machine(MachineDimensions machineDimensions, Iterable<Chip> chips,
			HasChipLocation boot) {
		this(machineDimensions, boot);
		addChips(chips);
	}

	/**
	 * Creates a Machine from a Bean.
	 *
	 * @param bean
	 *            Bean holding the Values to set.
	 */
	public Machine(MachineBean bean) {
		this(bean.getMachineDimensions(), bean.getRoot());
		for (ChipBean chipBean : bean.getChips()) {
			chipBean.addDefaults(bean);
			addChip(new Chip(chipBean, this));
		}
		addSpinnakerLinks();
		addFpgaLinks();
	}

	/**
	 * Provides a machine without abnormal chips or links.
	 * <p>
	 * This may be the original machine or it may be a shallow near copy.
	 * <p>
	 * Once this method is run it is expected that the original Machine is no
	 * longer used. The original internal objects are reused whenever possible.
	 * Changes made to the original Machine may or may not effect the new
	 * Machine. This includes changes made to the chips, their routers or their
	 * processors.
	 * <p>
	 * For what makes up an abnormal link see {@link #findAbnormalLinks()}.
	 * <p>
	 * For what makes up an abnormal chip see {@link #findAbnormalChips()}.
	 *
	 * @return A Machine (possibly the original one) without the abnormal bits.
	 */
	public Machine rebuild() {
		return rebuild(null, null);
	}

	/**
	 * Provides a machine without ignored chips or links.
	 * <p>
	 * This may be the original machine or it may be a shallow near copy.
	 * <p>
	 * Once this method is run it is expected that the original Machine is no
	 * longer used. The original internal objects are reused whenever possible.
	 * Changes made to the original Machine may or may not effect the new
	 * Machine. This includes changes made to the chips, their routers or their
	 * processors.
	 *
	 * @param ignoreChips
	 *            The locations of the chips (if any) that should not be in the
	 *            rebuilt machine. Chips not specified to be ignore or not
	 *            checked in any way. If this parameter is {@code null} the
	 *            result of {@link #findAbnormalLinks()} is used. If this
	 *            parameter is empty it is ignored as are any location that do
	 *            not represent a chip in the machine.
	 * @param ignoreLinks
	 *            A mapping of Locations to Directions of links that should be
	 *            in the rebuilt machine. Links not specified to be ignored are
	 *            not checked in any way. If this parameter is {@code null} the
	 *            result of {@link #findAbnormalChips()} is used. If this
	 *            parameter is empty it is ignored as are any location that do
	 *            not represent a chip in the machine, or direction that do not
	 *            represent an existing link.
	 * @return A Machine (possibly the original one) without the ignore/abnormal
	 *         bits.
	 */
	public Machine rebuild(Set<ChipLocation> ignoreChips,
			Map<ChipLocation, Set<Direction>> ignoreLinks) {
		if (ignoreChips == null) {
			ignoreChips = this.findAbnormalChips();
		}
		if (ignoreLinks == null) {
			ignoreLinks = this.findAbnormalLinks();
		}
		if (ignoreLinks.isEmpty() && ignoreChips.isEmpty()) {
			return this;
		}
		Machine rebuilt = new Machine(this.machineDimensions, this.boot);
		for (Chip chip : this) {
			ChipLocation location = chip.asChipLocation();
			if (ignoreChips.contains(location)) {
				log.info("Rebuilt machine without Chip " + location);
			} else if (ignoreLinks.containsKey(location)) {
				Collection<Direction> downDirections =
						ignoreLinks.get(location);
				ArrayList<Link> links = new ArrayList<>();
				for (Link link : chip.router) {
					if (downDirections.contains(link.sourceLinkDirection)) {
						log.info("Rebuilt machine without Link " + location
								+ " " + link.sourceLinkDirection);
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
	 *             On an attempt to add a second Chip with the same location.
	 */
	public final void addChip(Chip chip) {
		ChipLocation location = chip.asChipLocation();
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
	 *            an iterable of chips.
	 */
	public final void addChips(Iterable<Chip> chips) {
		for (Chip chip : chips) {
			addChip(chip);
		}
	}

	/**
	 * The chips in the machine.
	 * <p>
	 * The Chips will be returned in the natural order of their ChipLocation.
	 *
	 * @return An Unmodifiable Ordered Collection of the chips.
	 */
	public final Collection<Chip> chips() {
		return Collections.unmodifiableCollection(this.chips.values());
	}

	/**
	 * The locations of each chip in the machine.
	 *
	 * @return An unmodifiable
	 */
	public final Set<ChipLocation> chipLocations() {
		return Collections.unmodifiableSet(this.chips.keySet());
	}

	@Override
	/**
	 * Returns an iterator over the Chips in this Machine.
	 * <p>
	 * The Chips will be returned in the natural order of their ChipLocation.
	 *
	 * @return An iterator over the Chips in this Machine.
	 */
	public final Iterator<Chip> iterator() {
		return this.chips.values().iterator();
	}

	/**
	 * The number of Chips on this Machine.
	 *
	 * @return The number of Chips on this Machine.
	 */
	public final int nChips() {
		return chips.size();
	}

	/**
	 * A Set of all the Locations of the Chips.
	 * <p>
	 * This set is guaranteed to iterate in the natural order of the locations.
	 *
	 * @return (ordered) set of the locations of each chip in the Machine.
	 */
	public final Set<ChipLocation> chipCoordinates() {
		return Collections.unmodifiableSet(this.chips.keySet());
	}

	/**
	 * An unmodifiable view over the map from ChipLocations to Chips.
	 * <p>
	 * This map is sorted by the natural order of the locations.
	 *
	 * @return An unmodifiable view over the map from ChipLocations to Chips.
	 */
	public final SortedMap<ChipLocation, Chip> chipsMap() {
		return Collections.unmodifiableSortedMap(chips);
	}

	/**
	 * Get the chip at a specific (x, y) location.
	 * <p>
	 * Will return {@code null} if {@link #hasChipAt(ChipLocation) hasChipAt}
	 * for the same location returns {@code false}.
	 *
	 * @param location
	 *            coordinates of the requested chip.
	 * @return A Chip or {@code null} if no Chip found at that location.
	 */
	public final Chip getChipAt(ChipLocation location) {
		return chips.get(location);
	}

	/**
	 * Get the chip at a specific (x, y) location.
	 * <p>
	 * Will return {@code null} if {@link #hasChipAt(ChipLocation) hasChipAt}
	 * for the same location returns {@code false}.
	 *
	 * @param location
	 *            coordinates of the requested chip.
	 * @return A Chip or {@code null} if no Chip found at that location.
	 */
	public final Chip getChipAt(HasChipLocation location) {
		return chips.get(location.asChipLocation());
	}

	/**
	 * Get the chip at a specific (x, y) location.
	 * <p>
	 * Will return {@code null} if {@link #hasChipAt(ChipLocation) hasChipAt}
	 * for the same location returns {@code false}.
	 *
	 * @param x
	 *            The x-coordinate of the requested chip
	 * @param y
	 *            The y-coordinate of the requested chip
	 * @return A Chip or {@code null} if no Chip found at that location.
	 * @throws IllegalArgumentException
	 *             Thrown is either x or y is negative or too big.
	 */
	public final Chip getChipAt(int x, int y) {
		ChipLocation location = new ChipLocation(x, y);
		return chips.get(location);
	}

	/**
	 * Determine if a chip exists at the given coordinates.
	 *
	 * @param location
	 *            coordinates of the requested chip.
	 * @return True if and only if the machine has a Chip at that location.
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
	 * @return True if and only if the machine has a Chip at that location.
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
	 *            The x-coordinate of the requested chip
	 * @param y
	 *            The y-coordinate of the requested chip
	 * @return True if and only if the machine has a Chip at that location.
	 * @throws IllegalArgumentException
	 *             Thrown is either x or y is negative or too big.
	 */
	public final boolean hasChipAt(int x, int y) {
		ChipLocation location = new ChipLocation(x, y);
		return chips.containsKey(location);
	}
	// public Chip getChipAt(int x, int y) {
	// return this.chipArray[x][y];
	// }

	// public boolean hasChipAt(int x, int y) {
	// return this.chipArray[x][y] != null;
	// }

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
		Chip chip = chips.get(source);
		if (chip == null) {
			return false;
		}
		return chip.router.hasLink(link);
	}

	/**
	 * Get the coordinates of a possible chip over the given link.
	 * <p>
	 * This method will take wrap-arounds into account if appropriate.
	 * <p>
	 * This method intentionally <em>does not</em> check if a Chip at the
	 * resulting location already exists.
	 *
	 * @param source
	 *            The coordinates of the source of the link.
	 * @param direction
	 *            The Direction of the link to traverse
	 * @return Location of a possible chip that would be connected by this link.
	 */
	public final ChipLocation getLocationOverLink(HasChipLocation source,
			Direction direction) {
		return this.normalizedLocation(source.getX() + direction.xChange,
				source.getY() + direction.yChange);
	}

	/**
	 * Get the existing Chip over the given link.
	 * <p>
	 * This method is just a combination of getLocationOverLink and getChipAt.
	 * It therefore takes wrap-around into account and does check for the
	 * existence of the destination chip.
	 * <p>
	 * This method returns the destination chip WITHOUT checking if the physical
	 * link is active.
	 *
	 * @param source
	 *            The coordinates of the source of the link.
	 * @param direction
	 *            The Direction of the link to traverse
	 * @return The Destination Chip connected by this (possible) link. or
	 *         {@code null} if it does not exist.
	 */
	public final Chip getChipOverLink(HasChipLocation source,
			Direction direction) {
		return getChipAt(getLocationOverLink(source, direction));
	}

	/**
	 * The maximum possible x-coordinate of any chip in the board.
	 * <p>
	 * Currently no check is carried out to guarantee there is actually a Chip
	 * with this x value.
	 *
	 * @return The maximum possible x-coordinate.
	 */
	public final int maxChipX() {
		return machineDimensions.width - 1;
	}

	/**
	 * The maximum possible y-coordinate of any chip in the board.
	 * <p>
	 * Currently no check is carried out to guarantee there is actually a Chip
	 * with this y value.
	 *
	 * @return The maximum possible y-coordinate.
	 */
	public final int maxChipY() {
		return machineDimensions.height - 1;
	}

	/**
	 * The chips in the machine that have an Ethernet connection. These are
	 * defined as the Chip that have a non-{@code null} INET address.
	 * <p>
	 * While these are typically the bottom-left Chip of each board, this is not
	 * guaranteed.
	 * <p>
	 * There is no guarantee regarding the order of the Chips.
	 *
	 * @return An unmodifiable list of the Chips with an INET address.
	 */
	public List<Chip> ethernetConnectedChips() {
		return Collections.unmodifiableList(this.ethernetConnectedChips);
	}

	/**
	 * Collection of the spinnaker links on this machine.
	 *
	 * @return An unmodifiable unordered collection of all the spinnaker links
	 *         on this machine.
	 */
	public final Collection<SpinnakerLinkData> spinnakerLinks() {
		return Collections.unmodifiableCollection(spinnakerLinks.values());
	}

	/**
	 * Get a SpiNNaker link with a given ID.
	 *
	 * @param id
	 *            The ID of the link
	 * @param address
	 *            The board address that this SpiNNaker link is associated with.
	 *            If {@code null} the boot INET address will be used.
	 * @return The associated SpinnakerLink or {@code null} if not found.
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
	 * @return The associated SpinnakerLink or {@code null} if not found.
	 */
	public final SpinnakerLinkData getBootSpinnakerLink(int id) {
		InetIdTuple key = new InetIdTuple(bootEthernetAddress, id);
		return spinnakerLinks.get(key);
	}

	/**
	 * Add SpiNNaker links that are on a given machine depending on the height
	 * and width and therefore version of the board.
	 * <p>
	 * If a link already exists the original link is retain and that spinnaker
	 * link is not added.
	 */
	public final void addSpinnakerLinks() {
		if (version.isFourChip) {
			Chip chip00 = getChipAt(new ChipLocation(0, 0));
			if (!chip00.router.hasLink(Direction.WEST)) {
				spinnakerLinks.put(new InetIdTuple(chip00.ipAddress, 0),
						new SpinnakerLinkData(0, chip00, Direction.WEST,
								chip00.ipAddress));
			}
			Chip chip10 = getChipAt(new ChipLocation(1, 0));
			if (!chip10.router.hasLink(Direction.EAST)) {
				// As in Python the Ethernet adddress of chip 0 0 is used.
				spinnakerLinks.put(new InetIdTuple(chip00.ipAddress, 1),
						new SpinnakerLinkData(1, chip10, Direction.WEST,
								chip00.ipAddress));
			}
		} else {
			for (Chip chip : ethernetConnectedChips) {
				if (!chip.router.hasLink(Direction.SOUTHWEST)) {
					spinnakerLinks.put(new InetIdTuple(chip.ipAddress, 0),
							new SpinnakerLinkData(0, chip, Direction.SOUTHWEST,
									chip.ipAddress));
				}
			}
		}
	}

	/**
	 * Converts x and y to a chip location, if required (and applicable)
	 * adjusting for wrap around.
	 * <p>
	 * The only check that the coordinates are valid is to check they are
	 * greater than zero. Otherwise {@code null} is returned.
	 *
	 * @param x
	 *            X coordinate
	 * @param y
	 *            Y coordinate
	 * @return A ChipLocation based on X and Y with possible wrap around, or
	 *         {@code null} if either coordinate is less than zero or greater
	 *         than the dimensions of the machine.
	 */
	public final ChipLocation normalizedLocation(int x, int y) {
		if (version.horizontalWrap) {
			x = (x + machineDimensions.width) % machineDimensions.width;
		} else if (x < 0 || x >= this.machineDimensions.width) {
			return null;
		}
		if (version.verticalWrap) {
			y = (y + machineDimensions.height) % machineDimensions.height;
		} else if (y < 0 || y >= this.machineDimensions.height) {
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
	 *            the original x and y coordinates.
	 * @param direction
	 *            which way to move.
	 * @return A ChipLocation based on a move in this direction with possible
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
	 * @return A ChipLocation based on a move in this direction with possible
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
		Map<FpgaId, Map<Integer, FPGALinkData>> byAddress;
		byAddress = fpgaLinks.get(address);
		if (byAddress == null) {
			return null;
		}
		Map<Integer, FPGALinkData> byId = byAddress.get(fpgaId);
		if (byId == null) {
			return null;
		}
		return byId.get(fpgaLinkId);
	}

	/**
	 * An iterable over all the added FPGA link data items. The Iterable may be
	 * empty.
	 * <p>
	 * No Guarantee of order is provided.
	 *
	 * @return All added FPGA link data items.
	 */
	public final Iterable<FPGALinkData> getFpgaLinks() {
		return new TripleMapIterable<>(fpgaLinks);
	}

	/**
	 * An iterable over all the added FPGA link data items for this address. The
	 * Iterable may be empty.
	 * <p>
	 * No Guarantee of order is provided.
	 *
	 * @param address
	 *            The board address that this FPGA link is associated with.
	 * @return All added FPGA link data items for this address.
	 */
	public final Iterable<FPGALinkData> getFpgaLinks(InetAddress address) {
		Map<FpgaId, Map<Integer, FPGALinkData>> byAddress =
				fpgaLinks.get(address);
		if (byAddress == null) {
			return emptyList();
		}
		return new DoubleMapIterable<>(byAddress);
	}

	private void addFpgaLinks(int rootX, int rootY, InetAddress address) {
		for (FpgaEnum fpgaEnum : FpgaEnum.values()) {
			ChipLocation location = normalizedLocation(rootX + fpgaEnum.getX(),
					rootY + fpgaEnum.getY());
			if (hasChipAt(location)
					&& !hasLinkAt(location, fpgaEnum.direction)) {
				Map<Integer, FPGALinkData> byId = fpgaLinks
						.computeIfAbsent(address, k -> new HashMap<>())
						.computeIfAbsent(fpgaEnum.fpgaId, k -> new HashMap<>());
				byId.put(fpgaEnum.id,
						new FPGALinkData(fpgaEnum.id, fpgaEnum.fpgaId, location,
								fpgaEnum.direction, address));
			}
		}
	}

	/**
	 * Add FPGA links that are on a given machine depending on the version of
	 * the board.
	 * <p>
	 * Note: This implementation assumes the Ethernet Chip is the 0, 0 chip on
	 * each board
	 */
	public final void addFpgaLinks() {
		if (version.isFourChip) {
			return; // NO fpga links
		}
		for (Chip ethernetConnectedChip : ethernetConnectedChips) {
			addFpgaLinks(ethernetConnectedChip.getX(),
					ethernetConnectedChip.getY(),
					ethernetConnectedChip.ipAddress);
		}
	}

	/**
	 * Get a string detailing the number of cores and links.
	 * <p>
	 * Warning: the current implementation makes the simplification assumption
	 * that every link exists in both directions.
	 *
	 * @return A quick description of the machine.
	 */
	public final String coresAndLinkOutputString() {
		int cores = 0;
		int everyLink = 0;
		for (Chip chip : chips.values()) {
			cores += chip.nProcessors();
			everyLink += chip.router.size();
		}
		return cores + " cores and " + (everyLink / 2.0) + " links";
	}

	/**
	 * The coordinates of the chip used to boot the machine. This is typically
	 * Chip (zero, zero), and will typically have an associated InetAddress, but
	 * there is no guarantee of either of these facts.
	 * <p>
	 * If not Chip has been added to the machine at the boot location this
	 * method returns {@code null}.
	 *
	 * @return The Chip at the location specified as boot or {@code null}.
	 */
	public final Chip bootChip() {
		return chips.get(boot);
	}

	/**
	 * Iterable over the destinations of each link.
	 * <p>
	 * There will be exactly one destination for each Link. While normally all
	 * destinations will be unique the is no guarantee.
	 *
	 * @param chip
	 *            x and y coordinates for any chip on the board
	 * @return A Stream over the destination locations.
	 */
	public final Iterable<Chip> iterChipsOnBoard(Chip chip) {
		return new Iterable<Chip>() {
			@Override
			public Iterator<Chip> iterator() {
				return new ChipOnBoardIterator(chip.nearestEthernet);
			}
		};
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
	 * @return The number of user cores over all Chips.
	 */
	public final int totalAvailableUserCores() {
		int count = 0;
		for (Chip chip : chips.values()) {
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
		for (Chip chip : chips.values()) {
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
		Chip source = getChipOverLink(chip, direction);
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
	 * @return A Map of ChipLocations to Direction (hopefully empty) which
	 *         identifies links to remove
	 */
	public Map<ChipLocation, Set<Direction>> findAbnormalLinks() {
		Map<ChipLocation, Set<Direction>> abnormalLinks =
				new DefaultMap<>(HashSet::new);
		for (Chip chip : chips.values()) {
			for (Link link : chip.router) {
				if (!this.hasChipAt(link.destination)) {
					abnormalLinks.get(link.source)
							.add(link.sourceLinkDirection);
				} else {
					Chip destChip = this.getChipAt(link.destination);
					Link inverse = destChip.router
							.getLink(link.sourceLinkDirection.inverse());
					if (inverse == null) {
						abnormalLinks.get(link.source)
								.add(link.sourceLinkDirection);
					}
				}
			}
		}
		return abnormalLinks;
	}

	/**
	 * Returns a list of the abnormal Chips that are recommended for removal.
	 * <p>
	 * The current implementation identifies Chips with no outgoing links as
	 * abnormal.
	 * <p>
	 * Future implementations may add other tests for abnormal Chips.
	 *
	 * @return A set (hopefully empty) of ChipLocations to remove.
	 */
	public Set<ChipLocation> findAbnormalChips() {
		Set<ChipLocation> abnormalCores = new HashSet<>();
		for (Chip chip : chips.values()) {
			if (chip.router.size() == 0) {
				abnormalCores.add(chip.asChipLocation());
			}
		}
		return abnormalCores;
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
		if (!(obj instanceof Machine)) {
			return false;
		}
		return difference((Machine) obj) == null;
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
	 * Warning: This method could change over time, so there is no implied
	 * guarantee to the order that variables are checked or to the message that
	 * is returned.
	 * <p>
	 * The only guarantee is that {@code null} is returned if no difference is
	 * detected.
	 *
	 * @param other
	 *            Another machine to check if it has the same variables.
	 * @return {@code null} if no difference is detected otherwise a string.
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
		for (ChipLocation loc : chips.keySet()) {
			Chip c1 = chips.get(loc);
			Chip c2 = other.chips.get(loc);
			if (!c1.equals(c2)) {
				return c1 + " != " + c2;
			}
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
	 * Warning: As this method is mainly a support method for
	 * {@link #difference(Machine)}, the returned result can be changed at any
	 * time.
	 *
	 * @param that
	 *            Another machine with suspected difference in the location of
	 *            Chips.
	 * @return Some useful human readable information.
	 */
	public String chipLocationDifference(Machine that) {
		Set<ChipLocation> setThis = chips.keySet();
		Set<ChipLocation> setThat = that.chips.keySet();
		if (setThis.size() < setThat.size()) {
			Set<ChipLocation> temp = new HashSet<>(setThat);
			temp.removeAll(setThis);
			return "other has extra Chips at " + temp;
		} else if (setThis.size() > setThat.size()) {
			Set<ChipLocation> temp = new HashSet<>(setThis);
			temp.removeAll(setThat);
			return "other has missing Chips at " + temp;
		} else {
			Set<ChipLocation> temp1 = new HashSet<>(setThis);
			temp1.removeAll(setThat);
			if (temp1.isEmpty()) {
				return "No difference between chip key sets found.";
			}
			Set<ChipLocation> temp2 = new HashSet<>(setThat);
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
		return this.bootEthernetAddress;
	}

	private class ChipOnBoardIterator implements Iterator<Chip> {
		private HasChipLocation root;

		private Chip nextChip;

		private Iterator<ChipLocation> singleBoardIterator;

		ChipOnBoardIterator(HasChipLocation root) {
			this.root = root;
			SpiNNakerTriadGeometry geometry =
					SpiNNakerTriadGeometry.getSpinn5Geometry();
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
			Chip result = nextChip;
			prepareNextChip();
			return result;
		}

		private void prepareNextChip() {
			while (singleBoardIterator.hasNext()) {
				ChipLocation local = singleBoardIterator.next();
				ChipLocation global = normalizedLocation(
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
