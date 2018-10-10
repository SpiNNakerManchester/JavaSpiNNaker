package uk.ac.manchester.spinnaker.messages.scp;

import java.nio.ByteBuffer;

import uk.ac.manchester.spinnaker.machine.CoreLocation;
import uk.ac.manchester.spinnaker.machine.HasCoreLocation;
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
	private static final int DEFAULT_DEST_X_COORD = 255;
	private static final int DEFAULT_DEST_Y_COORD = 255;
	/**
	 * The location of the default SCAMP.
	 */
	static final CoreLocation DEFAULT_MONITOR_CORE =
			new CoreLocation(DEFAULT_DEST_X_COORD, DEFAULT_DEST_Y_COORD, 0);

	/** The first argument. */
	public final int argument1;
	/** The second argument. */
	public final int argument2;
	/** The third argument. */
	public final int argument3;
	/** The payload data as a buffer, or {@code null} if no payload data. */
	public final ByteBuffer data;
	/** The SCP request header of the message. */
	public final SCPRequestHeader scpRequestHeader;

	/**
	 * The constant value used to indicate that no payload data is in the
	 * message.
	 */
	protected static final ByteBuffer NO_DATA = null;

	/**
	 * Create a new request that goes to the default port and needs a reply.
	 *
	 * @param core
	 *            The core to send the request to.
	 * @param command
	 *            The command ID.
	 */
	protected SCPRequest(HasCoreLocation core, SCPCommand command) {
		this(new SCPSDPHeader(core), command, 0, 0, 0, NO_DATA);
	}

	/**
	 * Create a new request that goes to the default port and needs a reply.
	 *
	 * @param core
	 *            The core to send the request to.
	 * @param command
	 *            The command ID.
	 * @param argument1
	 *            The first argument.
	 */
	protected SCPRequest(HasCoreLocation core, SCPCommand command,
			int argument1) {
		this(new SCPSDPHeader(core), command, argument1, 0, 0, NO_DATA);
	}

	/**
	 * Create a new request that goes to the default port and needs a reply.
	 *
	 * @param core
	 *            The core to send the request to.
	 * @param command
	 *            The command ID.
	 * @param argument1
	 *            The first argument.
	 * @param argument2
	 *            The second argument.
	 */
	protected SCPRequest(HasCoreLocation core, SCPCommand command,
			int argument1, int argument2) {
		this(new SCPSDPHeader(core), command, argument1, argument2, 0,
				NO_DATA);
	}

	/**
	 * Create a new request that goes to the default port and needs a reply.
	 *
	 * @param core
	 *            The core to send the request to.
	 * @param command
	 *            The command ID.
	 * @param argument1
	 *            The first argument.
	 * @param argument2
	 *            The second argument.
	 * @param argument3
	 *            The third argument.
	 */
	protected SCPRequest(HasCoreLocation core, SCPCommand command,
			int argument1, int argument2, int argument3) {
		this(new SCPSDPHeader(core), command, argument1, argument2, argument3,
				NO_DATA);
	}

	/**
	 * Create a new request that goes to the default port and needs a reply.
	 *
	 * @param core
	 *            The core to send the request to.
	 * @param command
	 *            The command ID.
	 * @param argument1
	 *            The first argument.
	 * @param argument2
	 *            The second argument.
	 * @param argument3
	 *            The third argument.
	 * @param data
	 *            The additional data. Starts at the <i>position</i> and goes to
	 *            the <i>limit</i>.
	 */
	protected SCPRequest(HasCoreLocation core, SCPCommand command,
			int argument1, int argument2, int argument3, ByteBuffer data) {
		this(new SCPSDPHeader(core), command, argument1, argument2, argument3,
				data);
	}

	/**
	 * Create a new request.
	 *
	 * @param sdpHeader
	 *            The header.
	 * @param command
	 *            The command ID.
	 * @param argument1
	 *            The first argument.
	 * @param argument2
	 *            The second argument.
	 * @param argument3
	 *            The third argument.
	 * @param data
	 *            The additional data. Starts at the <i>position</i> and goes to
	 *            the <i>limit</i>.
	 */
	protected SCPRequest(SDPHeader sdpHeader, SCPCommand command, int argument1,
			int argument2, int argument3, ByteBuffer data) {
		super(sdpHeader);
		this.scpRequestHeader = new SCPRequestHeader(command);
		this.argument1 = argument1;
		this.argument2 = argument2;
		this.argument3 = argument3;
		this.data = data;
	}

	@Override
	public void addToBuffer(ByteBuffer buffer) {
		sdpHeader.addToBuffer(buffer);
		scpRequestHeader.addToBuffer(buffer);
		buffer.putInt(argument1);
		buffer.putInt(argument2);
		buffer.putInt(argument3);
		if (data != NO_DATA) {
			buffer.put(data.duplicate());
		}
	}

	/**
	 * Parse the response to this message.
	 *
	 * @param buffer
	 *            The buffer to parse.
	 * @return The message response.
	 * @throws Exception
	 *             If anything goes wrong with parsing.
	 */
	public abstract T getSCPResponse(ByteBuffer buffer) throws Exception;
}
