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

import static java.util.Objects.hash;
import static org.slf4j.LoggerFactory.getLogger;
import static uk.ac.manchester.spinnaker.messages.Constants.SCP_SCAMP_PORT;
import static uk.ac.manchester.spinnaker.utils.InetFactory.getByName;
import static uk.ac.manchester.spinnaker.utils.Ping.ping;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.google.errorprone.annotations.RestrictedApi;
import com.google.errorprone.annotations.concurrent.GuardedBy;

import uk.ac.manchester.spinnaker.alloc.ForTestingOnly;
import uk.ac.manchester.spinnaker.alloc.ServiceMasterControl;
import uk.ac.manchester.spinnaker.alloc.SpallocProperties.TxrxProperties;
import uk.ac.manchester.spinnaker.alloc.allocator.SpallocAPI.Machine;
import uk.ac.manchester.spinnaker.connections.BMPConnection;
import uk.ac.manchester.spinnaker.machine.board.BMPCoords;
import uk.ac.manchester.spinnaker.messages.model.BMPConnectionData;
import uk.ac.manchester.spinnaker.messages.model.Blacklist;
import uk.ac.manchester.spinnaker.transceiver.BMPTransceiverInterface;
import uk.ac.manchester.spinnaker.transceiver.Transceiver;
import uk.ac.manchester.spinnaker.transceiver.exceptions.BMPSendTimedOutException;
import uk.ac.manchester.spinnaker.transceiver.exceptions.ProcessException;
import uk.ac.manchester.spinnaker.transceiver.exceptions.SpinnmanException;
import uk.ac.manchester.spinnaker.utils.ValueHolder;

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
			if (o instanceof Key) {
				var other = (Key) o;
				return machine.equals(other.machine) && bmp.equals(other.bmp);
			}
			return false;
		}

		@Override
		public int hashCode() {
			return hash(machine, bmp);
		}
	}

	@GuardedBy("itself")
	private final Map<Key, BMPTransceiverInterface> txrxMap = new HashMap<>();

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
			BMPCoords bmp)
			throws IOException, SpinnmanException, InterruptedException {
		try {
			synchronized (txrxMap) {
				return txrxMap
						.computeIfAbsent(
						new Key(machineDescription.getName(), bmp),
						__ -> makeTransceiver(machineDescription, bmp));
			}
		} catch (TransceiverFactoryException e) {
			var t = e.getCause();
			if (t instanceof IOException) {
				throw (IOException) t;
			} else if (t instanceof SpinnmanException) {
				throw (SpinnmanException) t;
			} else if (t instanceof InterruptedException) {
				throw (InterruptedException) t;
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

	private final ValueHolder<Blacklist> setBlacklist = new ValueHolder<>();

	private TestAPI.TestTransceiverFactory testFactory = null;

	private BMPTransceiverInterface makeTransceiver(Machine machineDescription,
			BMPCoords bmp) {
		var connData = makeConnectionData(machineDescription, bmp);
		try {
			if (control.isUseDummyBMP()) {
				return testFactory.create(machineDescription.getName(),
						connData, setBlacklist);
			} else {
				return makeTransceiver(connData);
			}
		} catch (IOException | SpinnmanException | InterruptedException e) {
			throw new TransceiverFactoryException(
					"failed to build BMP transceiver", e);
		}
	}

	private BMPConnectionData makeConnectionData(Machine machine,
			BMPCoords bmp) {
		try {
			var address = machine.getBMPAddress(bmp);
			var boards = machine.getBoardNumbers(bmp);
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
	 * @throws InterruptedException
	 *             If the communications were interrupted.
	 */
	private Transceiver makeTransceiver(BMPConnectionData data)
			throws IOException, SpinnmanException, InterruptedException {
		int count = 0;
		while (true) {
			try {
				return new Transceiver(null, List.of(new BMPConnection(data)),
						null, null, null, null, null);
			} catch (ProcessException e) {
				if (e.getCause() instanceof BMPSendTimedOutException
						&& ++count > props.getBuildAttempts()) {
					log.error("completely failed to connect to BMP {}; "
							+ "service is unstable!", data);
					throw e;
				}
				log.error("failed to connect to BMP; will ping and retry", e);
				log.debug("ping result was {}", ping(data.ipAddress));
			}
		}
	}

	private Collection<BMPTransceiverInterface> transceivers() {
		synchronized (txrxMap) {
			return List.copyOf(txrxMap.values());
		}
	}

	@PreDestroy
	void closeTransceivers() throws Exception {
		for (var txrx : transceivers()) {
			txrx.close();
		}
	}

	/** Not a public API! Operations for testing only. */
	@ForTestingOnly
	public interface TestAPI {
		/** @return The current blacklist. */
		Blacklist getCurrentBlacklist();

		/**
		 * @param factory
		 *            The mock transceiver factory to use.
		 */
		void setFactory(TestTransceiverFactory factory);

		/**
		 * Not a public API! A factory for transceivers. Use to install a
		 * suitable mock.
		 */
		interface TestTransceiverFactory {
			/**
			 * Make a test transceiver.
			 *
			 * @param machineName
			 *            The name of the machine.
			 * @param data
			 *            The connection data.
			 * @param setBlacklist
			 *            Where to record the current blacklist
			 * @return Transceiver for testing.
			 */
			BMPTransceiverInterface create(String machineName,
					BMPConnectionData data,
					ValueHolder<Blacklist> setBlacklist);
		}
	}

	/**
	 * Not a public API! Do not call outside of test code!
	 * @return The test interface.
	 * @deprecated This interface is just for testing.
	 */
	@ForTestingOnly
	@RestrictedApi(explanation = "just for testing", link = "index.html",
			allowedOnPath = ".*/src/test/java/.*")
	@Deprecated
	public final TestAPI getTestAPI() {
		ForTestingOnly.Utils.checkForTestClassOnStack();
		return new TestAPI() {
			@Override
			public Blacklist getCurrentBlacklist() {
				synchronized (setBlacklist) {
					return setBlacklist.getValue();
				}
			}

			@Override
			public void setFactory(TestTransceiverFactory factory) {
				testFactory = factory;
			}
		};
	}
}
