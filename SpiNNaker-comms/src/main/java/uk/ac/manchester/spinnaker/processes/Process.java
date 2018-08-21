package uk.ac.manchester.spinnaker.processes;

import static java.lang.String.format;

import java.io.IOException;
import java.util.function.Consumer;

import javax.xml.ws.Holder;

import uk.ac.manchester.spinnaker.connections.SCPErrorHandler;
import uk.ac.manchester.spinnaker.machine.HasCoreLocation;
import uk.ac.manchester.spinnaker.messages.scp.SCPRequest;
import uk.ac.manchester.spinnaker.messages.scp.SCPResponse;
import uk.ac.manchester.spinnaker.messages.sdp.SDPHeader;

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
	 * @throws Exception
	 *             an exception that wraps the original exception that occurred.
	 */
	public final void checkForError() throws Exception {
		if (!isError()) {
			return;
		}
		SDPHeader hdr = errorRequest.sdpHeader;
		Exception ex = new Exception(hdr.getDestination(), exception);
		exception = ex;
		throw ex;
	}

	/**
	 * Send a request.
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
		sendRequest(request, null, this::receiveError);
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
	protected final <T extends SCPResponse> void sendRequest(
			SCPRequest<T> request, Consumer<T> callback) throws IOException {
		sendRequest(request, callback, this::receiveError);
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
	 * @param errorCallback
	 *            The callback that handles errors.
	 * @throws IOException
	 *             If sending fails.
	 */
	protected abstract <T extends SCPResponse> void sendRequest(
			SCPRequest<T> request, Consumer<T> callback,
			SCPErrorHandler errorCallback) throws IOException;

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
	 * @param errorCallback
	 *            A custom error handler
	 * @return The successful response to the request
	 * @throws IOException
	 *             If the communications fail
	 * @throws Exception
	 *             If the other side responds with a failure code
	 */
	protected final <T extends SCPResponse> T synchronousCall(
			SCPRequest<T> request, SCPErrorHandler errorCallback)
			throws IOException, Exception {
		Holder<T> holder = new Holder<>();
		sendRequest(request, response -> holder.value = response,
				errorCallback);
		finish();
		checkForError();
		return holder.value;
	}

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
	 * @throws Exception
	 *             If the other side responds with a failure code
	 */
	protected final <T extends SCPResponse> T synchronousCall(
			SCPRequest<T> request) throws IOException, Exception {
		Holder<T> holder = new Holder<>();
		sendRequest(request, response -> holder.value = response,
				this::receiveError);
		finish();
		checkForError();
		return holder.value;
	}

	/**
	 * Encapsulates exceptions from processes which communicate with some
	 * core/chip.
	 */
	public static class Exception extends java.lang.Exception {
		private static final long serialVersionUID = -1157220025479591572L;
		private static final String S = "     "; // five spaces

		/**
		 * Create an exception.
		 *
		 * @param core
		 *            What core were we talking to.
		 * @param cause
		 *            What exception caused problems.
		 */
		public Exception(HasCoreLocation core, Throwable cause) {
			super(format("\n" + S + "Received exception class: %s\n" + S
					+ "With message: %s\n" + S + "When sending to %d:%d:%d\n",
					cause.getClass().getName(), cause.getMessage(), core.getX(),
					core.getY(), core.getP()), cause);
		}
	}
}
