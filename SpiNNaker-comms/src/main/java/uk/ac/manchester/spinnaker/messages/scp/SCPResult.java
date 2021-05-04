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

import java.util.HashMap;
import java.util.Map;

/** The SCP Result codes. */
public enum SCPResult {
	/** SCPCommand completed OK. */
	RC_OK(0x80),
	/** Bad packet length. */
	RC_LEN(0x81),
	/** Bad checksum. */
	RC_SUM(0x82),
	/** Bad/invalid command. */
	RC_CMD(0x83),
	/** Invalid arguments. */
	RC_ARG(0x84),
	/** Bad port number. */
	RC_PORT(0x85),
	/** Timeout. */
	RC_TIMEOUT(0x86),
	/** No P2P route. */
	RC_ROUTE(0x87),
	/** Bad CPU number. */
	RC_CPU(0x88),
	/** SHM destination dead. */
	RC_DEAD(0x89),
	/** No free shared memory buffers. */
	RC_BUF(0x8a),
	/** No reply to open. */
	RC_P2P_NOREPLY(0x8b),
	/** Message was rejected. */
	RC_P2P_REJECT(0x8c),
	/** Destination busy. */
	RC_P2P_BUSY(0x8d),
	/** Destination did not respond. */
	RC_P2P_TIMEOUT(0x8e),
	/** Packet transmission failed. */
	RC_PKT_TX(0x8f);

	/** The encoded result value. */
	public final short value;

	private static final Map<Short, SCPResult> MAP = new HashMap<>();

	SCPResult(int value) {
		this.value = (short) value;
	}

	static {
		for (SCPResult r : values()) {
			MAP.put(r.value, r);
		}
	}

	/**
	 * Decode a result.
	 *
	 * @param value
	 *            The value to decode
	 * @return The decoded value, or {@code null} if unrecognised.
	 */
	public static SCPResult get(short value) {
		return MAP.get(value);
	}
}
