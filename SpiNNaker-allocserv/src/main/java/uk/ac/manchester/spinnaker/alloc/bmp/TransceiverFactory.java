/*
 * Copyright (c) 2021 The University of Manchester
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
package uk.ac.manchester.spinnaker.alloc.bmp;

import static java.nio.ByteBuffer.allocate;
import static java.nio.ByteOrder.LITTLE_ENDIAN;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Arrays.asList;
import static java.util.Collections.unmodifiableMap;
import static org.slf4j.LoggerFactory.getLogger;
import static uk.ac.manchester.spinnaker.messages.Constants.SCP_SCAMP_PORT;
import static uk.ac.manchester.spinnaker.messages.model.PowerCommand.POWER_ON;
import static uk.ac.manchester.spinnaker.utils.InetFactory.getByName;
import static uk.ac.manchester.spinnaker.utils.Ping.ping;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import uk.ac.manchester.spinnaker.alloc.ServiceMasterControl;
import uk.ac.manchester.spinnaker.alloc.SpallocProperties.TxrxProperties;
import uk.ac.manchester.spinnaker.alloc.allocator.SpallocAPI.Machine;
import uk.ac.manchester.spinnaker.connections.BMPConnection;
import uk.ac.manchester.spinnaker.messages.bmp.BMPBoard;
import uk.ac.manchester.spinnaker.messages.bmp.BMPCoords;
import uk.ac.manchester.spinnaker.messages.model.BMPConnectionData;
import uk.ac.manchester.spinnaker.messages.model.FPGA;
import uk.ac.manchester.spinnaker.messages.model.PowerCommand;
import uk.ac.manchester.spinnaker.messages.model.VersionInfo;
import uk.ac.manchester.spinnaker.transceiver.BMPSendTimedOutException;
import uk.ac.manchester.spinnaker.transceiver.BMPTransceiverInterface;
import uk.ac.manchester.spinnaker.transceiver.ProcessException;
import uk.ac.manchester.spinnaker.transceiver.SpinnmanException;
import uk.ac.manchester.spinnaker.transceiver.Transceiver;
import uk.ac.manchester.spinnaker.transceiver.UnimplementedBMPTransceiver;

/**
 * Creates transceivers for talking to the BMPs of machines. Note that each
 * machine only has the one BMP that is talked to, and only ever one transceiver
 * that is used to do it.
 * <p>
 * Can support running with a dummy transceiver (but not in production, of
 * course). Set the {@code spalloc.transceiver.dummy} configuration value to
 * {@code true} to enable that.
 *
 * @author Donal Fellows
 */
