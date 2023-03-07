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
package uk.ac.manchester.spinnaker.alloc.client;

import java.net.URI;
import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.google.errorprone.annotations.Immutable;

/**
 * Describes a job. This includes the state that the job is in and the
 * keep-alive configuration.
 */
@JsonIgnoreProperties({
	"keepalive-ref", "machine-ref", "power-ref", "chip-ref"
})
@JsonDeserialize(builder = JobDescription.Builder.class)
@Immutable
public final class JobDescription {
	private final State state;

	private final String owner;

	private final Instant startTime;

	private final Instant finishTime;

	private final String reason;

	private final String keepaliveHost;

	private final Instant keepaliveTime;

	private final URI proxyRef;

	private JobDescription(State state, String owner, Instant startTime,
			Instant finishTime, String reason, String keepaliveHost,
			Instant keepaliveTime, URI proxyRef) {
		this.state = state;
		this.owner = owner;
		this.startTime = startTime;
		this.finishTime = finishTime;
		this.reason = reason;
		this.keepaliveHost = keepaliveHost;
		this.keepaliveTime = keepaliveTime;
		this.proxyRef = proxyRef;
	}

	/** @return The state of the job. */
	public State getState() {
		return state;
	}

	/**
	 * @return Who owns the job. {@code null} if the information is shrouded
	 *         from you.
	 */
	public String getOwner() {
		return owner;
	}

	/** @return When the job started. */
	@JsonProperty("start-time")
	public Instant getStartTime() {
		return startTime;
	}

	/** @return When the job was destroyed. {@code null} if not yet finished. */
	@JsonProperty("finish-time")
	public Instant getFinishTime() {
		return finishTime;
	}

	/** @return Why the job was destroyed. {@code null} if the job is alive. */
	public String getReason() {
		return reason;
	}

	/**
	 * @return Which host is believed to be keeping a job alive. May be
	 *         {@code null} if the information is not known or shrouded from
	 *         you.
	 */
	@JsonProperty("keepalive-host")
	public String getKeepaliveHost() {
		return keepaliveHost;
	}

	/** @return The most recent keepalive timestamp. */
	@JsonProperty("keep-alive-time")
	public Instant getKeepaliveTime() {
		return keepaliveTime;
	}

	/** @return The URL for connecting to the job's websocket-based proxy. */
	@JsonProperty("proxy-ref")
	public URI getProxyAddress() {
		return proxyRef;
	}

	@JsonPOJOBuilder
	@JsonIgnoreProperties({
		"keepalive-ref", "machine-ref", "power-ref", "chip-ref",
		"original-request"
	})
	static class Builder {
		private State state;

		private String owner;

		private Instant startTime;

		private Instant finishTime;

		private String reason;

		private String keepaliveHost;

		private Instant keepaliveTime;

		private URI proxyRef;

		void withState(State state) {
			this.state = state;
		}

		void withOwner(String owner) {
			this.owner = owner;
		}

		@JsonProperty("start-time")
		void withStartTime(Instant startTime) {
			this.startTime = startTime;
		}

		@JsonProperty("finish-time")
		void withFinishTime(Instant finishTime) {
			this.finishTime = finishTime;
		}

		void withReason(String reason) {
			this.reason = reason;
		}

		@JsonProperty("keepalive-host")
		void withKeepaliveHost(String keepaliveHost) {
			this.keepaliveHost = keepaliveHost;
		}

		@JsonProperty("keep-alive-time")
		void withKeepaliveTime(Instant keepaliveTime) {
			this.keepaliveTime = keepaliveTime;
		}

		@JsonProperty("proxy-ref")
		void withProxyRef(URI proxyRef) {
			this.proxyRef = proxyRef;
		}

		JobDescription build() {
			return new JobDescription(state, owner, startTime, finishTime,
					reason, keepaliveHost, keepaliveTime, proxyRef);
		}
	}
}
