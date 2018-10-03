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

	private short encodeAddrTuple(int destPort, int destCPU, int srcPort,
			int srcCPU) {
		return (short) (destPort << 13 | destCPU << 8 | srcPort << 5 | srcCPU);
	}

	@Test
	void testReadOKResponse() throws UnexpectedResponseCodeException {
		short result = RC_OK.value;
		byte flags = REPLY_NOT_EXPECTED.value;
		byte tag = 0x01;
		short flagTagShort = (short) (tag << 8 | flags); // flags << 8 | tag
		byte destPort = 7;
		byte destCPU = 15;
		byte srcPort = 7;
		byte srcCPU = 31;
		short destSourceShort =
				encodeAddrTuple(destPort, destCPU, srcPort, srcCPU);

		byte destX = 1;
		byte destY = 8;

		short destXYShort = (short) (destX << 8 | destY);

		byte srcX = (byte) 255;
		byte srcY = 0;

		short srcXYShort = (short) (srcX << 8 | srcY);

		short seq = 103;
		ByteBuffer bytes = allocate(14).order(LITTLE_ENDIAN).putShort(PADDING);
		bytes.putShort(flagTagShort).putShort(destSourceShort);
		bytes.putShort(destXYShort).putShort(srcXYShort);
		bytes.putShort(result).putShort(seq).flip();

		assertNotNull(
				new CheckOKResponse("testing operation", CMD_DPRI, bytes));
	}

	@Test
	void testNotOKResponse() throws UnexpectedResponseCodeException {
		short result = RC_TIMEOUT.value;
		byte flags = REPLY_NOT_EXPECTED.value;
		byte tag = 0x01;
		short flagTagShort = (short) (tag << 8 | flags); // flags << 8 | tag
		byte destPort = 7;
		byte destCpu = 15;
		byte srcPort = 7;
		byte srcCpu = 31;
		short destSourceShort =
				encodeAddrTuple(destPort, destCpu, srcPort, srcCpu);

		byte destX = 1;
		byte destY = 8;

		short destXYShort = (short) (destX << 8 | destY);

		byte srcX = (byte) 255;
		byte srcY = 0;

		short srcXYShort = (short) (srcX << 8 | srcY);

		short seq = 103;
		ByteBuffer bytes = allocate(14).order(LITTLE_ENDIAN).putShort(PADDING);
		bytes.putShort(flagTagShort).putShort(destSourceShort);
		bytes.putShort(destXYShort).putShort(srcXYShort);
		bytes.putShort(result).putShort(seq).flip();

		assertThrows(UnexpectedResponseCodeException.class,
				() -> new CheckOKResponse("testing operation", CMD_DPRI,
						bytes));
	}
}
