package uk.ac.manchester.spinnaker.messages.scp;

import static uk.ac.manchester.spinnaker.messages.scp.SCPCommand.CMD_LED;
import static uk.ac.manchester.spinnaker.messages.sdp.SDPHeader.Flag.REPLY_EXPECTED;

import java.nio.ByteBuffer;
import java.util.Map;
import java.util.Map.Entry;

import uk.ac.manchester.spinnaker.machine.HasCoreLocation;
import uk.ac.manchester.spinnaker.messages.sdp.SDPHeader;

/** A request to change the state of an BMPSetLED. */
public class SetLED extends SCPRequest<CheckOKResponse> {
	/**
	 * @param core
	 *            The SpiNNaker core that will set the BMPSetLED
	 * @param ledStates
	 *            A mapping of BMPSetLED index to state with 0 being off, 1 on and
	 *            2 inverted.
	 */
	public SetLED(HasCoreLocation core, Map<Integer, Integer> ledStates) {
		super(new SDPHeader(REPLY_EXPECTED, core, 0), CMD_LED,
				argument1(ledStates), null, null);
	}

	private static Integer argument1(Map<Integer, Integer> ledStates) {
		int encoded = 0;
		for (Entry<Integer, Integer> e : ledStates.entrySet()) {
			switch (e.getValue()) {
			case 0:
				encoded |= 2 << (2 * e.getKey());
				break;
			case 1:
				encoded |= 3 << (2 * e.getKey());
				break;
			case 2:
				encoded |= 1 << (2 * e.getKey());
				break;
			}
		}
		return encoded;
	}

	@Override
	public CheckOKResponse getSCPResponse(ByteBuffer buffer) throws Exception {
		return new CheckOKResponse("Set BMPSetLED", CMD_LED, buffer);
	}
}
