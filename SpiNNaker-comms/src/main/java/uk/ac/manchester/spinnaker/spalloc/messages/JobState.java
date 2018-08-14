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
	private String reason;

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
}
