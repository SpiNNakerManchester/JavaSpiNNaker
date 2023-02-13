/*
 * Copyright (c) 2018-2023 The University of Manchester
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package uk.ac.manchester.spinnaker.transceiver;

import static java.lang.Integer.getInteger;
import static java.lang.Short.toUnsignedInt;
import static java.lang.String.format;
import static java.lang.System.nanoTime;
import static java.lang.Thread.sleep;
import static java.util.Collections.synchronizedMap;
import static java.util.Objects.requireNonNull;
import static org.slf4j.LoggerFactory.getLogger;
import static uk.ac.manchester.spinnaker.messages.Constants.SCP_RETRY_DEFAULT;
import static uk.ac.manchester.spinnaker.messages.Constants.SCP_TIMEOUT_DEFAULT;
import static uk.ac.manchester.spinnaker.messages.scp.SequenceNumberSource.SEQUENCE_LENGTH;
import static uk.ac.manchester.spinnaker.transceiver.ProcessException.makeInstance;
import static uk.ac.manchester.spinnaker.utils.UnitConstants.MSEC_PER_SEC;
import static uk.ac.manchester.spinnaker.utils.WaitUtils.waitUntil;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.function.Consumer;

import org.slf4j.Logger;

import com.google.errorprone.annotations.concurrent.GuardedBy;

import uk.ac.manchester.spinnaker.connections.ConnectionSelector;
import uk.ac.manchester.spinnaker.connections.SCPConnection;
import uk.ac.manchester.spinnaker.machine.HasCoreLocation;
import uk.ac.manchester.spinnaker.messages.scp.CheckOKResponse;
import uk.ac.manchester.spinnaker.messages.scp.CommandCode;
import uk.ac.manchester.spinnaker.messages.scp.ConnectionAwareMessage;
import uk.ac.manchester.spinnaker.messages.scp.EmptyResponse;
import uk.ac.manchester.spinnaker.messages.scp.NoResponse;
import uk.ac.manchester.spinnaker.messages.scp.PayloadedResponse;
import uk.ac.manchester.spinnaker.messages.scp.SCPRequest;
import uk.ac.manchester.spinnaker.messages.scp.SCPResponse;
import uk.ac.manchester.spinnaker.messages.scp.SCPResult;
import uk.ac.manchester.spinnaker.messages.scp.SCPResultMessage;
import uk.ac.manchester.spinnaker.utils.ValueHolder;

/**
 * A process for talking to SpiNNaker efficiently that uses multiple connections
 * in communication (if appropriate).
 */
public class TxrxProcess {
	/** The default for the number of parallel channels. */
	protected static final int DEFAULT_NUM_CHANNELS = 8;

	/** The default for the number of instantaneously active channels. */
	protected static final int DEFAULT_INTERMEDIATE_CHANNEL_WAITS = 7;

	/**
	 * The name of a <em>system property</em> that can override the default
	 * timeouts. If specified as an integer, it gives the number of
	 * milliseconds to wait before timing out a communication.
	 */
	private static final String TIMEOUT_PROPERTY = "spinnaker.scp_timeout";

	/**
	 * The name of a <em>system property</em> that can override the default
	 * retries. If specified as an integer, it gives the number of retries
	 * to perform (on timeout of receiving a reply) before timing out a
	 * communication.
	 */
	private static final String RETRY_PROPERTY = "spinnaker.scp_retries";

	private static final Logger log = getLogger(RequestPipeline.class);

	/**
	 * The default number of outstanding responses to wait for before
	 * continuing sending requests.
	 */
	protected static final int DEFAULT_INTERMEDIATE_TIMEOUT_WAITS = 0;

	/**
	 * The default number of times to resend any packet for any reason
	 * before an error is triggered.
	 */
	protected static final int SCP_RETRIES;

	/**
	 * How long to wait between retries, in milliseconds.
	 */
	protected static final int RETRY_DELAY_MS = 1;

