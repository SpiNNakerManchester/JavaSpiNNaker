/*
 * Copyright (c) 2018 The University of Manchester
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
package uk.ac.manchester.spinnaker.spalloc.messages;

import static java.util.Collections.unmodifiableMap;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static uk.ac.manchester.spinnaker.utils.UnitConstants.MSEC_PER_SEC;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;

/**
 * A description of the state of a job.
 */
@JsonDeserialize(builder = JobDescription.Builder.class)
public final class JobDescription {
	private final int jobID;

	private final String owner;

	private final Double startTime;

	private final State state;

	private final Boolean power;

	private final double keepAlive;

	private final String reason;

	private final String machine;

	private final List<Integer> args;

	private final Map<String, Object> kwargs;

	private final List<BoardCoordinates> boards;

	private final String keepAliveHost;

	/** Number of boards to list individually in the toString(). */
	private static final int PRINT_EXACT_BOARDS_THRESHOLD = 6;

	/**
	 * @param jobID
	 *            The job's identifier
	 * @param owner
	 *            The job's owner. This is not necessarily validated!
	 * @param startTime
	 *            When the job started, in seconds from the epoch.
	 * @param state
	 *            The job state.
	 * @param power
	 *            Whether the job's allocated boards are powered on.
	 * @param keepAlive
	 *            The job's maximum keepalive interval, in seconds.
	 * @param reason
	 *            The reason why the job terminated.
	 * @param machine
	 *            The name of the machine that the job is allocated to.
	 * @param args
	 *            The positional arguments used to create the job.
	 * @param kwargs
	 *            The keyword arguments used to create the job.
	 * @param boards
	 *            The boards allocated to the job and their locations.
	 * @param keepAliveHost
	 *            The host believed to be keeping the job alive.
	 */
	public JobDescription(int jobID, String owner, Double startTime,
			State state, Boolean power, double keepAlive, String reason,
			String machine, List<Integer> args, Map<String, Object> kwargs,
			List<BoardCoordinates> boards, String keepAliveHost) {
		this.jobID = jobID;
		this.owner = owner;
		this.startTime = startTime;
		this.state = state;
		this.power = power;
		this.keepAlive = keepAlive;
		this.reason = reason;
		this.machine = machine;
		this.args = args;
		this.kwargs = kwargs;
		this.boards = boards;
		this.keepAliveHost = keepAliveHost;
	}

	/** @return The job state. */
	public State getState() {
		return state;
	}

	/** @return Whether the job's allocated boards are powered on. */
	public Boolean getPower() {
		return power;
	}

	/** @return The job's maximum keepalive interval, in seconds. */
	@JsonProperty("keepalive")
	public double getKeepAlive() {
		return keepAlive;
	}

	/** @return The reason why the job terminated. */
	public String getReason() {
		return reason;
	}

	/** @return The job's identifier. */
	@JsonProperty("job_id")
	public int getJobID() {
		return jobID;
	}

	/** @return The job's owner. This is not necessarily validated! */
	public String getOwner() {
		return owner;
	}

	/** @return When the job started, in seconds from the epoch. */
	@JsonProperty("start_time")
	public Double getStartTime() {
		return startTime;
	}

	/** @return The name of the machine that the job is allocated to. */
	@JsonProperty("allocated_machine_name")
	public String getMachine() {
		return machine;
	}

	/** @return The positional arguments used to create the job. */
	public List<Integer> getArgs() {
		return args;
	}

	/** @return The keyword arguments used to create the job. */
	public Map<String, Object> getKwargs() {
		return kwargs;
	}

	/** @return The boards allocated to the job and their locations. */
	public List<BoardCoordinates> getBoards() {
		return boards;
	}

	/** @return The host believed to be keeping the job alive. */
	@JsonProperty("keepalivehost")
	public String getKeepAliveHost() {
		return keepAliveHost;
	}

