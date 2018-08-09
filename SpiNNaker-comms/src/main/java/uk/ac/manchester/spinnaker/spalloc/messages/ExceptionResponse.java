package uk.ac.manchester.spinnaker.spalloc.messages;

import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.databind.JsonNode;

/**
 * A response to a request that indicates a failure.
 */
public class ExceptionResponse implements Response {
	private String exception;

	public String getException() {
		return exception;
	}

	@JsonSetter("exception")
	public void setException(JsonNode exception) {
		this.exception = exception == null ? "" : exception.toString();
	}
}
