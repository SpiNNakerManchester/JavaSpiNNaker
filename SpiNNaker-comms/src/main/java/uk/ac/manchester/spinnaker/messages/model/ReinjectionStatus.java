/*
 * Copyright (c) 2019 The University of Manchester
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package uk.ac.manchester.spinnaker.messages.model;

import java.nio.ByteBuffer;

/**
 * Represents a status information message obtained from the dropped packet
 * reinjection core (an "extra monitor" core).
 *
 * @param timeout
 *            The WAIT1 timeout value of the router in cycles.
 * @param emergencyTimeout
 *            The WAIT2 timeout value of the router in cycles.
 * @param droppedPackets
 *            The number of packets dropped by the router and received by the
 *            reinjection functionality (may not fit in the queue though).
 * @param missedDroppedPackets
 *            The number of times that when a dropped packet was read it was
 *            found that another one or more packets had also been dropped, but
 *            had been missed.
 * @param droppedPacketOverflows
 *            Of the {@link #droppedPackets} received, how many were lost due to
 *            not having enough space in the queue of packets to reinject.
 * @param reinjectedPackets
 *            Of the {@link #droppedPackets} received, how many packets were
 *            successfully reinjected.
 * @param linkDumps
 *            The number of times that when a dropped packet was caused due to a
 *            link failing to take the packet.
 * @param processorDumps
 *            The number of times that when a dropped packet was caused due to a
 *            processor failing to take the packet.
 * @param flags
 *            The flags that states which types of packets were being recorded.
 */
public record ReinjectionStatus(RouterTimeout timeout,
		RouterTimeout emergencyTimeout, int droppedPackets,
		int missedDroppedPackets, int droppedPacketOverflows,
		int reinjectedPackets, int linkDumps, int processorDumps, int flags) {
	/** Used to pick low nybble of value. */
	static final int MASK = 0xF;

	/** Used to move value by one nybble. */
	static final int SHIFT = 4;

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

	/**
	 * @param buffer
	 *            The message containing the status to parse.
	 */
	public ReinjectionStatus(ByteBuffer buffer) {
		this(new RouterTimeout(buffer.getInt()),
				new RouterTimeout(buffer.getInt()), buffer.getInt(),
				buffer.getInt(), buffer.getInt(), buffer.getInt(),
				buffer.getInt(), buffer.getInt(), buffer.getInt());
	}

	private boolean flag(DPRIFlags flag) {
		return (flags & flag.mask()) != 0;
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

		private int mask() {
			return 1 << ordinal();
		}
	}

	@Override
	public String toString() {
		return "flags:" + flags + ",timeout:" + timeout + ",emergencyTimeout:"
				+ emergencyTimeout + ",drops:" + droppedPackets;
	}
}
