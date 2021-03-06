/*
 * Copyright (c) 2018-2019 The University of Manchester
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
package uk.ac.manchester.spinnaker.machine.tags;

import static java.lang.Integer.rotateLeft;

import java.net.InetAddress;

import uk.ac.manchester.spinnaker.machine.CoreLocation;
import uk.ac.manchester.spinnaker.machine.HasCoreLocation;

/**
 * Used to hold data that is contained within a Reverse IP tag. Reverse IP tags
 * allow data to flow at runtime from the outside world into SpiNNaker.
 */
public final class ReverseIPTag extends Tag {
	private static final int DEFAULT_SDP_PORT = 1;

	private final CoreLocation destination;

	private final int sdpPort;

	/**
	 * Create a reverse IP tag.
	 *
	 * @param boardAddress
	 *            The IP address of the board on which the tag is allocated.
	 * @param tagID
	 *            The tag of the SDP packet.
	 * @param udpPort
	 *            The UDP port on which SpiNNaker will listen for packets.
	 * @param destination
	 *            The coordinates of the core to send packets to.
	 */
	public ReverseIPTag(InetAddress boardAddress, int tagID, int udpPort,
			HasCoreLocation destination) {
		this(boardAddress, tagID, udpPort, destination, DEFAULT_SDP_PORT);
	}

	/**
	 * Create a reverse IP tag.
	 *
	 * @param boardAddress
	 *            The IP address of the board on which the tag is allocated.
	 * @param tagID
	 *            The tag of the SDP packet.
	 * @param udpPort
	 *            The UDP port on which SpiNNaker will listen for packets.
	 * @param destination
	 *            The coordinates of the core to send packets to.
	 * @param sdpPort
	 *            The port number to use for SDP packets that are formed on the
	 *            machine.
	 */
	public ReverseIPTag(InetAddress boardAddress, int tagID, int udpPort,
			HasCoreLocation destination, int sdpPort) {
		super(boardAddress, tagID, udpPort);
		this.destination = destination.asCoreLocation();
		this.sdpPort = sdpPort;
	}

	/**
	 * @return The destination coordinates of a core in the SpiNNaker machine
	 *         that packets should be sent to for this reverse IP tag.
	 */
	public CoreLocation getDestination() {
		return destination;
	}

	/**
	 * @return The SDP port number of the tag that these packets are to be
	 *         received on for the processor.
	 */
	public int getSdpPort() {
		return sdpPort;
	}

	@Override
	public boolean equals(Object o) {
		return (o instanceof ReverseIPTag) && equals((ReverseIPTag) o);
	}

	/**
	 * An optimised test for whether two {@link ReverseIPTag}s are equal.
	 *
	 * @param otherTag
	 *            The other tag
	 * @return whether they are equal
	 */
	public boolean equals(ReverseIPTag otherTag) {
		if (otherTag == null) {
			return false;
		}
		return partialEquals(otherTag) && sdpPort == otherTag.sdpPort
				&& destination.equals(otherTag.destination);
	}

	@Override
	public int hashCode() {
		int h = partialHashCode();
		h ^= rotateLeft(sdpPort, 11);
		h ^= rotateLeft(destination.hashCode(), 19);
		return h;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder("ReverseIPTag(");
		sb.append(getTag()).append(" {").append(getBoardAddress());
		if (getPort() != null) {
			sb.append(":").append(getPort());
		}
		sb.append("} <-- {?:?}");
		sb.append(" : ").append(sdpPort);
		sb.append(" : ").append(destination);
		return sb.append(")").toString();
	}
}
