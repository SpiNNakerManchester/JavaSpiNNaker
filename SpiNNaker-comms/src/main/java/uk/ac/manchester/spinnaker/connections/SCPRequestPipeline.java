package uk.ac.manchester.spinnaker.connections;

import static java.lang.Math.max;
import static java.lang.String.format;
import static java.lang.Thread.sleep;
import static java.util.Collections.synchronizedMap;
import static java.util.Objects.requireNonNull;
import static org.slf4j.LoggerFactory.getLogger;
import static uk.ac.manchester.spinnaker.messages.Constants.MS_PER_S;
import static uk.ac.manchester.spinnaker.messages.Constants.SCP_TIMEOUT;
import static uk.ac.manchester.spinnaker.messages.scp.SequenceNumberSource.SEQUENCE_LENGTH;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import org.slf4j.Logger;

import uk.ac.manchester.spinnaker.machine.HasCoreLocation;
import uk.ac.manchester.spinnaker.messages.scp.SCPCommand;
import uk.ac.manchester.spinnaker.messages.scp.SCPRequest;
import uk.ac.manchester.spinnaker.messages.scp.SCPResponse;
import uk.ac.manchester.spinnaker.messages.scp.SCPResultMessage;
import uk.ac.manchester.spinnaker.transceiver.RetryTracker;

/**
 * Allows a set of SCP requests to be grouped together in a communication across
 * a number of channels for a given connection.
 * <p>
 * This class implements an SCP windowing, first suggested by Andrew Mundy. This
 * extends the idea by having both send and receive windows. These are
 * represented by the <i>numChannels</i> and the <i>intermediateChannelWaits</i>
 * parameters respectively. This seems to help with the timeout issue; when a
 * timeout is received, all requests for which a reply has not been received can
 * also timeout.
 *
 * @author Andrew Mundy
 * @author Andrew Rowley
 * @author Donal Fellows
 */
public class SCPRequestPipeline {
	private static final Logger log = getLogger(SCPRequestPipeline.class);
	/** The default number of requests to send before checking for responses. */
	public static final int DEFAULT_NUM_CHANNELS = 1;
	/**
	 * The default number of outstanding responses to wait for before continuing
	 * sending requests.
	 */
	public static final int DEFAULT_INTERMEDIATE_TIMEOUT_WAITS = 0;
	/**
	 * The default number of times to resend any packet for any reason before an
	 * error is triggered.
	 */
	public static final int DEFAULT_RETRIES = 3;
	private static final int HEADROOM = 8;
	private static final int DEFAULT_MAX_CHANNELS = 12;
	private static final int RETRY_DELAY_MS = 100;
	private static final String REASON_TIMEOUT = "timeout";

	/** The connection over which the communication is to take place. */
	private SCPConnection connection;
	/** The number of requests to send before checking for responses. */
	private Integer numChannels;
	/**
	 * The number of outstanding responses to wait for before continuing sending
	 * requests.
	 */
	private int intermediateChannelWaits;
	/**
	 * The number of times to resend any packet for any reason before an error
	 * is triggered.
	 */
	private int numRetries;
	/**
	 * The number of elapsed milliseconds after sending a packet before it is
	 * considered a timeout.
	 */
	private int packetTimeout;

	/** The number of responses outstanding. */
	private int inProgress;
	/** The number of packets that have been resent. */
	private int numResent;
	private int numRetryCodeResent;
	/** The number of timeouts that occurred. */
	private int numTimeouts;

	/** A dictionary of sequence number -> requests in progress. */
	private final Map<Integer, Request<?>> requests;
	/**
	 * An object used to track how many retries have been done, or {@code null}
	 * if no such tracking is required.
	 */
	private final RetryTracker retryTracker;

