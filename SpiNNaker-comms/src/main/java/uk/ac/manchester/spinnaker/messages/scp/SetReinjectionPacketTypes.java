/*
 * Copyright (c) 2019 The University of Manchester
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

import static java.nio.ByteBuffer.allocate;
import static uk.ac.manchester.spinnaker.messages.scp.ReinjectorCommand.SET_PACKET_TYPES;

import java.nio.ByteBuffer;

import uk.ac.manchester.spinnaker.machine.HasCoreLocation;
import uk.ac.manchester.spinnaker.messages.model.UnexpectedResponseCodeException;

/**
 * An SCP Request to set the dropped packet reinjected packet types. There is no
 * response payload.
 * <p>
 * Handled by {@code reinjection_set_packet_types()} in
 * {@code extra_monitor_support.c}.
 */
public final class SetReinjectionPacketTypes
		extends ReinjectorRequest<EmptyResponse> {
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
		super(core, SET_PACKET_TYPES, encode(multicast), encode(pointToPoint),
				encode(fixedRoute), encodeAsBA(nearestNeighbour));
	}

	private static ByteBuffer encodeAsBA(boolean flag) {
		var b = allocate(1);
		b.put(encode(flag));
		return b.flip();
	}

	private static byte encode(boolean flag) {
		return (byte) (flag ? 1 : 0);
	}

	@Override
	public EmptyResponse getSCPResponse(ByteBuffer buffer)
			throws UnexpectedResponseCodeException {
		return new EmptyResponse("Set reinjected packet types",
				SET_PACKET_TYPES, buffer);
	}
}
