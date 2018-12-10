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
package uk.ac.manchester.spinnaker.transceiver.processes;

import java.io.IOException;
import java.util.function.Consumer;

import uk.ac.manchester.spinnaker.messages.scp.SCPRequest;
import uk.ac.manchester.spinnaker.messages.scp.SCPResponse;
import uk.ac.manchester.spinnaker.messages.sdp.SDPHeader;
import uk.ac.manchester.spinnaker.utils.ValueHolder;

/** An abstract process for talking to SpiNNaker efficiently. */
public abstract class Process {
	private SCPRequest<?> errorRequest;

	private Throwable exception;

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
	 * @return Whether an exception is waiting to be thrown.
	 */
	public final boolean isError() {
		return exception != null;
	}

	/**
	 * Test if an error occurred, and throw it if it did.
	 *
	 * @throws ProcessException
	 *             an exception that wraps the original exception that occurred.
	 */
	public final void checkForError() throws ProcessException {
		if (!isError()) {
			return;
		}
		SDPHeader hdr = errorRequest.sdpHeader;
		ProcessException ex =
				new ProcessException(hdr.getDestination(), exception);
		exception = ex;
		throw ex;
	}

	/**
	 * Send a request. The actual payload of the response to this request is to
	 * be considered to be uninteresting provided it doesn't indicate a failure.
	 *
	 * @param <T>
	 *            The type of response expected to the request.
	 * @param request
	 *            The request to send.
	 * @throws IOException
	 *             If sending fails.
	 */
	protected final <T extends SCPResponse> void sendRequest(
			SCPRequest<T> request) throws IOException {
		sendRequest(request, null);
	}

	/**
	 * Send a request.
	 *
	 * @param <T>
	 *            The type of response expected to the request.
	 * @param request
	 *            The request to send.
	 * @param callback
	 *            The callback that handles the request's response.
	 * @throws IOException
	 *             If sending fails.
	 */
	protected abstract <T extends SCPResponse> void sendRequest(
			SCPRequest<T> request, Consumer<T> callback) throws IOException;

	/**
	 * Wait for all outstanding requests sent by this process to receive replies
	 * or time out.
	 *
	 * @throws IOException
	 *             If communications fail.
	 */
	protected abstract void finish() throws IOException;

	/**
	 * Do a synchronous call of an SCP operation, sending the given message and
	 * completely processing the interaction before returning its response.
	 *
	 * @param <T>
	 *            The type of the response; implicit in the type of the request.
	 * @param request
	 *            The request to send
	 * @return The successful response to the request
	 * @throws IOException
	 *             If the communications fail
	 * @throws ProcessException
	 *             If the other side responds with a failure code
	 */
	protected final <T extends SCPResponse> T synchronousCall(
			SCPRequest<T> request) throws IOException, ProcessException {
		ValueHolder<T> holder = new ValueHolder<>();
		sendRequest(request, holder::setValue);
		finish();
		checkForError();
		return holder.getValue();
	}
}
