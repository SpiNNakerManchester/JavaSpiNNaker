package uk.ac.manchester.spinnaker.connections;

import static java.nio.ByteOrder.LITTLE_ENDIAN;
import static uk.ac.manchester.spinnaker.connections.SDPConnection.updateSDPHeaderForUDPSend;
import static uk.ac.manchester.spinnaker.messages.Constants.SCP_SCAMP_PORT;
import static uk.ac.manchester.spinnaker.messages.sdp.SDPFlag.REPLY_EXPECTED;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Collection;

import uk.ac.manchester.spinnaker.connections.model.SCPReceiver;
import uk.ac.manchester.spinnaker.connections.model.SCPSender;
import uk.ac.manchester.spinnaker.machine.ChipLocation;
import uk.ac.manchester.spinnaker.messages.bmp.BMPRequest;
import uk.ac.manchester.spinnaker.messages.model.BMPConnectionData;
import uk.ac.manchester.spinnaker.messages.scp.SCPRequest;
import uk.ac.manchester.spinnaker.messages.scp.SCPResult;
import uk.ac.manchester.spinnaker.messages.scp.SCPResultMessage;

public class BMPConnection extends UDPConnection
		implements SCPReceiver, SCPSender {
	/** Defined to satisfy the SCPSender; always 0,0 for a BMP */
	private static final ChipLocation BMP_LOCATION = new ChipLocation(0, 0);
	public final int cabinet;
	public final int frame;
	public final Collection<Integer> boards;

	public BMPConnection(BMPConnectionData connectionData) throws IOException {
		super(null, null, connectionData.ipAddress,
				(connectionData.portNumber == null ? SCP_SCAMP_PORT
						: connectionData.portNumber));
		cabinet = connectionData.cabinet;
		frame = connectionData.frame;
		boards = connectionData.boards;
	}

	@Override
	public ByteBuffer getSCPData(SCPRequest<?> scpRequest) {
		ByteBuffer buffer = ByteBuffer.allocate(300).order(LITTLE_ENDIAN);
		if (scpRequest.sdpHeader.getFlags() == REPLY_EXPECTED) {
			updateSDPHeaderForUDPSend(scpRequest.sdpHeader, 0, 0);
		}
		scpRequest.addToBuffer(buffer);
		buffer.flip();
		return buffer;
	}

	@Override
	public final void sendSCPRequest(SCPRequest<?> scpRequest)
			throws IOException {
		sendBMPRequest((BMPRequest<?>) scpRequest);
	}

	public void sendBMPRequest(BMPRequest<?> scpRequest) throws IOException {
		send(getSCPData(scpRequest));
	}

	@Override
	public ChipLocation getChip() {
		return BMP_LOCATION;
	}

	@Override
	public SCPResultMessage receiveSCPResponse(Integer timeout)
			throws IOException {
		ByteBuffer buffer = receive(timeout);
		short result = buffer.getShort();
		short sequence = buffer.getShort();
		return new SCPResultMessage(SCPResult.get(result), sequence, buffer);
	}
}
