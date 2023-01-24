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
import static uk.ac.manchester.spinnaker.transceiver.BMPCommandProcess.BMP_RETRIES;

import java.io.IOException;
import java.util.List;

import uk.ac.manchester.spinnaker.machine.HasCoreLocation;
import uk.ac.manchester.spinnaker.messages.scp.SCPRequest;

/**
 * Indicates that message sending to a BMP failed for various reasons.
 */
public final class BMPSendFailedException extends IOException {
	private static final long serialVersionUID = -7806549580351626377L;

	BMPSendFailedException(SCPRequest<?> req, HasCoreLocation core,
			List<String> retryReason) {
		super(format(
				"Errors sending request %s (%s) to %d,%d,%d over %d retries:"
				+ " %s",
				req.scpRequestHeader.command, req, core.getX(), core.getY(),
				core.getP(), BMP_RETRIES, retryReason));
	}
}
