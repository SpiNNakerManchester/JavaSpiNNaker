package uk.ac.manchester.spinnaker.spalloc.messages;

import static com.fasterxml.jackson.annotation.JsonFormat.Shape.NUMBER;

import com.fasterxml.jackson.annotation.JsonFormat;

@JsonFormat(shape = NUMBER)
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
