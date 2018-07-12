package uk.ac.manchester.spinnaker.connections;

import static java.lang.Math.max;
import static java.lang.String.format;
import static java.lang.Thread.sleep;
import static java.util.Collections.synchronizedMap;
import static org.slf4j.LoggerFactory.getLogger;
import static uk.ac.manchester.spinnaker.messages.Constants.SCP_TIMEOUT;
import static uk.ac.manchester.spinnaker.messages.scp.SCPResult.RC_LEN;
import static uk.ac.manchester.spinnaker.messages.scp.SCPResult.RC_P2P_NOREPLY;
import static uk.ac.manchester.spinnaker.messages.scp.SCPResult.RC_P2P_TIMEOUT;
import static uk.ac.manchester.spinnaker.messages.scp.SCPResult.RC_TIMEOUT;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import org.slf4j.Logger;

import uk.ac.manchester.spinnaker.machine.HasCoreLocation;
import uk.ac.manchester.spinnaker.messages.scp.SCPRequest;
import uk.ac.manchester.spinnaker.messages.scp.SCPResponse;
import uk.ac.manchester.spinnaker.messages.scp.SCPResult;
import uk.ac.manchester.spinnaker.messages.scp.SCPResultMessage;

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
@SuppressWarnings("unused")
public class SCPRequestPipeline {
	private Logger log = getLogger(SCPRequestPipeline.class);
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
	private static final int MAX_SEQUENCE = 65536;
	private static final Set<SCPResult> RETRY_CODES = new HashSet<>();
	static {
		RETRY_CODES.add(RC_TIMEOUT);
		RETRY_CODES.add(RC_P2P_TIMEOUT);
		RETRY_CODES.add(RC_LEN);
		RETRY_CODES.add(RC_P2P_NOREPLY);
	}

	/** The connection over which the communication is to take place */
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

	/** The number of responses outstanding */
	private int inProgress;
	/** The number of packets that have been resent */
	private int numResent;
	private int numRetryCodeResent;
	/** The number of timeouts that occurred */
	private int numTimeouts;

	/** A dictionary of sequence number -> requests in progress */
	private final Map<Integer, Request<?>> requests;

	/**
	 * Per message record
	 */
	private class Request<T extends SCPResponse> {
		/** request in progress */
		final SCPRequest<T> request;
		/** payload of request in progress */
		private final ByteBuffer requestData;
		/** callback function for response */
		private final Consumer<T> callback;
		/** callback function for errors */
		final SCPErrorHandler errorCallback;
		/** retry reason */
		final List<String> retryReason;
		/** number of retries for the packet */
		int retries;

		Request(SCPRequest<T> request, Consumer<T> callback,
				SCPErrorHandler errorCallback) {
			this.request = request;
			this.requestData = connection.getSCPData(request);
			this.callback = callback;
			this.errorCallback = errorCallback;
			retryReason = new ArrayList<>();
			retries = numRetries;
		}

		private void send() throws IOException {
			connection.send(requestData.asReadOnlyBuffer());
		}

		private void resend(Object reason) throws IOException {
			retries--;
			retryReason.add(reason.toString());
			send();
		}

		private boolean allOneReason(String reason) {
			return retryReason.stream().allMatch(r -> reason.equals(r));
		}

		public void received(ByteBuffer responseData) throws Exception {
			T response = request.getSCPResponse(responseData);
			if (callback != null) {
				callback.accept(response);
			}
		}

		HasCoreLocation getDestination() {
			return request.sdpHeader.getDestination();
		}
	}

	/** Keep a global track of the sequence numbers used. */
	private static int nextSequence = 0;

	/**
	 * Get the next number from the global sequence, applying appropriate
	 * wrapping rules as the sequence numbers have a fixed number of bits.
	 */
	private static synchronized int getNextSequenceNumber() {
		int seq = nextSequence;
		nextSequence = (nextSequence + 1) % MAX_SEQUENCE;
		return seq;
	}

