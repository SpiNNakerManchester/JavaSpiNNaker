/*
 * Copyright (c) 2018-2022 The University of Manchester
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
package uk.ac.manchester.spinnaker.transceiver.exceptions;

import static java.lang.String.format;

import java.io.IOException;

/**
 * Indicates that a request could not be sent.
 */
public class SendFailedException extends IOException {
	private static final long serialVersionUID = -5555562816486761027L;

	/**
	 * @param req
	 *            The request that timed out.
	 * @param numRetries
	 *            How many attempts to send it were made.
	 */
	public SendFailedException(TxrxCommand req, int numRetries) {
		super(format(
				"Errors sending request %s to %d,%d,%d over %d retries: %s",
				req.getCommand(), req.getDestination().getX(),
				req.getDestination().getY(), req.getDestination().getP(),
				numRetries, req.getRetryReason()));
	}
}
