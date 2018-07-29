package uk.ac.manchester.spinnaker.connections;

import static java.lang.Math.max;
import static java.lang.String.format;
import static java.lang.Thread.sleep;
import static java.util.Collections.synchronizedMap;
import static org.slf4j.LoggerFactory.getLogger;
import static uk.ac.manchester.spinnaker.connections.SequenceNumberSource.SEQUENCE_LENGTH;
import static uk.ac.manchester.spinnaker.connections.SequenceNumberSource.getNextSequenceNumber;
import static uk.ac.manchester.spinnaker.messages.Constants.SCP_TIMEOUT;
import static uk.ac.manchester.spinnaker.messages.scp.SCPResult.RC_LEN;
import static uk.ac.manchester.spinnaker.messages.scp.SCPResult.RC_P2P_NOREPLY;
import static uk.ac.manchester.spinnaker.messages.scp.SCPResult.RC_P2P_TIMEOUT;
import static uk.ac.manchester.spinnaker.messages.scp.SCPResult.RC_TIMEOUT;
import static uk.ac.manchester.spinnaker.messages.sdp.SDPFlag.REPLY_EXPECTED;
import static uk.ac.manchester.spinnaker.transceiver.Utils.newMessageBuffer;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import org.slf4j.Logger;

import uk.ac.manchester.spinnaker.machine.ChipLocation;
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
	private static final int HEADROOM = 8;
	private static final int DEFAULT_MAX_CHANNELS = 12;
	private static final Set<SCPResult> RETRY_CODES = new HashSet<>();
	static {
		RETRY_CODES.add(RC_TIMEOUT);
		RETRY_CODES.add(RC_P2P_TIMEOUT);
		RETRY_CODES.add(RC_LEN);
		RETRY_CODES.add(RC_P2P_NOREPLY);
	}

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
	 * Per message record.
	 *
	 * @param <T>
	 *            The type of response expected to the request in the message.
	 */
	private class Request<T extends SCPResponse> {
		/** Request in progress. */
		final SCPRequest<T> request;
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
		 * @param request The request.
		 * @param callback The success callback.
		 * @param errorCallback The failure callback.
		 */
		Request(SCPRequest<T> request, Consumer<T> callback,
				SCPErrorHandler errorCallback) {
			this.request = request;
			this.requestData = getSCPData(request, connection.getChip());
			this.callback = callback;
			this.errorCallback = errorCallback;
			retryReason = new ArrayList<>();
			retries = numRetries;
		}

		private ByteBuffer getSCPData(SCPRequest<T> scpRequest,
				ChipLocation chip) {
			ByteBuffer buffer = newMessageBuffer();
			if (scpRequest.sdpHeader.getFlags() == REPLY_EXPECTED) {
				scpRequest.updateSDPHeaderForUDPSend(chip);
			}
			scpRequest.addToBuffer(buffer);
			buffer.flip();
			return buffer;
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

		/**
		 * Handle the reception of a message.
		 *
		 * @param responseData
		 *            the content of the message, in a little-endian buffer.
		 */
		public void received(ByteBuffer responseData) throws Exception {
			T response = request.getSCPResponse(responseData);
			if (callback != null) {
				callback.accept(response);
			}
		}

		private HasCoreLocation getDestination() {
			return request.sdpHeader.getDestination();
		}
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
	 * Create a request handling pipeline using default settings.
	 *
	 * @param connection
	 *            The connection over which the communication is to take place.
	 * @param packetTimeout
	 *            The number of elapsed milliseconds after sending a packet
	 *            before it is considered a timeout.
	 */
	public SCPRequestPipeline(SCPConnection connection, int packetTimeout) {
		this(connection, DEFAULT_NUM_CHANNELS,
				DEFAULT_INTERMEDIATE_TIMEOUT_WAITS, DEFAULT_RETRIES,
				packetTimeout);
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
	 * @param <T>
	 *            The type of response expected to the request.
	 * @param request
	 *            The SCP request to be sent
	 * @throws IOException
	 *             If things go really wrong.
	 */
	public final <T extends SCPResponse> void sendRequest(SCPRequest<T> request)
			throws IOException {
		sendRequest(request, null, null);
	}

	/**
	 * Add an SCP request to the set to be sent where we don't care about the
	 * response when it is successful.
	 *
	 * @param <T>
	 *            The type of response expected to the request.
	 * @param request
	 *            The SCP request to be sent
	 * @param errorCallback
	 *            A callback function to call when an error is found when
	 *            processing the message; takes the original SCPRequest, and the
	 *            exception caught while sending it. If <tt>null</tt>, a simple
	 *            default logging function is used.
	 * @throws IOException
	 *             If things go really wrong.
	 */
	public final <T extends SCPResponse> void sendRequest(SCPRequest<T> request,
			SCPErrorHandler errorCallback) throws IOException {
		sendRequest(request, null, errorCallback);
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
	 *            <tt>null</tt> if the response doesn't need to be processed.
	 * @throws IOException
	 *             If things go really wrong.
	 */
	public final <T extends SCPResponse> void sendRequest(SCPRequest<T> request,
			Consumer<T> callback) throws IOException {
		sendRequest(request, callback, null);
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
	 *            <tt>null</tt> if the response doesn't need to be processed.
	 * @param errorCallback
	 *            A callback function to call when an error is found when
	 *            processing the message; takes the original SCPRequest, and the
	 *            exception caught while sending it. If <tt>null</tt>, a simple
	 *            default logging function is used.
	 * @throws IOException
	 *             If things go really wrong.
	 */
	public <T extends SCPResponse> void sendRequest(SCPRequest<T> request,
			Consumer<T> callback, SCPErrorHandler errorCallback)
			throws IOException {
		// Set the default error callback if needed
		if (errorCallback == null) {
			errorCallback = ((r, e) -> log
					.error("problem sending " + r.getClass(), e));
		}

		// If the connection has not been measured
		if (numChannels == null && connection.isReadyToReceive()) {
			numChannels = max(inProgress + HEADROOM, DEFAULT_MAX_CHANNELS);
			intermediateChannelWaits = numChannels - HEADROOM;
		}

		// If all the channels are used, start to receive packets
		while (numChannels != null && inProgress >= numChannels) {
			multiRetrieve(intermediateChannelWaits);
		}

		// Get the next sequence to be used
		int sequence = getNextSequenceNumber();

		// Update the packet and store required details
		request.scpRequestHeader.sequence = (short) sequence;
		Request<T> req = new Request<>(request, callback, errorCallback);
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

	private static final int RETRY_DELAY_MS = 100;

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
				sleep(RETRY_DELAY_MS);
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
				resend(req, "timeout");
			} catch (Exception e) {
				req.errorCallback.handleError(req.request, e);
				toRemove.set(seq);
			}
		}

		toRemove.stream().forEach(seq -> requests.remove(seq));
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

	/**
	 * Indicates that a request timed out.
	 */
	@SuppressWarnings("serial")
	static class SendTimedOutException extends SocketTimeoutException {
		private static final double MS_PER_S = 1000.0;

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
					req.request.scpRequestHeader.command, timeout / MS_PER_S));
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
