package uk.ac.manchester.spinnaker.messages.eieio;

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
		EIEIOCommand command = EIEIOCommandMessage.peekCommand(data);
		if (!(command instanceof EIEIOCommandID)) {
			return new EIEIOCommandMessage(data);
		}
		switch ((EIEIOCommandID) command) {
		case DATABASE_CONFIRMATION:
			return new DatabaseConfirmation(data);
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
