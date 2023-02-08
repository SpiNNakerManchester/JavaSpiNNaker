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

/** SCP Command codes for reinjection. */
enum ReinjectorCommand implements CommandCode {
	/** Set the router's main timeout. */
	SET_ROUTER_TIMEOUT(0),
	/** Set the router's emergency timeout. */
	SET_ROUTER_EMERGENCY_TIMEOUT(1),
	/** Set what packet types are reinjected. */
	SET_PACKET_TYPES(2),
	/** Get the status of the reinjector. */
	GET_STATUS(3),
	/** Reset the counters inside the reinjector. */
	RESET_COUNTERS(4),
	/** Stop the reinjector. */
	EXIT(5),
	/** Clear the reinjector's queues. */
	CLEAR(6);

	/**
	 * The encoded form of the command.
	 */
	private final short value;

	ReinjectorCommand(int cmd) {
		this.value = (short) cmd;
	}

	@Override
	public short getValue() {
		return value;
	}
}
