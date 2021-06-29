package uk.ac.manchester.spinnaker.alloc.bmp;

import static java.util.Arrays.asList;
import static uk.ac.manchester.spinnaker.messages.Constants.SCP_SCAMP_PORT;
import static uk.ac.manchester.spinnaker.utils.InetFactory.getByName;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PreDestroy;

import org.springframework.stereotype.Component;

import uk.ac.manchester.spinnaker.alloc.allocator.SpallocAPI.Machine;
import uk.ac.manchester.spinnaker.connections.BMPConnection;
import uk.ac.manchester.spinnaker.messages.model.BMPConnectionData;
import uk.ac.manchester.spinnaker.transceiver.SpinnmanException;
import uk.ac.manchester.spinnaker.transceiver.Transceiver;

/**
 * Creates transceivers for talking to the BMPs of machines. Note that each
 * machine only has the one BMP that is talked to, and only ever one transceiver
 * that is used to do it.
 *
 * @author Donal Fellows
 */
@Component
public class TransceiverFactory {
	private Map<String, Transceiver> txrxMap = new HashMap<>();

	/**
	 * Get the transceiver for talking to a given machine's BMPs.
	 *
	 * @param machine
	 *            The machine we're talking about.
	 * @return The transceiver. Only operations relating to BMPs are guaranteed
	 *         to be supported.
	 * @throws IOException If low-level things go wrong.
	 * @throws SpinnmanException If the transceiver can't be built.
	 * @throws SQLException If the database can't be talked to.
	 */
	public Transceiver getTransciever(Machine machineDescription)
			throws IOException, UnknownHostException, SQLException,
			SpinnmanException {
		synchronized (txrxMap) {
			Transceiver t = txrxMap.get(machineDescription.getName());
			if (t == null) {
				/*
				 * The original spalloc server also does everything through the
				 * root BMP.
				 */
				InetAddress address =
						getByName(machineDescription.getRootBoardBMPAddress());
				List<Integer> boards = machineDescription.getBoardNumbers();
				BMPConnectionData c = new BMPConnectionData(0, 0, address,
						boards, SCP_SCAMP_PORT);
				t = new Transceiver(null, asList(new BMPConnection(c)), null,
						null, null, null, null);
				txrxMap.put(machineDescription.getName(), t);
			}
			return t;
		}
	}

	@PreDestroy
	void closeTransceivers() throws Exception {
		for (Transceiver txrx : txrxMap.values()) {
			txrx.close();
		}
	}
}
