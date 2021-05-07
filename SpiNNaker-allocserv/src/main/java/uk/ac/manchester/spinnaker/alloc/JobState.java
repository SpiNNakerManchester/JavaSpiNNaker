package uk.ac.manchester.spinnaker.alloc;

/** All the possible states that a job may be in. */
public enum JobState {
	/** The job ID requested was not recognised, or is in the initial state. */
	UNKNOWN,
	/** The job is waiting in a queue for a suitable machine. */
	QUEUED,
	/**
	 * The boards allocated to the job are currently being powered on or powered
	 * off.
	 */
	POWER,
	/**
	 * The job has been allocated boards and the boards are not currently
	 * powering on or powering off.
	 */
	READY,
	/** The job has been destroyed. */
	DESTROYED
}
