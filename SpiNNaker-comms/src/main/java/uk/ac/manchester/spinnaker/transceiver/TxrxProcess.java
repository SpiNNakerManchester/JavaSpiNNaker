/*
 * Copyright (c) 2018 The University of Manchester
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

import static java.util.Objects.nonNull;
import static uk.ac.manchester.spinnaker.connections.SCPRequestPipeline.SCP_RETRIES;
import static uk.ac.manchester.spinnaker.connections.SCPRequestPipeline.SCP_TIMEOUT;
import static uk.ac.manchester.spinnaker.transceiver.ProcessException.makeInstance;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import uk.ac.manchester.spinnaker.connections.ConnectionSelector;
import uk.ac.manchester.spinnaker.connections.SCPConnection;
import uk.ac.manchester.spinnaker.connections.SCPRequestPipeline;
import uk.ac.manchester.spinnaker.connections.model.AsyncCommsTask;
import uk.ac.manchester.spinnaker.messages.scp.CheckOKResponse;
import uk.ac.manchester.spinnaker.messages.scp.NoResponse;
import uk.ac.manchester.spinnaker.messages.scp.SCPRequest;
import uk.ac.manchester.spinnaker.utils.ValueHolder;

/**
 * A process for talking to SpiNNaker efficiently that uses multiple connections
 * in communication (if appropriate).
 */
class TxrxProcess {
	/** The default for the number of parallel channels. */
	public static final int DEFAULT_NUM_CHANNELS = 8;

	/** The default for the number of instantaneously active channels. */
	public static final int DEFAULT_INTERMEDIATE_CHANNEL_WAITS = 7;

	private final int numWaits;

	private final int numChannels;

	private final int numRetries;

	/**
	 * How to select how to communicate.
	 */
	private final ConnectionSelector<? extends SCPConnection> selector;

	private final Map<SCPConnection, AsyncCommsTask> requestPipelines;

	private final int timeout;

	private final RetryTracker retryTracker;

	private SCPRequest<?> errorRequest;

	private Throwable exception;

	/**
	 * @param connectionSelector
	 *            How to select how to communicate.
	 * @param retryTracker
	 *            Object used to track how many retries were used in an
	 *            operation. May be {@code null} if no suck tracking is
	 *            required.
	 */
	TxrxProcess(ConnectionSelector<? extends SCPConnection> connectionSelector,
			RetryTracker retryTracker) {
		this(connectionSelector, SCP_RETRIES, SCP_TIMEOUT, DEFAULT_NUM_CHANNELS,
				DEFAULT_INTERMEDIATE_CHANNEL_WAITS, retryTracker);
	}

	/**
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
	TxrxProcess(ConnectionSelector<? extends SCPConnection> connectionSelector,
			int numRetries, int timeout, int numChannels,
			int intermediateChannelWaits, RetryTracker retryTracker) {
		this.requestPipelines = new HashMap<>();
		this.numRetries = numRetries;
		this.timeout = timeout;
		this.numChannels = numChannels;
		this.numWaits = intermediateChannelWaits;
		this.selector = connectionSelector;
		this.retryTracker = retryTracker;
	}

	/**
	 * Manufacture a pipeline to talk to a connection using the configured
	 * pipeline parameters.
	 *
	 * @param conn
	 *            The connection.
	 * @return The pipeline instance.
	 */
	private AsyncCommsTask newPipelineInstance(SCPConnection conn) {
		return new SCPRequestPipeline(conn, numChannels, numWaits, numRetries,
				timeout, retryTracker);
	}

	/**
	 * Manufacture a pipeline to handle a request using the configured pipeline
	 * parameters. Reuses an existing pipeline if it can.
	 *
	 * @param request
	 *            The request it will handle.
	 * @return The pipeline instance.
	 */
	private AsyncCommsTask getPipeline(SCPRequest<?> request) {
		return requestPipelines.computeIfAbsent(
				selector.getNextConnection(request),
				this::newPipelineInstance);
	}

	/**
	 * Put the state in such a way that it definitely isn't recording an error.
	 */
	private void resetState() {
		this.errorRequest = null;
		this.exception = null;
	}

	/**
	 * A default handler for exceptions that arranges for them to be rethrown
	 * later.
	 *
	 * @param request
	 *            The request that caused the exception
	 * @param exception
	 *            The exception that was causing the problem
	 */
	protected final void receiveError(SCPRequest<?> request,
			Throwable exception) {
		this.errorRequest = request;
		this.exception = exception;
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
	 */
	protected final void finishBatch() throws ProcessException, IOException {
		for (var pipe : requestPipelines.values()) {
			pipe.finish();
		}
		if (nonNull(exception)) {
			var hdr = errorRequest.sdpHeader;
			throw makeInstance(hdr.getDestination(), exception);
		}
	}

	/**
	 * Send a request. The actual payload of the response to this request is to
	 * be considered to be uninteresting provided it doesn't indicate a failure.
	 *
	 * @param <Resp>
	 *            The type of response expected to the request.
	 * @param request
	 *            The request to send.
	 * @throws IOException
	 *             If sending fails.
	 */
	protected final <Resp extends CheckOKResponse> void sendRequest(
			SCPRequest<Resp> request) throws IOException {
		sendRequest(request, null);
	}

	/**
	 * Send a request.
	 *
	 * @param <Resp>
	 *            The type of response expected to the request.
	 * @param request
	 *            The request to send.
	 * @param callback
	 *            The callback that handles the request's response. If
	 *            {@code null}, the response to the message will be constructed
	 *            (i.e., checked for any failures) and then discarded.
	 * @throws IOException
	 *             If sending fails.
	 */
	protected final <Resp extends CheckOKResponse> void sendRequest(
			SCPRequest<Resp> request, Consumer<Resp> callback)
			throws IOException {
		getPipeline(request).sendRequest(request, callback, this::receiveError);
	}

	/**
	 * Do a synchronous call of an SCP operation, sending the given message and
	 * completely processing the interaction before returning its response.
	 *
	 * @param <Resp>
	 *            The type of the response; implicit in the type of the request.
	 * @param request
	 *            The request to send
	 * @return The successful response to the request
	 * @throws IOException
	 *             If the communications fail
	 * @throws ProcessException
	 *             If the other side responds with a failure code
	 */
	protected final <Resp extends CheckOKResponse> Resp synchronousCall(
			SCPRequest<Resp> request) throws IOException, ProcessException {
		var holder = new ValueHolder<Resp>();
		resetState();
		sendRequest(request, holder::setValue);
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
	 */
	protected final void sendOneWayRequest(
			SCPRequest<NoResponse> request) throws IOException {
		getPipeline(request).sendOneWayRequest(request);
	}
}
