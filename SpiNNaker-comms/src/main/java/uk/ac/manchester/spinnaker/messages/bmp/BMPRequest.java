package uk.ac.manchester.spinnaker.messages.bmp;

import static java.util.Collections.min;
import static uk.ac.manchester.spinnaker.messages.scp.SCPResult.RC_OK;
import static uk.ac.manchester.spinnaker.messages.sdp.SDPHeader.Flag.REPLY_EXPECTED;
import static uk.ac.manchester.spinnaker.messages.sdp.SDPPort.DEFAULT_PORT;

import java.nio.ByteBuffer;
import java.util.Collection;

import uk.ac.manchester.spinnaker.machine.CoreLocation;
import uk.ac.manchester.spinnaker.messages.model.UnexpectedResponseCodeException;
import uk.ac.manchester.spinnaker.messages.scp.SCPCommand;
import uk.ac.manchester.spinnaker.messages.scp.SCPRequest;
import uk.ac.manchester.spinnaker.messages.scp.SCPResponse;
import uk.ac.manchester.spinnaker.messages.sdp.SDPHeader;

/**
 * The base class of a request following the BMP protocol.
 *
 * @author Donal Fellows
 * @param <T>
 *            The type of the response to the request.
 */
public abstract class BMPRequest<T extends BMPRequest.BMPResponse>
		extends SCPRequest<T> {
	private static SDPHeader bmpHeader(int board) {
		return new SDPHeader(REPLY_EXPECTED, new CoreLocation(0, 0, board),
				DEFAULT_PORT);
	}

	private static SDPHeader bmpHeader(Collection<Integer> boards) {
		int board = min(boards);
		return new SDPHeader(REPLY_EXPECTED, new CoreLocation(0, 0, board),
				DEFAULT_PORT);
	}

	/**
	 * Make a request.
	 *
	 * @param board
	 *            The board to talk to
	 * @param command
	 *            The command to send
	 */
	protected BMPRequest(int board, SCPCommand command) {
		this(board, command, null, null, null, (byte[]) null);
	}

	/**
	 * Make a request.
	 *
	 * @param board
	 *            The board to talk to
	 * @param command
	 *            The command to send
	 * @param argument1
	 *            The first argument
	 * @param argument2
	 *            The second argument
	 * @param argument3
	 *            The third argument
	 */
	protected BMPRequest(int board, SCPCommand command, Integer argument1,
			Integer argument2, Integer argument3) {
		this(board, command, argument1, argument2, argument3, (byte[]) null);
	}

	/**
	 * Make a request.
	 *
	 * @param board
	 *            The board to talk to
	 * @param command
	 *            The command to send
	 * @param data
	 *            The payload
	 */
	protected BMPRequest(int board, SCPCommand command, byte[] data) {
		this(board, command, null, null, null, data);
	}

	/**
	 * Make a request.
	 *
	 * @param board
	 *            The board to talk to
	 * @param command
	 *            The command to send
	 * @param data
	 *            The payload
	 */
	protected BMPRequest(int board, SCPCommand command, ByteBuffer data) {
		this(board, command, null, null, null, data);
	}

	/**
	 * Make a request.
	 *
	 * @param board
	 *            The board to talk to
	 * @param command
	 *            The command to send
	 * @param argument1
	 *            The first argument
	 * @param argument2
	 *            The second argument
	 * @param argument3
	 *            The third argument
	 * @param data
	 *            The payload
	 */
	protected BMPRequest(int board, SCPCommand command, Integer argument1,
			Integer argument2, Integer argument3, byte[] data) {
		super(bmpHeader(board), command, argument1, argument2, argument3, data);
	}

	/**
	 * Make a request.
	 *
	 * @param board
	 *            The board to talk to
	 * @param command
	 *            The command to send
	 * @param argument1
	 *            The first argument
	 * @param argument2
	 *            The second argument
	 * @param argument3
	 *            The third argument
	 * @param data
	 *            The payload
	 */
	protected BMPRequest(int board, SCPCommand command, Integer argument1,
			Integer argument2, Integer argument3, ByteBuffer data) {
		super(bmpHeader(board), command, argument1, argument2, argument3, data);
	}

	/**
	 * Make a request.
	 *
	 * @param boards
	 *            The boards to talk to
	 * @param command
	 *            The command to send
	 */
	protected BMPRequest(Collection<Integer> boards, SCPCommand command) {
		this(boards, command, null, null, null, (byte[]) null);
	}

	/**
	 * Make a request.
	 *
	 * @param boards
	 *            The boards to talk to
	 * @param command
	 *            The command to send
	 * @param argument1
	 *            The first argument
	 * @param argument2
	 *            The second argument
	 * @param argument3
	 *            The third argument
	 */
	protected BMPRequest(Collection<Integer> boards, SCPCommand command,
			Integer argument1, Integer argument2, Integer argument3) {
		this(boards, command, argument1, argument2, argument3, (byte[]) null);
	}

	/**
	 * Make a request.
	 *
	 * @param boards
	 *            The boards to talk to
	 * @param command
	 *            The command to send
	 * @param data
	 *            The payload
	 */
	protected BMPRequest(Collection<Integer> boards, SCPCommand command,
			byte[] data) {
		this(boards, command, null, null, null, data);
	}

	/**
	 * Make a request.
	 *
	 * @param boards
	 *            The boards to talk to
	 * @param command
	 *            The command to send
	 * @param data
	 *            The payload
	 */
	protected BMPRequest(Collection<Integer> boards, SCPCommand command,
			ByteBuffer data) {
		this(boards, command, null, null, null, data);
	}

	/**
	 * Make a request.
	 *
	 * @param boards
	 *            The boards to talk to
	 * @param command
	 *            The command to send
	 * @param argument1
	 *            The first argument
	 * @param argument2
	 *            The second argument
	 * @param argument3
	 *            The third argument
	 * @param data
	 *            The payload
	 */
	protected BMPRequest(Collection<Integer> boards, SCPCommand command,
			Integer argument1, Integer argument2, Integer argument3,
			byte[] data) {
		super(bmpHeader(boards), command, argument1, argument2, argument3,
				data);
	}

	/**
	 * Make a request.
	 *
	 * @param boards
	 *            The boards to talk to
	 * @param command
	 *            The command to send
	 * @param argument1
	 *            The first argument
	 * @param argument2
	 *            The second argument
	 * @param argument3
	 *            The third argument
	 * @param data
	 *            The payload
	 */
	protected BMPRequest(Collection<Integer> boards, SCPCommand command,
			Integer argument1, Integer argument2, Integer argument3,
			ByteBuffer data) {
		super(bmpHeader(boards), command, argument1, argument2, argument3,
				data);
	}

	/** Represents an SCP request thats tailored for the BMP connection. */
	public abstract static class BMPResponse extends SCPResponse {
		/**
		 * Make a response object.
		 *
		 * @param operation
		 *            The operation that this part of.
		 * @param command
		 *            The command that this is a response to.
		 * @param buffer
		 *            The buffer to read the response from.
		 * @throws UnexpectedResponseCodeException
		 *             If the response is not a success.
		 */
		protected BMPResponse(String operation, SCPCommand command,
				ByteBuffer buffer) throws UnexpectedResponseCodeException {
			super(buffer);
			if (result != RC_OK) {
				throw new UnexpectedResponseCodeException(operation, command,
						result);
			}
		}
	};
}
