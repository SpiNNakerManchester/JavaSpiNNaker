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

import static java.util.Collections.unmodifiableList;
import static java.util.Collections.unmodifiableMap;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static uk.ac.manchester.spinnaker.utils.UnitConstants.MSEC_PER_SEC;

import java.util.Date;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * A description of the state of a job.
 */
public class JobDescription {
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

	/** Number of boards to list individually in the toString. */
	private static final int PRINT_EXACT_BOARDS_THRESHOLD = 6;

	/** @return The job state. */
	public State getState() {
		return state;
	}

	/** @param state The job state. */
	public void setState(State state) {
		this.state = state;
	}

	/** @return Whether the job's allocated boards are powered on. */
	public Boolean getPower() {
		return power;
	}

	/** @param power Whether the job's allocated boards are powered on. */
	public void setPower(Boolean power) {
		this.power = power;
	}

	/** @return The job's maximum keepalive interval, in seconds. */
	@JsonProperty("keepalive")
	public double getKeepAlive() {
		return keepAlive;
	}

	/** @param keepAlive The job's maximum keepalive interval, in seconds. */
	public void setKeepAlive(double keepAlive) {
		this.keepAlive = keepAlive;
	}

	/** @return The reason why the job terminated. */
	public String getReason() {
		return reason;
	}

	/** @param reason The reason why the job terminated. */
	public void setReason(String reason) {
		this.reason = reason;
	}

	/** @return The job's identifier. */
	@JsonProperty("job_id")
	public int getJobID() {
		return jobID;
	}

	/** @param jobID The job's identifier. */
	public void setJobID(int jobID) {
		this.jobID = jobID;
	}

	/** @return The job's owner. This is not necessarily validated! */
	public String getOwner() {
		return owner;
	}

	/** @param owner The job's owner. */
	public void setOwner(String owner) {
		this.owner = owner;
	}

	/** @return When the job started, in seconds from the epoch. */
	@JsonProperty("start_time")
	public Double getStartTime() {
		return startTime;
	}

	/** @param startTime When the job started, in seconds from the epoch. */
	public void setStartTime(Double startTime) {
		this.startTime = startTime;
	}

	/** @return The name of the machine that the job is allocated to. */
	@JsonProperty("allocated_machine_name")
	public String getMachine() {
		return machine;
	}

	/** @param machine The name of the machine that the job is allocated to. */
	public void setMachine(String machine) {
		this.machine = machine;
	}

	/** @return The positional arguments used to create the job. */
	public List<Integer> getArgs() {
		return args;
	}

	/** @param args The positional arguments used to create the job. */
	public void setArgs(List<Integer> args) {
		this.args = unmodifiableList(args);
	}

	/** @return The keyword arguments used to create the job. */
	public Map<String, Object> getKwargs() {
		return kwargs;
	}

	/** @param kwargs The keyword arguments used to create the job. */
	public void setKwargs(Map<String, Object> kwargs) {
		this.kwargs = unmodifiableMap(kwargs);
	}

	/** @return The boards allocated to the job and their locations. */
	public List<BoardCoordinates> getBoards() {
		return boards;
	}

	/** @param boards The boards allocated to the job and their locations. */
	public void setBoards(List<BoardCoordinates> boards) {
		this.boards = boards;
	}

	/** @return The host believed to be keeping the job alive. */
	@JsonProperty("keepalivehost")
	public String getKeepAliveHost() {
		return keepAliveHost;
	}

	/** @param keepAliveHost The host believed to be keeping the job alive. */
	public void setKeepAliveHost(String keepAliveHost) {
		this.keepAliveHost = keepAliveHost;
	}

	@Override
	public String toString() {
		var builder = new StringBuilder("Job: ").append(jobID);
		builder.append(" owner: ").append(owner);
		if (nonNull(startTime)) {
			builder.append(" startTime: ")
					.append(new Date((long) (startTime * MSEC_PER_SEC)));
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
}
