package uk.ac.manchester.spinnaker.transceiver;

import static java.util.stream.Collectors.toSet;
import static java.util.stream.IntStream.rangeClosed;

import java.util.Iterator;
import java.util.Set;

/** A tracker of application IDs to make it easier to allocate new IDs. */
public class AppIdTracker {
	private static final int MIN_APP_ID = 17;
	private static final int MAX_APP_ID = 254;
	private final Set<Integer> freeIDs;
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
	public AppIdTracker(Set<Integer> appIDsInUse) {
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
	public AppIdTracker(Set<Integer> appIDsInUse, int minAppID,
			int maxAppID) {
		freeIDs = rangeClosed(minAppID, maxAppID).boxed().collect(toSet());
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
	public int allocateNewID() {
		Iterator<Integer> it = freeIDs.iterator();
		if (!it.hasNext()) {
			throw new RuntimeException("no remaining free IDs");
		}
		int val = it.next();
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
	public void allocateID(int id) {
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
	public void freeID(int id) {
		if (id < minID || id > maxID) {
			throw new IllegalArgumentException(
					"ID " + id + " out of allowed range of " + minID
							+ " to " + maxID);
		}
		freeIDs.add(id);
	}
}
