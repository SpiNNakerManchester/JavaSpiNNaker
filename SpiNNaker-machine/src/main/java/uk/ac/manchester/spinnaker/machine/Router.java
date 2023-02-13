/*
 * Copyright (c) 2018-2023 The University of Manchester
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package uk.ac.manchester.spinnaker.machine;

import static java.util.Collections.unmodifiableCollection;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toUnmodifiableList;
import static uk.ac.manchester.spinnaker.machine.MachineDefaults.ROUTER_AVAILABLE_ENTRIES;

import java.util.Collection;
import java.util.EnumMap;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Stream;

import uk.ac.manchester.spinnaker.machine.bean.ChipDetails;
import uk.ac.manchester.spinnaker.utils.MappableIterable;

/**
 * Routers are parts of SpiNNaker chips responsible for directing where
 * on-machine messages are delivered to.
 *
 * @author Christian-B
 */
public final class Router implements MappableIterable<Link> {
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
		this(ROUTER_AVAILABLE_ENTRIES);
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
				var destination = details.getLinkDestination(direction, source,
						machine);
				addLink(new Link(source, direction,
						requireNonNull(destination)));
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
		return unmodifiableCollection(links.values());
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
	public MappableIterable<ChipLocation> iterNeighbouringChipsCoords() {
		return () -> new NeighbourIterator(links.values());
	}

	/**
	 * List of the destination for all links.
	 * <p>
	 * This function returns an unmodifiable list.
	 *
	 * @return The destination locations
	 */
	public List<ChipLocation> neighbouringChipsCoords() {
		return links.values().stream().map(link -> link.destination)
				.collect(toUnmodifiableList());
	}

	@Override
	public String toString() {
		return links.entrySet().stream().map(
				entry -> entry.getKey() + ":" + entry.getValue().destination)
				.collect(joining(" ", "Router[", "]"));
	}

	@Override
	public Iterator<Link> iterator() {
		return unmodifiableCollection(links.values()).iterator();
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
