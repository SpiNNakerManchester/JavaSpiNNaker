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

import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.NON_PRIVATE;

import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Describes a job. This includes the state that the job is in and the
 * keep-alive configuration.
 */
@JsonIgnoreProperties({
	"keepalive-ref", "machine-ref", "power-ref", "chip-ref"
})
@JsonAutoDetect(setterVisibility = NON_PRIVATE)
public class JobDescription {
	private State state;

	private String owner;

	private Instant startTime;

	private Instant finishTime;

	private String reason;

	private String keepaliveHost;

	private Instant keepaliveTime;

	/** @return The state of the job. */
	public State getState() {
		return state;
	}

	void setState(State state) {
		this.state = state;
	}

	/**
	 * @return Who owns the job. {@code null} if the information is shrouded
	 *         from you.
	 */
	public String getOwner() {
		return owner;
	}

	void setOwner(String owner) {
		this.owner = owner;
	}

	/** @return When the job started. */
	@JsonAlias("start-time")
	public Instant getStartTime() {
		return startTime;
	}

	void setStartTime(Instant startTime) {
		this.startTime = startTime;
	}

	/** @return When the job was destroyed. {@code null} if not yet finished. */
	@JsonAlias("finish-time")
	public Instant getFinishTime() {
		return finishTime;
	}

	void setFinishTime(Instant finishTime) {
		this.finishTime = finishTime;
	}

	/** @return Why the job was destroyed. {@code null} if the job is alive. */
	public String getReason() {
		return reason;
	}

	void setReason(String reason) {
		this.reason = reason;
	}

	/**
	 * @return Which host is believed to be keeping a job alive. May be
	 *         {@code null} if the information is not known or shrouded from
	 *         you.
	 */
	@JsonAlias("keepalive-host")
	public String getKeepaliveHost() {
		return keepaliveHost;
	}

	void setKeepaliveHost(String keepaliveHost) {
		this.keepaliveHost = keepaliveHost;
	}

	/** @return The most recent keepalive timestamp. */
	@JsonAlias("keepalive-time")
	public Instant getKeepaliveTime() {
		return keepaliveTime;
	}

	void setKeepaliveTime(Instant keepaliveTime) {
		this.keepaliveTime = keepaliveTime;
	}
}
