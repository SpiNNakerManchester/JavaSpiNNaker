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
package uk.ac.manchester.spinnaker.messages.model;

import java.net.InetAddress;

/** The IP addresses associated with a SpiNNaker board. */
public final class Addresses {
	// TODO convert to record in 17
	/** The IPv4 address of the BMP. */
	public final InetAddress bmpIPAddress;

	/** The IPv4 address of the managed SpiNNaker board. */
	public final InetAddress spinIPAddress;

	/**
	 * @param bmpIPAddress
	 *            The IPv4 address of the BMP.
	 * @param spinIPAddress
	 *            The IPv4 address of the managed SpiNNaker board.
	 */
	public Addresses(InetAddress bmpIPAddress, InetAddress spinIPAddress) {
		this.bmpIPAddress = bmpIPAddress;
		this.spinIPAddress = spinIPAddress;
	}
}
