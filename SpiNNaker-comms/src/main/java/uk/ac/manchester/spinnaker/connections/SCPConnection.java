package uk.ac.manchester.spinnaker.connections;

import static java.util.Objects.requireNonNull;
import static uk.ac.manchester.spinnaker.messages.Constants.SCP_SCAMP_PORT;

import java.io.IOException;
import java.net.InetAddress;

import uk.ac.manchester.spinnaker.connections.model.SCPSenderReceiver;
import uk.ac.manchester.spinnaker.machine.ChipLocation;
import uk.ac.manchester.spinnaker.machine.HasChipLocation;
import uk.ac.manchester.spinnaker.messages.scp.SCPRequest;
import uk.ac.manchester.spinnaker.messages.scp.SCPResultMessage;

/** A UDP connection to SC&amp;MP on the board. */
public class SCPConnection extends SDPConnection
		implements SCPSenderReceiver {
	private static final HasChipLocation DEFAULT_CHIP = new ChipLocation(255,
			255);

	/**
	 * Create a connection to a particular instance of SCAMP.
	 *
	 * @param remoteHost
	 *            The remote host to send messages to.
	 * @throws IOException
	 *             If anything goes wrong with socket setup.
	 */
	public SCPConnection(InetAddress remoteHost) throws IOException {
		this(DEFAULT_CHIP, null, null, remoteHost, SCP_SCAMP_PORT);
	}

	/**
	 * Create a connection to a particular instance of SCAMP.
	 *
	 * @param remoteHost
	 *            The remote host to send messages to.
	 * @param remotePort
	 *            The optional remote port number to send messages to. If
	 *            <tt>null</tt>, the default remote port is used.
	 * @throws IOException
	 *             If anything goes wrong with socket setup.
	 */
	public SCPConnection(InetAddress remoteHost, Integer remotePort)
			throws IOException {
		this(DEFAULT_CHIP, null, null, remoteHost, remotePort);
	}

	/**
	 * Create a connection to a particular instance of SCAMP.
	 *
	 * @param localHost
	 *            The optional host of the local interface to
	 *            listen on; use <tt>null</tt> to listen on all local
	 *            interfaces.
	 * @param localPort
	 *            The optional local port to listen on; use <tt>null</tt> to
	 *            pick a random port.
	 * @param remoteHost
	 *            The remote host to send messages to.
	 * @throws IOException
	 *             If anything goes wrong with socket setup.
	 */
	public SCPConnection(InetAddress localHost, Integer localPort,
			InetAddress remoteHost) throws IOException {
		this(DEFAULT_CHIP, localHost, localPort, remoteHost, SCP_SCAMP_PORT);
	}

	/**
	 * Create a connection to a particular instance of SCAMP.
	 *
	 * @param localHost
	 *            The optional host of the local interface to
	 *            listen on; use <tt>null</tt> to listen on all local
	 *            interfaces.
	 * @param localPort
	 *            The optional local port to listen on; use <tt>null</tt> to
	 *            pick a random port.
	 * @param remoteHost
	 *            The remote host to send messages to.
	 * @param remotePort
	 *            The optional remote port number to send messages to. If
	 *            <tt>null</tt>, the default remote port is used.
	 * @throws IOException
	 *             If anything goes wrong with socket setup.
	 */
	public SCPConnection(InetAddress localHost, Integer localPort,
			InetAddress remoteHost, Integer remotePort) throws IOException {
		this(DEFAULT_CHIP, localHost, localPort, remoteHost, remotePort);
	}

	/**
	 * Create a connection to a particular instance of SCAMP.
	 *
	 * @param chip
	 *            The location of the chip on the board with this remoteHost
	 * @param remoteHost
	 *            The remote host to send messages to.
	 * @throws IOException
	 *             If anything goes wrong with socket setup.
	 */
	public SCPConnection(HasChipLocation chip, InetAddress remoteHost)
			throws IOException {
		this(chip, null, null, remoteHost, SCP_SCAMP_PORT);
	}

	/**
	 * Create a connection to a particular instance of SCAMP.
	 *
	 * @param chip
	 *            The location of the chip on the board with this remoteHost
	 * @param remoteHost
	 *            The remote host nto send messages to.
	 * @param remotePort
	 *            The optional remote port number to send messages to. If
	 *            <tt>null</tt>, the default remote port is used.
	 * @throws IOException
	 *             If anything goes wrong with socket setup.
	 */
	public SCPConnection(HasChipLocation chip, InetAddress remoteHost,
			Integer remotePort) throws IOException {
		this(chip, null, null, remoteHost, remotePort);
	}

	/**
	 * Create a connection to a particular instance of SCAMP.
	 *
	 * @param chip
	 *            The location of the chip on the board with this remoteHost
	 * @param localHost
	 *            The optional host name of the local interface to
	 *            listen on; use <tt>null</tt> to listen on all local
	 *            interfaces.
	 * @param localPort
	 *            The optional local port to listen on; use <tt>null</tt> to
	 *            pick a random port.
	 * @param remoteHost
	 *            The remote host to send messages to.
	 * @throws IOException
	 *             If anything goes wrong with socket setup.
	 */
	public SCPConnection(HasChipLocation chip, InetAddress localHost,
			Integer localPort, InetAddress remoteHost) throws IOException {
		this(chip, localHost, localPort, remoteHost, SCP_SCAMP_PORT);
	}

	/**
	 * Create a connection to a particular instance of SCAMP.
	 *
	 * @param chip
	 *            The location of the chip on the board with this remoteHost
	 * @param localHost
	 *            The optional host of the local interface to
	 *            listen on; use <tt>null</tt> to listen on all local
	 *            interfaces.
	 * @param localPort
	 *            The optional local port to listen on; use <tt>null</tt> to
	 *            pick a random port.
	 * @param remoteHost
	 *            The remote host to send messages to.
	 * @param remotePort
	 *            The optional remote port number to send messages to. If
	 *            <tt>null</tt>, the default remote port is used.
	 * @throws IOException
	 *             If anything goes wrong with socket setup.
	 */
	public SCPConnection(HasChipLocation chip, InetAddress localHost,
			Integer localPort, InetAddress remoteHost, Integer remotePort)
			throws IOException {
		super(chip, localHost, localPort, requireNonNull(remoteHost,
				"SCPConnection only meaningful with a real remote host"),
				(remotePort == null) ? SCP_SCAMP_PORT : remotePort);
	}

	@Override
	public SCPResultMessage receiveSCPResponse(Integer timeout)
			throws IOException {
		return new SCPResultMessage(receive(timeout));
	}

	@Override
	public void sendSCPRequest(SCPRequest<?> scpRequest) throws IOException {
		send(getSCPData(scpRequest));
	}
}
