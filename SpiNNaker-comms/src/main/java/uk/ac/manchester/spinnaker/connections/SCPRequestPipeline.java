/*
 * Copyright (c) 2018-2019 The University of Manchester
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package uk.ac.manchester.spinnaker.connections;

import static java.lang.Integer.getInteger;
import static java.lang.Short.toUnsignedInt;
import static java.lang.String.format;
import static java.lang.System.nanoTime;
import static java.lang.Thread.sleep;
import static java.util.Collections.emptySet;
import static java.util.Collections.synchronizedMap;
import static java.util.Objects.requireNonNull;
import static org.slf4j.LoggerFactory.getLogger;
import static uk.ac.manchester.spinnaker.messages.Constants.SCP_RETRY_DEFAULT;
import static uk.ac.manchester.spinnaker.messages.Constants.SCP_TIMEOUT_DEFAULT;
import static uk.ac.manchester.spinnaker.messages.scp.SequenceNumberSource.SEQUENCE_LENGTH;
import static uk.ac.manchester.spinnaker.utils.UnitConstants.MSEC_PER_SEC;
import static uk.ac.manchester.spinnaker.utils.WaitUtils.waitUntil;

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
import uk.ac.manchester.spinnaker.messages.scp.CheckOKResponse;
import uk.ac.manchester.spinnaker.messages.scp.CommandCode;
import uk.ac.manchester.spinnaker.messages.scp.NoResponse;
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
	/**
	 * The name of a <em>system property</em> that can override the default
	 * timeouts. If specified as an integer, it gives the number of milliseconds
	 * to wait before timing out a communication.
	 */
	private static final String TIMEOUT_PROPERTY = "spinnaker.scp_timeout";
	/**
	 * The name of a <em>system property</em> that can override the default
	 * retries. If specified as an integer, it gives the number of retries to
	 * perform (on timeout of receiving a reply) before timing out a
	 * communication.
	 */
	private static final String RETRY_PROPERTY = "spinnaker.scp_retries";
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
	public static final int SCP_RETRIES;
	/**
	 * How long to wait between retries, in milliseconds.
	 */
	public static final int RETRY_DELAY_MS = 1;
	private static final String REASON_TIMEOUT = "timeout";
	/**
	 * Packet minimum send interval, in <em>nanoseconds</em>.
	 */
	private static final int INTER_SEND_INTERVAL_NS = 60000;

	/** The default for the timeout (in ms). */
	public static final int SCP_TIMEOUT;

	static {
		SCP_TIMEOUT = getInteger(TIMEOUT_PROPERTY, SCP_TIMEOUT_DEFAULT);
		SCP_RETRIES = getInteger(RETRY_PROPERTY, SCP_RETRY_DEFAULT);
	}

	/** The connection over which the communication is to take place. */
	private SCPConnection connection;
	/** The number of requests to send before checking for responses. */
	private int numChannels;
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
	/** The number of requests issued to this pipeline. */
	private int numRequests;
	/** The number of packets that have been resent. */
	private int numResent;
	/** The number of retries due to restartable errors. */
	private int numRetryCodeResent;
	/** The number of timeouts that occurred. */
	private int numTimeouts;

	/** A dictionary of sequence number &rarr; requests in progress. */
	private final Map<Integer, Request<?>> outstandingRequests;
	/**
	 * An object used to track how many retries have been done, or {@code null}
	 * if no such tracking is required.
	 */
	private final RetryTracker retryTracker;
	private long nextSendTime = 0;

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
		private final SCPErrorHandler errorCallback;
		/** Retry reasons. */
		private final List<String> retryReason;
		/** Number of retries for the packet. */
		private int retries;

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
		private Request(SCPRequest<T> request, Consumer<T> callback,
				SCPErrorHandler errorCallback) {
			this.request = request;
			this.requestData = request.getMessageData(connection.getChip());
			this.callback = callback;
			this.errorCallback = errorCallback;
			retryReason = new ArrayList<>();
			retries = numRetries;
		}

		private void send() throws IOException {
			waitUntil(nextSendTime);
			connection.send(requestData.asReadOnlyBuffer());
			nextSendTime = nanoTime() + INTER_SEND_INTERVAL_NS;
		}

		/**
		 * Send the request again.
		 *
		 * @param reason
		 *            Why the request is being sent again.
		 * @throws IOException
		 *             If the connection throws.
		 */
		private void resend(Object reason) throws IOException {
			retries--;
			retryReason.add(reason.toString());
			if (retryTracker != null) {
				retryTracker.retryNeeded();
			}
			send();
		}

		/**
		 * Tests whether the reasons for resending are consistently timeouts.
		 *
		 * @return True if all reasons are timeouts.
		 */
		private boolean allTimeoutFailures() {
			return retryReason.stream().allMatch(REASON_TIMEOUT::equals);
		}

		/**
		 * Handle the reception of a message.
		 *
		 * @param responseData
		 *            the content of the message, in a little-endian buffer.
		 */
		private void parseReceivedResponse(SCPResultMessage msg)
				throws Exception {
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
		private HasCoreLocation getDestination() {
			return request.sdpHeader.getDestination();
		}

		/**
		 * Report a failure to the higher-level code that created the request.
		 *
		 * @param exception
		 *            The problem that is being reported.
		 */
		private void handleError(Throwable exception) {
			if (errorCallback != null) {
				errorCallback.handleError(request, exception);
			}
		}

		/**
		 * Get the command being sent in the request.
		 *
		 * @return The request's SCP command.
		 */
		private CommandCode getCommand() {
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
				DEFAULT_INTERMEDIATE_TIMEOUT_WAITS, SCP_RETRIES, SCP_TIMEOUT,
				retryTracker);
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
				DEFAULT_INTERMEDIATE_TIMEOUT_WAITS, SCP_RETRIES, packetTimeout,
				retryTracker);
	}

	/**
	 * Create a request handling pipeline.
	 *
	 * @param connection
	 *            The connection over which the communication is to take place
	 * @param numChannels
	 *            The number of requests to send before checking for responses.
	 * @param intermediateChannelWaits
	 *            The number of outstanding responses to wait for before
	 *            continuing sending requests.
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
	public SCPRequestPipeline(SCPConnection connection, int numChannels,
			int intermediateChannelWaits, int numRetries, int packetTimeout,
			RetryTracker retryTracker) {
		this.connection = connection;
		this.numChannels = numChannels;
		this.intermediateChannelWaits = intermediateChannelWaits;
		this.numRetries = numRetries;
		this.packetTimeout = packetTimeout;
		this.retryTracker = retryTracker;

		outstandingRequests = synchronizedMap(new HashMap<>());
		numRequests = 0;
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
	public <T extends CheckOKResponse> void sendRequest(SCPRequest<T> request,
			Consumer<T> callback, SCPErrorHandler errorCallback)
			throws IOException {
		// If all the channels are used, start to receive packets
		while (outstandingRequests.size() >= numChannels) {
			multiRetrieve(intermediateChannelWaits);
		}

		// Update the packet and store required details
		int sequence = toUnsignedInt(request.scpRequestHeader
				.issueSequenceNumber(outstandingRequests.keySet()));

		log.debug("sending message with sequence {}", sequence);
		Request<T> req =
				new Request<>(request, callback, requireNonNull(errorCallback));
		if (outstandingRequests.put(sequence, req) != null) {
			throw new RuntimeException("duplicate sequence number catastrophe");
		}
		numRequests++;

		// Send the request
		req.send();
	}

	/**
	 * Send a one-way request.
	 *
	 * @param request
	 *            The one-way SCP request to be sent.
	 * @throws IOException
	 *             If things go really wrong.
	 */
	public void sendOneWayRequest(SCPRequest<? extends NoResponse> request)
			throws IOException {
		// Wait for all current in-flight responses to be received
		finish();

		// Update the packet with a (non-valuable) sequence number
		request.scpRequestHeader.issueSequenceNumber(emptySet());
		// Send the request
		new Request<>(request, null, null).send();
	}

	/**
	 * Indicate the end of the packets to be sent. This must be called to ensure
	 * that all responses are received and handled.
	 *
	 * @throws IOException
	 *             If anything goes wrong with communications.
	 */
	public void finish() throws IOException {
		while (!outstandingRequests.isEmpty()) {
			multiRetrieve(0);
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
		while (outstandingRequests.size() > numPackets) {
			try {
				// Receive the next response
				singleRetrieve();
			} catch (SocketTimeoutException e) {
				handleReceiveTimeout();
			}
		}
	}

	private void singleRetrieve() throws IOException {
		// Receive the next response
		log.debug("waiting for message... timeout of {}", packetTimeout);
		SCPResultMessage msg = connection.receiveSCPResponse(packetTimeout);
		if (log.isDebugEnabled()) {
			log.debug("received message {} with seq num {}", msg.getResult(),
					msg.getSequenceNumber());
		}
		Request<?> req = msg.pickRequest(outstandingRequests);

		// Only process responses which have matching requests
		if (req == null) {
			log.info("discarding message with unknown sequence number: {}",
					msg.getSequenceNumber());
			log.debug("current waiting on requests with seq's ");
			for (int seq : outstandingRequests.keySet()) {
				log.debug("{}", seq);
			}
			return;
		}

		// If the response can be retried, retry it
		if (msg.isRetriable()) {
			try {
				resend(req, msg.getResult(), msg.getSequenceNumber());
				numRetryCodeResent++;
			} catch (SocketTimeoutException e) {
				throw e;
			} catch (Exception e) {
				log.debug("throwing away request {} coz of {}",
						msg.getSequenceNumber(), e);
				req.handleError(e);
				msg.removeRequest(outstandingRequests);
			}
		} else {
			// No retry is possible - try constructing the result
			try {
				req.parseReceivedResponse(msg);
			} catch (Exception e) {
				req.handleError(e);
			} finally {
				// Remove the sequence from the outstanding responses
				msg.removeRequest(outstandingRequests);
			}
		}
	}

	private void handleReceiveTimeout() {
		numTimeouts++;

		// If there is a timeout, all packets remaining are resent
		BitSet toRemove = new BitSet(SEQUENCE_LENGTH);
		for (int seq : new ArrayList<>(outstandingRequests.keySet())) {
			log.debug("resending seq {}", seq);
			Request<?> req = outstandingRequests.get(seq);
			if (req == null) {
				// Shouldn't happen, but if it does we should nuke it.
				toRemove.set(seq);
				continue;
			}

			try {
				resend(req, REASON_TIMEOUT, seq);
			} catch (Exception e) {
				log.debug("removing seq {}", seq);
				req.handleError(e);
				toRemove.set(seq);
			}
		}
		log.debug("finish resending");

		toRemove.stream().forEach(outstandingRequests::remove);
	}

	private void resend(Request<?> req, Object reason, int seq)
			throws IOException, InterruptedException {
		if (req.retries <= 0) {
			// Report timeouts as timeout exception
			if (req.allTimeoutFailures()) {
				throw new SendTimedOutException(req, packetTimeout, seq);
			}

			// Report any other exception
			throw new SendFailedException(req, numRetries);
		}

		// If the request can be retried, retry it, sleep for 1ms seems
		// to protect against weird errors. So don't remove this sleep.
		sleep(RETRY_DELAY_MS);
		req.resend(reason);
		numResent++;
	}

	/**
	 * Indicates that a request timed out.
	 */
	static class SendTimedOutException extends SocketTimeoutException {
		private static final long serialVersionUID = -7911020002602751941L;

		/**
		 * Instantiate.
		 *
		 * @param req
		 *            The request that timed out.
		 * @param timeout
		 *            The length of timeout, in milliseconds.
		 */
		SendTimedOutException(Request<?> req, int timeout, int seqNum) {
			super(format(
					"Operation %s timed out after %f seconds with seq num %d",
					req.getCommand(), timeout / (double) MSEC_PER_SEC, seqNum));
		}
	}

	/**
	 * Indicates that a request could not be sent.
	 */
	static class SendFailedException extends IOException {
		private static final long serialVersionUID = -5555562816486761027L;

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

	@Override
	public String toString() {
		return "SCPPipe[channels=" + numChannels + ",width="
				+ intermediateChannelWaits + "resendLim" + numRetries
				+ "] (req=" + numRequests + ",outstanding="
				+ outstandingRequests.size() + ",resent=" + numResent
				+ ",restart=" + numRetryCodeResent + ",timeouts=" + numTimeouts
				+ ")";
	}
}