@Service("transceiverFactory")
public class TransceiverFactory
		implements TransceiverFactoryAPI<BMPTransceiverInterface> {
	private static final Logger log = getLogger(TransceiverFactory.class);

	private static final class Key {
		final String machine;

		final BMPCoords bmp;

		Key(String machine, BMPCoords bmp) {
			this.machine = machine;
			this.bmp = bmp;
		}

		@Override
		public boolean equals(Object o) {
			Key other = (Key) o;
			return machine.equals(other.machine) && bmp.equals(other.bmp);
		}

		@Override
		public int hashCode() {
			return machine.hashCode() ^ bmp.hashCode() * 7;
		}
	}

	private Map<Key, BMPTransceiverInterface> txrxMap = new HashMap<>();

	@Autowired
	private ServiceMasterControl control;

	@Autowired
	private TxrxProperties props;

	@PostConstruct
	private void setup() {
		// Whenever the useDummyBMP property is changed, flush the cache
		control.addUseDummyBMPListener(e -> {
			synchronized (txrxMap) {
				try {
					closeTransceivers();
				} catch (Exception ex) {
					log.warn("problem closing transceivers", ex);
				}
				txrxMap.clear();
			}
		});
	}

	@Override
	public BMPTransceiverInterface getTransciever(Machine machineDescription,
			BMPCoords bmp) throws IOException, SpinnmanException {
		try {
			synchronized (txrxMap) {
				return txrxMap.computeIfAbsent(
						new Key(machineDescription.getName(), bmp),
						k -> makeTransceiver(machineDescription, bmp));
			}
		} catch (TransceiverFactoryException e) {
			Throwable t = e.getCause();
			if (t instanceof IOException) {
				throw (IOException) t;
			} else if (t instanceof SpinnmanException) {
				throw (SpinnmanException) t;
			}
			throw e;
		}
	}

	private static class TransceiverFactoryException extends RuntimeException {
		private static final long serialVersionUID = 2102592240724419836L;

		TransceiverFactoryException(String msg, Exception e) {
			super(msg, e);
		}
	}

	private BMPTransceiverInterface makeTransceiver(Machine machineDescription,
			BMPCoords bmp) {
		BMPConnectionData connData =
				makeConnectionData(machineDescription, bmp);
		try {
			if (control.isUseDummyBMP()) {
				return new DummyTransceiver(machineDescription.getName(),
						connData);
			} else {
				return makeTransceiver(connData);
			}
		} catch (IOException | SpinnmanException e) {
			throw new TransceiverFactoryException(
					"failed to build BMP transceiver", e);
		}
	}

	private BMPConnectionData makeConnectionData(Machine machine,
			BMPCoords bmp) {
		try {
			String address = machine.getBMPAddress(bmp);
			List<Integer> boards = machine.getBoardNumbers(bmp);
			return new BMPConnectionData(0, 0, getByName(address), boards,
					SCP_SCAMP_PORT);
		} catch (IOException e) {
			throw new TransceiverFactoryException(
					"failed to build address of BMP transceiver", e);
		}
	}

	/**
	 * Build a transceiver connection.
	 * <p>
	 * The original spalloc server <em>also</em> does everything through the
	 * root BMP; the BMPs communicate with each other if necessary. I believe
	 * that communication is via an I<sup>2</sup>C bus, but I might be wrong.
	 *
	 * @param data
	 *            The information about the BMP and the boards to manage.
	 * @throws IOException
	 *             If network access fails
	 * @throws SpinnmanException
	 *             If transceiver building fails
	 */
	private Transceiver makeTransceiver(BMPConnectionData data)
			throws IOException, SpinnmanException {
		int count = 0;
		while (true) {
			try {
				return new Transceiver(null, asList(new BMPConnection(data)),
						null, null, null, null, null);
			} catch (ProcessException e) {
				if (e.getCause() instanceof BMPSendTimedOutException
						&& ++count > props.getBuildAttempts()) {
					log.error("completely failed to connect to BMP {}; "
							+ "service is unstable!", data);
					throw e;
				}
				log.error("failed to connect to BMP; will ping and retry", e);
				ping(data.ipAddress);
			}
		}
	}

	@PreDestroy
	void closeTransceivers() throws Exception {
		for (BMPTransceiverInterface txrx : txrxMap.values()) {
			if (txrx instanceof AutoCloseable) {
				((AutoCloseable) txrx).close();
			}
		}
	}
}

class DummyTransceiver extends UnimplementedBMPTransceiver {
	private static final Logger log = getLogger(DummyTransceiver.class);

	private static final int VERSION_INFO_SIZE = 32;

	private static final short VERSION = 0x202;

	private final VersionInfo version;

	private Map<Integer, Boolean> status;

	DummyTransceiver(String machineName, BMPConnectionData data) {
		log.info("constructed dummy transceiver for {} ({} : {})", machineName,
				data.ipAddress, data.boards);
		version = new VersionInfo(syntheticVersionData(), true);
		status = new HashMap<>();
	}

	/**
	 * @return The bytes of a response, correct in the places which Spalloc
	 *         checks, and arbitrary (zero) elsewhere.
	 */
	private static ByteBuffer syntheticVersionData() {
		ByteBuffer b = allocate(VERSION_INFO_SIZE);
		b.order(LITTLE_ENDIAN);
		b.putInt(0);
		b.putInt(0);
		b.putInt(0);
		b.putInt(0);
		b.putShort((short) 0);
		b.putShort(VERSION);
		b.putInt(0);
		b.put("abc/def".getBytes(UTF_8));
		b.flip();
		return b;
	}

	public Map<Integer, Boolean> getStatus() {
		return unmodifiableMap(status);
	}

	@Override
	public void power(PowerCommand powerCommand, BMPCoords bmp,
			Collection<BMPBoard> boards) {
		log.info("power({},{},{})", powerCommand, bmp, boards);
		for (BMPBoard b : boards) {
			status.put(b.board, powerCommand == POWER_ON);
		}
	}

	@Override
	public int readFPGARegister(FPGA fpga, int register, BMPCoords bmp,
			BMPBoard board) {
		log.info("readFPGARegister({},{},{},{})", fpga, register, bmp, board);
		return fpga.value;
	}

	@Override
	public void writeFPGARegister(FPGA fpga, int register, int value,
			BMPCoords bmp, BMPBoard board) {
		log.info("writeFPGARegister({},{},{},{},{})", fpga, register, value,
				bmp, board);
	}

	@Override
	public VersionInfo readBMPVersion(BMPCoords bmp, BMPBoard board) {
		return version;
	}
}
