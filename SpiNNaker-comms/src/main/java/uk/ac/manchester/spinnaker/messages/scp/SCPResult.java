package uk.ac.manchester.spinnaker.messages.scp;

import java.util.HashMap;
import java.util.Map;

/** The SCP Result codes */
public enum SCPResult {
	/** SCPCommand completed OK */
	RC_OK(0x80),
	/** Bad packet length */
	RC_LEN(0x81),
	/** Bad checksum */
	RC_SUM(0x82),
	/** Bad/invalid command */
	RC_CMD(0x83),
	/** Invalid arguments */
	RC_ARG(0x84),
	/** Bad port number */
	RC_PORT(0x85),
	/** Timeout */
	RC_TIMEOUT(0x86),
	/** No P2P route */
	RC_ROUTE(0x87),
	/** Bad CPU number */
	RC_CPU(0x88),
	/** SHM destination dead */
	RC_DEAD(0x89),
	/** No free Shared Memory buffers */
	RC_BUF(0x8a),
	/** No reply to open */
	RC_P2P_NOREPLY(0x8b),
	/** Open rejected */
	RC_P2P_REJECT(0x8c),
	/** Destination busy */
	RC_P2P_BUSY(0x8d),
	/** Dest did not respond */
	RC_P2P_TIMEOUT(0x8e),
	/** Pkt Transmission failed */
	RC_PKT_TX(0x8f);
	public final byte value;
	private static final Map<Byte, SCPResult> map = new HashMap<>();

	private SCPResult(int value) {
		this.value = (byte) value;
	}

	static {
		for (SCPResult r : values()) {
			map.put(r.value, r);
		}
	}

	public static SCPResult get(byte value) {
		return map.get(value);
	}
}
