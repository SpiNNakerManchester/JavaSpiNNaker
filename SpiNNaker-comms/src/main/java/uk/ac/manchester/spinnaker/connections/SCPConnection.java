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
package uk.ac.manchester.spinnaker.connections;

import static java.util.Objects.requireNonNull;
import static java.util.concurrent.Executors.newSingleThreadScheduledExecutor;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.slf4j.LoggerFactory.getLogger;
import static uk.ac.manchester.spinnaker.messages.Constants.SCP_SCAMP_PORT;

import java.io.IOException;
import java.net.InetAddress;
import java.util.concurrent.ScheduledExecutorService;

import org.slf4j.Logger;

import uk.ac.manchester.spinnaker.connections.model.SCPSenderReceiver;
import uk.ac.manchester.spinnaker.machine.HasChipLocation;
import uk.ac.manchester.spinnaker.messages.scp.SCPRequest;
import uk.ac.manchester.spinnaker.messages.scp.SCPResultMessage;
import uk.ac.manchester.spinnaker.utils.Daemon;

/** A UDP connection to SC&amp;MP on the board. */
public class SCPConnection extends SDPConnection implements SCPSenderReceiver {
	private static final Logger log = getLogger(SCPConnection.class);

	private static final ScheduledExecutorService CLOSER;

	static {
		CLOSER = newSingleThreadScheduledExecutor(
				r -> new Daemon(r, "SCPConnection.Closer"));
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
	 * Create a connection to a particular instance of SCAMP. Can use a
	 * specified local network interface.
	 *
	 * @param chip
	 *            The location of the chip on the board with this remoteHost
	 * @param localHost
	 *            The optional host of the local interface to listen on; use
	 *            {@code null} to listen on all local interfaces.
	 * @param localPort
	 *            The optional local port to listen on; use {@code null} to pick
	 *            a random port.
	 * @param remoteHost
	 *            The remote host to send messages to.
	 * @param remotePort
	 *            The optional remote port number to send messages to. If
	 *            {@code null}, the default remote port is used.
	 * @throws IOException
	 *             If anything goes wrong with socket setup.
	 */
	protected SCPConnection(HasChipLocation chip, InetAddress localHost,
			Integer localPort, InetAddress remoteHost, int remotePort)
			throws IOException {
		super(chip, localHost, localPort, requireNonNull(remoteHost,
				"SCPConnection only meaningful with a real remote host"),
				remotePort);
	}

	/**
	 * Create a connection where the mechanism for sending and receiving
	 * messages is being overridden by a subclass.
	 *
	 * @param chip
	 *            The location of the target chip on the board.
	 * @throws IOException
	 *             If anything goes wrong with socket manipulation.
	 */
	protected SCPConnection(HasChipLocation chip) throws IOException {
		super(chip, true);
	}

	@Override
	public SCPResultMessage receiveSCPResponse(int timeout)
			throws IOException, InterruptedException {
		return new SCPResultMessage(receive(timeout));
	}

	@Override
	public void send(SCPRequest<?> scpRequest) throws IOException {
		send(getSCPData(scpRequest));
	}

	/**
	 * Close this connection eventually. The close might not happen immediately.
	 */
	@SuppressWarnings("FutureReturnValueIgnored")
	public void closeEventually() {
		CLOSER.schedule(this::closeAndLogNoExcept, 1, SECONDS);
	}

	/**
	 * Close this connection, logging failures instead of throwing.
	 * <p>
	 * Core of implementation of {@link #closeEventually()}.
	 */
	protected final void closeAndLogNoExcept() {
		try {
			var name = "";
			if (log.isInfoEnabled()) {
				name = toString();
			}
			close();
			log.info("closed {}", name);
		} catch (IOException e) {
			log.warn("failed to close connection", e);
		}
	}
}
