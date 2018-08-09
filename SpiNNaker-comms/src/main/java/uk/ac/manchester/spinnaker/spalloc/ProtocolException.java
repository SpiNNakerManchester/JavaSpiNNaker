package uk.ac.manchester.spinnaker.spalloc;

import java.io.IOException;

/** Thrown when a network-level problem occurs during protocol handling. */
@SuppressWarnings("serial")
class ProtocolException extends IOException {

	public ProtocolException(Throwable e) {
		super(e);
	}

	public ProtocolException(String string) {
		super(string);
	}
}