	/**
	 * Per message record.
	 *
	 * @param <T>
	 *            The type of response expected to the request in the message.
	 */
	private final class Request<T extends SCPResponse> {
		/** Request in progress. */
		private final SCPRequest<T> request;
		/** Payload of request in progress. */
		private final ByteBuffer requestData;
		/** Callback function for response. */
		private final Consumer<T> callback;
		/** Callback function for errors. */
		final SCPErrorHandler errorCallback;
		/** Retry reasons. */
		final List<String> retryReason;
		/** Number of retries for the packet. */
		int retries;

		/**
		 * Make a record.
		 *
		 * @param request
		 *            The request.
		 * @param callback
		 *            The success callback.
		 * @param errorCallback
		 *            The failure callback.
		 */
		Request(SCPRequest<T> request, Consumer<T> callback,
				SCPErrorHandler errorCallback) {
			this.request = request;
			this.requestData = request.getMessageData(connection.getChip());
			this.callback = callback;
			this.errorCallback = errorCallback;
			retryReason = new ArrayList<>();
			retries = numRetries;
		}

		private void send() throws IOException {
			connection.send(requestData.asReadOnlyBuffer());
		}

		/**
		 * Send the request again.
		 *
		 * @param reason
		 *            Why the request is being sent again.
		 * @throws IOException
		 *             If the connection throws.
		 */
		void resend(Object reason) throws IOException {
			retries--;
			retryReason.add(reason.toString());
			if (retryTracker != null) {
				retryTracker.retryNeeded();
			}
			send();
		}

		/**
		 * Tests whether the reasons for resending are consistent.
		 *
		 * @param reason
		 *            The particular reason being checked for.
		 * @return True if all reasons are that reason.
		 */
		boolean allOneReason(String reason) {
			return retryReason.stream().allMatch(r -> reason.equals(r));
		}

		/**
		 * Handle the reception of a message.
		 *
		 * @param responseData
		 *            the content of the message, in a little-endian buffer.
		 */
		void parseReceivedResponse(SCPResultMessage msg) throws Exception {
			T response = msg.parsePayload(request);
			if (callback != null) {
				callback.accept(response);
			}
		}

		/**
		 * Which core is the destination of the request?
		 *
		 * @return The core location.
		 */
		HasCoreLocation getDestination() {
			return request.sdpHeader.getDestination();
		}

		/**
		 * Report a failure to the higher-level code that created the request.
		 *
		 * @param exception
		 *            The problem that is being reported.
		 */
		void handleError(Throwable exception) {
			if (errorCallback != null) {
				errorCallback.handleError(request, exception);
			}
		}

		/**
		 * Get the command being sent in the request.
		 *
		 * @return The request's SCP command.
		 */
		SCPCommand getCommand() {
			return request.scpRequestHeader.command;
		}
	}

	/**
	 * Create a request handling pipeline using default settings.
	 *
	 * @param connection
	 *            The connection over which the communication is to take place.
	 * @param retryTracker
	 *            Object used to track how many retries were used in an
	 *            operation. May be {@code null} if no suck tracking is
	 *            required.
	 */
	public SCPRequestPipeline(SCPConnection connection,
			RetryTracker retryTracker) {
		this(connection, DEFAULT_NUM_CHANNELS,
				DEFAULT_INTERMEDIATE_TIMEOUT_WAITS, DEFAULT_RETRIES,
				SCP_TIMEOUT, retryTracker);
	}

	/**
	 * Create a request handling pipeline using default settings.
	 *
	 * @param connection
	 *            The connection over which the communication is to take place.
	 * @param packetTimeout
	 *            The number of elapsed milliseconds after sending a packet
	 *            before it is considered a timeout.
	 * @param retryTracker
	 *            Object used to track how many retries were used in an
	 *            operation. May be {@code null} if no suck tracking is
	 *            required.
	 */
	public SCPRequestPipeline(SCPConnection connection, int packetTimeout,
			RetryTracker retryTracker) {
		this(connection, DEFAULT_NUM_CHANNELS,
				DEFAULT_INTERMEDIATE_TIMEOUT_WAITS, DEFAULT_RETRIES,
				packetTimeout, retryTracker);
	}

