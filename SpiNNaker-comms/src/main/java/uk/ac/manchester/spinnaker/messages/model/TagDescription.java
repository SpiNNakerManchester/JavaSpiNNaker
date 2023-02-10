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
package uk.ac.manchester.spinnaker.messages.model;

import java.net.InetAddress;

import uk.ac.manchester.spinnaker.machine.ChipLocation;
import uk.ac.manchester.spinnaker.machine.CoreLocation;
import uk.ac.manchester.spinnaker.machine.tags.IPTag;
import uk.ac.manchester.spinnaker.machine.tags.ReverseIPTag;
import uk.ac.manchester.spinnaker.machine.tags.Tag;

/**
 * Description of a tag.
 *
 * @param count
 *            The count of the number of packets that have been sent with the
 *            tag.
 * @param flags
 *            The flags of the tag.
 * @param ipAddress
 *            The IP address of the tag.
 * @param macAddress
 *            The MAC address of the tag, as an array of 6 bytes.
 * @param port
 *            The port of the tag.
 * @param rxPort
 *            The receive port of the tag.
 * @param spinCore
 *            The location of the core on the chip which the tag is defined on
 *            and where the core that handles the tag's messages resides.
 * @param spinPort
 *            The spin-port of the IP tag.
 * @param timeout
 *            The timeout of the tag.
 * @param src
 *            Where did the message that we've parsed this from originate from?
 * @param host
 *            On what SpiNNaker board did this info originate? Assumes we were
 *            talking directly to the board.
 * @param tagId
 *            What tag was this info about?
 * @author Donal Fellows
 */
public record TagDescription(int count, short flags, InetAddress ipAddress,
		byte[] macAddress, int port, int rxPort, CoreLocation spinCore,
		int spinPort, IPTagTimeOutWaitTime timeout, ChipLocation src,
		InetAddress host, int tagId) {
	private static final int USE_BIT = 15;

	private static final int TEMP_BIT = 14;

	private static final int ARP_BIT = 13;

	private static final int REV_BIT = 9;

	private static final int STRIP_BIT = 8;

	private boolean bitset(int bit) {
		return (flags & (1 << bit)) != 0;
	}

	/** @return True if the tag is marked as being in use. */
	public boolean inUse() {
		return bitset(USE_BIT);
	}

	/** @return True if the tag is temporary. */
	public boolean temporary() {
		return bitset(TEMP_BIT);
	}

	/**
	 * @return True if the tag is in the ARP state (where the MAC address is
	 *         being looked up; this is a transient state so unlikely).
	 */
	public boolean arp() {
		return bitset(ARP_BIT);
	}

	/** @return True if the tag is a reverse tag. */
	public boolean reverse() {
		return bitset(REV_BIT);
	}

	/** @return True if the tag is to strip the SDP header. */
	public boolean strippingSDP() {
		return bitset(STRIP_BIT);
	}

	/**
	 * Get the standard tag descriptor. Not properly meaningful unless the tag
	 * is {@linkplain #isInUse() in use}.
	 *
	 * @return The tag descriptor. May be an {@link IPTag} or a
	 *         {@link ReverseIPTag}.
	 */
	public Tag tag() {
		if (reverse()) {
			return new ReverseIPTag(host, tagId, rxPort, spinCore, spinPort);
		} else {
			return new IPTag(host, src, tagId, ipAddress, port, strippingSDP());
		}
	}
}
