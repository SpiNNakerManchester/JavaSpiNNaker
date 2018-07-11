package uk.ac.manchester.spinnaker.connections;

import static java.nio.ByteBuffer.allocate;
import static java.nio.ByteOrder.LITTLE_ENDIAN;
import static uk.ac.manchester.spinnaker.messages.sdp.SDPFlag.REPLY_EXPECTED;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.nio.ByteBuffer;

import uk.ac.manchester.spinnaker.connections.model.Listenable;
import uk.ac.manchester.spinnaker.connections.model.SDPReceiver;
import uk.ac.manchester.spinnaker.connections.model.SDPSender;
import uk.ac.manchester.spinnaker.machine.CoreLocation;
import uk.ac.manchester.spinnaker.machine.HasChipLocation;
import uk.ac.manchester.spinnaker.messages.sdp.SDPHeader;
import uk.ac.manchester.spinnaker.messages.sdp.SDPMessage;

public class SDPConnection extends UDPConnection
		implements SDPReceiver, SDPSender, Listenable<SDPMessage> {
	private static final int BUFFER_SIZE = 300;
	private static final int SDP_SOURCE_PORT = 7;
	private static final int SDP_SOURCE_CPU = 31;
	private static final byte SDP_TAG = (byte) 0xFF;
	private HasChipLocation chip;

	public SDPConnection(HasChipLocation remoteChip, String localHost,
			Integer localPort, String remoteHost, Integer remotePort)
			throws IOException {
		super(localHost, localPort, remoteHost, remotePort);
		this.chip = remoteChip;
	}

	@Override
	public MessageReceiver<SDPMessage> getReceiver() {
		return this::receiveSDPMessage;
	}

	@Override
	public void sendSDPMessage(SDPMessage sdpMessage) throws IOException {
		if (sdpMessage.sdpHeader.getFlags() == REPLY_EXPECTED) {
			updateSDPHeaderForUDPSend(sdpMessage.sdpHeader, chip.getX(),
					chip.getY());
		} else {
			updateSDPHeaderForUDPSend(sdpMessage.sdpHeader, 0, 0);
		}
		ByteBuffer buffer = allocate(BUFFER_SIZE).order(LITTLE_ENDIAN);
		buffer.putShort((short) 0);
		sdpMessage.addToBuffer(buffer);
		send(buffer);
	}

	static void updateSDPHeaderForUDPSend(SDPHeader sdpHeader, int x, int y) {
		sdpHeader.setTag(SDP_TAG);
		sdpHeader.setSourcePort(SDP_SOURCE_PORT);
		sdpHeader.setSource(new CoreLocation(x, y, SDP_SOURCE_CPU));
	}

	@Override
	public SDPMessage receiveSDPMessage(Integer timeout)
			throws IOException, InterruptedIOException {
		return new SDPMessage(receive());
	}
}
