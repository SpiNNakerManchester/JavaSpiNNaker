/*
 * Copyright (c) 2018-2022 The University of Manchester
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
package uk.ac.manchester.spinnaker.transceiver;

import static java.lang.Thread.sleep;
import static java.util.Collections.synchronizedMap;
import static org.slf4j.LoggerFactory.getLogger;
import static uk.ac.manchester.spinnaker.messages.Constants.BMP_TIMEOUT;
import static uk.ac.manchester.spinnaker.messages.scp.SequenceNumberSource.SEQUENCE_LENGTH;
import static uk.ac.manchester.spinnaker.transceiver.ProcessException.makeInstance;
import static uk.ac.manchester.spinnaker.transceiver.TxrxProcess.RETRY_DELAY_MS;
import static uk.ac.manchester.spinnaker.utils.UnitConstants.MSEC_PER_SEC;

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

import uk.ac.manchester.spinnaker.connections.BMPConnection;
import uk.ac.manchester.spinnaker.connections.ConnectionSelector;
import uk.ac.manchester.spinnaker.machine.HasCoreLocation;
import uk.ac.manchester.spinnaker.messages.bmp.BMPRequest;
import uk.ac.manchester.spinnaker.messages.bmp.BMPRequest.BMPResponse;
import uk.ac.manchester.spinnaker.messages.scp.SCPResultMessage;
import uk.ac.manchester.spinnaker.utils.ValueHolder;

/**
 * A process for handling communicating with the BMP. Note that BMPs have
 * significantly lower limits for the number of messages in flight than SCAMP.
 * <p>
 * Does not inherit from {@link TxrxProcess} for ugly type reasons.
 *
 * @param <R>
 *            The type of the response; implicit in the type of the request.
 * @author Donal Fellows
 */
class BMPCommandProcess<R extends BMPResponse> {
	private static final Logger log = getLogger(BMPCommandProcess.class);

	/*
	 * The weird casts in this class are OK; they're narrowing return types in
	 * accordance with the guarantees we can make (but the compiler can't
	 * prove).
	 */

	/** How long to wait for a BMP to respond. */
	private static final int DEFAULT_TIMEOUT =
			(int) (MSEC_PER_SEC * BMP_TIMEOUT);

	private static final String TIMEOUT_TOKEN = "BMP timed out";

	/** Number of times we retry a BMP action. */
	static final int BMP_RETRIES = 3;

	private final ConnectionSelector<BMPConnection> connectionSelector;

	private final int timeout;

	private final RetryTracker retryTracker;

	private BMPRequest<?> errorRequest;

	private Throwable exception;

	/**
	 * @param connectionSelector
	 *            How to select how to communicate.
	 * @param retryTracker
	 *            Object used to track how many retries were used in an
	 *            operation. May be {@code null} if no suck tracking is
	 *            required.
	 */
	BMPCommandProcess(ConnectionSelector<BMPConnection> connectionSelector,
			RetryTracker retryTracker) {
		this(connectionSelector, DEFAULT_TIMEOUT, retryTracker);
	}

	/**
	 * @param connectionSelector
	 *            How to select how to communicate.
	 * @param timeout
	 *            The timeout on the connection, in milliseconds.
	 * @param retryTracker
	 *            Object used to track how many retries were used in an
	 *            operation. May be {@code null} if no suck tracking is
	 *            required.
	 */
	BMPCommandProcess(ConnectionSelector<BMPConnection> connectionSelector,
			int timeout, RetryTracker retryTracker) {
		this.timeout = timeout;
		this.connectionSelector = connectionSelector;
		this.retryTracker = retryTracker;
	}

