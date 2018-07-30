package uk.ac.manchester.spinnaker.messages.scp;

import java.nio.ByteBuffer;

import uk.ac.manchester.spinnaker.machine.CoreLocation;
import uk.ac.manchester.spinnaker.messages.sdp.SDPHeader;
import uk.ac.manchester.spinnaker.messages.sdp.SpinnakerRequest;

/**
 * Represents an Abstract SCP Request.
 *
 * @param <T>
 *            The type of response expected to the request, if any.
 */
public abstract class SCPRequest<T extends SCPResponse>
		extends SpinnakerRequest {
	static final int DEFAULT_DEST_X_COORD = 255;
	static final int DEFAULT_DEST_Y_COORD = 255;
	static final CoreLocation DEFAULT_MONITOR_CORE = new CoreLocation(
			DEFAULT_DEST_X_COORD, DEFAULT_DEST_Y_COORD, 0);

	/** The first argument, or <tt>null</tt> if no first argument. */
	public final Integer argument1;
	/** The second argument, or <tt>null</tt> if no second argument. */
	public final Integer argument2;
	/** The third argument, or <tt>null</tt> if no third argument. */
	public final Integer argument3;
	/** The data, or <tt>null</tt> if no data this way. */
	public final byte[] data;
	/** The data as a buffer, or <tt>null</tt> if no data this way. */
	public final ByteBuffer dataBuffer;
	/** The SCP request header of the message. */
	public final SCPRequestHeader scpRequestHeader;

	private static final byte[] NO_DATA = null;

	protected SCPRequest(SDPHeader sdpHeader, SCPCommand command) {
		this(sdpHeader, command, null, null, null, NO_DATA);
	}

	protected SCPRequest(SDPHeader sdpHeader, SCPCommand command,
			Integer argument1, Integer argument2, Integer argument3) {
		this(sdpHeader, command, argument1, argument2, argument3, NO_DATA);
	}

	protected SCPRequest(SDPHeader sdpHeader, SCPCommand command, byte[] data) {
		this(sdpHeader, command, null, null, null, data);
	}

	protected SCPRequest(SDPHeader sdpHeader, SCPCommand command,
			ByteBuffer data) {
		this(sdpHeader, command, null, null, null, data);
	}

	protected SCPRequest(SDPHeader sdpHeader, SCPCommand command,
			Integer argument1, Integer argument2, Integer argument3,
			byte[] data) {
		super(sdpHeader);
		this.scpRequestHeader = new SCPRequestHeader(command);
		this.argument1 = argument1;
		this.argument2 = argument2;
		this.argument3 = argument3;
		this.data = (data == NO_DATA || data.length == 0) ? null : data;
		this.dataBuffer = null;
	}

	protected SCPRequest(SDPHeader sdpHeader, SCPCommand command,
			Integer argument1, Integer argument2, Integer argument3,
			ByteBuffer data) {
		super(sdpHeader);
		this.scpRequestHeader = new SCPRequestHeader(command);
		this.argument1 = argument1;
		this.argument2 = argument2;
		this.argument3 = argument3;
		this.data = null;
		this.dataBuffer = data.asReadOnlyBuffer();
	}

	@Override
	public void addToBuffer(ByteBuffer buffer) {
		sdpHeader.addToBuffer(buffer);
		scpRequestHeader.addToBuffer(buffer);
		buffer.putInt(argument1 == null ? 0 : argument1);
		buffer.putInt(argument2 == null ? 0 : argument2);
		buffer.putInt(argument3 == null ? 0 : argument3);
		if (data != null) {
			buffer.put(data);
		} else if (dataBuffer != null) {
			buffer.put(dataBuffer.array(), dataBuffer.position(),
					dataBuffer.remaining());
		}
	}

	/**
	 * Parse the response to this message.
	 *
	 * @param buffer
	 *            The buffer to parse.
	 * @return The message response.
	 */
	public abstract T getSCPResponse(ByteBuffer buffer) throws Exception;
}
