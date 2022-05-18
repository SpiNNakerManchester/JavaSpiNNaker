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

import static uk.ac.manchester.spinnaker.connections.SCPRequestPipeline.SCP_RETRIES;
import static uk.ac.manchester.spinnaker.connections.SCPRequestPipeline.SCP_TIMEOUT;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import uk.ac.manchester.spinnaker.connections.ConnectionSelector;
import uk.ac.manchester.spinnaker.connections.SCPConnection;
import uk.ac.manchester.spinnaker.connections.SCPRequestPipeline;
import uk.ac.manchester.spinnaker.messages.scp.CheckOKResponse;
import uk.ac.manchester.spinnaker.messages.scp.NoResponse;
import uk.ac.manchester.spinnaker.messages.scp.SCPRequest;

/**
 * A process that uses multiple connections in communication.
 *
 * @param <T>
 *            The type of connection used by the process.
 */
abstract class MultiConnectionProcess<T extends SCPConnection> extends Process {
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
	final ConnectionSelector<T> selector;

	private final Map<T, SCPRequestPipeline> requestPipelines;

	private final int timeout;

	private final RetryTracker retryTracker;

	/**
	 * @param connectionSelector
	 *            How to select how to communicate.
	 * @param retryTracker
	 *            Object used to track how many retries were used in an
	 *            operation. May be {@code null} if no suck tracking is
	 *            required.
	 */
	MultiConnectionProcess(ConnectionSelector<T> connectionSelector,
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
	MultiConnectionProcess(ConnectionSelector<T> connectionSelector,
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

	private SCPRequestPipeline getPipeline(T connection) {
		var pipeline = requestPipelines.get(connection);
		if (pipeline == null) {
			pipeline = new SCPRequestPipeline(connection, numChannels, numWaits,
					numRetries, timeout, retryTracker);
			requestPipelines.put(connection, pipeline);
		}
		return pipeline;
	}

	@Override
	protected <R extends CheckOKResponse> void sendRequest(
			SCPRequest<R> request, Consumer<R> callback) throws IOException {
		getPipeline(selector.getNextConnection(request)).sendRequest(request,
				callback, this::receiveError);
	}

	@Override
	protected void sendOneWayRequest(SCPRequest<? extends NoResponse> request)
			throws IOException {
		getPipeline(selector.getNextConnection(request))
				.sendOneWayRequest(request);
	}

	@Override
	protected void finish() throws IOException {
		for (var pipe : requestPipelines.values()) {
			pipe.finish();
		}
	}
}
