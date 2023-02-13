/*
 * Copyright (c) 2018 The University of Manchester
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
package uk.ac.manchester.spinnaker.messages.eieio;

import static uk.ac.manchester.spinnaker.messages.eieio.EIEIOCommandID.STOP_SENDING_REQUESTS;

/**
 * Packet used in the context of buffering input for the host computer to signal
 * to the SpiNNaker system that to stop sending "SpinnakerRequestBuffers"
 * packet.
 */
public final class StopRequests extends EIEIOCommandMessage {
	/** Make an instance. */
	public StopRequests() {
		super(STOP_SENDING_REQUESTS);
	}
}
