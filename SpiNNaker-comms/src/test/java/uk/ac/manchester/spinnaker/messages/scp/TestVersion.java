/*
 * Copyright (c) 2018 The University of Manchester
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package uk.ac.manchester.spinnaker.messages.scp;

import static java.lang.String.join;
import static java.nio.ByteBuffer.allocate;
import static java.nio.ByteOrder.LITTLE_ENDIAN;
import static java.nio.charset.StandardCharsets.US_ASCII;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static uk.ac.manchester.spinnaker.messages.scp.SCPCommand.CMD_VER;
import static uk.ac.manchester.spinnaker.messages.scp.SCPResult.RC_OK;
import static uk.ac.manchester.spinnaker.messages.sdp.SDPHeader.Flag.REPLY_NOT_EXPECTED;

import org.junit.jupiter.api.Test;

import uk.ac.manchester.spinnaker.machine.CoreLocation;
import uk.ac.manchester.spinnaker.messages.model.UnexpectedResponseCodeException;
import uk.ac.manchester.spinnaker.messages.model.Version;

class TestVersion {
	private static final short PADDING = 0;

	@Test
	void testNewVersionRequest() {
		var verRequest = new GetVersion(new CoreLocation(0, 1, 2));
		assertEquals(CMD_VER, verRequest.scpRequestHeader.command);
		assertEquals(new CoreLocation(0, 1, 2),
				verRequest.sdpHeader.getDestination());
	}

	@Test
	void testParseVersionResponseFormat1()
			throws UnexpectedResponseCodeException {
		// SCP Stuff
		short rc = RC_OK.value;
		short seq = 105;
		short p2pAddr = 1024;
		byte physCPU = 31;
		byte virtCPU = 14;
		short version = 234;
		short buffer = 250;
		int buildDate = 103117;
		var verString = "sark/spinnaker".getBytes(US_ASCII);

		// SDP stuff
		byte flags = REPLY_NOT_EXPECTED.value;
		byte tag = 5;
		byte destPortCPU = 0x4f;
		byte srcPortCPU = 0x6a;
		byte destX = 0x11;
		byte destY = (byte) 0xab;
		byte srcX = 0x7;
		byte srcY = 0x0;

		var data = allocate(41).order(LITTLE_ENDIAN).putShort(PADDING);
		data.put(flags).put(tag).put(destPortCPU).put(srcPortCPU);
		data.put(destY).put(destX).put(srcY).put(srcX);
		data.putShort(rc).putShort(seq).putShort(p2pAddr);
		data.put(physCPU).put(virtCPU).putShort(buffer).putShort(version);
		data.putInt(buildDate).put(verString);
		data.flip();

		var response = new GetVersion.Response(data);
		var ver = response.get();
		assertEquals("sark", ver.name);
		assertEquals("spinnaker", ver.hardware);
		assertEquals(new Version(2, 34, 0), ver.versionNumber);
		assertEquals(new CoreLocation(14, 31, 0), ver.core);
	}

	@Test
	void testParseVersionResponseFormat2()
			throws UnexpectedResponseCodeException {
		// SCP Stuff
		short rc = RC_OK.value;
		short seq = 105;
		short p2pAddr = 1024;
		byte physCPU = 31;
		byte virtCPU = 14;
		short version = -1;
		short buffer = 250;
		int buildDate = 103117;
		var verString = join("\u0000", "SC&MP/SpiNNaker", "3.2.0", "")
				.getBytes(US_ASCII);

		// SDP stuff
		byte flags = REPLY_NOT_EXPECTED.value;
		byte tag = 5;
		byte destPortCpu = 0x4f;
		byte srcPortCpu = 0x6a;
		byte destX = 0x11;
		byte destY = (byte) 0xab;
		byte srcX = 0x7;
		byte srcY = 0x0;

		var data = allocate(60).order(LITTLE_ENDIAN).putShort(PADDING);
		data.put(flags).put(tag).put(destPortCpu).put(srcPortCpu);
		data.put(destY).put(destX).put(srcY).put(srcX);
		data.putShort(rc).putShort(seq).putShort(p2pAddr);
		data.put(physCPU).put(virtCPU).putShort(buffer).putShort(version);
		data.putInt(buildDate).put(verString);
		data.flip();

		var response = new GetVersion.Response(data);
		assertEquals("SC&MP", response.get().name);
		assertEquals("SpiNNaker", response.get().hardware);
		assertEquals(new Version(3, 2, 0), response.get().versionNumber);
		assertEquals(new CoreLocation(14, 31, 0), response.get().core);
	}
}
