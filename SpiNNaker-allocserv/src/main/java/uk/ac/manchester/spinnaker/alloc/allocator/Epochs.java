/*
 * Copyright (c) 2021 The University of Manchester
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
package uk.ac.manchester.spinnaker.alloc.allocator;

import static java.lang.System.currentTimeMillis;

import java.time.Duration;

import javax.annotation.PreDestroy;

import org.springframework.stereotype.Service;

import com.google.errorprone.annotations.concurrent.GuardedBy;

/**
 * Manages epoch counters.
 *
 * @author Donal Fellows
 */
@Service
public class Epochs {
	@GuardedBy("this")
	private long jobsEpoch = 0L;

	@GuardedBy("this")
	private long machineEpoch = 0L;

	@GuardedBy("this")
	private long blacklistEpoch = 0L;

	private volatile boolean shutdown;

	@PreDestroy
	private void inShutdown() {
		shutdown = true;
	}

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
	 * epoch with {@link Epoch#waitForChange(Duration)
	 * waitForChange()} on a {@code jobsEpoch} handle.
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
	 * epoch with {@link Epoch#waitForChange(Duration)
	 * waitForChange()} on a {@code machineEpoch} handle.
	 */
	public synchronized void nextMachineEpoch() {
		try {
			machineEpoch++;
		} finally {
			notifyAll();
		}
	}

	/**
	 * Get the current blacklist epoch.
	 *
	 * @return Blacklist epoch handle.
	 */
	public Epoch getBlacklistEpoch() {
		return new BlacklistEpoch();
	}

	/**
	 * Advance the blacklist epoch. Will wake any thread waiting on changes to
	 * the epoch with {@link Epoch#waitForChange(Duration) waitForChange()} on a
	 * {@code blacklistEpoch} handle.
	 */
	public synchronized void nextBlacklistEpoch() {
		try {
			blacklistEpoch++;
		} finally {
			notifyAll();
		}
	}

	private static long expiry(Duration timeout) {
		return currentTimeMillis() + timeout.toMillis();
	}

	private boolean waiting(long expiry) {
		return currentTimeMillis() < expiry && !shutdown;
	}

	// The loops are in the callers
	@SuppressWarnings("WaitNotInLoop")
	private void waitUntil(long expiry) throws InterruptedException {
		long t = expiry - currentTimeMillis();
		wait(t);
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
		void waitForChange(Duration timeout) throws InterruptedException;

		/**
		 * Check if this epoch is the current one.
		 *
		 * @return Whether this is a valid epoch.
		 */
		boolean isValid();
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
		public void waitForChange(Duration timeout)
				throws InterruptedException {
			long expiry = expiry(timeout);
			synchronized (Epochs.this) {
				while (jobsEpoch <= epoch && waiting(expiry)) {
					waitUntil(expiry);
				}
			}
		}

		@Override
		public boolean isValid() {
			synchronized (Epochs.this) {
				return jobsEpoch <= epoch;
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
		public void waitForChange(Duration timeout)
				throws InterruptedException {
			long expiry = expiry(timeout);
			synchronized (Epochs.this) {
				while (machineEpoch <= epoch && waiting(expiry)) {
					waitUntil(expiry);
				}
			}
		}

		@Override
		public boolean isValid() {
			synchronized (Epochs.this) {
				return machineEpoch <= epoch;
			}
		}
	}

	/**
	 * A waitable blacklist epoch checkpoint. Note that this is a restartable
	 * epoch.
	 *
	 * @author Donal Fellows
	 */
	private final class BlacklistEpoch implements Epoch {
		private long epoch;

		private BlacklistEpoch() {
			synchronized (Epochs.this) {
				epoch = blacklistEpoch;
			}
		}

		@Override
		public void waitForChange(Duration timeout)
				throws InterruptedException {
			long expiry = expiry(timeout);
			synchronized (Epochs.this) {
				long ble;
				while ((ble = blacklistEpoch) <= epoch && waiting(expiry)) {
					waitUntil(expiry);
				}
				epoch = ble;
			}
		}

		@Override
		public boolean isValid() {
			synchronized (Epochs.this) {
				return blacklistEpoch <= epoch;
			}
		}
	}
}
