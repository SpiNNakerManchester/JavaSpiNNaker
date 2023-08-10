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

import org.springframework.stereotype.Service;

import com.google.errorprone.annotations.concurrent.GuardedBy;

/**
 * Manages epoch counters.
 *
 * @author Donal Fellows
 */
@Service
public class Epochs {

	@GuardedBy("jobs")
	private final Map<Integer, Set<Epoch>> jobs = new HashMap<>();

	@GuardedBy("machines")
	private final Map<Integer, Set<Epoch>> machines = new HashMap<>();

	@GuardedBy("blacklists")
	private final Map<Integer, Set<Epoch>> blacklists = new HashMap<>();

	private static void changed(Map<Integer, Set<Epoch>> map, int id) {
		Set<Epoch> items;
		synchronized (map) {
			items = map.get(id);
		}
		if (nonNull(items)) {
			for (Epoch item : items) {
				synchronized (item) {
					item.changed.add(id);
					item.notifyAll();
				}
			}
		}
	}

	private static void add(Map<Integer, Set<Epoch>> map, Epoch epoch,
			int id) {
		if (!map.containsKey(id)) {
			map.put(id, new HashSet<>());
		}
		map.get(id).add(epoch);
	}

	private static void remove(Map<Integer, Set<Epoch>> map, Epoch epoch,
			int id) {
		if (map.containsKey(id)) {
			map.get(id).remove(epoch);
		}
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
	public synchronized void jobChanged(int job) {
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
	public synchronized void machineChanged(int machine) {
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
	public synchronized void blacklistChanged(int board) {
		changed(blacklists, board);
	}

	/**
	 * A waitable epoch checkpoint.
	 *
	 * @author Donal Fellows
	 */
	public final class Epoch {

		private final Map<Integer, Set<Epoch>> map;

		private final List<Integer> ids;

		private final Set<Integer> changed = new HashSet<>();

		private Epoch(Map<Integer, Set<Epoch>> map, int id) {
			this.map = map;
			this.ids = List.of(id);
		}

		private Epoch(Map<Integer, Set<Epoch>> map, List<Integer> ids) {
			this.map = map;
			this.ids = List.copyOf(ids);
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

		public Collection<Integer> getChanged(Duration timeout)
				throws InterruptedException {
			synchronized (map) {
				for (var id : ids) {
					add(map, this, id);
				}
			}
			synchronized (this) {
				wait(timeout.toMillis());
			}
			synchronized (map) {
				for (var id : ids) {
					remove(map, this, id);
				}
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