	@Override
	public String toString() {
		var builder = new StringBuilder("Job: ").append(jobID);
		builder.append(" owner: ").append(owner);
		if (nonNull(startTime)) {
			var time = Instant.ofEpochMilli((long) (startTime * MSEC_PER_SEC));
			builder.append(" startTime: ").append(time);
		}
		builder.append(" power: ").append(power);
		builder.append(" reason: ").append(reason);
		builder.append(" machine: ").append(machine);
		builder.append(" args: ").append(args);
		builder.append(" kwargs: ").append(kwargs);
		if (isNull(boards)) {
			builder.append("No Boards");
		} else if (boards.size() < PRINT_EXACT_BOARDS_THRESHOLD) {
			builder.append(" boards: ").append(boards);
		} else {
			builder.append(" # boards: ").append(boards.size());
		}
		builder.append(" keepAliveHost ").append(keepAliveHost);
		return builder.toString();
	}

	/**
	 * Builder for {@link JobDescription}.
	 */
	@JsonPOJOBuilder(withPrefix = "set")
	public static class Builder {
		private int jobID;

		private String owner;

		private Double startTime;

		private State state;

		private Boolean power;

		private double keepAlive;

		private String reason;

		private String machine;

		private List<Integer> args;

		private Map<String, Object> kwargs;

		private List<BoardCoordinates> boards = List.of();

		private String keepAliveHost;

		/**
		 * @param jobID The job's identifier.
		 */
		@JsonProperty("job_id")
		public void setJobID(int jobID) {
			this.jobID = jobID;
		}

		/**
		 * @param owner
		 *            The job's owner.
		 */
		public void setOwner(String owner) {
			this.owner = owner;
		}

		/**
		 * @param startTime
		 *            When the job started, in seconds from the epoch.
		 */
		@JsonProperty("start_time")
		public void setStartTime(Double startTime) {
			this.startTime = startTime;
		}

		/**
		 * @param state
		 *            The job state.
		 */
		public void setState(State state) {
			this.state = state;
		}

		/**
		 * @param power
		 *            Whether the job's allocated boards are powered on.
		 */
		public void setPower(Boolean power) {
			this.power = power;
		}

		/**
		 * @param keepAlive
		 *            The job's maximum keepalive interval, in seconds.
		 */
		@JsonProperty("keepalive")
		public void setKeepAlive(double keepAlive) {
			this.keepAlive = keepAlive;
		}

		/**
		 * @param reason
		 *            The reason why the job terminated.
		 */
		public void setReason(String reason) {
			this.reason = reason;
		}

		/**
		 * @param machine
		 *            The name of the machine that the job is allocated to.
		 */
		@JsonProperty("allocated_machine_name")
		public void setMachine(String machine) {
			this.machine = machine;
		}

		/**
		 * @param args
		 *            The positional arguments used to create the job.
		 */
		public void setArgs(List<Integer> args) {
			this.args = isNull(args) ? List.of() : List.copyOf(args);
		}

		/**
		 * @param kwargs
		 *            The keyword arguments used to create the job.
		 */
		public void setKwargs(Map<String, Object> kwargs) {
			this.kwargs = isNull(args) ? Map.of()
					// Careful: could be null values in map!
					: unmodifiableMap(new HashMap<>(kwargs));
		}

		/**
		 * @param boards
		 *            The boards allocated to the job and their locations.
		 */
		public void setBoards(List<BoardCoordinates> boards) {
			this.boards = isNull(boards) ? List.of() : List.copyOf(boards);
		}

		/**
		 * @param keepAliveHost
		 *            The host believed to be keeping the job alive.
		 */
		@JsonProperty("keepalivehost")
		public void setKeepAliveHost(String keepAliveHost) {
			this.keepAliveHost = keepAliveHost;
		}

		/**
		 * Build an instance of the immutable {@link JobDescription}.
		 *
		 * @return The instance.
		 */
		public JobDescription build() {
			return new JobDescription(jobID, owner, startTime, state, power,
					keepAlive, reason, machine, args, kwargs, boards,
					keepAliveHost);
		}
	}
}
