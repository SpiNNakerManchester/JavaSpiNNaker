package uk.ac.manchester.spinnaker.spalloc.messages;

import static java.util.Collections.unmodifiableList;
import static java.util.Collections.unmodifiableMap;
import static uk.ac.manchester.spinnaker.messages.Constants.MS_PER_S;

import java.util.Collections;
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
	private double startTime;
	private State state;
	private Boolean power;
	private double keepAlive;
	private String reason;
	private String machine;
	private List<Integer> args;
	private Map<String, Object> kwargs;
	private List<BoardCoordinates> boards = Collections.emptyList();
	private String keepAliveHost;

    /** Number of boards to list individually in the toString. */
	private static final int PRINT_EXACT_BOARDS_THRESHOLD = 6;

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
	public double getStartTime() {
		return startTime;
	}

	public void setStartTime(double startTime) {
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

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder("Job: ").append(jobID);
		builder.append(" owner: ").append(owner);
		builder.append(" startTime: ")
				.append(new Date((long) (startTime * MS_PER_S)));
		builder.append(" power: ").append(power);
		builder.append(" reason: ").append(reason);
		builder.append(" machine: ").append(machine);
		builder.append(" args: ").append(args);
		builder.append(" kwargs: ").append(kwargs);
		if (boards.size() < PRINT_EXACT_BOARDS_THRESHOLD) {
			builder.append(" boards: ").append(boards);
		} else {
			builder.append(" # boards: ").append(boards.size());
		}
		builder.append(" keepAliveHost ").append(keepAliveHost);
		return builder.toString();
	}
}
