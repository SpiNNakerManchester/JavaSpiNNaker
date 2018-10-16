package uk.ac.manchester.spinnaker.messages.scp;

import static uk.ac.manchester.spinnaker.messages.sdp.SDPHeader.Flag.REPLY_EXPECTED;
import static uk.ac.manchester.spinnaker.messages.sdp.SDPPort.DEFAULT_PORT;

import uk.ac.manchester.spinnaker.machine.HasCoreLocation;
import uk.ac.manchester.spinnaker.messages.sdp.SDPHeader;

/**
 * SCP uses a limited subset of SDP. It <i>always</i> wants a reply and always
 * talks to a particular SDP port (the port for SCAMP).
 *
 * @author Donal Fellows
 */
class SCPSDPHeader extends SDPHeader {
	/**
	 * Make a header.
	 *
	 * @param core
	 *            The SpiNNaker core that we want to talk to. Should be running
	 *            SCAMP.
	 */
	SCPSDPHeader(HasCoreLocation core) {
		super(REPLY_EXPECTED, core, DEFAULT_PORT);
	}
}
