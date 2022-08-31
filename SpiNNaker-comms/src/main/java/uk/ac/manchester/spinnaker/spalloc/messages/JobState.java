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

/**
 * A description of the state of a job, in terms of its state, whether its
 * boards are powered, the advised keep-alive polling interval and the reason
 * that the job died (if in the {@link State#DESTROYED DESTROYED} state).
 */
public class JobState {
	private State state;

	private Boolean power;

	private double keepAlive;

	private double startTime;

	private String reason;

	private String keepalivehost;

	/** @return the state of the job */
	public State getState() {
		return state;
	}

	/** @param state the state of the job */
	public void setState(State state) {
		this.state = state;
	}

	/** @return whether the job's boards are powered */
	public Boolean getPower() {
		return power;
	}

	/** @param power whether the job's boards are powered */
	public void setPower(Boolean power) {
		this.power = power;
	}

	/** @return the keepalive interval, in seconds */
	public double getKeepalive() {
		return keepAlive;
	}

	/** @param keepAlive the keepalive interval, in seconds */
	public void setKeepalive(double keepAlive) {
		this.keepAlive = keepAlive;
	}

	/** @return the reason why the job was destroyed */
	public String getReason() {
		return reason;
	}

	/** @param reason the reason why the job was destroyed */
	public void setReason(String reason) {
		this.reason = reason;
	}

	/** @return the time the job started, in seconds from the epoch */
	public double getStartTime() {
		return startTime;
	}

	/**
	 * @param startTime
	 *            the start time to set
	 */
	public void setStartTime(double startTime) {
		this.startTime = startTime;
	}

	/** @return the host keeping the job alive */
	public String getKeepalivehost() {
		return keepalivehost;
	}

	/**
	 * @param keepalivehost
	 *            the keepalive host to set
	 */
	public void setKeepalivehost(String keepalivehost) {
		this.keepalivehost = keepalivehost;
	}

	@Override
	public String toString() {
		return "State: " + state + " power: " + power + " keepalive "
				+ keepAlive + " reason: " + reason;
	}
}
