package uk.ac.manchester.spinnaker.processes;

import static java.lang.String.format;
import static java.lang.Thread.sleep;
import static java.util.Collections.synchronizedMap;
import static org.slf4j.LoggerFactory.getLogger;
import static uk.ac.manchester.spinnaker.messages.Constants.BMP_TIMEOUT;
import static uk.ac.manchester.spinnaker.messages.Constants.MS_PER_S;
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

import javax.xml.ws.Holder;

import org.slf4j.Logger;

import uk.ac.manchester.spinnaker.connections.BMPConnection;
import uk.ac.manchester.spinnaker.connections.selectors.ConnectionSelector;
import uk.ac.manchester.spinnaker.machine.HasCoreLocation;
import uk.ac.manchester.spinnaker.messages.bmp.BMPRequest;
import uk.ac.manchester.spinnaker.messages.bmp.BMPRequest.BMPResponse;
import uk.ac.manchester.spinnaker.messages.scp.SCPRequestHeader;
import uk.ac.manchester.spinnaker.messages.scp.SCPResultMessage;
import uk.ac.manchester.spinnaker.processes.Process.Exception;

/**
 * A process for handling communicating with the BMP.
 * <p>
 * Does not inherit from {@link Process} for ugly type reasons.
 *
 * @param <R>
 *            The type of the response; implicit in the type of the request.
 * @author Donal Fellows
 */
public class SendSingleBMPCommandProcess<R extends BMPResponse> {
	private static final Logger log =
			getLogger(SendSingleBMPCommandProcess.class);
	/** How long to wait for a BMP to respond. */
	public static final int DEFAULT_TIMEOUT = (int) (MS_PER_S * BMP_TIMEOUT);
	/**
	 * The default number of times to resend any packet for any reason before an
	 * error is triggered.
	 */
	private static final int DEFAULT_RETRIES = 3;
	private static final int RETRY_SLEEP = 100;

	private final ConnectionSelector<BMPConnection> connectionSelector;
	private final int timeout;
	private BMPRequest<?> errorRequest;
	private Throwable exception;

	/**
	 * @param connectionSelector
	 *            How to select how to communicate.
	 */
	public SendSingleBMPCommandProcess(
			ConnectionSelector<BMPConnection> connectionSelector) {
		this(connectionSelector, DEFAULT_TIMEOUT);
	}

	/**
	 * @param connectionSelector
	 *            How to select how to communicate.
	 * @param timeout
	 *            The timeout on the connection, in milliseconds.
	 */
	public SendSingleBMPCommandProcess(
			ConnectionSelector<BMPConnection> connectionSelector, int timeout) {
		this.timeout = timeout;
		this.connectionSelector = connectionSelector;
	}

