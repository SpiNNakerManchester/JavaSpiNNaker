package uk.ac.manchester.spinnaker.spalloc;

import java.io.IOException;

/** Thrown upon a protocol-level timeout. */
@SuppressWarnings("serial")
public class ProtocolTimeoutException extends IOException {

	public ProtocolTimeoutException(String string, Throwable e) {
		super(string, e);
	}

	public ProtocolTimeoutException(String string) {
		super(string);
	}
}
