package uk.ac.manchester.spinnaker.spalloc.exceptions;

/** Thrown when the job was destroyed while waiting for it to become ready. */
public class JobDestroyedException extends Exception {
	private static final long serialVersionUID = 6082560756316191208L;

	public JobDestroyedException(String destroyReason) {
		super(destroyReason);
	}
}
