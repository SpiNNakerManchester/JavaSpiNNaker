/*
 * Copyright (c) 2019 The University of Manchester
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
package uk.ac.manchester.spinnaker.front_end.dse;

import static uk.ac.manchester.spinnaker.messages.Constants.SCP_SCAMP_PORT;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;

import uk.ac.manchester.spinnaker.connections.SCPConnection;
import uk.ac.manchester.spinnaker.machine.tags.IPTag;
import uk.ac.manchester.spinnaker.messages.model.UnexpectedResponseCodeException;
import uk.ac.manchester.spinnaker.messages.scp.IPTagSet;
import uk.ac.manchester.spinnaker.messages.sdp.SDPMessage;
import uk.ac.manchester.spinnaker.storage.DSEStorage.Ethernet;

class ThrottledConnection implements Closeable {
	private static final int TIMEOUT_SECONDS = 1000;
	private static final int THROTTLE_NS = 1000;
	private static final int IPTAG_REPROGRAM_TIMEOUT = 1;
	private static final int IPTAG_REPROGRAM_ATTEMPTS = 3;

	private final InetAddress addr;
	private final Ethernet board;
	private SCPConnection connection;
	private long lastSend;

	ThrottledConnection(Ethernet board) throws IOException {
		this.board = board;
		addr = InetAddress.getByName(board.ethernetAddress);
		connection = new SCPConnection(board.location, addr, SCP_SCAMP_PORT);
	}

	ByteBuffer receive() throws SocketTimeoutException, IOException {
		return connection.receive(TIMEOUT_SECONDS);
	}

	private static final byte[] INADDR_ANY = new byte[] {
		0, 0, 0, 0
	};
	private static final int ANY_PORT = 0;

	void reprogramTag(IPTag iptag)
			throws IOException, UnexpectedResponseCodeException {
		IPTagSet tagSet = new IPTagSet(board.location, INADDR_ANY, ANY_PORT,
				iptag.getTag(), true, true);
		ByteBuffer data = connection.getSCPData(tagSet);
		SocketTimeoutException e = null;
		for (int i = 0; i < IPTAG_REPROGRAM_ATTEMPTS; i++) {
			try {
				connection.send(data);
				lastSend = System.nanoTime();
				connection.receiveSCPResponse(IPTAG_REPROGRAM_TIMEOUT)
						.parsePayload(tagSet);
				return;
			} catch (SocketTimeoutException timeout) {
				e = timeout;
			} catch (IOException | RuntimeException
					| UnexpectedResponseCodeException ex) {
				throw ex;
			} catch (Exception ex) {
				throw new RuntimeException("unexpected exception", e);
			}
		}
		if (e != null) {
			throw e;
		}
	}

	/**
	 * Shut down and reopen the connection. It sometimes unsticks things
	 * apparently.
	 */
	void restart() throws IOException {
		InetAddress localAddr = connection.getLocalIPAddress();
		int localPort = connection.getLocalPort();
		connection.close();
		connection = new SCPConnection(board.location, localAddr, localPort,
				addr, SCP_SCAMP_PORT);
	}

	/** Throttled send. */
	void send(SDPMessage message) throws IOException, InterruptedException {
		long waited = System.nanoTime() - lastSend;
		if (waited < THROTTLE_NS) {
			// BUSY LOOP! https://stackoverflow.com/q/11498585/301832
			while (System.nanoTime() - lastSend < THROTTLE_NS) {
			}
		}
		connection.sendSDPMessage(message);
		lastSend = System.nanoTime();
	}

	@Override
	public void close() throws IOException {
		this.connection.close();
	}
}
