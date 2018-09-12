package uk.ac.manchester.spinnaker.messages.scp;

import static java.nio.ByteBuffer.allocate;
import static java.nio.ByteOrder.LITTLE_ENDIAN;
import static org.junit.jupiter.api.Assertions.*;
import static uk.ac.manchester.spinnaker.messages.scp.SCPCommand.CMD_VER;
import static uk.ac.manchester.spinnaker.messages.scp.SCPResult.RC_OK;
import static uk.ac.manchester.spinnaker.messages.sdp.SDPHeader.Flag.REPLY_NOT_EXPECTED;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;

import org.junit.jupiter.api.Test;

import uk.ac.manchester.spinnaker.machine.CoreLocation;
import uk.ac.manchester.spinnaker.messages.model.UnexpectedResponseCodeException;
import uk.ac.manchester.spinnaker.messages.model.Version;
import uk.ac.manchester.spinnaker.messages.scp.GetVersion.Response;

class TestVersion {

	@Test
	void testNewVersionRequest() {
		GetVersion verRequest = new GetVersion(new CoreLocation(0, 1, 2));
		assertEquals(CMD_VER, verRequest.scpRequestHeader.command);
		assertEquals(new CoreLocation(0, 1, 2),
				verRequest.sdpHeader.getDestination());
	}

	@Test
	void testParseVersionResponse() throws UnsupportedEncodingException,
			UnexpectedResponseCodeException {
		// SCP Stuff
		short rc = RC_OK.value;
		short seq = 105;
		short p2pAddr = 1024;
		byte physCPU = 31;
		byte virtCPU = 14;
		short version = 234;
		short buffer = 250;
		int buildDate = 103117;
		byte[] verString = "sark/spinnaker".getBytes("ASCII");

		// SDP stuff
		byte flags = REPLY_NOT_EXPECTED.value;
		byte tag = 5;
		byte destPortCPU = 0x4f;
		byte srcPortCPU = 0x6a;
		byte destX = 0x11;
		byte destY = (byte) 0xab;
		byte srcX = 0x7;
		byte srcY = 0x0;

		ByteBuffer data = allocate(39).order(LITTLE_ENDIAN);
		data.put(flags).put(tag).put(destPortCPU).put(srcPortCPU);
		data.put(destY).put(destX).put(srcY).put(srcX);
		data.putShort(rc).putShort(seq).putShort(p2pAddr);
		data.put(physCPU).put(virtCPU).putShort(buffer).putShort(version);
		data.putInt(buildDate).put(verString);
		data.flip();

		Response response = new GetVersion.Response(data);
		assertEquals("sark", response.versionInfo.name);
		assertEquals("sark/spinnaker", response.versionInfo.versionString);
		assertEquals(new Version(2, 34, 0), response.versionInfo.versionNumber);
	}
}
