/*
 * Copyright (c) 2018-2019 The University of Manchester
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
package uk.ac.manchester.spinnaker.machine.tags;

import java.net.InetAddress;
import java.util.Objects;

import javax.validation.Valid;

import uk.ac.manchester.spinnaker.machine.CoreLocation;
import uk.ac.manchester.spinnaker.machine.HasCoreLocation;

/**
 * Used to hold data that is contained within a Reverse IP tag. Reverse IP tags
 * allow data to flow at runtime from the outside world into SpiNNaker.
 */
public final class ReverseIPTag extends Tag {
	private static final int DEFAULT_SDP_PORT = 1;

	@Valid
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
		return (o instanceof ReverseIPTag otherTag) && partialEquals(otherTag)
				&& (sdpPort == otherTag.sdpPort)
				&& destination.equals(otherTag.destination);
	}

	@Override
	public int hashCode() {
		return partialHashCode() ^ Objects.hash(sdpPort, destination);
	}

	@Override
	public String toString() {
		var sb = new StringBuilder("ReverseIPTag(");
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
