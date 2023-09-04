/*
 * Copyright (c) 2021 The University of Manchester
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
package uk.ac.manchester.spinnaker.alloc.allocator;

import static java.util.Objects.nonNull;
import static org.slf4j.LoggerFactory.getLogger;

import java.time.Duration;
import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.function.BiConsumer;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Service;

import com.google.errorprone.annotations.concurrent.GuardedBy;

import uk.ac.manchester.spinnaker.utils.UsedInJavadocOnly;

/**
 * Manages waiting for values.
 *
 * @author Donal Fellows
 * @author Andrew Rowley
 */
@Service
public class Epochs {
	private static final Logger log = getLogger(Epochs.class);

	/** How long to wait between cleaning of the maps. */
	private static final Duration CLEANING_SCHEDULE = Duration.ofSeconds(30);

	@Autowired
	private TaskScheduler scheduler;

	/**
	 * Mapping from job ID to set of epoch handles waiting on it for a state
	 * change.
	 */
	private final EpochMap jobs = new EpochMap();

	/**
	 * Mapping from machine ID to set of epoch handles waiting on it for a state
	 * change.
	 */
	private final EpochMap machines = new EpochMap();

	/**
	 * Mapping from board ID to set of epoch handles waiting on it for blacklist
	 * handling.
	 */
	private final EpochMap blacklists = new EpochMap();

	@PostConstruct
	private void init() {
		scheduler.scheduleAtFixedRate(this::checkEmpties, CLEANING_SCHEDULE);
	}

	/**
	 * The maps contain weak reference maps, where Epochs are removed when they
	 * are no longer referenced, but the map will still contain the empty weak
	 * reference map unless removed. This tasklet handles that cleanup.
	 */
	private void checkEmpties() {
		if (jobs.checkEmptyValues()) {
			log.debug("Job map now contains jobs {}", jobs.getIds());
		}
		if (machines.checkEmptyValues()) {
			log.debug("Machine map now contains machines {}",
					machines.getIds());
		}
		if (blacklists.checkEmptyValues()) {
			log.debug("Blacklist map now contains boards {}",
					blacklists.getIds());
		}
	}

	/**
	 * Get a job epoch for a job.
	 *
	 * @param jobId
	 *            The job identifier.
	 * @return The epoch handle.
	 */
	public Epoch getJobsEpoch(int jobId) {
		return new Epoch(jobs, jobId);
	}

	/**
	 * Get a job epoch for a list of jobs.
	 *
	 * @param jobIds
	 *            The job identifiers.
	 * @return The epoch handle.
	 */
	public Epoch getJobsEpoch(List<Integer> jobIds) {
		return new Epoch(jobs, jobIds);
	}

	/**
	 * Indicate a change in a job. Will wake any thread waiting on changes to
	 * the job epoch with {@link Epoch#waitForChange(Duration) waitForChange()}
	 * on a {@code Epoch} handle.
	 *
	 * @param job
	 *            The job that has changed
	 */
	public void jobChanged(int job) {
		jobs.changed(job);
	}

	/**
	 * Get a machine epoch for a machine.
	 *
	 * @param machineId
	 *            The identifier of the machine.
	 * @return The epoch handle.
	 */
	public Epoch getMachineEpoch(int machineId) {
		return new Epoch(machines, machineId);
	}

	/**
	 * Get a machine epoch for a set of machines.
	 *
	 * @param machineIds
	 *            The identifiers of the machine.
	 * @return The epoch handle.
	 */
	public Epoch getMachinesEpoch(List<Integer> machineIds) {
		return new Epoch(machines, machineIds);
	}

	/**
	 * Indicate a change in a machine. Will wake any thread waiting on changes
	 * to the machine epoch with {@link Epoch#waitForChange(Duration)
	 * waitForChange()} on a {@code Epoch} handle.
	 *
	 * @param machine
	 *            The machine that has changed
	 */
	public void machineChanged(int machine) {
		machines.changed(machine);
	}

	/**
	 * Get a blacklist epoch for a board.
	 *
	 * @param boardId
	 *            The id of the board.
	 * @return The epoch handle.
	 */
	public Epoch getBlacklistEpoch(int boardId) {
		return new Epoch(blacklists, boardId);
	}