	/**
	 * Do a synchronous call of a BMP operation, sending the given message and
	 * completely processing the interaction before returning its response.
	 *
	 * @param <T>
	 *            The real type of the response
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
	@SuppressWarnings("unchecked")
	<T extends R> T execute(BMPRequest<T> request)
			throws IOException, ProcessException, InterruptedException {
		var holder = new ValueHolder<R>();
		/*
		 * If no pipeline built yet, build one on the connection selected for
		 * it.
		 */
		var requestPipeline = new RequestPipeline(
				connectionSelector.getNextConnection(request));
		requestPipeline.sendRequest((BMPRequest<R>) request, holder::setValue);
		requestPipeline.finish();
		if (exception != null) {
			throw makeInstance(errorRequest.sdpHeader.getDestination(),
					exception);
		}
		return (T) holder.getValue();
	}

	/**
	 * Do a synchronous call of a BMP operation, sending the given message and
	 * completely processing the interaction before returning its response.
	 *
	 * @param <T>
	 *            The real type of the response
	 * @param request
	 *            The request to send
	 * @param retries
	 *            The number of times to retry
	 * @return The successful response to the request
	 * @throws IOException
	 *             If the communications fail
	 * @throws ProcessException
	 *             If the other side responds with a failure code
	 * @throws InterruptedException
	 *             If the communications were interrupted.
	 */
	@SuppressWarnings("unchecked")
	<T extends R> T execute(BMPRequest<T> request, int retries)
			throws IOException, ProcessException, InterruptedException {
		var holder = new ValueHolder<R>();
		/*
		 * If no pipeline built yet, build one on the connection selected for
		 * it.
		 */
		var requestPipeline = new RequestPipeline(
				connectionSelector.getNextConnection(request));
		requestPipeline.sendRequest((BMPRequest<R>) request, retries,
				holder::setValue);
		requestPipeline.finish();
		if (exception != null) {
			throw makeInstance(errorRequest.sdpHeader.getDestination(),
					exception);
		}
		return (T) holder.getValue();
	}

	/**
	 * Do a synchronous call of a sequence of BMP operations, sending each of
	 * the given messages and completely processing the interaction before doing
	 * the next one.
	 *
	 * @param <T>
	 *            The real type of the responses
	 * @param requests
	 *            The sequence of requests to send; note that these are handled
	 *            sequentially, as the BMP typically cannot handle parallel
	 *            access. This will be iterated over exactly once.
	 * @return The list of successful responses.
	 * @throws IOException
	 *             If the communications fail
	 * @throws ProcessException
	 *             If the other side responds with a failure code
	 * @throws InterruptedException
	 *             If the communications were interrupted.
	 */
	@SuppressWarnings("unchecked")
	<T extends R> List<T> execute(Iterable<? extends BMPRequest<T>> requests)
			throws IOException, ProcessException, InterruptedException {
		var results = new ArrayList<R>();
		RequestPipeline requestPipeline = null;
		for (var request : requests) {
			if (requestPipeline == null) {
				/*
				 * If no pipeline built yet, build one on the connection
				 * selected for it.
				 */

				requestPipeline = new RequestPipeline(
						connectionSelector.getNextConnection(request));
			}
			requestPipeline.sendRequest((BMPRequest<R>) request, results::add);
			requestPipeline.finish();
		}
		if (exception != null) {
			throw makeInstance(errorRequest.sdpHeader.getDestination(),
					exception);
		}
		return (List<T>) results;
	}

	/**
	 * Do a synchronous call of a sequence of BMP operations, sending each of
	 * the given messages and completely processing the interaction before doing
	 * the next one.
	 *
	 * @param <T>
	 *            The real type of the response
	 * @param requests
	 *            The sequence of requests to send; note that these are handled
	 *            sequentially, as the BMP typically cannot handle parallel
	 *            access. This will be iterated over exactly once.
	 * @param retries
	 *            The number of times to retry
	 * @return The list of successful responses.
	 * @throws IOException
	 *             If the communications fail
	 * @throws ProcessException
	 *             If the other side responds with a failure code
	 * @throws InterruptedException
	 *             If the communications were interrupted.
	 */
	@SuppressWarnings("unchecked")
	<T extends R> List<T> execute(Iterable<? extends BMPRequest<T>> requests,
			int retries)
			throws IOException, ProcessException, InterruptedException {
		var results = new ArrayList<R>();
		RequestPipeline requestPipeline = null;
		for (var request : requests) {
			if (requestPipeline == null) {
				/*
				 * If no pipeline built yet, build one on the connection
				 * selected for it.
				 */

				requestPipeline = new RequestPipeline(
						connectionSelector.getNextConnection(request));
			}
			requestPipeline.sendRequest((BMPRequest<R>) request, retries,
					results::add);
			requestPipeline.finish();
		}
		if (exception != null) {
			throw makeInstance(errorRequest.sdpHeader.getDestination(),
					exception);
		}
		return (List<T>) results;
	}

	/**
	 * Allows a set of BMP requests to be grouped together in a communication
	 * across a number of channels for a given connection.
	 * <p>
	 * This class implements SCP windowing, first suggested by Andrew Mundy.
	 * This extends the idea by having both send and receive windows. These are
	 * represented by the <i>numChannels</i> and the
	 * <i>intermediateChannelWaits</i> parameters respectively. This seems to
	 * help with the timeout issue; when a timeout is received, all requests for
	 * which a reply has not been received can also timeout.
	 *
	 * @author Andrew Mundy
	 * @author Andrew Rowley
	 * @author Donal Fellows
	 */
	private final class RequestPipeline {
		/** The connection over which the communication is to take place. */
		private BMPConnection connection;

		/** A dictionary of sequence number &rarr; requests in progress. */
		private final Map<Integer, Request> requests =
				synchronizedMap(new HashMap<>());

		/** Per message record. */
		private final class Request {
			/** request in progress. */
			private final BMPRequest<R> request;

			/** payload of request in progress. */
			private final ByteBuffer requestData;

			/** callback function for response. */
			private final Consumer<R> callback;

			/** retry reason. */
			private final List<String> retryReason = new ArrayList<>();

			/** number of retries for the packet. */
			private int retries = BMP_RETRIES;

			private Request(BMPRequest<R> request, Consumer<R> callback) {
				this.request = request;
				this.requestData = request.getMessageData(connection.getChip());
				this.callback = callback;
			}

			private Request(BMPRequest<R> request, int retries,
					Consumer<R> callback) {
				this(request, callback);
				this.retries = retries;
			}

			private void send() throws IOException {
				connection.send(requestData);
			}

			private void resend(String reason) throws IOException {
				retries--;
				retryReason.add(reason);
				if (retryTracker != null) {
					retryTracker.retryNeeded();
				}
				send();
			}

			private boolean hasRetries() {
				return retries > 0;
			}

			private boolean allTimeoutFailures() {
				return retryReason.stream().allMatch(TIMEOUT_TOKEN::equals);
			}

			private void parseReceivedResponse(SCPResultMessage msg)
					throws Exception {
				var response = msg.parsePayload(request);
				if (callback != null) {
					callback.accept(response);
				}
			}

			private HasCoreLocation dest() {
				return request.sdpHeader.getDestination();
			}
		}

		/**
		 * Create a request handling pipeline.
		 *
		 * @param connection
		 *            The connection over which the communication is to take
		 *            place.
		 */
		private RequestPipeline(BMPConnection connection) {
			this.connection = connection;
		}

		/**
		 * Add a BMP request to the set to be sent.
		 *
		 * @param request
		 *            The BMP request to be sent
		 * @param callback
		 *            A callback function to call when the response has been
		 *            received; takes an SCPResponse as a parameter, or a
		 *            {@code null} if the response doesn't need to be processed.
		 * @throws IOException
		 *             If things go really wrong.
		 */
		private void sendRequest(BMPRequest<R> request, Consumer<R> callback)
				throws IOException {
			// Get the next sequence to be used and store it in the header
			int sequence = request.scpRequestHeader
					.issueSequenceNumber(requests.keySet());

			// Send the request, keeping track of how many are sent
			var req = new Request(request, callback);
			if (requests.put(sequence, req) != null) {
				throw new RuntimeException(
						"duplicate sequence number catastrophe");
			}
			req.send();
		}

		/**
		 * Add a BMP request to the set to be sent.
		 *
		 * @param request
		 *            The BMP request to be sent
		 * @param retries
		 *            The number of times to retry on failure.
		 * @param callback
		 *            A callback function to call when the response has been
		 *            received; takes an SCPResponse as a parameter, or a
		 *            {@code null} if the response doesn't need to be processed.
		 * @throws IOException
		 *             If things go really wrong.
		 */
		private void sendRequest(BMPRequest<R> request, int retries,
				Consumer<R> callback) throws IOException {
			// Get the next sequence to be used and store it in the header
			int sequence = request.scpRequestHeader
					.issueSequenceNumber(requests.keySet());

			// Send the request, keeping track of how many are sent
			var req = new Request(request, retries, callback);
			if (requests.put(sequence, req) != null) {
				throw new RuntimeException(
						"duplicate sequence number catastrophe");
			}
			req.send();
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
		private void finish() throws IOException, InterruptedException {
			// While there are still more packets in progress than some
			// threshold
			while (!requests.isEmpty()) {
				try {
					// Receive the next response
					retrieve();
				} catch (SocketTimeoutException e) {
					handleReceiveTimeout();
				}
			}
		}

		private void retrieve() throws IOException, InterruptedException {
			// Receive the next response
			var msg = connection.receiveSCPResponse(timeout);
			var req = msg.pickRequest(requests);
			if (req == null) {
				// Only process responses which have matching requests
				log.info("discarding message with unknown sequence number: {}",
						msg.getSequenceNumber());
				log.info("discarded message: {}", msg);
				return;
			}

			// If the response can be retried, retry it
			try {
				if (msg.isRetriable()) {
					sleep(RETRY_DELAY_MS);
					resend(req, msg.getResult());
				} else {
					// No retry is possible. Try constructing the result
					req.parseReceivedResponse(msg);
					// Remove the sequence from the outstanding responses
					msg.removeRequest(requests);
				}
			} catch (InterruptedException e) {
				throw e;
			} catch (Exception e) {
				errorRequest = req.request;
				exception = e;
				msg.removeRequest(requests);
			}
		}

		private void handleReceiveTimeout() {
			// If there is a timeout, all packets remaining are resent
			var toRemove = new BitSet(SEQUENCE_LENGTH);
			for (int seq : List.copyOf(requests.keySet())) {
				var req = requests.get(seq);
				if (req == null) {
					// Shouldn't happen, but if it does we should nuke it.
					toRemove.set(seq);
					continue;
				}

				try {
					resend(req, TIMEOUT_TOKEN);
				} catch (Exception e) {
					errorRequest = req.request;
					exception = e;
					toRemove.set(seq);
				}
			}

			toRemove.stream().forEach(requests::remove);
		}

		private void resend(Request req, Object reason) throws IOException {
			if (!req.hasRetries()) {
				// Report timeouts as timeout exception
				if (req.allTimeoutFailures()) {
					throw new BMPSendTimedOutException(
							req.request, timeout);
				}

				// Report any other exception
				throw new BMPSendFailedException(req.request,
						req.dest(), req.retryReason);
			}

			// If the request can be retried, retry it
			req.resend(reason.toString());
		}
	}
}
