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

import static java.net.InetAddress.getByName;
import static uk.ac.manchester.spinnaker.machine.tags.TrafficIdentifier.DEFAULT;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.Valid;
import uk.ac.manchester.spinnaker.machine.ChipLocation;

/**
 * Used to hold data that is contained within an IP tag. IP tags allow data to
 * flow at runtime from SpiNNaker to the outside world.
 */
public final class IPTag extends Tag {
	/** Default traffic identifier. */
	public static final TrafficIdentifier DEFAULT_TRAFFIC_IDENTIFIER = DEFAULT;

	private static final boolean DEFAULT_STRIP_SDP = false;

	private static final Integer DEFAULT_PORT = null;

	/** The IP address to which SDP packets with the tag will be sent. */
	private final InetAddress ipAddress;

	/** Indicates whether the SDP header should be removed. */
	private final boolean stripSDP;

	/** The identifier for traffic transmitted using this tag. */
	private final TrafficIdentifier trafficIdentifier;

	/** The coordinates where users of this tag should send packets to. */
	@Valid
	private final ChipLocation destination;

	/**
	 * Create an IP tag.
	 *
	 * @param boardAddress
	 *            The IP address of the board on which the tag is allocated
	 * @param destination
	 *            The coordinates where users of this tag should send packets to
	 * @param tagID
	 *            The ID of the tag (0..7)
	 * @param targetAddress
	 *            The IP address to which SDP packets with the tag will be sent
	 */
	public IPTag(InetAddress boardAddress, ChipLocation destination, int tagID,
			InetAddress targetAddress) {
		this(boardAddress, destination, tagID, targetAddress, DEFAULT_PORT,
				DEFAULT_STRIP_SDP, DEFAULT_TRAFFIC_IDENTIFIER);
	}

	/**
	 * Create an IP tag.
	 *
	 * @param boardAddress
	 *            The IP address of the board on which the tag is allocated
	 * @param destination
	 *            The coordinates where users of this tag should send packets to
	 * @param tagID
	 *            The ID of the tag (0..7)
	 * @param targetAddress
	 *            The IP address to which SDP packets with the tag will be sent
	 * @param udpPort
	 *            The port to which the SDP packets with the tag will be sent,
	 *            or {@code null} if not yet assigned.
	 */
	public IPTag(InetAddress boardAddress, ChipLocation destination, int tagID,
			InetAddress targetAddress, Integer udpPort) {
		this(boardAddress, destination, tagID, targetAddress, udpPort,
				DEFAULT_STRIP_SDP, DEFAULT_TRAFFIC_IDENTIFIER);
	}

	/**
	 * @param boardAddress
	 *            The IP address of the board on which the tag is allocated
	 * @param destination
	 *            The coordinates where users of this tag should send packets to
	 * @param tagID
	 *            The ID of the tag (0..7)
	 * @param targetAddress
	 *            The IP address to which SDP packets with the tag will be sent
	 * @param stripSDP
	 *            Indicates whether the SDP header should be removed
	 */
	public IPTag(InetAddress boardAddress, ChipLocation destination, int tagID,
			InetAddress targetAddress, boolean stripSDP) {
		this(boardAddress, destination, tagID, targetAddress, DEFAULT_PORT,
				stripSDP, DEFAULT_TRAFFIC_IDENTIFIER);
	}

	/**
	 * @param boardAddress
	 *            The IP address of the board on which the tag is allocated
	 * @param destination
	 *            The coordinates where users of this tag should send packets to
	 * @param tagID
	 *            The ID of the tag (0..7)
	 * @param targetAddress
	 *            The IP address to which SDP packets with the tag will be sent
	 * @param udpPort
	 *            The port to which the SDP packets with the tag will be sent,
	 *            or {@code null} if not yet assigned.
	 * @param stripSDP
	 *            Indicates whether the SDP header should be removed
	 */
	public IPTag(InetAddress boardAddress, ChipLocation destination, int tagID,
			InetAddress targetAddress, Integer udpPort, boolean stripSDP) {
		this(boardAddress, destination, tagID, targetAddress, udpPort, stripSDP,
				DEFAULT_TRAFFIC_IDENTIFIER);
	}

