/*
 * Copyright (c) 2018 The University of Manchester
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
		if (!(command instanceof EIEIOCommandID)) {
			return new EIEIOCommandMessage(data);
		}
		switch ((EIEIOCommandID) command) {
		case EVENT_PADDING:
			// Fill in buffer area with padding
			return new PaddingRequest();
		case EVENT_STOP:
			// End of all buffers, stop execution
			return new EventStopRequest();
		case STOP_SENDING_REQUESTS:
			// Stop complaining that there is SDRAM free space for buffers
			return new StopRequests();
		case START_SENDING_REQUESTS:
			// Start complaining that there is SDRAM free space for buffers
			return new StartRequests();
		case SPINNAKER_REQUEST_BUFFERS:
			// SpiNNaker requesting new buffers for spike source population
			return new SpinnakerRequestBuffers(data);
		case HOST_SEND_SEQUENCED_DATA:
			// Buffers being sent from host to SpiNNaker
			return new HostSendSequencedData(data);
		case SPINNAKER_REQUEST_READ_DATA:
			// Buffers available to be read from a buffered out vertex
			return new SpinnakerRequestReadData(data);
		case HOST_DATA_READ:
			// Host confirming data being read form SpiNNaker memory
			return new HostDataRead(data);
		default:
			return new EIEIOCommandMessage(data);
		}
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
