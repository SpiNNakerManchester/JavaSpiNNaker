/*
 * Copyright (c) 2018 The University of Manchester
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
package uk.ac.manchester.spinnaker.spalloc.messages;

import javax.validation.constraints.Positive;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
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

	@JsonPOJOBuilder
	public static class Builder {
		private State state = null;

		private Boolean power = null;

		private double keepAlive;

		private double startTime;

		private String reason;

		private String keepalivehost;

		@CanIgnoreReturnValue
		public Builder withState(State state) {
			this.state = state;
			return this;
		}

		@CanIgnoreReturnValue
		public Builder withPower(Boolean power) {
			this.power = power;
			return this;
		}

		@CanIgnoreReturnValue
		public Builder withKeepalive(double keepAlive) {
			this.keepAlive = keepAlive;
			return this;
		}

		@CanIgnoreReturnValue
		public Builder withStartTime(double startTime) {
			this.startTime = startTime;
			return this;
		}

		@CanIgnoreReturnValue
		public Builder withReason(String reason) {
			this.reason = reason;
			return this;
		}

		@CanIgnoreReturnValue
		public Builder withKeepalivehost(String keepalivehost) {
			this.keepalivehost = keepalivehost;
			return this;
		}

		public JobState build() {
			return new JobState(state, power, keepAlive, startTime, reason,
					keepalivehost);
		}
	}
}
