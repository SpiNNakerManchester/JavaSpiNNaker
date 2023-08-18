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

import java.time.Duration;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Service;

/**
 * Manages waiting for values.
 *
 * @author Donal Fellows
 */
@Service
public class Epochs {

	/** How long to wait between cleaning of the maps. */
	private static final Duration CLEANING_SCHEDULE = Duration.ofSeconds(30);

	@Autowired
	private TaskScheduler scheduler;

	private final Map<Integer, WeakHashMap<Epoch, Boolean>> jobs =
			new HashMap<>();

	private final Map<Integer, WeakHashMap<Epoch, Boolean>> machines =
			new HashMap<>();

	private final Map<Integer, WeakHashMap<Epoch, Boolean>> blacklists =
			new HashMap<>();

	@PostConstruct
	private void init() {
		// The maps contain weak reference maps, where Epochs are removed when
		// they are no longer referenced, but the map will still contain the
		// empty weak reference map unless removed.
		scheduler.scheduleAtFixedRate(this::checkEmpties, CLEANING_SCHEDULE);
	}

	private void checkEmpties() {
		checkEmptyValues(jobs);
		checkEmptyValues(machines);
		checkEmptyValues(blacklists);
	}

	private void checkEmptyValues(
			Map<Integer, WeakHashMap<Epoch, Boolean>> map) {
		synchronized (map) {
			map.entrySet().removeIf(entry -> entry.getValue().isEmpty());
		}
	}

	private static void changed(Map<Integer, WeakHashMap<Epoch, Boolean>> map,
			int id) {
		Map<Epoch, Boolean> items;
		synchronized (map) {
			items = map.remove(id);
		}
		if (nonNull(items)) {
			for (Epoch item : items.keySet()) {
				synchronized (item) {
					item.changed.add(id);
					item.notifyAll();
				}
			}
		}
	}

	private static void add(Map<Integer, WeakHashMap<Epoch, Boolean>> map,
			Epoch epoch, int id) {
		if (!map.containsKey(id)) {
			map.put(id, new WeakHashMap<>());
		}
		map.get(id).put(epoch, true);
	}

	/**
	 * Get a job epoch for a job.
	 *
	 * @param jobId The job identifier.
	 * @return The epoch handle.
	 */
	public Epoch getJobsEpoch(int jobId) {
		return new Epoch(jobs, jobId);
	}

	/**
	 * Get a job epoch for a list of jobs.
	 *
	 * @param jobIds The job identifiers.
	 * @return The epoch handle.
	 */
	public Epoch getJobsEpoch(List<Integer> jobIds) {
		return new Epoch(jobs, jobIds);
	}

	/**
	 * Indicate a change in a job. Will wake any thread waiting on changes to
	 * the job epoch with {@link Epoch#waitForChange(Duration)
	 * waitForChange()} on a {@code Epoch} handle.
	 *
	 * @param job The job that has changed
	 */
	public void jobChanged(int job) {
		changed(jobs, job);
	}

	/**
	 * Get a machine epoch for a machine.
	 *
	 * @param machineId The identifier of the machine.
	 * @return The epoch handle.
	 */
	public Epoch getMachineEpoch(int machineId) {
		return new Epoch(machines, machineId);
	}

	/**
	 * Get a machine epoch for a set of machines.
	 *
	 * @param machineIds The identifiers of the machine.
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
	 * @param machine The machine that has changed
	 */
	public void machineChanged(int machine) {
		changed(machines, machine);
	}

	/**
	 * Get a blacklist epoch for a board.
	 *
	 * @param boardId The id of the board.
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
	 * @param board The board that has changed.
	 */
	public void blacklistChanged(int board) {
		changed(blacklists, board);
	}

	/**
	 * A waitable epoch checkpoint.
	 *
	 * @author Donal Fellows
	 */
	public final class Epoch {

		private final Map<Integer, WeakHashMap<Epoch, Boolean>> map;

		private final List<Integer> ids;

		private final Set<Integer> changed = new HashSet<>();

		private Epoch(Map<Integer, WeakHashMap<Epoch, Boolean>> map, int id) {
			this(map, List.of(id));
		}

		private Epoch(Map<Integer, WeakHashMap<Epoch, Boolean>> map,
				List<Integer> ids) {
			this.map = map;
			this.ids = List.copyOf(ids);
			synchronized (map) {
				for (var id : ids) {
					add(map, this, id);
				}
			}
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
		 * @param timeout The timeout to wait for one item to change.
		 * @return The changed items.
		 * @throws InterruptedException If the wait is interrupted.
		 */
		public Collection<Integer> getChanged(Duration timeout)
				throws InterruptedException {
			synchronized (this) {
				wait(timeout.toMillis());
			}
			return changed;
		}

		/**
		 * Check if this epoch is the current one.
		 *
		 * @return Whether this is a valid epoch.
		 */
		public boolean isValid() {
			synchronized (map) {
				for (var id : ids) {
					if (!map.containsKey(id)) {
						return false;
					}
				}
				return true;
			}
		}
	}
}
