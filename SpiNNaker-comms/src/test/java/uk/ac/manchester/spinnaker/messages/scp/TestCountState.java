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

        int argument_count = 5;

        // SDP stuff
        Flag flags = SDPHeader.Flag.REPLY_NOT_EXPECTED;
        int tag = 5;
        int dest_port_cpu = 0x4f;
        int src_port_cpu = 0x6a;
        int dest_x = 0x11;
        int dest_y = 0xab;
        int src_x = 0x7;
        int src_y = 0x0;

        ByteBuffer data = allocate(18).order(LITTLE_ENDIAN).putShort(PADDING);
        data.put(flags.value);
        data.put((byte) tag);
        data.put((byte) dest_port_cpu);
        data.put((byte) src_port_cpu);
        data.put((byte) dest_y);
        data.put((byte) dest_x);
        data.put((byte) src_y);
        data.put((byte) src_x);
        data.putShort(rc.value);
        data.putShort((short) seq);
        data.putInt(argument_count);
        data.flip();

        Response response = new CountState.Response(data);
        assertEquals(5, response.count);
	}

	@Test
	void testFailedDeserialise() throws UnsupportedEncodingException {
		// SCP Stuff
		SCPResult rc = SCPResult.RC_TIMEOUT;
		short seq = 105;
		short p2p_addr = 1024;
		byte phys_cpu = 31;
		byte virt_cpu = 14;
		short version = 234;
		short buffer = 250;
		int build_date = 103117;
		byte[] ver_string = "sark/spinnaker".getBytes("ASCII");

		// SDP stuff
		Flag flags = SDPHeader.Flag.REPLY_NOT_EXPECTED;
		byte tag = 5;
		byte dest_port_cpu = 0x4f;
		byte src_port_cpu = 0x6a;
		byte dest_x = 0x11;
		byte dest_y = (byte) 0xab;
		byte src_x = 0x7;
		byte src_y = 0x0;

		ByteBuffer data = allocate(41).order(LITTLE_ENDIAN).putShort(PADDING);
		data.put(flags.value).put(tag).put(dest_port_cpu).put(src_port_cpu);
		data.put(dest_y).put(dest_x).put(src_y).put(src_x);
		data.putShort(rc.value).putShort(seq).putShort(p2p_addr);
		data.put(phys_cpu).put(virt_cpu);
		data.putShort(version).putShort(buffer).putInt(build_date);
		data.put(ver_string);
		data.flip();

		assertThrows(UnexpectedResponseCodeException.class,
				() -> new CountState.Response(data));
	}
}
