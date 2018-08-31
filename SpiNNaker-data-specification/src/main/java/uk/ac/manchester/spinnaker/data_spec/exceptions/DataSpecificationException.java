package uk.ac.manchester.spinnaker.data_spec.exceptions;

/**
 * Exceptions thrown by the Data Specification code.
 */
@SuppressWarnings("serial")
public class DataSpecificationException extends Exception {
	/**
	 * Create an exception.
	 * @param msg The message in the exception.
	 */
	public DataSpecificationException(String msg) {
		super(msg);
	}

	/**
	 * Create an exception.
	 */
	DataSpecificationException() {
	}
}
