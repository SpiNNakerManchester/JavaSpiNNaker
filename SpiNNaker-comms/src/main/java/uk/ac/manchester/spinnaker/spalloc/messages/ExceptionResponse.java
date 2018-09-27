package uk.ac.manchester.spinnaker.spalloc.messages;

/**
 * A response to a request that indicates a failure.
 */
public class ExceptionResponse implements Response {
	private String exception;

	public String getException() {
		return exception;
	}

	public void setException(String exception) {
		this.exception = exception == null ? "" : exception.toString();
	}
}