	/**
	 * @param boardAddress
	 *            The IP address of the board on which the tag is allocated
	 * @param destination
	 *            The coordinates where users of this tag should send packets to
	 * @param tagID
	 *            The ID of the tag (0..7)
	 * @param targetAddress
	 *            The IP address to which SDP packets with the tag will be sent
	 * @param udpPort
	 *            The port to which the SDP packets with the tag will be sent,
	 *            or {@code null} if not yet assigned.
	 * @param stripSDP
	 *            Indicates whether the SDP header should be removed
	 * @param trafficIdentifier
	 *            The identifier for traffic transmitted using this tag
	 */
	public IPTag(InetAddress boardAddress, ChipLocation destination, int tagID,
			InetAddress targetAddress, Integer udpPort, boolean stripSDP,
			TrafficIdentifier trafficIdentifier) {
		super(boardAddress, tagID, udpPort);
		this.destination = destination;
		this.ipAddress = targetAddress;
		this.stripSDP = stripSDP;
		this.trafficIdentifier = trafficIdentifier;
	}

	/**
	 * Constructor for JSON deserialization.
	 *
	 * @param boardAddress
	 *            The IP address of the board on which the tag is allocated
	 * @param tagID
	 *            The ID of the tag (0..7)
	 * @param x
	 *            The X coordinate of the destination.
	 * @param y
	 *            The Y coordinate of the destination.
	 * @param targetAddress
	 *            The IP address to which SDP packets with the tag will be sent
	 * @param udpPort
	 *            The port to which the SDP packets with the tag will be sent,
	 *            or {@code null} if not yet assigned.
	 * @param stripSDP
	 *            Indicates whether the SDP header should be removed
	 * @param trafficIdentifier
	 *            The identifier for traffic transmitted using this tag
	 * @throws UnknownHostException
	 *             If an IP address doesn't resolve.
	 */
	public IPTag(
			@JsonProperty(value = "boardAddress", required = true)
			String boardAddress,
			@JsonProperty(value = "tagID", required = true) int tagID,
			@JsonProperty(value = "x", required = true) int x,
			@JsonProperty(value = "y", required = true) int y,
			@JsonProperty(value = "targetAddress", required = true)
			String targetAddress,
			@JsonProperty(value = "port") Integer udpPort,
			@JsonProperty(value = "stripSDP") Boolean stripSDP,
			@JsonProperty(value = "trafficIdentifier")
			String trafficIdentifier)
			throws UnknownHostException {
		super(getByName(boardAddress), tagID,
				(udpPort == null ? DEFAULT_PORT : udpPort));
		destination = new ChipLocation(x, y);
		ipAddress = getByName(targetAddress);
		if (stripSDP == null) {
			this.stripSDP = DEFAULT_STRIP_SDP;
		} else {
			this.stripSDP = stripSDP;
		}
		if (trafficIdentifier == null) {
			this.trafficIdentifier = null;
		} else {
			this.trafficIdentifier =
					TrafficIdentifier.getInstance(trafficIdentifier);
		}
	}

	/**
	 * @return The IP address to which SDP packets with this tag will be sent.
	 */
	public InetAddress getIPAddress() {
		return ipAddress;
	}

	/** @return Return if the SDP header is to be stripped. */
	public boolean isStripSDP() {
		return stripSDP;
	}

	/** @return The identifier of traffic using this tag. */
	public TrafficIdentifier getTrafficIdentifier() {
		return trafficIdentifier;
	}

	/**
	 * @return The coordinates where users of this tag should send packets to.
	 */
	public ChipLocation getDestination() {
		return destination;
	}

	@Override
	public boolean equals(Object o) {
		if (o instanceof IPTag) {
			var otherTag = (IPTag) o;
			return partialEquals(otherTag)
					&& ipAddress.equals(otherTag.ipAddress)
					&& stripSDP == otherTag.stripSDP
					&& destination.equals(otherTag.destination);
		}
		return false;
	}

	@Override
	public int hashCode() {
		int h = partialHashCode();
		return h ^ Objects.hash(ipAddress, stripSDP, destination);
	}

	@Override
	public String toString() {
		var sb = new StringBuilder("IPTag(");
		sb.append(getTag()).append(" {").append(getBoardAddress());
		sb.append("} -");
		if (stripSDP) {
			sb.append("strip");
		}
		sb.append("-> {").append(getIPAddress());
		if (getPort() != null) {
			sb.append(":").append(getPort());
		}
		sb.append("}");
		if (trafficIdentifier != null) {
			sb.append(" TRF:").append(trafficIdentifier.label);
		}
		sb.append(" ").append(destination);
		return sb.append(")").toString();
	}
}
