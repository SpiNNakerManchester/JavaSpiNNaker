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

import static uk.ac.manchester.spinnaker.messages.eieio.EIEIOCommandMessage.peekCommand;

import java.nio.ByteBuffer;

/**
 * Main interface for deserialising a message.
 *
 * @author Donal Fellows
 */
public abstract class EIEIOMessageFactory {
	private EIEIOMessageFactory() {
	}

	/**
	 * Reads the content of an EIEIO command message and returns an object
	 * identifying the command which was contained in the packet, including any
	 * parameter, if required by the command.
	 *
	 * @param data
	 *            data received from the network
	 * @return an object which inherits from EIEIOCommandMessage which contains
	 *         parsed data received from the network
	 */
	public static EIEIOCommandMessage readCommandMessage(ByteBuffer data) {
		var command = peekCommand(data);
		if (!(command instanceof EIEIOCommandID cmd)) {
			return new EIEIOCommandMessage(data);
		}
		return switch (cmd) {
		// Fill in buffer area with padding
		case EVENT_PADDING -> new PaddingRequest();
		// End of all buffers, stop execution
		case EVENT_STOP -> new EventStopRequest();
		// Stop complaining that there is SDRAM free space for buffers
		case STOP_SENDING_REQUESTS -> new StopRequests();
		// Start complaining that there is SDRAM free space for buffers
		case START_SENDING_REQUESTS -> new StartRequests();
		// SpiNNaker requesting new buffers for spike source population
		case SPINNAKER_REQUEST_BUFFERS -> new SpinnakerRequestBuffers(data);
		// Buffers being sent from host to SpiNNaker
		case HOST_SEND_SEQUENCED_DATA -> new HostSendSequencedData(data);
		// Buffers available to be read from a buffered out vertex
		case SPINNAKER_REQUEST_READ_DATA -> new SpinnakerRequestReadData(data);
		// Host confirming data being read form SpiNNaker memory
		case HOST_DATA_READ -> new HostDataRead(data);
		// Some non-standard message
		default -> new EIEIOCommandMessage(data);
		};
	}

	/**
	 * Reads the content of an EIEIO data message and returns an object
	 * identifying the data which was contained in the packet.
	 *
	 * @param data
	 *            data received from the network
	 * @return an object which inherits from EIEIODataMessage which contains
	 *         parsed data received from the network
	 */
	public static EIEIODataMessage readDataMessage(ByteBuffer data) {
		return new EIEIODataMessage(data);
	}
}
