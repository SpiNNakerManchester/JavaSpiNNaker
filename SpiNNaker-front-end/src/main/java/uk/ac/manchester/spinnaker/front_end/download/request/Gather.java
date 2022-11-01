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
import static uk.ac.manchester.spinnaker.utils.CollectionUtils.copy;

import java.io.IOException;
import java.util.List;

import javax.validation.Valid;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;

import uk.ac.manchester.spinnaker.machine.HasCoreLocation;
import uk.ac.manchester.spinnaker.machine.ValidP;
import uk.ac.manchester.spinnaker.machine.ValidX;
import uk.ac.manchester.spinnaker.machine.ValidY;
import uk.ac.manchester.spinnaker.machine.tags.IPTag;
import uk.ac.manchester.spinnaker.transceiver.TransceiverInterface;
import uk.ac.manchester.spinnaker.transceiver.exceptions.ProcessException;

/**
 * Data speed up packet gatherer description.
 *
 * @author Christian-B
 * @author Alan Stokes
 */
@JsonFormat(shape = OBJECT)
public class Gather implements HasCoreLocation {
	/**
	 * Type reference for deserializing a list of gatherer descriptions.
	 */
	public static final TypeReference<List<Gather>> LIST = new TR();

	private static class TR extends TypeReference<List<Gather>> {
	}

	/** The x value of the core this placement is on. */
	@ValidX
	private final int x;

	/** The y value of the core this placement is on. */
	@ValidY
	private final int y;

	/** The p value of the core this placement is on. */
	@ValidP
	private final int p;

	/** The IPTag of the package gatherer. */
	@Valid
	private final IPTag iptag;

	/** The extra monitor cores, and what to retrieve from them. */
	private final List<@Valid Monitor> monitors;

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
		this.monitors = copy(monitors);
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
	 * @throws InterruptedException
	 *             If communications are interrupted.
	 */
	public void updateTransactionIdFromMachine(TransceiverInterface txrx)
			throws IOException, ProcessException, InterruptedException {
		transactionId = txrx.readUser1(this);
	}

	/**
	 * @return the iptag
	 */
	public IPTag getIptag() {
		return iptag;
	}

	/**
	 * @return the monitors (read-only)
	 */
	public List<Monitor> getMonitors() {
		return monitors;
	}
}
