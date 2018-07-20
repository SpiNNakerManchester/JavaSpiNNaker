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
	private SCPRequest<?> error_request;
	private Throwable exception;

	protected Process() {
	}

	protected final void receiveError(SCPRequest<?> request,
			Throwable exception) {
		this.error_request = request;
		this.exception = exception;
	}

	public final boolean isError() {
		return exception != null;
	}

	public final void checkForError() throws Exception {
		if (!isError()) {
			return;
		}
		SDPHeader hdr = error_request.sdpHeader;
		Exception ex = new Exception(hdr.getDestination(), exception);
		exception = ex;
		throw ex;
	}

	protected final <T extends SCPResponse> void sendRequest(
			SCPRequest<T> request) throws IOException {
		sendRequest(request, null, this::receiveError);
	}

	protected final <T extends SCPResponse> void sendRequest(
			SCPRequest<T> request, SCPErrorHandler errorCallback)
			throws IOException {
		sendRequest(request, null, errorCallback);
	}

	protected final <T extends SCPResponse> void sendRequest(
			SCPRequest<T> request, Consumer<T> callback) throws IOException {
		sendRequest(request, callback, this::receiveError);
	}

	protected abstract <T extends SCPResponse> void sendRequest(
			SCPRequest<T> request, Consumer<T> callback,
			SCPErrorHandler errorCallback) throws IOException;

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
	 * core/chip
	 */
	public static class Exception extends java.lang.Exception {
		private static final long serialVersionUID = -1157220025479591572L;
		private static final String S = "     "; // five spaces

		public Exception(HasCoreLocation core, Throwable cause) {
			super(format("\n" + S + "Received exception class: %s\n" + S
					+ "With message: %s\n" + S + "When sending to %d:%d:%d\n",
					cause.getClass().getName(), cause.getMessage(), core.getX(),
					core.getY(), core.getP()), cause);
		}
	}
}