	private static final String REASON_TIMEOUT = "timeout";

	/**
	 * Packet minimum send interval, in <em>nanoseconds</em>.
	 */
	private static final int INTER_SEND_INTERVAL_NS = 60000;

	/** The default for the timeout (in ms). */
	protected static final int SCP_TIMEOUT;

	static {
		// Read system properties
		SCP_TIMEOUT = getInteger(TIMEOUT_PROPERTY, SCP_TIMEOUT_DEFAULT);
		SCP_RETRIES = getInteger(RETRY_PROPERTY, SCP_RETRY_DEFAULT);
	}

	/**
	 * The number of outstanding responses to wait for before continuing
	 * sending requests.
	 */
	final int numWaits;

	/** The number of requests to send before checking for responses. */
	final int numChannels;

	/**
	 * The number of times to resend any packet for any reason before an
	 * error is triggered.
	 */
	final int numRetries;

	/**
	 * The number of elapsed milliseconds after sending a packet before it
	 * is considered a timeout.
	 */
	final int packetTimeout;

	/**
	 * How to select how to communicate.
	 */
	private final ConnectionSelector<? extends SCPConnection> selector;

	private final Map<SCPConnection, RequestPipeline> requestPipelines;

	/**
	 * The API of a single request.
	 *
	 * @author Donal Fellows
	 */
	private interface Req {
		/**
		 * Tests whether the reasons for resending are consistently
		 * timeouts.
		 *
		 * @return True if all reasons are timeouts.
		 */
		boolean allTimeoutFailures();

		/**
		 * Get the command being sent in the request.
		 *
		 * @return The request's SCP command.
		 */
		CommandCode getCommand();

		/**
		 * Which core is the destination of the request?
		 *
		 * @return The core location.
		 */
		HasCoreLocation getDestination();

		/**
		 * Number of retries remaining for the packet.
		 *
		 * @return The number of retries that may still be performed.
		 */
		int getRetries();

		/**
		 * Get the list of reasons why a message was retried.
		 *
		 * @return The retry reasons.
		 */
		List<String> getRetryReasons();

		/**
		 * Report a failure to the process.
		 *
		 * @param exception
		 *            The problem that is being reported.
		 */
		void handleError(Exception exception);

		/**
		 * Handle the reception of a message.
		 *
		 * @param msg
		 *            the content of the message, in a little-endian buffer.
		 * @throws Exception
		 *             If something goes wrong.
		 */
		void parseReceivedResponse(SCPResultMessage msg) throws Exception;

		/**
		 * Send the request again.
		 *
		 * @param reason
		 *            Why the request is being sent again.
		 * @throws IOException
		 *             If the connection throws.
		 */
		void resend(Object reason) throws IOException;

		/**
		 * Send the request.
		 *
		 * @throws IOException
		 *             If the connection throws.
		 */
		void send() throws IOException;
	}

	/**
	 * The maps of outstanding requests on each connection.
	 */
	private static final Map<SCPConnection,
			Map<Integer, Req>> OUTSTANDING_REQUESTS = new WeakHashMap<>();

	/**
	 * An object used to track how many retries have been done, or
	 * {@code null} if no such tracking is required.
	 */
	private final RetryTracker retryTracker;

	// TODO handle multiple failures
	private Failure failure;

	/**
	 * @param <Conn>
	 *            The type of connection.
	 * @param connectionSelector
	 *            How to select how to communicate.
	 * @param retryTracker
	 *            Object used to track how many retries were used in an
	 *            operation. May be {@code null} if no suck tracking is
	 *            required.
	 */
	protected <Conn extends SCPConnection> TxrxProcess(
			ConnectionSelector<Conn> connectionSelector,
			RetryTracker retryTracker) {
		this(connectionSelector, SCP_RETRIES, SCP_TIMEOUT, DEFAULT_NUM_CHANNELS,
				DEFAULT_INTERMEDIATE_CHANNEL_WAITS, retryTracker);
	}