	/**
	 * Do a synchronous call of a BMP operation, sending the given message and
	 * completely processing the interaction before returning its response.
	 *
	 * @param request
	 *            The request to send
	 * @return The successful response to the request
	 * @throws IOException
	 *             If the communications fail
	 * @throws Exception
	 *             If the other side responds with a failure code
	 */
	public R execute(BMPRequest<R> request) throws IOException, Exception {
		Holder<R> holder = new Holder<>();
		/*
		 * If no pipeline built yet, build one on the connection selected for
		 * it.
		 */
		RequestPipeline requestPipeline = new RequestPipeline(
				connectionSelector.getNextConnection(request));
		requestPipeline.sendRequest(request,
				response -> holder.value = response);
		requestPipeline.finish();
		if (exception != null) {
			throw new Exception(errorRequest.sdpHeader.getDestination(),
					exception);
		}
		return holder.value;
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
		/** The number of responses outstanding. */
		private int inProgress;
		/** A dictionary of sequence number -> requests in progress. */
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
			private int retries = DEFAULT_RETRIES;

			private Request(BMPRequest<R> request, Consumer<R> callback) {
				this.request = request;
				this.requestData = request.getMessageData(connection.getChip());
				this.callback = callback;
			}

			private void send() throws IOException {
				connection.send(requestData.asReadOnlyBuffer());
			}

			private void resend(String reason) throws IOException {
				retries--;
				retryReason.add(reason);
				send();
			}

			private boolean hasRetries() {
				return retries > 0;
			}

			private boolean allOneReason(String reason) {
				return retryReason.stream().allMatch(r -> reason.equals(r));
			}

			private void received(SCPResultMessage msg)
					throws java.lang.Exception {
				R response = msg.parsePayload(request);
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
		 *            <tt>null</tt> if the response doesn't need to be
		 *            processed.
		 * @throws IOException
		 *             If things go really wrong.
		 */
		private void sendRequest(BMPRequest<R> request, Consumer<R> callback)
				throws IOException {
			// Get the next sequence to be used and store it in the header
			int sequence = request.scpRequestHeader.issueSequenceNumber();

			// Send the request, keeping track of how many are sent
			Request req = new Request(request, callback);
			requests.put(sequence, req);
			req.send();
			inProgress++;
		}

		/**
		 * Indicate the end of the packets to be sent. This must be called to
		 * ensure that all responses are received and handled.
		 *
		 * @throws IOException
		 *             If anything goes wrong with communications.
		 */
		private void finish() throws IOException {
			// While there are still more packets in progress than some
			// threshold
			while (inProgress > 0) {
				try {
					// Receive the next response
					retrieve();
				} catch (SocketTimeoutException e) {
					handleReceiveTimeout();
				}
			}
		}

		private void retrieve() throws IOException {
			// Receive the next response
			SCPResultMessage msg = connection.receiveSCPResponse(timeout);
			Request req = msg.pickRequest(requests);
			if (req == null) {
				// Only process responses which have matching requests
				log.info("discarding message with unknown sequence number: {}",
						msg.getSequenceNumber());
				return;
			}
			inProgress--;

			// If the response can be retried, retry it
			try {
				if (msg.isRetriable()) {
					sleep(RETRY_SLEEP);
					resend(req, msg.getResult());
				} else {
					// No retry is possible. Try constructing the result
					req.received(msg);
					// Remove the sequence from the outstanding responses
					msg.removeRequest(requests);
				}
			} catch (java.lang.Exception e) {
				errorRequest = req.request;
				exception = e;
				msg.removeRequest(requests);
			}
		}

		private void handleReceiveTimeout() {
			// If there is a timeout, all packets remaining are resent
			BitSet toRemove = new BitSet(SEQUENCE_LENGTH);
			for (int seq : new ArrayList<>(requests.keySet())) {
				Request req = requests.get(seq);
				if (req == null) {
					// Shouldn't happen, but if it does we should nuke it.
					toRemove.set(seq);
					continue;
				}

				inProgress--;
				try {
					resend(req, "timeout");
				} catch (java.lang.Exception e) {
					errorRequest = req.request;
					exception = e;
					toRemove.set(seq);
				}
			}

			toRemove.stream().forEach(seq -> requests.remove(seq));
		}

		private void resend(Request req, Object reason) throws IOException {
			if (!req.hasRetries()) {
				// Report timeouts as timeout exception
				if (req.allOneReason("timeout")) {
					throw new SendTimedOutException(
							req.request.scpRequestHeader, timeout);
				}

				// Report any other exception
				throw new SendFailedException(req.request.scpRequestHeader,
						req.dest(), req.retryReason);
			}

			// If the request can be retried, retry it
			inProgress++;
			req.resend(reason.toString());
		}
	}

	/**
	 * Indicates that message sending timed out.
	 */
	@SuppressWarnings("serial")
	static final class SendTimedOutException extends SocketTimeoutException {
		SendTimedOutException(SCPRequestHeader hdr, int timeout) {
			super(format("Operation %s timed out after %f seconds", hdr.command,
					timeout / MS_PER_S));
		}
	}

	/**
	 * Indicates that message sending failed for various reasons.
	 */
	@SuppressWarnings("serial")
	static final class SendFailedException extends IOException {
		SendFailedException(SCPRequestHeader hdr, HasCoreLocation core,
				List<String> retryReason) {
			super(format(
					"Errors sending request %s to %d,%d,%d over %d retries: %s",
					hdr.command, core.getX(), core.getY(), core.getP(),
					DEFAULT_RETRIES, retryReason));
		}
	}
}
