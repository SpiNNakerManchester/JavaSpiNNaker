/*
 * Copyright (c) 2018 The University of Manchester
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
package uk.ac.manchester.spinnaker.transceiver;

import static java.lang.String.format;
import static uk.ac.manchester.spinnaker.utils.UnitConstants.MSEC_PER_SEC;

import java.net.SocketTimeoutException;

import uk.ac.manchester.spinnaker.messages.scp.SCPRequest;

/**
 * Indicates that message sending to a BMP timed out.
 */
public final class BMPSendTimedOutException
		extends SocketTimeoutException {
	private static final long serialVersionUID = 1660563278795501381L;

	BMPSendTimedOutException(SCPRequest<?> req, int timeout) {
		super(format("Operation %s (%s) timed out after %f seconds",
				req.scpRequestHeader.command, req,
				timeout / (double) MSEC_PER_SEC));
	}
}