	/**
	 * @param <Conn>
	 *            The type of connection.
	 * @param connectionSelector
	 *            How to select how to communicate.
	 * @param numRetries
	 *            The number of times to retry a communication.
	 * @param timeout
	 *            The timeout (in ms) for the communication.
	 * @param numChannels
	 *            The number of parallel communications to support
	 * @param intermediateChannelWaits
	 *            How many parallel communications to launch at once. (??)
	 * @param retryTracker
	 *            Object used to track how many retries were used in an
	 *            operation. May be {@code null} if no suck tracking is
	 *            required.
	 */
	protected <Conn extends SCPConnection> TxrxProcess(
			ConnectionSelector<Conn> connectionSelector,
			int numRetries, int timeout, int numChannels,
			int intermediateChannelWaits, RetryTracker retryTracker) {
		this.requestPipelines = new HashMap<>();
		this.numRetries = numRetries;
		this.packetTimeout = timeout;
		this.numChannels = numChannels;
		this.numWaits = intermediateChannelWaits;
		this.selector = Objects.requireNonNull(connectionSelector);
		this.retryTracker = Objects.requireNonNull(retryTracker);
	}

	/**
	 * Manufacture a pipeline to handle a request using the configured pipeline
	 * parameters. Reuses an existing pipeline if it can.
	 *
	 * @param request
	 *            The request it will handle.
	 * @return The pipeline instance.
	 */
	private RequestPipeline pipeline(SCPRequest<?> request) {
		return requestPipelines.computeIfAbsent(
				selector.getNextConnection(request), RequestPipeline::new);
	}

	/**
	 * Put the state in such a way that it definitely isn't recording an error.
	 */
	private void resetFailureState() {
		this.failure = null;
	}

	/**
	 * Wait for all outstanding requests sent by this process to receive replies
	 * or time out. Then test if an error occurred on the SpiNNaker side, and
	 * throw a process exception it if it did.
	 *
	 * @throws IOException
	 *             If communications fail.
	 * @throws ProcessException
	 *             an exception that wraps the original exception that occurred.
	 * @throws InterruptedException
	 *             If the communications were interrupted.
	 */
	protected final void finishBatch()
			throws ProcessException, IOException, InterruptedException {
		for (var pipe : requestPipelines.values()) {
			pipe.finish();
		}
		if (failure != null) {
			var hdr = failure.req.sdpHeader;
			throw makeInstance(hdr.getDestination(), failure.exn);
		}
	}

	/**
	 * Send a request. The actual payload of the response to this request is to
	 * be considered to be uninteresting provided it doesn't indicate a failure.
	 * In particular, the response is a {@link EmptyResponse}.
	 *
	 * @param request
	 *            The request to send.
	 * @throws IOException
	 *             If sending fails.
	 * @throws InterruptedException
	 *             If communications are interrupted while preparing to send.
	 */
	protected final void sendRequest(SCPRequest<EmptyResponse> request)
			throws IOException, InterruptedException {
		pipeline(request).send(request, null);
	}

	/**
	 * Send a request and handle the response.
	 *
	 * @param <Resp>
	 *            The type of response expected to the request.
	 * @param request
	 *            The request to send.
	 * @param callback
	 *            The callback that handles the request's response.
	 * @throws IOException
	 *             If sending fails.
	 * @throws InterruptedException
	 *             If communications are interrupted.
	 */
	protected final <Resp extends CheckOKResponse> void sendRequest(
			SCPRequest<Resp> request, Consumer<Resp> callback)
			throws IOException, InterruptedException {
		pipeline(request).send(request,
				requireNonNull(callback, "callback must be non-null"));
	}

