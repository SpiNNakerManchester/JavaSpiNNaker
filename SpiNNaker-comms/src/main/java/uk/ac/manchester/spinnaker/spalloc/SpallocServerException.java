package uk.ac.manchester.spinnaker.spalloc;

import uk.ac.manchester.spinnaker.spalloc.messages.ExceptionResponse;

/**
 * Thrown when something went wrong on the server side that caused us to be sent
 * a message.
 */
@SuppressWarnings("serial")
class SpallocServerException extends Exception {
	public SpallocServerException(String string) {
		super(string);
	}

	SpallocServerException(ExceptionResponse r) {
		super(r.getException());
	}
}