	/**
	 * Create a request handling pipeline using default settings.
	 *
	 * @param connection
	 *            The connection over which the communication is to take place.
	 */
	public SCPRequestPipeline(SCPConnection connection) {
		this(connection, DEFAULT_NUM_CHANNELS,
				DEFAULT_INTERMEDIATE_TIMEOUT_WAITS, DEFAULT_RETRIES,
				SCP_TIMEOUT);
	}

	/**
	 * Create a request handling pipeline.
	 *
	 * @param connection
	 *            The connection over which the communication is to take place
	 * @param numChannels
	 *            The number of requests to send before checking for responses.
	 *            (If <tt>null</tt>, this will be determined automatically.)
	 * @param intermediateChannelWaits
	 *            The number of outstanding responses to wait for before
	 *            continuing sending requests. (If <tt>null</tt>, this will be
	 *            determined automatically.)
	 * @param numRetries
	 *            The number of times to resend any packet for any reason before
	 *            an error is triggered.
	 * @param packetTimeout
	 *            The number of elapsed milliseconds after sending a packet
	 *            before it is considered a timeout.
	 */
	public SCPRequestPipeline(SCPConnection connection, Integer numChannels,
			Integer intermediateChannelWaits, int numRetries,
			int packetTimeout) {
		if (numChannels != null && intermediateChannelWaits == null) {
			intermediateChannelWaits = numChannels - 8;
			if (intermediateChannelWaits < 0) {
				intermediateChannelWaits = 0;
			}
		}

		this.connection = connection;
		this.numChannels = numChannels;
		this.intermediateChannelWaits = intermediateChannelWaits;
		this.numRetries = numRetries;
		this.packetTimeout = packetTimeout;

		requests = synchronizedMap(new HashMap<>());
		inProgress = 0;
		numTimeouts = 0;
		numResent = 0;
		numRetryCodeResent = 0;
	}

	/**
	 * Add an SCP request to the set to be sent where we don't care about the
	 * response when it is successful.
	 *
	 * @param request
	 *            The SCP request to be sent
	 * @throws IOException
	 *             If things go really wrong.
	 */
	public <T extends SCPResponse> void sendRequest(SCPRequest<T> request)
			throws IOException {
		sendRequest(request, null, null);
	}

	/**
	 * Add an SCP request to the set to be sent where we don't care about the
	 * response when it is successful.
	 *
	 * @param request
	 *            The SCP request to be sent
	 * @param error_callback
	 *            A callback function to call when an error is found when
	 *            processing the message; takes the original SCPRequest, and the
	 *            exception caught while sending it. If <tt>null</tt>, a simple
	 *            default logging function is used.
	 * @throws IOException
	 *             If things go really wrong.
	 */
	public <T extends SCPResponse> void sendRequest(SCPRequest<T> request,
			SCPErrorHandler error_callback) throws IOException {
		sendRequest(request, null, error_callback);
	}

	/**
	 * Add an SCP request to the set to be sent.
	 *
	 * @param request
	 *            The SCP request to be sent
	 * @param callback
	 *            A callback function to call when the response has been
	 *            received; takes an SCPResponse as a parameter, or a
	 *            <tt>null</tt> if the response doesn't need to be processed.
	 * @throws IOException
	 *             If things go really wrong.
	 */
	public <T extends SCPResponse> void sendRequest(SCPRequest<T> request,
			Consumer<T> callback) throws IOException {
		sendRequest(request, callback, null);
	}

