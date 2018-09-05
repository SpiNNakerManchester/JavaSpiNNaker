package uk.ac.manchester.spinnaker.spalloc.exceptions;

import java.io.IOException;

/** Thrown upon a protocol-level timeout. */
@SuppressWarnings("serial")
public class SpallocProtocolTimeoutException extends IOException {

	public SpallocProtocolTimeoutException(String string, Throwable e) {
		super(string, e);
	}

	public SpallocProtocolTimeoutException(String string) {
		super(string);
	}
}
