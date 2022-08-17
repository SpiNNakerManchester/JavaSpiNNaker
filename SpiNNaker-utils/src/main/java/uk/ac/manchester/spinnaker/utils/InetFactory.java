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
package uk.ac.manchester.spinnaker.utils;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * Convenience methods for creating IPv4 Addresses.
 *
 * @author Christian-B
 */
public abstract class InetFactory {
	private InetFactory() {
	}

	/**
	 * Creates a IPv4 address from the bytes representing the raw address.
	 *
	 * @param addr
	 *            An array of bytes of length 4
	 * @return {@code addr} as an IPv4 address
	 * @throws Inet6NotSupportedException
	 *             If the {@code addr} would translate to a valid IPv6 address;
	 *             this is not supported by Spinnaker
	 * @throws UnknownHostException
	 *             if IP address is of illegal length
	 */
	public static Inet4Address getByAddress(byte[] addr)
			throws UnknownHostException, Inet6NotSupportedException {
		var general = InetAddress.getByAddress(addr);
		try {
			return (Inet4Address) general;
		} catch (ClassCastException ex) {
			if (general.getClass() == Inet6Address.class) {
				throw new Inet6NotSupportedException(
						"Spinnaker does not support IPv6.");
			}
			throw ex;
		}
	}

	/**
	 * Creates a IPv4 address from a host name.
	 *
	 * @param host
	 *            the specified host name
	 * @return {@code host} as an IPv4 address
	 * @throws Inet6NotSupportedException
	 *             If the {@code host} would translate to a valid IPv6 address;
	 *             this is not supported by Spinnaker
	 * @throws UnknownHostException
	 *             if no IP address for the host could be found, or if a
	 *             scope_id was specified for a global IPv6 address.
	 */
	public static Inet4Address getByName(String host)
			throws UnknownHostException, Inet6NotSupportedException {
		var general = InetAddress.getByName(host);
		try {
			return (Inet4Address) general;
		} catch (ClassCastException ex) {
			if (general.getClass() == Inet6Address.class) {
				throw new Inet6NotSupportedException(
						host + " converts to an IPv6 address, "
								+ "which Spinnaker does not support");
			}
			throw ex;
		}
	}

	/**
	 * Specific Exception to show IPv6 is not supported.
	 */
	public static class Inet6NotSupportedException
			extends UnknownHostException {
		private static final long serialVersionUID = -7430619278827122304L;

		/**
		 * Constructs a new {@code Inet6NotSupportedException} with the
		 * specified detail message.
		 *
		 * @param msg
		 *            the detail message.
		 */
		public Inet6NotSupportedException(String msg) {
			super(msg);
		}
	}
}
