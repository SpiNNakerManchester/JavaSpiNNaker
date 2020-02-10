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

import uk.ac.manchester.spinnaker.connections.SCPConnection;
import uk.ac.manchester.spinnaker.connections.selectors.ConnectionSelector;
import uk.ac.manchester.spinnaker.messages.scp.CheckOKResponse;
import uk.ac.manchester.spinnaker.messages.scp.SCPRequest;

// TODO refactor this to have the functionality exposed higher up
/**
 * A simple wrapper round the basic underlying connection process system.
 *
 * @author Donal Fellows
 */
class SendSingleSCPCommandProcess
		extends MultiConnectionProcess<SCPConnection> {
	/**
	 * @param connectionSelector
	 *            How to select which connection to use for communication.
	 * @param retryTracker
	 *            Object used to track how many retries were used in an
	 *            operation. May be {@code null} if no suck tracking is
	 *            required.
	 */
	SendSingleSCPCommandProcess(
			ConnectionSelector<SCPConnection> connectionSelector,
			RetryTracker retryTracker) {
		super(connectionSelector, SCP_RETRIES, SCP_TIMEOUT,
				DEFAULT_NUM_CHANNELS, DEFAULT_INTERMEDIATE_CHANNEL_WAITS,
				retryTracker);
	}

	/**
	 * Execute a call of a request and get a response from it.
	 *
	 * @param <T>
	 *            The type of the response.
	 * @param request
	 *            The request to make
	 * @return The response to the request
	 * @throws IOException
	 *             If communications fail.
	 * @throws ProcessException
	 *             If SCAMP on SpiNNaker reports a failure.
	 */
	public <T extends CheckOKResponse> T execute(SCPRequest<T> request)
			throws IOException, ProcessException {
		return synchronousCall(request);
	}
}
