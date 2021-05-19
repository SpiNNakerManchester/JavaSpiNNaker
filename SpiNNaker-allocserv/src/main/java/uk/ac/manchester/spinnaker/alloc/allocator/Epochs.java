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
	public JobsEpoch getJobsEpoch() {
		return new JobsEpoch();
	}

	synchronized void nextJobsEpoch() {
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
	public MachinesEpoch getMachineEpoch() {
		return new MachinesEpoch();
	}

	synchronized void nextMachineEpoch() {
		try {
			machineEpoch++;
		} finally {
			notifyAll();
		}
	}

	synchronized void waitForMachinesChange(long currentMachineEpoch,
			long timeout) throws InterruptedException {
		while (machineEpoch <= currentMachineEpoch) {
			wait(timeout);
		}
	}

	public final class JobsEpoch {
		private final long epoch;

		private JobsEpoch() {
			synchronized (Epochs.this) {
				epoch = jobsEpoch;
			}
		}

		public void waitForChange(long timeout) throws InterruptedException {
			synchronized (Epochs.this) {
				while (jobsEpoch <= epoch) {
					wait(timeout);
				}
			}
		}
	}

	public final class MachinesEpoch {
		private final long epoch;

		private MachinesEpoch() {
			synchronized (Epochs.this) {
				epoch = machineEpoch;
			}
		}

		public void waitForChange(long timeout) throws InterruptedException {
			synchronized (Epochs.this) {
				while (machineEpoch <= epoch) {
					wait(timeout);
				}
			}
		}
	}
}
