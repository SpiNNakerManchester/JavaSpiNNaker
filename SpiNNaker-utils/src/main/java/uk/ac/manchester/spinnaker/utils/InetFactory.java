/*
 * Copyright (c) 2018 The University of Manchester
 */
package uk.ac.manchester.spinnaker.utils;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * Convenience methods for creating Ipv4 Addresses.
 *
 * @author Christian-B
 */
public abstract class InetFactory {

    private InetFactory() {
    }

    /**
     * Creates a Ipv4 address from bytes.
     *
     * @param addr An array of bytes length 4
     * @return bytes as an Ipv4 address
     * @throws Inet6NotSupportedException
     *      If the addr would translate to a valid Ipv6 address
     *      as this is not supported by Spinnaker
     * @throws UnknownHostException
     *          if IP address is of illegal length
     */
    public static Inet4Address getByAddress(byte[] addr)
            throws UnknownHostException, Inet6NotSupportedException {
        InetAddress general = InetAddress.getByAddress(addr);
        try {
            return (Inet4Address) general;
        } catch (ClassCastException ex) {
            if (general.getClass() == Inet6Address.class) {
                throw new Inet6NotSupportedException(
                        "Spinnaker does not support IpV6.");
            }
            throw ex;
        }
    }

    /**
     * Creates a Ipv4 address from String.
     *
     * @param host the specified hos
     * @return host as an Ipv4 address
     * @throws Inet6NotSupportedException
     *      If the host would translate to a valid Ipv6 address
     *      as this is not supported by Spinnaker
     * @throws UnknownHostException
     *      if no IP address for the host could be found,
     *      or if a scope_id was specified for a global IPv6 address.
     */
    public static Inet4Address getByName(String host)
            throws UnknownHostException, Inet6NotSupportedException {
        InetAddress general = InetAddress.getByName(host);
        try {
            return (Inet4Address) general;
        } catch (ClassCastException ex) {
            if (general.getClass() == Inet6Address.class) {
                throw new Inet6NotSupportedException(host
                    + " converts to an IpV6 which Spinnaker does not support");
            }
            throw ex;
        }
    }

	/**
	 * Specific Exception to show Ipv6 is not supported.
	 */
	@SuppressWarnings("serial")
	public static class Inet6NotSupportedException
			extends UnknownHostException {
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
