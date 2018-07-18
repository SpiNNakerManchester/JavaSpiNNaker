package uk.ac.manchester.spinnaker.transceiver;

import static java.util.stream.Collectors.toSet;
import static java.util.stream.IntStream.rangeClosed;

import java.util.Iterator;
import java.util.Set;

/** A tracker of application IDs to make it easier to allocate new IDs. */
public class AppIdTracker {
	private static final int MIN_APP_ID = 17;
	private static final int MAX_APP_ID = 254;
	private final Set<Integer> free_ids;
	private final int max_app_id;
	private final int min_app_id;

	/**
	 * Allocate an application ID tracker.
	 */
	public AppIdTracker() {
		this(null, MIN_APP_ID, MAX_APP_ID);
	}

	/**
	 * Allocate an application ID tracker.
	 *
	 * @param min_app_id
	 *            The smallest application ID to use
	 * @param max_app_id
	 *            The largest application ID to use
	 */
	public AppIdTracker(int min_app_id, int max_app_id) {
		this(null, min_app_id, max_app_id);
	}

	/**
	 * Allocate an application ID tracker.
	 *
	 * @param app_ids_in_use
	 *            The IDs that are already in use
	 */
	public AppIdTracker(Set<Integer> app_ids_in_use) {
		this(app_ids_in_use, MIN_APP_ID, MAX_APP_ID);
	}

	/**
	 * Allocate an application ID tracker.
	 *
	 * @param app_ids_in_use
	 *            The IDs that are already in use
	 * @param min_app_id
	 *            The smallest application ID to use
	 * @param max_app_id
	 *            The largest application ID to use
	 */
	public AppIdTracker(Set<Integer> app_ids_in_use, int min_app_id,
			int max_app_id) {
		free_ids = rangeClosed(min_app_id, max_app_id).boxed().collect(toSet());
		if (app_ids_in_use != null) {
			free_ids.removeAll(app_ids_in_use);
		}
		this.min_app_id = min_app_id;
		this.max_app_id = max_app_id;
	}

	/**
	 * Get a new unallocated ID
	 *
	 * @throws RuntimeException
	 *             if there are no IDs available
	 */
	public int allocateNewID() {
		Iterator<Integer> it = free_ids.iterator();
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
	 *            The ID to allocate
	 * @throws IllegalArgumentException
	 *             if the ID is not present
	 */
	public void allocateID(int id) {
		if (!free_ids.remove(id)) {
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
		if (id < min_app_id || id > max_app_id) {
			throw new IllegalArgumentException(
					"ID " + id + " out of allowed range of " + min_app_id
							+ " to " + max_app_id);
		}
		free_ids.add(id);
	}
}
