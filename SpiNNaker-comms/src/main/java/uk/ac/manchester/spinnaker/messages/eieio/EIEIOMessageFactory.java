package uk.ac.manchester.spinnaker.messages.eieio;

import java.nio.ByteBuffer;

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
	 * @param offset
	 *            offset at which the parsing operation should start
	 * @return an object which inherits from EIEIOCommandMessage which contains
	 *         parsed data received from the network
	 */
	public static EIEIOCommandMessage readCommandMessage(ByteBuffer data) {
		EIEIOCommandHeader commandHeader = new EIEIOCommandHeader(data);
		if (!(commandHeader.command instanceof EIEIOCommandID)) {
			return new EIEIOCommandMessage(commandHeader);
		}
		switch ((EIEIOCommandID) commandHeader.command) {
		case DATABASE_CONFIRMATION:
			return new DatabaseConfirmation(commandHeader, data);
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
			return new SpinnakerRequestBuffers(commandHeader, data);
		case HOST_SEND_SEQUENCED_DATA:
			// Buffers being sent from host to SpiNNaker
			return new HostSendSequencedData(commandHeader, data);
		case SPINNAKER_REQUEST_READ_DATA:
			// Buffers available to be read from a buffered out vertex
			return new SpinnakerRequestReadData(commandHeader, data);
		case HOST_DATA_READ:
			// Host confirming data being read form SpiNNaker memory
			return new HostDataRead(commandHeader, data);
		default:
			return new EIEIOCommandMessage(commandHeader);
		}
	}

	/**
	 * Reads the content of an EIEIO data message and returns an object
	 * identifying the data which was contained in the packet.
	 *
	 * @param data
	 *            data received from the network
	 * @param offset
	 *            offset at which the parsing operation should start
	 * @return an object which inherits from EIEIODataMessage which contains
	 *         parsed data received from the network
	 */
	public static EIEIODataMessage readDataMessage(ByteBuffer data) {
		return new EIEIODataMessage(new EIEIODataHeader(data), data);
	}
}
