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
package uk.ac.manchester.spinnaker.connections.model;

import java.io.IOException;

/**
 * An abstract connection to the SpiNNaker board over some medium.
 */
public interface Connection extends SocketHolder {
	/**
	 * Determines if the medium is connected at this point in time. Connected
	 * media are not {@linkplain #isClosed() closed}. Disconnected media might
	 * not be open.
	 *
	 * @return true if the medium is connected, false otherwise
	 * @throws IOException
	 *             If there is an error when determining the connectivity of the
	 *             medium.
	 */
	boolean isConnected() throws IOException;

	/**
	 * Determines if the medium is closed at this point in time. Closed media
	 * are not {@linkplain #isConnected() connected}. Open media might not be
	 * connected.
	 *
	 * @return true if the medium is closed, false otherwise
	 */
	boolean isClosed();
}
