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

import java.util.Objects;

import static uk.ac.manchester.spinnaker.machine.MachineDefaults.ROUTER_AVAILABLE_ENTRIES;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Iterator;
import java.util.stream.Stream;
import java.util.List;
import uk.ac.manchester.spinnaker.machine.bean.ChipDetails;

/**
 *
 * @author Christian-B
 */
public final class Router implements Iterable<Link> {

	private final EnumMap<Direction, Link> links =
			new EnumMap<>(Direction.class);

	/** The number of entries available in the routing table. */
	public final int nAvailableMulticastEntries;

	// Note: emergency_routing_enabled not implemented as not used
	// TODO convert_routing_table_entry_to_spinnaker_route

	/**
	 * Default Constructor to add links later.
	 *
	 * @param nAvailableMulticastEntries
	 *            The number of entries available in the routing table.
	 */
	public Router(int nAvailableMulticastEntries) {
		this.nAvailableMulticastEntries = nAvailableMulticastEntries;
	}

	/**
	 * Default Constructor to add links later using default values.
	 */
	public Router() {
		this(MachineDefaults.ROUTER_AVAILABLE_ENTRIES);
	}

	/**
	 * Main Constructor that allows setting of all values.
	 *
	 * @param links
	 *            Known Link(s) to add. All must have unique
	 *            {@code sourceLinkDirection}(s).
	 * @param nAvailableMulticastEntries
	 *            The number of entries available in the routing table.
	 * @throws IllegalArgumentException
	 *             Indicates that there are two Links with the same
	 *             {@code sourceLinkDirection}.
	 */
	public Router(Iterable<Link> links, int nAvailableMulticastEntries)
			throws IllegalArgumentException {
		this(nAvailableMulticastEntries);
		links.forEach(this::addLink);
	}

	/**
	 * Main Constructor that allows setting of all values.
	 *
	 * @param links
	 *            Known Link(s) to add. All must have unique
	 *            {@code sourceLinkDirection}(s).
	 * @param nAvailableMulticastEntries
	 *            The number of entries available in the routing table.
	 * @throws IllegalArgumentException
	 *             Indicates that there are two Links with the same
	 *             {@code sourceLinkDirection}.
	 */
	public Router(Stream<Link> links, int nAvailableMulticastEntries)
			throws IllegalArgumentException {
		this(nAvailableMulticastEntries);
		links.forEach(this::addLink);
	}

	/**
	 * Pass through Constructor that uses default values.
	 *
	 * @param links
	 *            Known Link(s) to add. All must have unique
	 *            {@code sourceLinkDirection}(s).
	 * @throws IllegalArgumentException
	 *             Indicates that there are two Links with the same
	 *             {@code sourceLinkDirection}.
	 */
	public Router(Iterable<Link> links) throws IllegalArgumentException {
		this(links, ROUTER_AVAILABLE_ENTRIES);
	}

	/**
	 * Pass through Constructor that uses default values.
	 *
	 * @param links
	 *            Known Link(s) to add. All must have unique
	 *            {@code sourceLinkDirection}(s).
	 * @throws IllegalArgumentException
	 *             Indicates that there are two Links with the same
	 *             {@code sourceLinkDirection}.
	 */
	public Router(Stream<Link> links) throws IllegalArgumentException {
		this(links, ROUTER_AVAILABLE_ENTRIES);
	}

	/**
	 * Shallow copy of all values except the links.
	 *
	 * @param router
	 *            original to copy other parameters from.
	 * @param links
	 *            Known Link(s) to add. All must have unique
	 *            {@code sourceLinkDirection}(s).
	 * @throws IllegalArgumentException
	 *             Indicates that there are two Links with the same
	 *             {@code sourceLinkDirection}.
	 */
	Router(Router router, Iterable<Link> links) {
		this(links, router.nAvailableMulticastEntries);
	}

