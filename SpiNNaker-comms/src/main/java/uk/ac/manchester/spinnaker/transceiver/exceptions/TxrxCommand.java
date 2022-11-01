/*
 * Copyright (c) 2022 The University of Manchester
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

import java.util.List;

import uk.ac.manchester.spinnaker.machine.HasCoreLocation;
import uk.ac.manchester.spinnaker.messages.scp.CommandCode;

/**
 * Information about an in-flight command message. Used for exception
 * generation.
 */
public interface TxrxCommand {
	/**
	 * Get the command being sent in the request.
	 *
	 * @return The request's SCP command.
	 */
	CommandCode getCommand();

	/**
	 * Which core is the destination of the request?
	 *
	 * @return The core location.
	 */
	HasCoreLocation getDestination();

	/**
	 * @return The list of reasons why a request was retried.
	 */
	List<String> getRetryReason();
}