	/**
	 * Create a request handling pipeline.
	 *
	 * @param connection
	 *            The connection over which the communication is to take place
	 * @param numChannels
	 *            The number of requests to send before checking for responses.
	 *            (If {@code null}, this will be determined automatically.)
	 * @param intermediateChannelWaits
	 *            The number of outstanding responses to wait for before
	 *            continuing sending requests. (If {@code null}, this will be
	 *            determined automatically.)
	 * @param numRetries
	 *            The number of times to resend any packet for any reason before
	 *            an error is triggered.
	 * @param packetTimeout
	 *            The number of elapsed milliseconds after sending a packet
	 *            before it is considered a timeout.
	 * @param retryTracker
	 *            Object used to track how many retries were used in an
	 *            operation. May be {@code null} if no suck tracking is
	 *            required.
	 */
	public SCPRequestPipeline(SCPConnection connection, Integer numChannels,
			Integer intermediateChannelWaits, int numRetries, int packetTimeout,
			RetryTracker retryTracker) {
		if (numChannels != null && intermediateChannelWaits == null) {
			intermediateChannelWaits = numChannels - HEADROOM;
			if (intermediateChannelWaits < 0) {
				intermediateChannelWaits = 0;
			}
		}

		this.connection = connection;
		this.numChannels = numChannels;
		this.intermediateChannelWaits = intermediateChannelWaits;
		this.numRetries = numRetries;
		this.packetTimeout = packetTimeout;
		this.retryTracker = retryTracker;

		requests = synchronizedMap(new HashMap<>());
		inProgress = 0;
		numTimeouts = 0;
		numResent = 0;
		numRetryCodeResent = 0;
	}

	/**
	 * Add an SCP request to the set to be sent.
	 *
	 * @param <T>
	 *            The type of response expected to the request.
	 * @param request
	 *            The SCP request to be sent
	 * @param callback
	 *            A callback function to call when the response has been
	 *            received; takes an SCPResponse as a parameter, or a
	 *            {@code null} if the response doesn't need to be processed.
	 * @param errorCallback
	 *            A callback function to call when an error is found when
	 *            processing the message; takes the original SCPRequest, and the
	 *            exception caught while sending it.
	 * @throws IOException
	 *             If things go really wrong.
	 */
	public <T extends SCPResponse> void sendRequest(SCPRequest<T> request,
			Consumer<T> callback, SCPErrorHandler errorCallback)
			throws IOException {
		// If the connection has not been measured
		if (numChannels == null && connection.isReadyToReceive()) {
			numChannels = max(inProgress + HEADROOM, DEFAULT_MAX_CHANNELS);
			intermediateChannelWaits = numChannels - HEADROOM;
		}

		// If all the channels are used, start to receive packets
		while (inProgress >= numChannels) {
			multiRetrieve(intermediateChannelWaits);
		}

		// Update the packet and store required details
		int sequence = request.scpRequestHeader.issueSequenceNumber();
		Request<T> req =
				new Request<>(request, callback, requireNonNull(errorCallback));
		requests.put(sequence, req);

		// Send the request, keeping track of how many are sent
		req.send();
		inProgress++;
	}

	/**
	 * Indicate the end of the packets to be sent. This must be called to ensure
	 * that all responses are received and handled.
	 *
	 * @throws IOException
	 *             If anything goes wrong with communications.
	 */
	public void finish() throws IOException {
		while (inProgress > 0) {
			multiRetrieve(0);
		}
	}

