package uk.ac.manchester.spinnaker.spalloc.responses;

import static com.fasterxml.jackson.annotation.JsonFormat.Shape.NUMBER;

import com.fasterxml.jackson.annotation.JsonFormat;

/**
 * A description of the state of a job, in terms of its state, whether its
 * boards are powered, the advised keep-alive polling interval and the reason
 * that the job died (if in the <tt>DESTROYED</tt> state).
 */
public class JobState {
	public enum State {
		/** Job is unknown. */
		UNKNOWN,
		/** Job is in the queue, awaiting allocation. */
		QUEUED,
		/** Job is having its boards powered up. */
		POWER,
		/** Job is running (or at least ready to run). */
		READY,
		/** Job has terminated, see the <tt>reason</tt> property for why. */
		DESTROYED;
	}

	private State state;
	private Boolean power;
	private double keepAlive;
	private String reason;

	@JsonFormat(shape = NUMBER)
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
