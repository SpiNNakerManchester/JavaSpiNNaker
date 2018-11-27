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
package uk.ac.manchester.spinnaker.connections;

import uk.ac.manchester.spinnaker.messages.scp.SCPRequest;

/**
 * A callback handler for reporting errors in receiving an SCP (or BMP) message.
 *
 * @author Donal Fellows
 */
@FunctionalInterface
public interface SCPErrorHandler {
	/**
	 * A callback function to call when an error is found when processing an SCP
	 * message.
	 *
	 * @param request
	 *            the original SCPRequest
	 * @param exception
	 *            the exception caught while sending the request.
	 */
	void handleError(SCPRequest<?> request, Throwable exception);
}
