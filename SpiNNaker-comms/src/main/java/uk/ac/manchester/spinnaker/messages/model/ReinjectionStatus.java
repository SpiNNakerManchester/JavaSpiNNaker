/*
 * Copyright (c) 2019 The University of Manchester
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
package uk.ac.manchester.spinnaker.messages.model;

import java.nio.ByteBuffer;

/** Represents a status information from dropped packet reinjection. */
public class ReinjectionStatus {
	private static final int MASK = 0xF;
	private static final int SHIFT = 4;
	private static final int MANTISSA_OFFSET = 16;
	private static final int EXPONENT_OFFSET = 4;

	/**
	 * How to convert a timeout from mantissa and exponent into a single 8-bit
	 * float.
	 *
	 * @param mantissa
	 *            The timeout mantissa.
	 * @param exponent
	 *            The timeout exponent.
	 * @return The encoded value.
	 */
	public static int encodeTimeout(int mantissa, int exponent) {
		return (mantissa & MASK) | ((exponent & MASK) << SHIFT);
	}

	/** The WAIT1 timeout value of the router in cycles. */
	private final int routerTimeout;

	/** The WAIT2 timeout value of the router in cycles. */
	private final int routerEmergencyTimeout;

	/**
	 * The number of packets dropped by the router and received by the
	 * reinjection functionality (may not fit in the queue though).
	 */
	private final int numDroppedPackets;

	/**
	 * The number of times that when a dropped packet was read it was found that
	 * another one or more packets had also been dropped, but had been missed.
	 */
	private final int numMissedDroppedPackets;

	/**
	 * Of the {@link #numDroppedPackets} received, how many were lost due to not
	 * having enough space in the queue of packets to reinject.
	 */
	private final int numDroppedPacketOverflows;

	/**
	 * Of the {@link #numDroppedPackets} received, how many packets were
	 * successfully reinjected.
	 */
	private final int numReinjectedPackets;

	/**
	 * The number of times that when a dropped packet was caused due to a link
	 * failing to take the packet.
	 */
	private final int numLinkDumps;

	/**
	 * The number of times that when a dropped packet was caused due to a
	 * processor failing to take the packet.
	 */
	private final int numProcessorDumps;

	/** The flags that states which types of packets were being recorded. */
	private final int flags;

	public ReinjectionStatus(ByteBuffer buffer) {
		this.routerTimeout = buffer.getInt();
		this.routerEmergencyTimeout = buffer.getInt();
		this.numDroppedPackets = buffer.getInt();
		this.numMissedDroppedPackets = buffer.getInt();
		this.numDroppedPacketOverflows = buffer.getInt();
		this.numReinjectedPackets = buffer.getInt();
		this.numLinkDumps = buffer.getInt();
		this.numProcessorDumps = buffer.getInt();
		this.flags = buffer.getInt();
	}

	/**
	 * Get the timeout value of a router in ticks, given an 8-bit floating point
	 * value stored in an int (!).
	 *
	 * @param value
	 *            The value to convert.
	 */
	private static int decodeTimeout(int value) {
		int mantissa = (value & MASK) + MANTISSA_OFFSET;
		int exponent = (value >> SHIFT) & MASK;
		if (exponent <= EXPONENT_OFFSET) {
			return (mantissa - (1 << (EXPONENT_OFFSET - exponent)))
					* (1 << exponent);
		}
		return mantissa * (1 << exponent);
	}

	/** @return The mantissa of the WAIT1 timeout value. */
	public int getRouterTimeoutMantissa() {
		return (routerTimeout & MASK);
	}

	/** @return The exponent of the WAIT1 timeout value. */
	public int getRouterTimeoutExponent() {
		return (routerTimeout >> SHIFT) & MASK;
	}

	/** @return The WAIT1 timeout value of the router in cycles. */
	public int getRouterTimeout() {
		return decodeTimeout(routerTimeout);
	}

	/** @return The mantissa of the WAIT2 timeout value. */
	public int getRouterEmergencyTimeoutMantissa() {
		return (routerEmergencyTimeout & MASK);
	}

	/** @return The exponent of the WAIT2 timeout value. */
	public int getRouterEmergencyTimeoutExponent() {
		return (routerEmergencyTimeout >> SHIFT) & MASK;
	}

	/** @return The WAIT2 timeout value of the router in cycles. */
	public int getRouterEmergencyTimeout() {
		return decodeTimeout(routerEmergencyTimeout);
	}

	/**
	 * @return The number of packets dropped by the router and received by the
	 *         reinjection functionality (may not fit in the queue though).
	 */
	public int getNumDroppedPackets() {
		return numDroppedPackets;
	}

	/**
	 * @return The number of times that when a dropped packet was read it was
	 *         found that another one or more packets had also been dropped, but
	 *         had been missed.
	 */
	public int getNumMissedDroppedPackets() {
		return numMissedDroppedPackets;
	}

	/**
	 * @return Of the n_dropped_packets received, how many were lost due to not
	 *         having enough space in the queue of packets to reinject.
	 */
	public int getNumDroppedPacketOverflows() {
		return numDroppedPacketOverflows;
	}

	/**
	 * @return The number of times that when a dropped packet was caused due to
	 *         a processor failing to take the packet.
	 */
	public int getNumProcessorDumps() {
		return numProcessorDumps;
	}

	/**
	 * @return The number of times that when a dropped packet was caused due to
	 *         a link failing to take the packet.
	 */
	public int getNumLinkDumps() {
		return numLinkDumps;
	}

	/**
	 * @return Of the number of dropped packets received, how many packets were
	 *         successfully reinjected.
	 */
	public int getNumReinjectedPackets() {
		return numReinjectedPackets;
	}

	private boolean flag(DPRIFlags flag) {
		return (flags & (1 << flag.ordinal())) != 0;
	}

	/** @return if re-injection of multicast packets is enabled. */
	public boolean isReinjectingMulticast() {
		return flag(DPRIFlags.MULTICAST);
	}

	/** @return if re-injection of point-to-point packets is enabled. */
	public boolean isReinjectingPointToPoint() {
		return flag(DPRIFlags.POINT_TO_POINT);
	}

	/** @return if re-injection of nearest neighbour packets is enabled. */
	public boolean isReinjectingNearestNeighbour() {
		return flag(DPRIFlags.NEAREST_NEIGHBOUR);
	}

	/** @return if re-injection of fixed-route packets is enabled. */
	public boolean isReinjectingFixedRoute() {
		return flag(DPRIFlags.FIXED_ROUTE);
	}

	private enum DPRIFlags {
		MULTICAST, POINT_TO_POINT, NEAREST_NEIGHBOUR, FIXED_ROUTE;
	}
}
