package uk.ac.manchester.spinnaker.spalloc;

/** Thrown when a state change takes too long to occur. */
public class SpallocStateChangeTimeoutException extends Exception {
	private static final long serialVersionUID = 4879238794331037892L;

	SpallocStateChangeTimeoutException() {
	}
}
