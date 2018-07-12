package uk.ac.manchester.spinnaker.messages.bmp;

import static java.util.Collections.min;
import static uk.ac.manchester.spinnaker.messages.scp.SCPResult.RC_OK;
import static uk.ac.manchester.spinnaker.messages.sdp.SDPFlag.REPLY_EXPECTED;

import java.nio.ByteBuffer;
import java.util.Collection;

import uk.ac.manchester.spinnaker.machine.CoreLocation;
import uk.ac.manchester.spinnaker.messages.model.UnexpectedResponseCodeException;
import uk.ac.manchester.spinnaker.messages.scp.SCPCommand;
import uk.ac.manchester.spinnaker.messages.scp.SCPRequest;
import uk.ac.manchester.spinnaker.messages.scp.SCPRequestHeader;
import uk.ac.manchester.spinnaker.messages.scp.SCPResponse;
import uk.ac.manchester.spinnaker.messages.scp.SCPResult;
import uk.ac.manchester.spinnaker.messages.sdp.SDPHeader;

public abstract class BMPRequest<T extends BMPRequest.BMPResponse>
		extends SCPRequest<T> {
	private static SDPHeader bmpHeader(int board) {
		return new SDPHeader(REPLY_EXPECTED, new CoreLocation(0, 0, board), 0);
	}

	private static SDPHeader bmpHeader(Collection<Integer> boards) {
		int board = min(boards);
		return new SDPHeader(REPLY_EXPECTED, new CoreLocation(0, 0, board), 0);
	}

	protected BMPRequest(int board, SCPCommand command) {
		this(board, command, null, null, null, (byte[]) null);
	}

	protected BMPRequest(int board, SCPCommand command, Integer argument1,
			Integer argument2, Integer argument3) {
		this(board, command, argument1, argument2, argument3, (byte[]) null);
	}

	protected BMPRequest(int board, SCPCommand command, byte[] data) {
		this(board, command, null, null, null, data);
	}

	protected BMPRequest(int board, SCPCommand command, ByteBuffer data) {
		this(board, command, null, null, null, data);
	}

	protected BMPRequest(int board, SCPCommand command, Integer argument1,
			Integer argument2, Integer argument3, byte[] data) {
		super(bmpHeader(board), new SCPRequestHeader(command), argument1,
				argument2, argument3, data);
	}

	protected BMPRequest(int board, SCPCommand command, Integer argument1,
			Integer argument2, Integer argument3, ByteBuffer data) {
		super(bmpHeader(board), new SCPRequestHeader(command), argument1,
				argument2, argument3, data);
	}

	protected BMPRequest(Collection<Integer> boards, SCPCommand command) {
		this(boards, command, null, null, null, (byte[]) null);
	}

	protected BMPRequest(Collection<Integer> boards, SCPCommand command,
			Integer argument1, Integer argument2, Integer argument3) {
		this(boards, command, argument1, argument2, argument3, (byte[]) null);
	}

	protected BMPRequest(Collection<Integer> boards, SCPCommand command,
			byte[] data) {
		this(boards, command, null, null, null, data);
	}

	protected BMPRequest(Collection<Integer> boards, SCPCommand command,
			ByteBuffer data) {
		this(boards, command, null, null, null, data);
	}

	protected BMPRequest(Collection<Integer> boards, SCPCommand command,
			Integer argument1, Integer argument2, Integer argument3,
			byte[] data) {
		super(bmpHeader(boards), new SCPRequestHeader(command), argument1,
				argument2, argument3, data);
	}

	protected BMPRequest(Collection<Integer> boards, SCPCommand command,
			Integer argument1, Integer argument2, Integer argument3,
			ByteBuffer data) {
		super(bmpHeader(boards), new SCPRequestHeader(command), argument1,
				argument2, argument3, data);
	}

	/** Represents an SCP request thats tailored for the BMP connection. */
	public static abstract class BMPResponse extends SCPResponse {
		protected BMPResponse(String operation, SCPCommand command,
				ByteBuffer buffer) throws UnexpectedResponseCodeException {
			super(buffer);
			SCPResult result = scpResponseHeader.result;
			if (result != RC_OK) {
				throw new UnexpectedResponseCodeException(operation, command,
						result);
			}
		}
	};
}
