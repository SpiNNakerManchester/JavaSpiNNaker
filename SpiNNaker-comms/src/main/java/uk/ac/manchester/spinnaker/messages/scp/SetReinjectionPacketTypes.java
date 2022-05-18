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
package uk.ac.manchester.spinnaker.messages.scp;

import static uk.ac.manchester.spinnaker.messages.scp.ReinjectorCommand.SET_PACKET_TYPES;

import java.nio.ByteBuffer;

import uk.ac.manchester.spinnaker.machine.HasCoreLocation;

/**
 * An SCP Request to set the dropped packet reinjected packet types.
 */
public class SetReinjectionPacketTypes extends SCPRequest<CheckOKResponse> {
	/**
	 * @param core
	 *            The coordinates of the monitor core.
	 * @param multicast
	 *            If multicast should be set
	 * @param pointToPoint
	 *            If point-to-point should be set
	 * @param fixedRoute
	 *            If fixed-route should be set
	 * @param nearestNeighbour
	 *            If nearest-neighbour should be set
	 */
	public SetReinjectionPacketTypes(HasCoreLocation core, boolean multicast,
			boolean pointToPoint, boolean fixedRoute,
			boolean nearestNeighbour) {
		super(new ReinjectionSDPHeader(core), SET_PACKET_TYPES,
				encode(multicast), encode(pointToPoint), encode(fixedRoute),
				encodeAsBA(nearestNeighbour));
	}

	private static ByteBuffer encodeAsBA(boolean flag) {
		var b = ByteBuffer.allocate(1);
		b.put(encode(flag));
		return b.flip();
	}

	private static byte encode(boolean flag) {
		return (byte) (flag ? 1 : 0);
	}

	@Override
	public CheckOKResponse getSCPResponse(ByteBuffer buffer) throws Exception {
		return new CheckOKResponse("Set reinjected packet types",
				SET_PACKET_TYPES, buffer);
	}
}