	/**
	 * Creates a new Router from this source with links in all but the missing
	 * directions. Used to build a router object from JSON.
	 *
	 * @param source
	 *            Chip which links are coming from
	 * @param nAvailableMulticastEntries
	 *            The number of entries available in the routing table.
	 * @param details
	 *            The description of the chip containing this router from JSON.
	 * @param machine
	 *            The Machine this chip will go on. Used for calculating
	 *            wrap-arounds
	 * @throws NullPointerException
	 *             if a non-valid direction is not in ignoredLinks
	 */
	public Router(HasChipLocation source, int nAvailableMulticastEntries,
			ChipDetails details, Machine machine) {
		this(nAvailableMulticastEntries);
		var ignoreDirections = details.getDeadDirections();
		for (var direction : Direction.values()) {
			if (!ignoreDirections.contains(direction)) {
				var destination =
						details.getLinkDestination(direction, source, machine);
				addLink(new Link(source, direction,
						Objects.requireNonNull(destination)));
			}
		}
	}

	/**
	 * Adds a link with a unique {@code sourceLinkDirection} to this router.
	 *
	 * @param link
	 *            Link to add, which must have a {@code sourceLinkDirection} not
	 *            yet used.
	 * @throws IllegalArgumentException
	 *             Indicates another Link with this {@code sourceLinkDirection}
	 *             has already been added.
	 */
	public void addLink(Link link) throws IllegalArgumentException {
		if (links.containsKey(link.sourceLinkDirection)) {
			throw new IllegalArgumentException("Link already exists: " + link);
		}
		links.put(link.sourceLinkDirection, link);
	}

	/**
	 * Indicates if there is a Link going in this direction.
	 *
	 * @param direction
	 *            Direction to find link for.
	 * @return True if and only if there is a link in this direction,
	 */
	public boolean hasLink(Direction direction) {
		return links.containsKey(direction);
	}

	/**
	 * Obtains a Link going in this direction.
	 * <p>
	 * {@code null} is returned if no link found.
	 *
	 * @param direction
	 *            Direction to find link for.
	 * @return The Link or {@code null}
	 */
	public Link getLink(Direction direction) {
		return links.get(direction);
	}

	/**
	 * Return a View over the links.
	 * <p>
	 * Each Link is guaranteed to differ in at least the
	 * {@code sourceLinkDirection}.
	 *
	 * @return An unmodifiable collection of Link(s).
	 */
	public Collection<Link> links() {
		return Collections.unmodifiableCollection(links.values());
	}

	/**
	 * The size of the Router which is the number of Link(s).
	 * <p>
	 * The number of NeighbouringChipsCoords will always be equal to the number
	 * of links.
	 *
	 * @return The number of Link(s) and therefore NeighbouringChipsCoords
	 */
	public int size() {
		return links.size();
	}

	/**
	 * Stream of the destinations of each link.
	 * <p>
	 * There will be exactly one destination for each Link. While normally all
	 * destinations will be unique the is no guarantee.
	 *
	 * @return A Stream over the destination locations.
	 */
	public Stream<ChipLocation> streamNeighbouringChipsCoords() {
		return links.values().stream().map(link -> link.destination);
	}

	/**
	 * Iterable over the destinations of each link.
	 * <p>
	 * There will be exactly one destination for each Link. While normally all
	 * destinations will be unique the is no guarantee.
	 *
	 * @return A Stream over the destination locations.
	 */
	public Iterable<ChipLocation> iterNeighbouringChipsCoords() {
		return () -> new NeighbourIterator(links.values().iterator());
	}

	/**
	 * List of the destination for all links.
	 * <p>
	 * Note: Changes to the resulting list will not effect the actual links.
	 * This function in the future may return an unmodifiable list.
	 *
	 * @return The destination locations
	 */
	public List<ChipLocation> neighbouringChipsCoords() {
		var neighbours = new ArrayList<ChipLocation>();
		for (Link link : links.values()) {
			neighbours.add(link.destination);
		}
		return neighbours;
	}

	@Override
	public String toString() {
		var result = new StringBuilder("Router[");
		String sep = "";
		for (var entry : links.entrySet()) {
			result.append(sep);
			result.append(entry.getKey());
			result.append(":");
			result.append(entry.getValue().destination);
			sep = " ";
		}
		result.append("]");
		return result.toString();
	}

	@Override
	public Iterator<Link> iterator() {
		return links.values().iterator();
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
		// TODO compare internal states
		return obj instanceof Router;
	}
}
