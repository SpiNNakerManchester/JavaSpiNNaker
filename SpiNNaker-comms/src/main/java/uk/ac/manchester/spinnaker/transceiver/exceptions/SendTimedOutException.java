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
import static uk.ac.manchester.spinnaker.utils.UnitConstants.MSEC_PER_SEC;

import java.net.SocketTimeoutException;

/**
 * Indicates that a request timed out.
 */
public class SendTimedOutException extends SocketTimeoutException {
	private static final long serialVersionUID = -7911020002602751941L;

	/**
	 * @param req
	 *            The request that timed out.
	 * @param timeout
	 *            The length of timeout, in milliseconds.
	 * @param seqNum
	 *            The sequence number.
	 */
	public SendTimedOutException(TxrxCommand req, int timeout, int seqNum) {
		super(format("Operation %s timed out after %f seconds with seq num %d",
				req.getCommand(), timeout / (double) MSEC_PER_SEC, seqNum));
	}
}
