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

import com.google.errorprone.annotations.Immutable;

/** A multicast packet routing table entry. */
@Immutable
public final class MulticastRoutingEntry extends RoutingEntry {
	private final int key;

	private final int mask;

	private final boolean defaultable;

	/**
	 * Create a multicast routing table entry.
	 *
	 * @param key
	 *            The key of the entry.
	 * @param mask
	 *            The mask of the entry.
	 * @param route
	 *            The route descriptor.
	 * @param defaultable
	 *            Whether this entry is default routable.
	 */
	public MulticastRoutingEntry(int key, int mask, int route,
			boolean defaultable) {
		super(route);
		this.key = key;
		this.mask = mask;
		this.defaultable = defaultable;
	}

	/**
	 * Create a multicast routing table entry.
	 *
	 * @param key
	 *            The key of the entry.
	 * @param mask
	 *            The mask of the entry.
	 * @param processorIDs
	 *            The IDs of the processors that this entry routes to. The
	 *            Duplicate IDs are ignored.
	 * @param linkIDs
	 *            The IDs of the links that this entry routes to. The Duplicate
	 *            IDs are ignored.
	 * @param defaultable
	 *            Whether this entry is default routable.
	 */
	public MulticastRoutingEntry(int key, int mask,
			Iterable<Integer> processorIDs, Iterable<Direction> linkIDs,
			boolean defaultable) {
		super(processorIDs, linkIDs);
		this.key = key;
		this.mask = mask;
		this.defaultable = defaultable;
	}

	/** @return the key of this entry */
	public int getKey() {
		return key;
	}

	/** @return the mask of this entry */
	public int getMask() {
		return mask;
	}

	/** @return whether this entry is default routable */
	public boolean isDefaultable() {
		return defaultable;
	}
}
