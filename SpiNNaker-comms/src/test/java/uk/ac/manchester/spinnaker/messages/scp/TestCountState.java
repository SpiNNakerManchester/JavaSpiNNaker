package uk.ac.manchester.spinnaker.messages.scp;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import org.junit.jupiter.api.Test;

import uk.ac.manchester.spinnaker.messages.model.CPUState;
import uk.ac.manchester.spinnaker.messages.model.UnexpectedResponseCodeException;
import uk.ac.manchester.spinnaker.messages.scp.CountState.Response;
import uk.ac.manchester.spinnaker.messages.sdp.SDPHeader;
import uk.ac.manchester.spinnaker.messages.sdp.SDPHeader.Flag;

class TestCountState {

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

        ByteBuffer data = ByteBuffer.allocate(16).order(ByteOrder.LITTLE_ENDIAN);
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
}
