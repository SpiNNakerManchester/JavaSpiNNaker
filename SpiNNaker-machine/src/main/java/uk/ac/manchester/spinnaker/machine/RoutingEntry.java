/*
 * Copyright (c) 2018 The University of Manchester
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

import static java.util.Collections.unmodifiableSet;
import static java.util.stream.IntStream.range;
import static uk.ac.manchester.spinnaker.machine.MachineDefaults.MAX_LINKS_PER_ROUTER;
import static uk.ac.manchester.spinnaker.machine.MachineDefaults.MAX_NUM_CORES;

import java.util.Collection;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.Set;

import javax.validation.constraints.NotNull;

import com.google.errorprone.annotations.Immutable;

/** A basic SpiNNaker routing entry. */
@Immutable
@SuppressWarnings("Immutable") // Error Prone can't figure out this is true
public sealed class RoutingEntry permits MulticastRoutingEntry {
	private final Set<@ValidP @NotNull Integer> processorIDs;

	private final Set<Direction> linkIDs;

	private static boolean bitset(int word, int bit) {
		return (word & (1 << bit)) != 0;
	}

	/**
	 * Create a routing entry from its encoded form.
	 *
	 * @param route
	 *            the encoded route
	 */
	public RoutingEntry(int route) {
		var ps = new LinkedHashSet<Integer>();
		range(0, MAX_NUM_CORES)
				.filter(pidx -> bitset(route, MAX_LINKS_PER_ROUTER + pidx))
				.forEach(ps::add);
		var ls = EnumSet.noneOf(Direction.class);
		range(0, MAX_LINKS_PER_ROUTER).filter(lidx -> bitset(route, lidx))
				.forEach(i -> ls.add(Direction.byId(i)));
		this.processorIDs = unmodifiableSet(ps);
		this.linkIDs = unmodifiableSet(ls);
	}

	/**
	 * Create a routing entry from its expanded description.
	 *
	 * @param processorIDs
	 *            The IDs of the processors that this entry routes to. The
	 *            Duplicate IDs are ignored.
	 * @param linkIDs
	 *            The IDs of the links that this entry routes to. The Duplicate
	 *            IDs are ignored.
	 * @throws IllegalArgumentException
	 *             If a bad processor ID is given (i.e., one that doesn't match
	 *             SpiNNaker hardware).
	 */
	public RoutingEntry(Iterable<Integer> processorIDs,
			Iterable<Direction> linkIDs) {
		var ps = new LinkedHashSet<Integer>();
		for (int procId : processorIDs) {
			if (procId >= MAX_NUM_CORES || procId < 0) {
				throw new IllegalArgumentException(
						"Processor IDs must be between 0 and "
								+ (MAX_NUM_CORES - 1) + " found " + procId);
			}
			ps.add(procId);
		}
		var ls = EnumSet.noneOf(Direction.class);
		linkIDs.forEach(ls::add);
		this.processorIDs = unmodifiableSet(ps);
		this.linkIDs = unmodifiableSet(ls);
	}

	/**
	 * gets the Entry as a single word.
	 *
	 * @return The word-encoded form of the routing entry.
	 */
	public int encode() {
		int route = 0;
		for (var processorID : processorIDs) {
			route |= 1 << (MAX_LINKS_PER_ROUTER + processorID);
		}
		for (var linkID : linkIDs) {
			route |= 1 << linkID.id;
		}
		return route;
	}

	/**
	 * The IDs of the processors that this entry routes to.
	 * <p>
	 * If the RoutingEntry was created from its encoded form and unmodified
	 * after that, the list is guaranteed to be sorted in natural order and
	 * contain no duplicates.
	 *
	 * @return An unmodifiable over the processor IDs.
	 */
	public Collection<Direction> getLinkIDs() {
		return linkIDs;
	}

	/**
	 * The ID/Directions of the links that this entry routes to.
	 * <p>
	 * If the RoutingEntry was created from its encoded form and unmodified
	 * after that, the list is guaranteed to be sorted in natural order and
	 * contain no duplicates.
	 *
	 * @return An unmodifiable view over the link IDs in natural order.
	 */
	public Collection<Integer> getProcessorIDs() {
		return processorIDs;
	}
}
