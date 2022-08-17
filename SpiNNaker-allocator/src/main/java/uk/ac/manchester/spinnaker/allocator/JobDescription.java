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
package uk.ac.manchester.spinnaker.allocator;

import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Describes a job. This includes the state that the job is in and the
 * keep-alive configuration.
 */
@JsonIgnoreProperties({
	"keepalive-ref", "machine-ref", "power-ref", "chip-ref"
})
public class JobDescription {
	private State state;

	private String owner;

	private Instant startTime;

	private Instant finishTime;

	private String reason;

	private String keepaliveHost;

	private Instant keepaliveTime;

	public State getState() {
		return state;
	}

	public void setState(State state) {
		this.state = state;
	}

	public String getOwner() {
		return owner;
	}

	public void setOwner(String owner) {
		this.owner = owner;
	}

	@JsonAlias("start-time")
	public Instant getStartTime() {
		return startTime;
	}

	public void setStartTime(Instant startTime) {
		this.startTime = startTime;
	}

	@JsonAlias("finish-time")
	public Instant getFinishTime() {
		return finishTime;
	}

	public void setFinishTime(Instant finishTime) {
		this.finishTime = finishTime;
	}

	public String getReason() {
		return reason;
	}

	public void setReason(String reason) {
		this.reason = reason;
	}

	@JsonAlias("keepalive-host")
	public String getKeepaliveHost() {
		return keepaliveHost;
	}

	public void setKeepaliveHost(String keepaliveHost) {
		this.keepaliveHost = keepaliveHost;
	}

	@JsonAlias("keepalive-time")
	public Instant getKeepaliveTime() {
		return keepaliveTime;
	}

	public void setKeepaliveTime(Instant keepaliveTime) {
		this.keepaliveTime = keepaliveTime;
	}
}
