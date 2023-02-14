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

import static java.net.InetAddress.getByAddress;
import static uk.ac.manchester.spinnaker.messages.model.IPTagFieldDefinitions.CORE_MASK;
import static uk.ac.manchester.spinnaker.messages.model.IPTagFieldDefinitions.PORT_SHIFT;
import static uk.ac.manchester.spinnaker.messages.model.IPTagFieldDefinitions.THREE_BITS_MASK;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;

import uk.ac.manchester.spinnaker.machine.ChipLocation;
import uk.ac.manchester.spinnaker.machine.CoreLocation;
import uk.ac.manchester.spinnaker.machine.HasChipLocation;
import uk.ac.manchester.spinnaker.machine.HasCoreLocation;
import uk.ac.manchester.spinnaker.machine.tags.IPTag;
import uk.ac.manchester.spinnaker.machine.tags.ReverseIPTag;
import uk.ac.manchester.spinnaker.machine.tags.Tag;

/** Description of a tag. */
public final class TagDescription {
	// TODO convert to a record in Java 17
	/**
	 * The count of the number of packets that have been sent with the tag.
	 */
	public final int count;

	/** The flags of the tag. */
	public final short flags;

	/** The IP address of the tag. */
	public final InetAddress ipAddress;

	/** The MAC address of the tag, as an array of 6 bytes. */
	public final byte[] macAddress;

	/** The port of the tag. */
	public final int port;

	/** The receive port of the tag. */
	public final int rxPort;

	/**
	 * The location of the core on the chip which the tag is defined on and
	 * where the core that handles the tag's messages resides.
	 */
	public final HasCoreLocation spinCore;

	/** The spin-port of the IP tag. */
	public final int spinPort;

	/** The timeout of the tag. */
	public final IPTagTimeOutWaitTime timeout;

	/** Where did the message that we've parsed this from originate from? */
	private final ChipLocation src;

	/**
	 * On what SpiNNaker board did this info originate? Assumes we were talking
	 * directly to the board.
	 */
	private final InetAddress host;

	/** What tag was this info about? */
	private final int tag;

	/**
	 * @param buffer
	 *            The buffer to parse.
	 * @param src
	 *            What chip did this come from?
	 * @param host
	 *            What address did that chip have? (Only for creating a
	 *            {@link Tag}.)
	 * @param tag
	 *            What was the tag ID? (Only for creating a {@link Tag}.)
	 * @throws UnknownHostException
	 *             If we can't parse the host. Unexpected.
	 */
	public TagDescription(ByteBuffer buffer, HasChipLocation src,
			InetAddress host, int tag) throws UnknownHostException {
		byte[] ipBytes = new byte[IPV4_BYTES];
		buffer.get(ipBytes);
		ipAddress = getByAddress(ipBytes);

		macAddress = new byte[MAC_BYTES];
		buffer.get(macAddress);

		port = Short.toUnsignedInt(buffer.getShort());
		timeout = IPTagTimeOutWaitTime.get(buffer.getShort());
		flags = buffer.getShort();
		count = buffer.getInt();
		rxPort = Short.toUnsignedInt(buffer.getShort());
		int y = Byte.toUnsignedInt(buffer.get());
		int x = Byte.toUnsignedInt(buffer.get());
		int pp = Byte.toUnsignedInt(buffer.get());
		spinCore = new CoreLocation(x, y, pp & CORE_MASK);
		spinPort = (pp >>> PORT_SHIFT) & THREE_BITS_MASK;
		this.src = src.asChipLocation();
		this.host = host;
		this.tag = tag;
	}

	private static final int IPV4_BYTES = 4;

	private static final int MAC_BYTES = 6;

	private static final int USE_BIT = 15;

	private static final int TEMP_BIT = 14;

	private static final int ARP_BIT = 13;

	private static final int REV_BIT = 9;

	private static final int STRIP_BIT = 8;

	private boolean bitset(int bit) {
		return (flags & (1 << bit)) != 0;
	}

	/** @return True if the tag is marked as being in use. */
	public boolean isInUse() {
		return bitset(USE_BIT);
	}

	/** @return True if the tag is temporary. */
	public boolean isTemporary() {
		return bitset(TEMP_BIT);
	}

	/**
	 * @return True if the tag is in the ARP state (where the MAC address is
	 *         being looked up; this is a transient state so unlikely).
	 */
	public boolean isARP() {
		return bitset(ARP_BIT);
	}

	/** @return True if the tag is a reverse tag. */
	public boolean isReverse() {
		return bitset(REV_BIT);
	}

	/** @return True if the tag is to strip the SDP header. */
	public boolean isStrippingSDP() {
		return bitset(STRIP_BIT);
	}

	/**
	 * Get the standard tag descriptor. Not properly meaningful unless the tag
	 * is {@linkplain #isInUse() in use}.
	 *
	 * @return The tag descriptor. May be an {@link IPTag} or a
	 *         {@link ReverseIPTag}.
	 */
	public Tag getTag() {
		if (isReverse()) {
			return new ReverseIPTag(host, tag, rxPort, spinCore, spinPort);
		} else {
			return new IPTag(host, src, tag, ipAddress, port, isStrippingSDP());
		}
	}
}
