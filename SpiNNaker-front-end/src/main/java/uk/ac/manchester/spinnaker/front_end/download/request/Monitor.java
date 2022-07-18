/*
 * Copyright (c) 2018-2020 The University of Manchester
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
package uk.ac.manchester.spinnaker.front_end.download.request;

import static com.fasterxml.jackson.annotation.JsonFormat.Shape.OBJECT;
import static java.util.Collections.unmodifiableList;

import java.io.IOException;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

import uk.ac.manchester.spinnaker.machine.HasCoreLocation;
import uk.ac.manchester.spinnaker.transceiver.ProcessException;
import uk.ac.manchester.spinnaker.transceiver.Transceiver;

/**
 * Extra monitor core information.
 *
 * @author Christian-B
 * @author Alan Stokes
 */
@JsonFormat(shape = OBJECT)
public class Monitor implements HasCoreLocation {
	/** The X coordinate of the core this monitor is placed on. */
	private final int x;

	/** The Y coordinate of the core this monitor is placed on. */
	private final int y;

	/** The processor ID of the core this monitor is placed on. */
	private final int p;

	/** The vertex placements that this monitor will read. */
	private final List<Placement> placements;

	/** The transaction id for this extra monitor. */
	private int transactionId = 0;

	/** cap of where a transaction id will get to. */
	private static final int TRANSACTION_ID_CAP = 0xFFFFFFFF;

	/**
	 * Constructor with minimum information needed.
	 * <p>
	 * Could be called from an unmarshaller.
	 *
	 * @param x
	 *            Monitor X coordinate.
	 * @param y
	 *            Monitor Y coordinate.
	 * @param p
	 *            Monitor processor ID.
	 * @param placements
	 *            Vertex placement info handled by this monitor.
	 */
	Monitor(@JsonProperty(value = "x", required = true) int x,
			@JsonProperty(value = "y", required = true) int y,
			@JsonProperty(value = "p", required = true) int p,
			@JsonProperty(value = "placements", required = false) List<
					Placement> placements) {
		this.x = x;
		this.y = y;
		this.p = p;
		if (placements == null) {
			this.placements = List.of();
		} else {
			this.placements = placements;
		}
	}

	@Override
	public int getX() {
		return x;
	}

	@Override
	public int getY() {
		return y;
	}

	@Override
	public int getP() {
		return p;
	}

	/**
	 * Updates the transaction id by 1 and wraps with the cap.
	 */
	public void updateTransactionId() {
		transactionId = (transactionId + 1) & TRANSACTION_ID_CAP;
	}

	public int getTransactionId() {
		return transactionId;
	}

	/**
	 * gets the transaction id from the machine and updates.
	 *
	 * @param txrx
	 *            the spinnman instance.
	 * @throws ProcessException
	 *             If SpiNNaker rejects a message.
	 * @throws IOException
	 *             If anything goes wrong with networking.
	 */
	public void updateTransactionIdFromMachine(Transceiver txrx)
			throws IOException, ProcessException {
		transactionId = txrx.readUser1(this);
	}

	/**
	 * @return the placements
	 */
	public List<Placement> getPlacements() {
		return unmodifiableList(placements);
	}

	@Override
	public String toString() {
		return "Monitor: " + asCoreLocation().toString();
	}
}
