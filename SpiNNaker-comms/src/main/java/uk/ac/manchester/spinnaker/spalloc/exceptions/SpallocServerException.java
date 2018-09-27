package uk.ac.manchester.spinnaker.spalloc.exceptions;

import uk.ac.manchester.spinnaker.spalloc.messages.ExceptionResponse;

/**
 * Thrown when something went wrong on the server side that caused us to be sent
 * a message.
 */
@SuppressWarnings("serial")
public class SpallocServerException extends Exception {
	public SpallocServerException(String string) {
		super(string);
	}

	public SpallocServerException(ExceptionResponse r) {
		super(r.getException());
	}
}
