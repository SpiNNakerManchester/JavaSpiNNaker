package uk.ac.manchester.spinnaker.messages.scp;

import static java.lang.String.join;
import static java.nio.ByteBuffer.allocate;
import static java.nio.ByteOrder.LITTLE_ENDIAN;
import static java.nio.charset.StandardCharsets.US_ASCII;
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
	private static final short PADDING = 0;

	@Test
	void testNewVersionRequest() {
		GetVersion verRequest = new GetVersion(new CoreLocation(0, 1, 2));
		assertEquals(CMD_VER, verRequest.scpRequestHeader.command);
		assertEquals(new CoreLocation(0, 1, 2),
				verRequest.sdpHeader.getDestination());
	}

	@Test
	void testParseVersionResponseFormat1() throws UnsupportedEncodingException,
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
		byte[] verString = "sark/spinnaker".getBytes(US_ASCII);

		// SDP stuff
		byte flags = REPLY_NOT_EXPECTED.value;
		byte tag = 5;
		byte destPortCPU = 0x4f;
		byte srcPortCPU = 0x6a;
		byte destX = 0x11;
		byte destY = (byte) 0xab;
		byte srcX = 0x7;
		byte srcY = 0x0;

		ByteBuffer data = allocate(41).order(LITTLE_ENDIAN).putShort(PADDING);
		data.put(flags).put(tag).put(destPortCPU).put(srcPortCPU);
		data.put(destY).put(destX).put(srcY).put(srcX);
		data.putShort(rc).putShort(seq).putShort(p2pAddr);
		data.put(physCPU).put(virtCPU).putShort(buffer).putShort(version);
		data.putInt(buildDate).put(verString);
		data.flip();

		Response response = new GetVersion.Response(data);
		assertEquals("sark", response.versionInfo.name);
		assertEquals("spinnaker", response.versionInfo.hardware);
		assertEquals(new Version(2, 34, 0), response.versionInfo.versionNumber);
		assertEquals(new CoreLocation(14, 31, 0), response.versionInfo.core);
	}

	@Test
	void testParseVersionResponseFormat2() throws UnsupportedEncodingException,
			UnexpectedResponseCodeException {
		// SCP Stuff
		short rc = RC_OK.value;
		short seq = 105;
		short p2pAddr = 1024;
		byte physCPU = 31;
		byte virtCPU = 14;
		short version = -1;
		short buffer = 250;
		int buildDate = 103117;
		byte[] verString = join("\u0000", "SC&MP/SpiNNaker", "3.2.0", "")
				.getBytes(US_ASCII);

		// SDP stuff
		byte flags = REPLY_NOT_EXPECTED.value;
		byte tag = 5;
		byte dest_port_cpu = 0x4f;
		byte src_port_cpu = 0x6a;
		byte dest_x = 0x11;
		byte dest_y = (byte) 0xab;
		byte src_x = 0x7;
		byte src_y = 0x0;

		ByteBuffer data = allocate(60).order(LITTLE_ENDIAN).putShort(PADDING);
		data.put(flags).put(tag).put(dest_port_cpu).put(src_port_cpu);
		data.put(dest_y).put(dest_x).put(src_y).put(src_x);
		data.putShort(rc).putShort(seq).putShort(p2pAddr);
		data.put(physCPU).put(virtCPU).putShort(buffer).putShort(version);
		data.putInt(buildDate).put(verString);
		data.flip();

		Response response = new GetVersion.Response(data);
		assertEquals("SC&MP", response.versionInfo.name);
		assertEquals("SpiNNaker", response.versionInfo.hardware);
		assertEquals(new Version(3, 2, 0), response.versionInfo.versionNumber);
		assertEquals(new CoreLocation(14, 31, 0), response.versionInfo.core);
	}
}
