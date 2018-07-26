package uk.ac.manchester.spinnaker.connections;

import static uk.ac.manchester.spinnaker.messages.sdp.SDPFlag.REPLY_EXPECTED;
import static uk.ac.manchester.spinnaker.transceiver.Utils.newMessageBuffer;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.nio.ByteBuffer;

import uk.ac.manchester.spinnaker.connections.model.Listenable;
import uk.ac.manchester.spinnaker.connections.model.SDPReceiver;
import uk.ac.manchester.spinnaker.connections.model.SDPSender;
import uk.ac.manchester.spinnaker.machine.ChipLocation;
import uk.ac.manchester.spinnaker.machine.HasChipLocation;
import uk.ac.manchester.spinnaker.messages.sdp.SDPMessage;

public class SDPConnection extends UDPConnection
		implements SDPReceiver, SDPSender, Listenable<SDPMessage> {
	private static final ChipLocation ONE_WAY_SOURCE = new ChipLocation(0, 0);
	private ChipLocation chip;

	public SDPConnection(HasChipLocation remoteChip, String localHost,
			Integer localPort, String remoteHost, Integer remotePort)
			throws IOException {
		super(localHost, localPort, remoteHost, remotePort);
		this.chip = remoteChip.asChipLocation();
	}

	@Override
	public MessageReceiver<SDPMessage> getReceiver() {
		return this::receiveSDPMessage;
	}

	@Override
	public void sendSDPMessage(SDPMessage sdpMessage) throws IOException {
		if (sdpMessage.sdpHeader.getFlags() == REPLY_EXPECTED) {
			sdpMessage.updateSDPHeaderForUDPSend(chip);
		} else {
			sdpMessage.updateSDPHeaderForUDPSend(ONE_WAY_SOURCE);
		}
		ByteBuffer buffer = newMessageBuffer();
		buffer.putShort((short) 0);
		sdpMessage.addToBuffer(buffer);
		send(buffer);
	}

	@Override
	public SDPMessage receiveSDPMessage(Integer timeout)
			throws IOException, InterruptedIOException {
		return new SDPMessage(receive());
	}

	public ChipLocation getChip() {
		return chip;
	}

	public void setChip(HasChipLocation chip) {
		this.chip = chip.asChipLocation();
	}
}
