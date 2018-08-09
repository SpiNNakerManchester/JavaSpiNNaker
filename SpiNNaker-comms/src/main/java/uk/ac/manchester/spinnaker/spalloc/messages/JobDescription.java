package uk.ac.manchester.spinnaker.spalloc.messages;

import static java.util.Collections.unmodifiableList;
import static java.util.Collections.unmodifiableMap;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * A description of the state of a job.
 */
public class JobDescription {
	private int jobID;
	private String owner;
	private long startTime;
	private State state;
	private Boolean power;
	private double keepAlive;
	private String reason;
	private String machine;
	private List<Integer> args;
	private Map<String,Object> kwargs;
	private List<BoardCoordinates> boards;
	private String keepAliveHost;

	public State getState() {
		return state;
	}

	public void setState(State state) {
		this.state = state;
	}

	public Boolean getPower() {
		return power;
	}

	public void setPower(Boolean power) {
		this.power = power;
	}

	@JsonProperty("keepalive")
	public double getKeepAlive() {
		return keepAlive;
	}

	public void setKeepAlive(double keepAlive) {
		this.keepAlive = keepAlive;
	}

	public String getReason() {
		return reason;
	}

	public void setReason(String reason) {
		this.reason = reason;
	}

	@JsonProperty("job_id")
	public int getJobID() {
		return jobID;
	}

	public void setJobID(int jobID) {
		this.jobID = jobID;
	}

	public String getOwner() {
		return owner;
	}

	public void setOwner(String owner) {
		this.owner = owner;
	}

	@JsonProperty("start_time")
	public long getStartTime() {
		return startTime;
	}

	public void setStartTime(long startTime) {
		this.startTime = startTime;
	}

	@JsonProperty("allocated_machine_name")
	public String getMachine() {
		return machine;
	}

	public void setMachine(String machine) {
		this.machine = machine;
	}

	public List<Integer> getArgs() {
		return args;
	}

	public void setArgs(List<Integer> args) {
		this.args = unmodifiableList(args);
	}

	public Map<String, Object> getKwargs() {
		return kwargs;
	}

	public void setKwargs(Map<String, Object> kwargs) {
		this.kwargs = unmodifiableMap(kwargs);
	}

	public List<BoardCoordinates> getBoards() {
		return boards;
	}

	public void setBoards(List<BoardCoordinates> boards) {
		this.boards = boards;
	}

	@JsonProperty("keepalivehost")
	public String getKeepAliveHost() {
		return keepAliveHost;
	}

	public void setKeepAliveHost(String keepAliveHost) {
		this.keepAliveHost = keepAliveHost;
	}
}
