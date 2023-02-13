/*
 * Copyright (c) 2018-2023 The University of Manchester
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package uk.ac.manchester.spinnaker.messages.scp;

import static uk.ac.manchester.spinnaker.utils.CollectionUtils.makeEnumBackingMap;

import java.util.Map;

/** The SCP Result codes. */
public enum SCPResult {
	/** Command completed successfully. */
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

	private static final Map<Short, SCPResult> MAP =
			makeEnumBackingMap(values(), v -> v.value);

	SCPResult(int value) {
		this.value = (short) value;
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