	/**
	 * Send a request for a response with a payload.
	 *
	 * @param <T>
	 *            The type of parsed payload expected.
	 * @param <R>
	 *            The type of response expected to the request.
	 * @param request
	 *            The request to send.
	 * @param callback
	 *            The callback that handles the parsed payload.
	 * @throws IOException
	 *             If sending fails.
	 * @throws InterruptedException
	 *             If communications are interrupted.
	 */
	protected final <T, R extends PayloadedResponse<T, ?>> void sendGet(
			SCPRequest<R> request, Consumer<T> callback)
			throws IOException, InterruptedException {
		pipeline(request).send(request, resp -> callback.accept(resp.get()));
	}

	/**
	 * Do a synchronous call of an SCP operation, sending the given message and
	 * completely processing the interaction before returning its response.
	 *
	 * @param request
	 *            The request to send
	 * @throws IOException
	 *             If the communications fail
	 * @throws ProcessException
	 *             If the other side responds with a failure code
	 * @throws InterruptedException
	 *             If the communications were interrupted.
	 */
	protected final void call(SCPRequest<EmptyResponse> request)
					throws IOException, ProcessException, InterruptedException {
		var holder = new ValueHolder<EmptyResponse>();
		resetFailureState();
		sendRequest(request, holder::setValue);
		finishBatch();
		assert holder.getValue().result == SCPResult.RC_OK;
	}

	/**
	 * Do a synchronous call of an SCP operation, sending the given message and
	 * completely processing the interaction before returning its parsed
	 * payload.
	 *
	 * @param <T>
	 *            The type of the payload of the response.
	 * @param <R>
	 *            The type of the response; implicit in the type of the request.
	 * @param request
	 *            The request to send
	 * @return The successful response to the request
	 * @throws IOException
	 *             If the communications fail
	 * @throws ProcessException
	 *             If the other side responds with a failure code
	 * @throws InterruptedException
	 *             If the communications were interrupted.
	 */
	protected final <T,
			R extends PayloadedResponse<T, ?>> T retrieve(SCPRequest<R> request)
					throws IOException, ProcessException, InterruptedException {
		var holder = new ValueHolder<T>();
		resetFailureState();
		sendGet(request, holder::setValue);
		finishBatch();
		return holder.getValue();
	}

	/**
	 * Send a one-way request. One way requests do not need to be finished.
	 *
	 * @param request
	 *            The request to send. <em>Must</em> be a one-way request!
	 * @throws IOException
	 *             If sending fails.
	 * @throws InterruptedException
	 *             If communications are interrupted while preparing to send.
	 */
	protected final void sendOneWayRequest(SCPRequest<NoResponse> request)
			throws IOException, InterruptedException {
		pipeline(request).send(request);
	}

	/**
	 * States that a particular request failed with a particular exception. The
	 * request should not be retried once this has been generated.
	 */
	// TODO make into a record once on a new enough language profile
	private static class Failure {
		private final SCPRequest<?> req;

		private final Exception exn;

		Failure(SCPRequest<?> req, Exception exn) {
			this.req = req;
			this.exn = exn;
		}
	}

	/**
	 * Allows a set of SCP requests to be grouped together in a communication
	 * across a number of channels for a given connection.
	 * <p>
	 * This class implements an SCP windowing, first suggested by Andrew Mundy.
	 * This extends the idea by having both send and receive windows. These are
	 * represented by the {@link TxrxProcess#numChannels} and the
	 * {@link TxrxProcess#numWaits} fields of the enclosing class respectively.
	 * This seems to help with the timeout issue; when a timeout is received,
	 * all requests for which a reply has not been received can also timeout.
	 *
	 * @author Andrew Mundy
	 * @author Andrew Rowley
	 * @author Donal Fellows
	 */
	class RequestPipeline {
		/** The connection over which the communication is to take place. */
		private final SCPConnection connection;

		/** The number of requests issued to this pipeline. */
		private int numRequests;

		/** The number of packets that have been resent. */
		private int numResent;

