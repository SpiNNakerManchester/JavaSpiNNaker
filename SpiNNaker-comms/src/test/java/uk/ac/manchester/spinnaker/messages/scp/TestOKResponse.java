package uk.ac.manchester.spinnaker.messages.scp;

import static java.nio.ByteBuffer.allocate;
import static java.nio.ByteOrder.LITTLE_ENDIAN;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static uk.ac.manchester.spinnaker.messages.scp.SCPCommand.CMD_DPRI;
import static uk.ac.manchester.spinnaker.messages.scp.SCPResult.RC_OK;
import static uk.ac.manchester.spinnaker.messages.scp.SCPResult.RC_TIMEOUT;
import static uk.ac.manchester.spinnaker.messages.sdp.SDPHeader.Flag.REPLY_NOT_EXPECTED;

import java.nio.ByteBuffer;

import org.junit.jupiter.api.Test;

import uk.ac.manchester.spinnaker.messages.model.UnexpectedResponseCodeException;

class TestOKResponse {
	private static final short PADDING = 0;

	private short encode_addr_tuple(int dest_port, int dest_cpu, int src_port,
			int src_cpu) {
		return (short) (dest_port << 13 | dest_cpu << 8 | src_port << 5
				| src_cpu);
	}

	@Test
	void testReadOKResponse() throws UnexpectedResponseCodeException {
		short result = RC_OK.value;
		byte flags = REPLY_NOT_EXPECTED.value;
		byte tag = 0x01;
		short flag_tag_short = (short) (tag << 8 | flags); // flags << 8 | tag
		byte dest_port = 7;
		byte dest_cpu = 15;
		byte src_port = 7;
		byte src_cpu = 31;
		short dest_source_short =
				encode_addr_tuple(dest_port, dest_cpu, src_port, src_cpu);

		byte dest_x = 1;
		byte dest_y = 8;

		short dest_x_y_short = (short) (dest_x << 8 | dest_y);

		byte src_x = (byte) 255;
		byte src_y = 0;

		short src_x_y_short = (short) (src_x << 8 | src_y);

		short seq = 103;
		ByteBuffer bytes = allocate(14).order(LITTLE_ENDIAN).putShort(PADDING);
		bytes.putShort(flag_tag_short).putShort(dest_source_short);
		bytes.putShort(dest_x_y_short).putShort(src_x_y_short);
		bytes.putShort(result).putShort(seq).flip();

		assertNotNull(
				new CheckOKResponse("testing operation", CMD_DPRI, bytes));
	}

	@Test
	void testNotOKResponse() throws UnexpectedResponseCodeException {
		short result = RC_TIMEOUT.value;
		byte flags = REPLY_NOT_EXPECTED.value;
		byte tag = 0x01;
		short flag_tag_short = (short) (tag << 8 | flags); // flags << 8 | tag
		byte dest_port = 7;
		byte dest_cpu = 15;
		byte src_port = 7;
		byte src_cpu = 31;
		short dest_source_short =
				encode_addr_tuple(dest_port, dest_cpu, src_port, src_cpu);

		byte dest_x = 1;
		byte dest_y = 8;

		short dest_x_y_short = (short) (dest_x << 8 | dest_y);

		byte src_x = (byte) 255;
		byte src_y = 0;

		short src_x_y_short = (short) (src_x << 8 | src_y);

		short seq = 103;
		ByteBuffer bytes = allocate(14).order(LITTLE_ENDIAN).putShort(PADDING);
		bytes.putShort(flag_tag_short).putShort(dest_source_short);
		bytes.putShort(dest_x_y_short).putShort(src_x_y_short);
		bytes.putShort(result).putShort(seq).flip();

		assertThrows(UnexpectedResponseCodeException.class,
				() -> new CheckOKResponse("testing operation", CMD_DPRI,
						bytes));
	}
}
