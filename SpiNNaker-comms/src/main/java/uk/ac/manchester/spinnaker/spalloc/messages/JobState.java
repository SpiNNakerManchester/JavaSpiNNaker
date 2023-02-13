/*
 * Copyright (c) 2018-2023 The University of Manchester
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
package uk.ac.manchester.spinnaker.spalloc.messages;

import javax.validation.constraints.Positive;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.google.errorprone.annotations.Immutable;

/**
 * A description of the state of a job, in terms of its state, whether its
 * boards are powered, the advised keep-alive polling interval and the reason
 * that the job died (if in the {@link State#DESTROYED DESTROYED} state).
 */
@Immutable
@JsonDeserialize(builder = JobState.Builder.class)
public final class JobState {
	private final State state;

	private final Boolean power;

	@Positive
	private final double keepAlive;

	@Positive
	private final double startTime;

	private final String reason;

	private final String keepalivehost;

	private JobState(State state, Boolean power, double keepAlive,
			double startTime, String reason, String keepalivehost) {
		this.state = state;
		this.power = power;
		this.keepAlive = keepAlive;
		this.startTime = startTime;
		this.reason = reason;
		this.keepalivehost = keepalivehost;
	}

	/** @return the state of the job */
	public State getState() {
		return state;
	}

	/** @return whether the job's boards are powered */
	public Boolean getPower() {
		return power;
	}

	/** @return the keepalive interval, in seconds */
	public double getKeepalive() {
		return keepAlive;
	}

	/** @return the reason why the job was destroyed */
	public String getReason() {
		return reason;
	}

	/** @return the time the job started, in seconds from the epoch */
	public double getStartTime() {
		return startTime;
	}

	/** @return the host keeping the job alive */
	public String getKeepalivehost() {
		return keepalivehost;
	}

	@Override
	public String toString() {
		return "State: " + state + " power: " + power + " keepalive "
				+ keepAlive + " reason: " + reason;
	}

	/**
	 * Builder for {@link JobState}.
	 */
	@JsonPOJOBuilder(withPrefix = "set")
	public static class Builder {
		private State state = null;

		private Boolean power = null;

		private double keepAlive;

		private double startTime;

		private String reason;

		private String keepalivehost;

		/**
		 * @param state
		 *            The state of the job.
		 */
		public void setState(State state) {
			this.state = state;
		}

		/**
		 * @param power
		 *            whether the job's boards are powered.
		 */
		public void setPower(Boolean power) {
			this.power = power;
		}

		/**
		 * @param keepAlive
		 *            The keepalive interval, in seconds.
		 */
		public void setKeepalive(double keepAlive) {
			this.keepAlive = keepAlive;
		}

		/**
		 * @param startTime
		 *            The time the job started, in seconds from the epoch.
		 */
		public void setStartTime(double startTime) {
			this.startTime = startTime;
		}

		/**
		 * @param reason
		 *            The reason why the job was destroyed.
		 */
		public void setReason(String reason) {
			this.reason = reason;
		}

		/**
		 * @param keepalivehost
		 *            The host keeping the job alive.
		 */
		public void setKeepalivehost(String keepalivehost) {
			this.keepalivehost = keepalivehost;
		}

		/**
		 * Build an instance of the immutable {@link JobState}.
		 *
		 * @return The instance.
		 */
		public JobState build() {
			return new JobState(state, power, keepAlive, startTime, reason,
					keepalivehost);
		}
	}
}
