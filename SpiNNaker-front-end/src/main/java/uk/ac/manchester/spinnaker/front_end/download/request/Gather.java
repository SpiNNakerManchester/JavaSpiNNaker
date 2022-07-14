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
import uk.ac.manchester.spinnaker.machine.tags.IPTag;
import uk.ac.manchester.spinnaker.transceiver.ProcessException;
import uk.ac.manchester.spinnaker.transceiver.Transceiver;

/**
 * Data speed up packet gatherer description.
 *
 * @author Christian-B
 * @author Alan Stokes
 */
@JsonFormat(shape = OBJECT)
public class Gather implements HasCoreLocation {
	/** The x value of the core this placement is on. */
	private final int x;

	/** The y value of the core this placement is on. */
	private final int y;

	/** The p value of the core this placement is on. */
	private final int p;

	/** The IPTag of the package gatherer. */
	private final IPTag iptag;

	/** The extra monitor cores, and what to retrieve from them. */
	private final List<Monitor> monitors;

	/** The current transaction id for the board. */
	private int transactionId;

	/**
	 * Constructor with minimum information needed.
	 * <p>
	 * Could be called from an unmarshaller.
	 *
	 * @param x
	 *            Gatherer X coordinate.
	 * @param y
	 *            Gatherer Y coordinate.
	 * @param p
	 *            Gatherer processor ID.
	 * @param iptag
	 *            Information about IPtag for the gatherer to use.
	 * @param monitors
	 *            What information to retrieve and from where. This should be
	 *            information about the extra monitor cores that have been
	 *            placed on the same board as this data speed up packet
	 *            gatherer.
	 */
	Gather(@JsonProperty(value = "x", required = true) int x,
			@JsonProperty(value = "y", required = true) int y,
			@JsonProperty(value = "p", required = true) int p,
			@JsonProperty(value = "iptag", required = true) IPTag iptag,
			@JsonProperty(value = "monitors", required = true) List<
					Monitor> monitors) {
		this.x = x;
		this.y = y;
		this.p = p;
		this.iptag = iptag;
		this.monitors = monitors;
		this.transactionId = 0;
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
	 * Sets the transaction ID to a new value and returns that new value.
	 *
	 * @return The new transaction ID.
	 */
	public int getNextTransactionId() {
		return ++transactionId;
	}

	/**
	 * sets the transaction id from the machine.
	 *
	 * @param txrx
	 *            spinnman instance
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
	 * @return the iptag
	 */
	public IPTag getIptag() {
		return iptag;
	}

	/**
	 * @return the monitors
	 */
	public List<Monitor> getMonitors() {
		return unmodifiableList(monitors);
	}
}