	/**
	 * Add an SCP request to the set to be sent.
	 *
	 * @param <T>
	 *
	 * @param request
	 *            The SCP request to be sent
	 * @param callback
	 *            A callback function to call when the response has been
	 *            received; takes an SCPResponse as a parameter, or a
	 *            <tt>null</tt> if the response doesn't need to be processed.
	 * @param error_callback
	 *            A callback function to call when an error is found when
	 *            processing the message; takes the original SCPRequest, and the
	 *            exception caught while sending it. If <tt>null</tt>, a simple
	 *            default logging function is used.
	 * @throws IOException
	 *             If things go really wrong.
	 */
	public <T extends SCPResponse> void sendRequest(SCPRequest<T> request,
			Consumer<T> callback, SCPErrorHandler error_callback)
			throws IOException {
		// Set the default error callback if needed
		if (error_callback == null) {
			error_callback = ((r, e) -> log
					.error("problem sending " + r.getClass(), e));
		}

		// If the connection has not been measured
		if (numChannels == null && connection.isReadyToReceive()) {
			numChannels = max(inProgress + 8, 12);
			intermediateChannelWaits = numChannels - 8;
		}

		// If all the channels are used, start to receive packets
		while (numChannels != null && inProgress >= numChannels) {
			multiRetrieve(intermediateChannelWaits);
		}

		// Get the next sequence to be used
		int sequence = getNextSequenceNumber();

		// Update the packet and store required details
		request.scpRequestHeader.sequence = (short) sequence;
		Request<T> req = new Request<>(request, callback, error_callback);
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
		SCPResultMessage msg = connection.receiveSCPResponse(timeout);

		// Only process responses which have matching requests
		if (!requests.containsKey(msg.sequenceNumber)) {
			log.info("discarding message with unknown sequence number: "
					+ msg.sequenceNumber);
			return;
		}

		inProgress--;
		Request<?> req = requests.get(msg.sequenceNumber);

		// If the response can be retried, retry it
		if (RETRY_CODES.contains(msg.result)) {
			try {
				sleep(100);
				resend(req, msg.result);
				numRetryCodeResent++;
			} catch (Exception e) {
				req.errorCallback.handleError(req.request, e);
				requests.remove(msg.sequenceNumber);
			}
		} else {

			// No retry is possible - try constructing the result
			try {
				req.received(msg.responseData);
			} catch (Exception e) {
				req.errorCallback.handleError(req.request, e);
			}

			// Remove the sequence from the outstanding responses
			requests.remove(msg.sequenceNumber);
		}
	}

	private void handleReceiveTimeout() {
		numTimeouts++;

		// If there is a timeout, all packets remaining are resent
		BitSet to_remove = new BitSet(nextSequence);
		for (int seq : new ArrayList<>(requests.keySet())) {
			Request<?> req = requests.get(seq);
			if (req == null) {
				// Shouldn't happen, but if it does we should nuke it.
				to_remove.set(seq);
				continue;
			}

			inProgress--;
			try {
				resend(req, "timeout");
			} catch (Exception e) {
				req.errorCallback.handleError(req.request, e);
				to_remove.set(seq);
			}
		}

		to_remove.stream().forEach(seq -> requests.remove(seq));
	}

	private void resend(Request<?> req, Object reason) throws IOException {
		if (req.retries <= 0) {
			// Report timeouts as timeout exception
			if (req.allOneReason("timeout")) {
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

	@SuppressWarnings("serial")
	static class SendTimedOutException extends SocketTimeoutException {
		SendTimedOutException(Request<?> req, int timeout) {
			super(format("Operation {} timed out after {} seconds",
					req.request.scpRequestHeader.command, timeout / 1000.0));
		}
	}

	@SuppressWarnings("serial")
	static class SendFailedException extends IOException {
		SendFailedException(Request<?> req, int numRetries) {
			super(format(
					"Errors sending request {} to {}, {}, {} over {} retries: {}",
					req.request.scpRequestHeader.command,
					req.getDestination().getX(), req.getDestination().getY(),
					req.getDestination().getP(), numRetries, req.retryReason));
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
	 * The number of requests to send before checking for responses.
	 */
	public Integer getNumChannels() {
		return numChannels;
	}

	/** The number of packets that have been resent. */
	public int getNumResent() {
		return numResent;
	}

	/** The number of retries due to restartable errors. */
	public int getNumRetryCodeResent() {
		return numRetryCodeResent;
	}

	/** The number of timeouts that occurred. */
	public int getNumTimeouts() {
		return numTimeouts;
	}
}
