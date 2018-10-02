package uk.ac.manchester.spinnaker.connections;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.InetAddress;
import java.nio.ByteBuffer;

import uk.ac.manchester.spinnaker.connections.model.SDPReceiver;
import uk.ac.manchester.spinnaker.connections.model.SDPSender;
import uk.ac.manchester.spinnaker.machine.ChipLocation;
import uk.ac.manchester.spinnaker.machine.HasChipLocation;
import uk.ac.manchester.spinnaker.messages.sdp.SDPMessage;

/** A UDP socket connection that talks SDP to SpiNNaker. */
public class SDPConnection extends UDPConnection<SDPMessage>
		implements SDPReceiver, SDPSender {
	private ChipLocation chip;

	/**
	 * @param remoteChip
	 *            Which chip are we talking to? This is not necessarily the chip
	 *            that is connected to the Ethernet connector on the SpiNNaker
	 *            board, or even on the same board.
	 * @param remoteHost
	 *            The address of the SpiNNaker board to route UDP packets to.
	 * @param remotePort
	 *            The UDP port on the SpiNNaker board to use.
	 * @throws IOException
	 *             If anything goes wrong with the setup.
	 */
	public SDPConnection(HasChipLocation remoteChip, InetAddress remoteHost,
			Integer remotePort) throws IOException {
		this(remoteChip, null, null, remoteHost, remotePort);
	}

	/**
	 * @param remoteChip
	 *            Which chip are we talking to? This is not necessarily the chip
	 *            that is connected to the Ethernet connector on the SpiNNaker
	 *            board, or even on the same board.
	 * @param localHost
	 *            The local host address to bind to, or <tt>null</tt> to bind to
	 *            all relevant local addresses.
	 * @param localPort
	 *            The local port to bind to, or <tt>null</tt> to pick a random
	 *            free port.
	 * @param remoteHost
	 *            The address of the SpiNNaker board to route UDP packets to.
	 * @param remotePort
	 *            The UDP port on the SpiNNaker board to use.
	 * @throws IOException
	 *             If anything goes wrong with the setup (e.g., if the local
	 *             port is specified and already bound).
	 */
	public SDPConnection(HasChipLocation remoteChip, InetAddress localHost,
			Integer localPort, InetAddress remoteHost, Integer remotePort)
			throws IOException {
		super(localHost, localPort, remoteHost, remotePort);
		this.chip = remoteChip.asChipLocation();
	}

	@Override
	public MessageReceiver<SDPMessage> getReceiver() {
		return this::receiveSDPMessage;
	}

	@Override
	public void sendSDPMessage(SDPMessage sdpMessage) throws IOException {
		send(sdpMessage.getMessageData(chip));
	}

	@Override
	public SDPMessage receiveSDPMessage(Integer timeout)
			throws IOException, InterruptedIOException {
		ByteBuffer buffer = receive();
		buffer.getShort(); // SKIP TWO PADDING BYTES
		return new SDPMessage(buffer);
	}

	/**
	 * @return The SpiNNaker chip that we are talking to with this connection.
	 */
	public ChipLocation getChip() {
		return chip;
	}

	/**
	 * Set the SpiNNaker chip that we are talking to with this connection.
	 *
	 * @param chip
	 *            The chip to talk to.
	 */
	public void setChip(HasChipLocation chip) {
		this.chip = chip.asChipLocation();
	}
}