		/** The number of retries due to restartable errors. */
		private int numRetryCodeResent;

		/** The number of timeouts that occurred. */
		private int numTimeouts;

		/** A dictionary of sequence number &rarr; requests in progress. */
		@GuardedBy("itself")
		private final Map<Integer, Req> outstandingRequests;

		private long nextSendTime = 0;

		/**
		 * Per message record.
		 *
		 * @param <T>
		 *            The type of response expected to the request in the
		 *            message.
		 */
		private final class Request<T extends SCPResponse> implements Req {
			/** Request in progress. */
			private final SCPRequest<T> request;

			/** Payload of request in progress. */
			private final ByteBuffer requestData;

			private short seq;

			/** Callback function for response. */
			private final Consumer<T> callback;

			/** Retry reasons. */
			private final List<String> retryReason;

			/** Number of retries remaining for the packet. */
			private int retries;

			/**
			 * Make a record.
			 *
			 * @param request
			 *            The request.
			 * @param callback
			 *            The success callback.
			 */
			private Request(SCPRequest<T> request, Consumer<T> callback) {
				this.request = request;
				this.requestData = request.getMessageData(connection.getChip());
				this.seq = request.scpRequestHeader.getSequence();
				this.callback = callback;
				retryReason = new ArrayList<>();
				retries = numRetries;
			}

			@Override
			public void send() throws IOException {
				if (waitUntil(nextSendTime)) {
					throw new InterruptedIOException(
							"interrupted while waiting to send");
				}
				switch (request.sdpHeader.getFlags()) {
				case REPLY_EXPECTED:
				case REPLY_EXPECTED_NO_P2P:
					connection.send(requestData, seq);
					break;
				default:
					connection.send(requestData);
				}
				nextSendTime = nanoTime() + INTER_SEND_INTERVAL_NS;
			}

			@Override
			public void resend(Object reason) throws IOException {
				retries--;
				retryReason.add(reason.toString());
				if (retryTracker != null) {
					retryTracker.retryNeeded();
				}
				// TODO reissue sequence number?
				send();
			}

			@Override
			public boolean allTimeoutFailures() {
				return retryReason.stream().allMatch(REASON_TIMEOUT::equals);
			}

			@Override
			public void parseReceivedResponse(SCPResultMessage msg)
					throws Exception {
				var response = msg.parsePayload(request);
				if (callback != null) {
					callback.accept(response);
				}
			}

			@Override
			public HasCoreLocation getDestination() {
				return request.sdpHeader.getDestination();
			}

			@Override
			public void handleError(Exception exception) {
				failure = new Failure(request, exception);
			}

			@Override
			public CommandCode getCommand() {
				return request.scpRequestHeader.command;
			}

			@Override
			public int getRetries() {
				return retries;
			}

			@Override
			public List<String> getRetryReasons() {
				return retryReason;
			}
		}

		/**
		 * Create a request handling pipeline.
		 *
		 * @param connection
		 *            The connection over which the communication is to take
		 *            place
		 */
		RequestPipeline(SCPConnection connection) {
			this.connection = connection;
			synchronized (OUTSTANDING_REQUESTS) {
				outstandingRequests = OUTSTANDING_REQUESTS.computeIfAbsent(
						connection, __ -> synchronizedMap(new HashMap<>()));
			}
		}

		// Various accessors for outstandingRequests to handle locking right

		private int numOutstandingRequests() {
			synchronized (outstandingRequests) {
				return outstandingRequests.size();
			}
		}

		private Req getRequestForResult(SCPResultMessage msg) {
			synchronized (outstandingRequests) {
				return msg.pickRequest(outstandingRequests);
			}
		}

		private void removeRequest(SCPResultMessage msg) {
			synchronized (outstandingRequests) {
				msg.removeRequest(outstandingRequests);
			}
		}

		private Req getRequestForSeq(int seq) {
			synchronized (outstandingRequests) {
				return outstandingRequests.get(seq);
			}
		}

