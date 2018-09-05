package uk.ac.manchester.spinnaker.spalloc.exceptions;

import java.io.IOException;

/** Thrown when a network-level problem occurs during protocol handling. */
@SuppressWarnings("serial")
public class SpallocProtocolException extends IOException {

	public SpallocProtocolException(Throwable e) {
		super(e);
	}

	public SpallocProtocolException(String string) {
		super(string);
	}
}
