/*
 * Copyright (c) 2018 The University of Manchester
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
	 * Creates a IPv4 address from the bytes representing the raw address.
	 *
	 * @param addr
	 *            An array of bytes of length 4
	 * @return {@code addr} as an IPv4 address
	 * @throws RuntimeException
	 *             if IP address is of illegal length or otherwise isn't
	 *             supported.
	 */
	public static Inet4Address getByAddressQuietly(byte[] addr) {
		try {
			var general = InetAddress.getByAddress(addr);
			return (Inet4Address) general;
		} catch (ClassCastException | UnknownHostException ex) {
			throw new RuntimeException(
					"unexpected problem when parsing IPv4 address", ex);
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
	 * Creates a IPv4 address from a host name.
	 *
	 * @param host
	 *            the specified host name
	 * @return {@code host} as an IPv4 address
	 * @throws RuntimeException
	 *             if IP address is isn't supported as an IPv4 address.
	 */
	public static Inet4Address getByNameQuietly(String host) {
		try {
			return (Inet4Address) InetAddress.getByName(host);
		} catch (ClassCastException | UnknownHostException ex) {
			throw new RuntimeException(
					"unexpected problem when parsing IPv4 address", ex);
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