		private List<Integer> listOutstandingSeqs() {
			synchronized (outstandingRequests) {
				return List.copyOf(outstandingRequests.keySet());
			}
		}

		private void removeManySeqs(BitSet toRemove) {
			synchronized (outstandingRequests) {
				for (var seq : toRemove.stream().toArray()) {
					outstandingRequests.remove(seq);
				}
			}
		}

		/**
		 * Add an SCP request to the set to be sent. The request expects a
		 * reply.
		 *
		 * @param <T>
		 *            The type of response expected to the request.
		 * @param request
		 *            The SCP request to be sent
		 * @param callback
		 *            A callback function to call when the response has been
		 *            received; takes an SCPResponse as a parameter, or a
		 *            {@code null} if the response doesn't need to be processed.
		 * @throws IOException
		 *             If things go really wrong.
		 * @throws InterruptedException
		 *             If communications are interrupted (prior to sending).
		 */
		<T extends CheckOKResponse> void send(SCPRequest<T> request,
				Consumer<T> callback) throws IOException, InterruptedException {
			// If all the channels are used, start to receive packets
			while (numOutstandingRequests() >= numChannels) {
				multiRetrieve(numWaits);
			}

			// Send the request
			registerRequest(request, callback).send();
		}

		/**
		 * Update the packet and store required details.
		 *
		 * @param <T>
		 *            The type of response expected to the request.
		 * @param request
		 *            The SCP request to be sent
		 * @param callback
		 *            A callback function to call when the response has been
		 *            received; takes an SCPResponse as a parameter, or a
		 *            {@code null} if the response doesn't need to be processed.
		 * @return The prepared and registered (but unsent) request.
		 * @throws DuplicateSequenceNumberException
		 *             If we couldn't mint a sequence number. Really shouldn't
		 *             happen!
		 */
		private <T extends CheckOKResponse> Request<T> registerRequest(
				SCPRequest<T> request, Consumer<T> callback) {
			if (request instanceof ConnectionAwareMessage) {
				ConnectionAwareMessage cam = (ConnectionAwareMessage) request;
				cam.setConnection(connection);
			}
			synchronized (outstandingRequests) {
				int sequence = toUnsignedInt(request.scpRequestHeader
						.issueSequenceNumber(outstandingRequests.keySet()));

				var req = new Request<>(request, callback);
				log.debug("sending message with sequence {}", sequence);
				if (outstandingRequests.put(sequence, req) != null) {
					throw new DuplicateSequenceNumberException();
				}
				numRequests++;
				return req;
			}
		}

		/**
		 * Update the packet but don't remember it; it's a one-shot.
		 *
		 * @param request
		 *            The one-way SCP request to be sent
		 * @return The prepared (but unsent) request.
		 */
		private Request<NoResponse> unregisteredRequest(
				SCPRequest<NoResponse> request) {
			// Update the packet with a (non-valuable) sequence number
			request.scpRequestHeader.issueSequenceNumber(Set.of());
			log.debug("sending one-way message");
			return new Request<>(request, null);
		}

		/**
		 * Send a one-way request.
		 *
		 * @param request
		 *            The one-way SCP request to be sent.
		 * @throws IOException
		 *             If things go really wrong.
		 * @throws InterruptedException
		 *             If communications are interrupted (prior to sending).
		 */
		void send(SCPRequest<NoResponse> request)
				throws IOException, InterruptedException {
			// Wait for all current in-flight responses to be received
			finish();

			// Send the request without registering it
			unregisteredRequest(request).send();
		}

		/**
		 * Indicate the end of the packets to be sent. This must be called to
		 * ensure that all responses are received and handled.
		 *
		 * @throws IOException
		 *             If anything goes wrong with communications.
		 * @throws InterruptedException
		 *             If communications are interrupted.
		 */
		void finish() throws IOException, InterruptedException {
			while (numOutstandingRequests() > 0) {
				multiRetrieve(0);
			}
		}