	private void singleRetrieve(int timeout) throws IOException {
		// Receive the next response
		log.debug("waiting for message...");
		SCPResultMessage msg = connection.receiveSCPResponse(timeout);
		log.debug("received message {}", msg.getResult());
		Request<?> req = msg.pickRequest(requests);

		// Only process responses which have matching requests
		if (req == null) {
			log.info("discarding message with unknown sequence number: {}",
					msg.getSequenceNumber());
			return;
		}
		inProgress--;

		// If the response can be retried, retry it
		if (msg.isRetriable()) {
			try {
				sleep(RETRY_DELAY_MS);
				resend(req, msg.getResult());
				numRetryCodeResent++;
			} catch (Exception e) {
				req.handleError(e);
				msg.removeRequest(requests);
			}
		} else {

			// No retry is possible - try constructing the result
			try {
				req.parseReceivedResponse(msg);
			} catch (Exception e) {
				req.handleError(e);
			}

			// Remove the sequence from the outstanding responses
			msg.removeRequest(requests);
		}
	}

	private void handleReceiveTimeout() {
		numTimeouts++;

		// If there is a timeout, all packets remaining are resent
		BitSet toRemove = new BitSet(SEQUENCE_LENGTH);
		for (int seq : new ArrayList<>(requests.keySet())) {
			Request<?> req = requests.get(seq);
			if (req == null) {
				// Shouldn't happen, but if it does we should nuke it.
				toRemove.set(seq);
				continue;
			}

			inProgress--;
			try {
				resend(req, REASON_TIMEOUT);
			} catch (Exception e) {
				req.handleError(e);
				toRemove.set(seq);
			}
		}

		toRemove.stream().forEach(seq -> requests.remove(seq));
	}

	private void resend(Request<?> req, Object reason) throws IOException {
		if (req.retries <= 0) {
			// Report timeouts as timeout exception
			if (req.allOneReason(REASON_TIMEOUT)) {
				throw new SendTimedOutException(req, packetTimeout);
			}

			// Report any other exception
			throw new SendFailedException(req, numRetries);
		}

		// If the request can be retried, retry it
		inProgress++;
		req.resend(reason);
		numResent++;
	}

	/**
	 * Indicates that a request timed out.
	 */
	@SuppressWarnings("serial")
	static class SendTimedOutException extends SocketTimeoutException {
		/**
		 * Instantiate.
		 *
		 * @param req
		 *            The request that timed out.
		 * @param timeout
		 *            The length of timeout, in milliseconds.
		 */
		SendTimedOutException(Request<?> req, int timeout) {
			super(format("Operation %s timed out after %f seconds",
					req.getCommand(), timeout / MS_PER_S));
		}
	}

	/**
	 * Indicates that a request could not be sent.
	 */
	@SuppressWarnings("serial")
	static class SendFailedException extends IOException {
		/**
		 * Instantiate.
		 *
		 * @param req
		 *            The request that timed out.
		 * @param numRetries
		 *            How many attempts to send it were made.
		 */
		SendFailedException(Request<?> req, int numRetries) {
			super(format(
					"Errors sending request %s to %d,%d,%d over %d retries: %s",
					req.getCommand(), req.getDestination().getX(),
					req.getDestination().getY(), req.getDestination().getP(),
					numRetries, req.retryReason));
		}
	}

	/**
	 * Receives responses until there are only numPackets responses left.
	 *
	 * @param numPackets
	 *            The number of packets that can remain after running.
	 * @throws IOException
	 *             If anything goes wrong with receiving a packet.
	 */
	private void multiRetrieve(int numPackets) throws IOException {
		// While there are still more packets in progress than some threshold
		while (inProgress > numPackets) {
			try {
				// Receive the next response
				singleRetrieve(packetTimeout);
			} catch (SocketTimeoutException e) {
				handleReceiveTimeout();
			}
		}
	}

	/**
	 * @return The number of requests to send before checking for responses.
	 */
	public Integer getNumChannels() {
		return numChannels;
	}

	/** @return The number of packets that have been resent. */
	public int getNumResent() {
		return numResent;
	}

	/** @return The number of retries due to restartable errors. */
	public int getNumRetryCodeResent() {
		return numRetryCodeResent;
	}

	/** @return The number of timeouts that occurred. */
	public int getNumTimeouts() {
		return numTimeouts;
	}
}
