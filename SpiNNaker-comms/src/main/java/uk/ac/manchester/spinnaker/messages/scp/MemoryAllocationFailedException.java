package uk.ac.manchester.spinnaker.messages.scp;

/**
 * Indicate that a memory allocation operation has failed.
 */
@SuppressWarnings("serial")
public class MemoryAllocationFailedException extends Exception {
	public MemoryAllocationFailedException(String message) {
		super(message);
	}
}
