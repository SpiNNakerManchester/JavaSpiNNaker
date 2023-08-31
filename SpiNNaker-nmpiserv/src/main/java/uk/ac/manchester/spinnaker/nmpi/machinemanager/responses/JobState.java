/*
 * Copyright (c) 2014 The University of Manchester
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
package uk.ac.manchester.spinnaker.nmpi.machinemanager.responses;

/**
 * A description of the state of a job, in terms of its state, whether its
 * boards are powered, the advised keep-alive polling interval and the reason
 * that the job died (if in the {@code DESTROYED} state).
 */
public class JobState {
	/** Job is unknown. */
	public static final int UNKNOWN = 0;

	/** Job is in the queue, awaiting allocation. */
	public static final int QUEUED = 1;

	/** Job is having its boards powered up. */
	public static final int POWER = 2;

	/** Job is running (or at least ready to run). */
	public static final int READY = 3;

	/** Job has terminated, see the {@code reason} property for why. */
	public static final int DESTROYED = 4;

	/** The state of the job in the queue (from the above list). */
	private int state;

	/** The power state of the job. */
	private Boolean power;

	/** The number of seconds to keep alive. */
	private double keepAlive;

	/** The reason for the job being destroyed. */
	private String reason;

	/**
	 * Get the state of the job in the queue.
	 *
	 * @return The state
	 */
	public int getState() {
		return state;
	}

	/**
	 * Set the state of the job in the queue.
	 *
	 * @param state
	 *            The state to set
	 */
	public void setState(final int state) {
		this.state = state;
	}

	/**
	 * Get the power state of the job.
	 *
	 * @return True if on, False if off
	 */
	public Boolean getPower() {
		return power;
	}

	/**
	 * Set the power state of the job.
	 *
	 * @param power
	 *            True for on, False for off
	 */
	public void setPower(final Boolean power) {
		this.power = power;
	}

	/**
	 * Get the number of seconds to keep alive.
	 *
	 * @return The number of seconds
	 */
	public double getKeepAlive() {
		return keepAlive;
	}

	/**
	 * Set the number of seconds to keep alive.
	 *
	 * @param keepAlive
	 *            The number of seconds to set
	 */
	public void setKeepAlive(final double keepAlive) {
		this.keepAlive = keepAlive;
	}

	/**
	 * Get the reason for the job being destroyed.
	 *
	 * @return The reason
	 */
	public String getReason() {
		return reason;
	}

	/**
	 * Set the reason for the job being destroyed.
	 *
	 * @param reason
	 *            The reason to set.
	 */
	public void setReason(final String reason) {
		this.reason = reason;
	}
}
