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

import static java.util.Arrays.asList;
import static uk.ac.manchester.spinnaker.messages.Constants.SCP_SCAMP_PORT;
import static uk.ac.manchester.spinnaker.utils.InetFactory.getByName;

import java.io.IOException;
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
@Component("transceiverFactory")
public class TransceiverFactory implements TransceiverFactoryAPI<Transceiver> {
	private Map<String, Transceiver> txrxMap = new HashMap<>();

	@Override
	public Transceiver getTransciever(Machine machineDescription)
			throws IOException, SQLException, SpinnmanException {
		// Can't use Map.computeIfAbsent(); checked exceptions in the way
		synchronized (txrxMap) {
			Transceiver t = txrxMap.get(machineDescription.getName());
			if (t == null) {
				t = makeTransceiver(
						machineDescription.getRootBoardBMPAddress(),
						machineDescription.getBoardNumbers());
				txrxMap.put(machineDescription.getName(), t);
			}
			return t;
		}
	}

	/**
	 * Build a transceiver connection.
	 * <p>
	 * The original spalloc server <em>also</em> does everything through the
	 * root BMP; the BMPs communicate with each other if necessary. I believe
	 * that communication is via an I<sup>2</sup>C bus, but I might be wrong.
	 *
	 * @param address
	 *            The address of the BMP
	 * @param boards
	 *            The boards to manage at that address
	 * @throws IOException
	 *             If network access fails
	 * @throws SpinnmanException
	 *             If transceiver building fails
	 */
	private Transceiver makeTransceiver(String address, List<Integer> boards)
			throws IOException, SpinnmanException {
		BMPConnectionData c = new BMPConnectionData(0, 0, getByName(address),
				boards, SCP_SCAMP_PORT);
		return new Transceiver(null, asList(new BMPConnection(c)), null, null,
				null, null, null);
	}

	@PreDestroy
	void closeTransceivers() throws Exception {
		for (Transceiver txrx : txrxMap.values()) {
			txrx.close();
		}
	}
}
