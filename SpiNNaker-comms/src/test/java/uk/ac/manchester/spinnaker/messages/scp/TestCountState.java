/*
 * Copyright (c) 2018 The University of Manchester
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package uk.ac.manchester.spinnaker.messages.scp;

import static java.nio.ByteBuffer.allocate;
import static java.nio.ByteOrder.LITTLE_ENDIAN;
import static org.junit.jupiter.api.Assertions.*;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;

import org.junit.jupiter.api.Test;

import uk.ac.manchester.spinnaker.messages.model.CPUState;
import uk.ac.manchester.spinnaker.messages.model.UnexpectedResponseCodeException;
import uk.ac.manchester.spinnaker.messages.scp.CountState.Response;
import uk.ac.manchester.spinnaker.messages.sdp.SDPHeader;
import uk.ac.manchester.spinnaker.messages.sdp.SDPHeader.Flag;

class TestCountState {
	private static final short PADDING = 0;

	@Test
	void testNewStateRequest() {
		assertNotNull(new CountState(32, CPUState.READY));
	}

	@Test
	void testNewStateResponse() throws UnexpectedResponseCodeException {
        // SCP Stuff
        SCPResult rc = SCPResult.RC_OK;
        int seq = 105;

        int argumentCount = 5;

        // SDP stuff
        Flag flags = SDPHeader.Flag.REPLY_NOT_EXPECTED;
        int tag = 5;
        int destPortCPU = 0x4f;
        int srcPortCPU = 0x6a;
        int destX = 0x11;
        int destY = 0xab;
        int srcX = 0x7;
        int srcY = 0x0;

        ByteBuffer data = allocate(18).order(LITTLE_ENDIAN).putShort(PADDING);
        data.put(flags.value);
        data.put((byte) tag);
        data.put((byte) destPortCPU);
        data.put((byte) srcPortCPU);
        data.put((byte) destY);
        data.put((byte) destX);
        data.put((byte) srcY);
        data.put((byte) srcX);
        data.putShort(rc.value);
        data.putShort((short) seq);
        data.putInt(argumentCount);
        data.flip();

        Response response = new CountState.Response(data);
        assertEquals(5, response.count);
	}

	@Test
	void testFailedDeserialise() throws UnsupportedEncodingException {
		// SCP Stuff
		SCPResult rc = SCPResult.RC_TIMEOUT;
		short seq = 105;
		short p2pAddr = 1024;
		byte physCPU = 31;
		byte virtCPU = 14;
		short version = 234;
		short buffer = 250;
		int buildDate = 103117;
		byte[] verString = "sark/spinnaker".getBytes("ASCII");

		// SDP stuff
		Flag flags = SDPHeader.Flag.REPLY_NOT_EXPECTED;
		byte tag = 5;
		byte destPortCPU = 0x4f;
		byte srcPortCPU = 0x6a;
		byte destX = 0x11;
		byte destY = (byte) 0xab;
		byte srcX = 0x7;
		byte srcY = 0x0;

		ByteBuffer data = allocate(41).order(LITTLE_ENDIAN).putShort(PADDING);
		data.put(flags.value).put(tag).put(destPortCPU).put(srcPortCPU);
		data.put(destY).put(destX).put(srcY).put(srcX);
		data.putShort(rc.value).putShort(seq).putShort(p2pAddr);
		data.put(physCPU).put(virtCPU);
		data.putShort(version).putShort(buffer).putInt(buildDate);
		data.put(verString);
		data.flip();

		assertThrows(UnexpectedResponseCodeException.class,
				() -> new CountState.Response(data));
	}
}