	/**
	 * Indicate a change in a blacklist. Will wake any thread waiting on changes
	 * to the blacklist epoch with {@link Epoch#waitForChange(Duration)
	 * waitForChange()} on a {@code Epoch} handle.
	 *
	 * @param board
	 *            The board that has changed.
	 */
	public void blacklistChanged(int board) {
		blacklists.changed(board);
	}

	/**
	 * A waitable epoch checkpoint.
	 *
	 * @author Donal Fellows
	 * @author Andrew Rowley
	 */
	public final class Epoch {
		private final EpochMap map;

		private final List<Integer> ids;

		private final Set<Integer> changed = new HashSet<>();

		private Epoch(EpochMap map, int id) {
			this.map = map;
			this.ids = List.of(id);
			map.addAll(this, ids);
		}

		private Epoch(EpochMap map, List<Integer> ids) {
			if (ids.isEmpty()) {
				log.warn("empty ID list; will never wake");
			}
			this.map = map;
			this.ids = List.copyOf(ids);
			map.addAll(this, ids);
		}

		synchronized void updateChanged(int id) {
			log.debug("Change to {}, id {}", this, id);
			changed.add(id);
			notifyAll();
		}

		/**
		 * Wait, for up to {@code timeout}, for a change.
		 *
		 * @param timeout
		 *            The time to wait, in milliseconds.
		 * @return Whether the item has changed or not.
		 * @throws InterruptedException
		 *             If the wait is interrupted.
		 */
		public boolean waitForChange(Duration timeout)
				throws InterruptedException {
			return !getChanged(timeout).isEmpty();
		}

		/**
		 * Get the set of changed items.
		 *
		 * @param timeout
		 *            The timeout to wait for one item to change.
		 * @return The changed items.
		 * @throws InterruptedException
		 *             If the wait is interrupted.
		 */
		public Collection<Integer> getChanged(Duration timeout)
				throws InterruptedException {
			synchronized (this) {
				log.debug("Waiting for change to {}", this);
				wait(timeout.toMillis());
				log.debug("After wait, changed: {}", changed);
				return Set.copyOf(changed);
			}
		}

		/**
		 * Check if this epoch is the current one.
		 *
		 * @return Whether this is a valid epoch.
		 */
		public boolean isValid() {
			return map.containsAnyKey(ids);
		}
	}
}

/**
 * A weak mapping from an ID to the epoch handles that care about it. Handles
 * will be removed when they get garbage collected.
 *
 * @author Donal Fellows
 */
class EpochMap {
	/** The value in {@link #map} leaves. Shared. Unimportant. */
	private static final Object OBJ = new Object();

	/** A map from integers to weak sets of epochs. */
	@GuardedBy("this")
	private final Map<Integer, Map<Epochs.Epoch, Object>> map = new HashMap<>();

	synchronized boolean checkEmptyValues() {
		return map.entrySet().removeIf(entry -> entry.getValue().isEmpty());
	}

	void changed(int id) {
		var items = getSet(id);
		if (nonNull(items)) {
			synchronized (items) {
				for (var item : items) {
					item.updateChanged(id);
				}
			}
		}
	}

	/**
	 * Take the set of epochs for a particular ID.
	 *
	 * @param id
	 *            The key into the map.
	 * @return The removed set of epochs. Empty if the key is absent.
	 */
	@UsedInJavadocOnly({
		BiConsumer.class, ConcurrentModificationException.class
	})
	private synchronized Set<Epochs.Epoch> getSet(Integer id) {
		var weakmap = map.get(id);
		if (weakmap == null) {
			return null;
		}
		// Copy the set here while still synchronized to avoid
		// ConcurrentModificationException when updated elsewhere.
		return Set.copyOf(weakmap.keySet());
	}

	synchronized void addAll(Epochs.Epoch epoch, List<Integer> ids) {
		for (var id : ids) {
			map.computeIfAbsent(id, key -> new WeakHashMap<>()).put(epoch, OBJ);
		}
	}

	@SuppressWarnings("GuardedBy")
	synchronized boolean containsAnyKey(Collection<Integer> ids) {
		return ids.stream().allMatch(map::containsKey);
	}

	synchronized Collection<Integer> getIds() {
		return map.keySet();
	}
}
