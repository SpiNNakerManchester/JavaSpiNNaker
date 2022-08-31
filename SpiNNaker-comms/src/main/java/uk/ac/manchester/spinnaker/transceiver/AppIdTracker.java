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
package uk.ac.manchester.spinnaker.transceiver;

import static java.util.stream.Collectors.toSet;
import static java.util.stream.IntStream.rangeClosed;

import java.util.Set;

import uk.ac.manchester.spinnaker.messages.model.AppID;

/** A tracker of application IDs to make it easier to allocate new IDs. */
public class AppIdTracker {
	private static final int MIN_APP_ID = 17;

	private static final int MAX_APP_ID = 254;

	private final Set<AppID> freeIDs;

	private final int maxID;

	private final int minID;

	/**
	 * Allocate an application ID tracker.
	 */
	public AppIdTracker() {
		this(null, MIN_APP_ID, MAX_APP_ID);
	}

	/**
	 * Allocate an application ID tracker.
	 *
	 * @param minAppID
	 *            The smallest application ID to use
	 * @param maxAppID
	 *            The largest application ID to use
	 */
	public AppIdTracker(int minAppID, int maxAppID) {
		this(null, minAppID, maxAppID);
	}

	/**
	 * Allocate an application ID tracker.
	 *
	 * @param appIDsInUse
	 *            The IDs that are already in use
	 */
	public AppIdTracker(Set<AppID> appIDsInUse) {
		this(appIDsInUse, MIN_APP_ID, MAX_APP_ID);
	}

	/**
	 * Allocate an application ID tracker.
	 *
	 * @param appIDsInUse
	 *            The IDs that are already in use
	 * @param minAppID
	 *            The smallest application ID to use
	 * @param maxAppID
	 *            The largest application ID to use
	 */
	public AppIdTracker(Set<AppID> appIDsInUse, int minAppID,
			int maxAppID) {
		freeIDs = rangeClosed(minAppID, maxAppID).mapToObj(AppID::new)
				.collect(toSet());
		if (appIDsInUse != null) {
			freeIDs.removeAll(appIDsInUse);
		}
		this.minID = minAppID;
		this.maxID = maxAppID;
	}

	/**
	 * Get a new unallocated ID.
	 *
	 * @return The new ID, now allocated.
	 * @throws RuntimeException
	 *             if there are no IDs available
	 */
	public AppID allocateNewID() {
		var it = freeIDs.iterator();
		if (!it.hasNext()) {
			throw new RuntimeException("no remaining free IDs");
		}
		var val = it.next();
		it.remove();
		return val;
	}

	/**
	 * Allocate a given ID.
	 *
	 * @param id
	 *            The ID to allocate.
	 * @throws IllegalArgumentException
	 *             if the ID is not present
	 */
	public void allocateID(AppID id) {
		if (!freeIDs.remove(id)) {
			throw new IllegalArgumentException(
					"id " + id + " was not available for allocation");
		}
	}

	/**
	 * Free a given ID.
	 *
	 * @param id
	 *            The ID to free
	 * @throws IllegalArgumentException
	 *             if the ID is out of range
	 */
	public void freeID(AppID id) {
		if (id.appID < minID || id.appID > maxID) {
			throw new IllegalArgumentException(
					"ID " + id + " out of allowed range of " + minID
							+ " to " + maxID);
		}
		freeIDs.add(id);
	}
}
