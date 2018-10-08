package uk.ac.manchester.spinnaker.storage;

/**
 * Exceptions caused by the storage system.
 *
 * @author Donal Fellows
 */
public class StorageException extends Exception {
	private static final long serialVersionUID = 3553555491656536568L;

	/**
	 * Create a storage exception.
	 *
	 * @param message
	 *            What overall operation was being done
	 * @param cause
	 *            What caused the problem
	 */
	StorageException(String message, Throwable cause) {
		super(message + ": " + cause.getMessage(), cause);
	}
}
