/*
 * Copyright (c) 2021 The University of Manchester
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
package uk.ac.manchester.spinnaker.alloc.allocator;

import static java.lang.System.currentTimeMillis;

import org.springframework.stereotype.Component;

/**
 * Manages epoch counters.
 *
 * @author Donal Fellows
 */
@Component
public class Epochs {
	private long jobsEpoch = 0L;

	private long machineEpoch = 0L;

	/**
	 * Get the current jobs epoch.
	 *
	 * @return Jobs epoch handle.
	 */
	public Epoch getJobsEpoch() {
		return new JobsEpoch();
	}

	/**
	 * Advance the jobs epoch. Will wake any thread waiting on changes to the
	 * epoch with {@link JobsEpoch#waitForChange(long)}.
	 */
	public synchronized void nextJobsEpoch() {
		try {
			jobsEpoch++;
		} finally {
			notifyAll();
		}
	}

	/**
	 * Get the current machines epoch.
	 *
	 * @return Machines epoch handle.
	 */
	public Epoch getMachineEpoch() {
		return new MachinesEpoch();
	}

	/**
	 * Advance the machine epoch. Will wake any thread waiting on changes to the
	 * epoch with {@link MachineEpoch#waitForChange(long)}.
	 */
	public synchronized void nextMachineEpoch() {
		try {
			machineEpoch++;
		} finally {
			notifyAll();
		}
	}

	private static long expiry(long timeout) {
		return currentTimeMillis() + timeout;
	}

	private static boolean waiting(long expiry) {
		return currentTimeMillis() < expiry;
	}

	private void waitUntil(long expiry) throws InterruptedException {
		wait(expiry - currentTimeMillis());
	}

	/**
	 * A waitable epoch checkpoint.
	 *
	 * @author Donal Fellows
	 */
	public interface Epoch {
		/**
		 * Wait, for up to {@code timeout}, for a change.
		 *
		 * @param timeout
		 *            The time to wait, in milliseconds.
		 * @throws InterruptedException
		 *             If the wait is interrupted.
		 */
		void waitForChange(long timeout) throws InterruptedException;
	}

	/**
	 * A waitable job epoch checkpoint.
	 *
	 * @author Donal Fellows
	 */
	private final class JobsEpoch implements Epoch {
		private final long epoch;

		private JobsEpoch() {
			synchronized (Epochs.this) {
				epoch = jobsEpoch;
			}
		}

		@Override
		public void waitForChange(long timeout) throws InterruptedException {
			long expiry = expiry(timeout);
			synchronized (Epochs.this) {
				while (jobsEpoch <= epoch && waiting(expiry)) {
					waitUntil(expiry);
				}
			}
		}
	}

	/**
	 * A waitable machine epoch checkpoint.
	 *
	 * @author Donal Fellows
	 */
	private final class MachinesEpoch implements Epoch {
		private final long epoch;

		private MachinesEpoch() {
			synchronized (Epochs.this) {
				epoch = machineEpoch;
			}
		}

		@Override
		public void waitForChange(long timeout) throws InterruptedException {
			long expiry = expiry(timeout);
			synchronized (Epochs.this) {
				while (machineEpoch <= epoch && waiting(expiry)) {
					waitUntil(expiry);
				}
			}
		}
	}
}
