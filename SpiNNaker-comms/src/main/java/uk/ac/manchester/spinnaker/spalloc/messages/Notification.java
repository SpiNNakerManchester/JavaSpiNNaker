package uk.ac.manchester.spinnaker.spalloc.messages;

/**
 * Notifications all implement. They are not sent in response to specific
 * requests unlike ordinary {@linkplain Response responses}.
 */
public interface Notification extends Response {
	// empty
}
