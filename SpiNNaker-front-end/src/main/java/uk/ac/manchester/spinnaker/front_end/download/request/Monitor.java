/*
 * Copyright (c) 2018 The University of Manchester
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package uk.ac.manchester.spinnaker.front_end.download.request;

import static com.fasterxml.jackson.annotation.JsonFormat.Shape.OBJECT;
import static uk.ac.manchester.spinnaker.utils.CollectionUtils.copy;

import java.io.IOException;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import uk.ac.manchester.spinnaker.machine.HasCoreLocation;
import uk.ac.manchester.spinnaker.machine.ValidP;
import uk.ac.manchester.spinnaker.machine.ValidX;
import uk.ac.manchester.spinnaker.machine.ValidY;
import uk.ac.manchester.spinnaker.transceiver.ProcessException;
import uk.ac.manchester.spinnaker.transceiver.TransceiverInterface;

/**
 * Extra monitor core information.
 *
 * @author Christian-B
 * @author Alan Stokes
 */
@JsonFormat(shape = OBJECT)
public class Monitor implements HasCoreLocation {
	/** The X coordinate of the core this monitor is placed on. */
	@ValidX
	private final int x;

	/** The Y coordinate of the core this monitor is placed on. */
	@ValidY
	private final int y;

	/** The processor ID of the core this monitor is placed on. */
	@ValidP
	private final int p;

	/** The vertex placements that this monitor will read. */
	private final List<@Valid @NotNull Placement> placements;

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
		this.placements = copy(placements);
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
	 * Updates the transaction ID by 1 and wraps with the cap.
	 */
	public void updateTransactionId() {
		transactionId = (transactionId + 1) & TRANSACTION_ID_CAP;
	}

	/** @return The current transaction ID. */
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
	 * @throws InterruptedException
	 *             If communications are interrupted.
	 */
	public void updateTransactionIdFromMachine(TransceiverInterface txrx)
			throws IOException, ProcessException, InterruptedException {
		transactionId = txrx.readUser1(this);
	}

	/**
	 * @return the placements (read-only)
	 */
	public List<Placement> getPlacements() {
		return placements;
	}

	@Override
	public String toString() {
		return "Monitor: " + asCoreLocation().toString();
	}
}
