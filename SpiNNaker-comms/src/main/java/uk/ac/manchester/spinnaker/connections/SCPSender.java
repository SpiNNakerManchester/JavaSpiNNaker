package uk.ac.manchester.spinnaker.connections;

import java.io.IOException;

import uk.ac.manchester.spinnaker.machine.ChipLocation;
import uk.ac.manchester.spinnaker.messages.scp.SCPRequest;

/** A sender of SCP messages */
public interface SCPSender extends Connection {
	/**
	 * Returns the data of an SCP request as it would be sent down this
	 * connection
	 */
	Object /* FIXME */ getSCPData(SCPRequest<?> scpRequest);

	/**
	 * Sends an SCP request down this connection.
	 * <p>
	 * Messages must have the following properties:
	 * <ul>
	 * <li><i>sourcePort</i> is <tt>null</tt> or 7
	 * <li><i>sourceCpu</i> is <tt>null</tt> or 31
	 * <li><i>sourceChipX</i> is <tt>null</tt> or 0
	 * <li><i>sourceChipY</i> is <tt>null</tt> or 0
	 * </ul>
	 * <i>tag</i> in the message is optional; if not set, the default set in the
	 * constructor will be used.
	 * <p>
	 * <i>sequence</i> in the message is optional; if not set, <i>(sequence
	 * number\ last assigned + 1) % 65536</i> will be used
	 *
	 * @param scpRequest
	 *            message packet to send
	 * @throws IOException
	 *             If there is an error sending the message
	 */
	void sendSCPRequest(SCPRequest<?> scpRequest) throws IOException;

	/**
	 * @return The chip at which messages sent down this connection will arrive
	 *         at first.
	 */
	ChipLocation getChip();
}