		/**
		 * Receives responses until there are only numPackets responses left.
		 *
		 * @param numPacketsOutstanding
		 *            The number of packets that can remain after running.
		 * @throws IOException
		 *             If anything goes wrong with receiving a packet.
		 * @throws InterruptedException
		 *             If communications are interrupted.
		 */
		private void multiRetrieve(int numPacketsOutstanding)
				throws IOException, InterruptedException {
			// While there are still more packets in progress than some
			// threshold
			while (numOutstandingRequests() > numPacketsOutstanding) {
				try {
					// Receive the next response
					singleRetrieve();
				} catch (SocketTimeoutException e) {
					handleReceiveTimeout();
				}
			}
		}

		private void singleRetrieve() throws IOException, InterruptedException {
			// Receive the next response
			log.debug("waiting for message... timeout of {}", packetTimeout);
			var msg = connection.receiveSCPResponse(packetTimeout);
			if (log.isDebugEnabled()) {
				log.debug("received message {} with seq num {}",
						msg.getResult(), msg.getSequenceNumber());
			}
			var req = getRequestForResult(msg);

			// Only process responses which have matching requests
			if (req == null) {
				log.info("discarding message with unknown sequence number: {}",
						msg.getSequenceNumber());
				if (log.isDebugEnabled()) {
					log.debug("current waiting on requests with seq's ");
					for (int seq : listOutstandingSeqs()) {
						log.debug("{}", seq);
					}
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
					removeRequest(msg);
				}
			} else {
				// No retry is possible - try constructing the result
				try {
					req.parseReceivedResponse(msg);
				} catch (Exception e) {
					req.handleError(e);
				} finally {
					// Remove the sequence from the outstanding responses
					removeRequest(msg);
				}
			}
		}

		private void handleReceiveTimeout() {
			numTimeouts++;

			// If there is a timeout, all packets remaining are resent
			var toRemove = new BitSet(SEQUENCE_LENGTH);
			for (int seq : listOutstandingSeqs()) {
				log.debug("resending seq {}", seq);
				var req = getRequestForSeq(seq);
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

			removeManySeqs(toRemove);
		}

		private void resend(Req req, Object reason, int seq)
				throws IOException, InterruptedException {
			if (req.getRetries() <= 0) {
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

		@Override
		public String toString() {
			return format(
					"ReqPipe(req=%d,outstanding=%d,resent=%d,"
							+ "restart=%d,timeouts=%d)",
					numRequests, numOutstandingRequests(), numResent,
					numRetryCodeResent, numTimeouts);
		}
	}

	/**
	 * Indicates that a request timed out.
	 */
	static class SendTimedOutException extends SocketTimeoutException {
		private static final long serialVersionUID = -7911020002602751941L;

		/**
		 * @param req
		 *            The request that timed out.
		 * @param timeout
		 *            The length of timeout, in milliseconds.
		 */
		SendTimedOutException(Req req, int timeout, int seqNum) {
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
		 * @param req
		 *            The request that timed out.
		 * @param numRetries
		 *            How many attempts to send it were made.
		 */
		SendFailedException(Req req, int numRetries) {
			super(format(
					"Errors sending request %s to %d,%d,%d over %d retries: %s",
					req.getCommand(), req.getDestination().getX(),
					req.getDestination().getY(), req.getDestination().getP(),
					numRetries, req.getRetryReasons()));
		}
	}

	/**
	 * There's a duplicate sequence number! This really shouldn't happen.
	 *
	 * @author Donal Fellows
	 */
	static class DuplicateSequenceNumberException
			extends IllegalThreadStateException {
		private static final long serialVersionUID = -4033792283948201730L;

		DuplicateSequenceNumberException() {
			super("duplicate sequence number catastrophe");
		}
	}
}
