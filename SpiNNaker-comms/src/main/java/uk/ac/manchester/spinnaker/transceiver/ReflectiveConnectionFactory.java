/*
 * Copyright (c) 2020 The University of Manchester
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

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.InetAddress;

import uk.ac.manchester.spinnaker.connections.UDPConnection;
import uk.ac.manchester.spinnaker.transceiver.UDPTransceiver.ConnectionFactory;

/**
 * A connection factory that makes connection instances using reflection.
 *
 * @param <Conn>
 *            The type of connections being built by this factory.
 * @author Donal Fellows
 */
public abstract class ReflectiveConnectionFactory<Conn extends UDPConnection<?>>
		implements ConnectionFactory<Conn> {
	private static final String BAD_EXCEPTION = "failed to build instance";
	private final Class<Conn> connClass;

	/**
	 * Create an instance of this class.
	 *
	 * @param connClass
	 *            The type of connections to manufacture.
	 */
	protected ReflectiveConnectionFactory(Class<Conn> connClass) {
		this.connClass = connClass;
	}

	@Override
	public final Class<Conn> getClassKey() {
		return connClass;
	}

	@Override
	public final Conn getInstance(InetAddress localAddress) throws IOException {
		try {
			return connClass.getConstructor(InetAddress.class)
					.newInstance(localAddress);
		} catch (InvocationTargetException e) {
			if (e.getCause() instanceof IOException) {
				throw (IOException) e.getCause();
			}
			throw new RuntimeException(BAD_EXCEPTION, e.getCause());
		} catch (InstantiationException | IllegalAccessException
				| IllegalArgumentException | NoSuchMethodException
				| SecurityException e) {
			throw new RuntimeException(BAD_EXCEPTION, e);
		}
	}

	@Override
	public final Conn getInstance(InetAddress localAddress, int localPort)
			throws IOException {
		try {
			return connClass.getConstructor(InetAddress.class, Integer.TYPE)
					.newInstance(localAddress, localPort);
		} catch (InvocationTargetException e) {
			if (e.getCause() instanceof IOException) {
				throw (IOException) e.getCause();
			}
			throw new RuntimeException(BAD_EXCEPTION, e.getCause());
		} catch (InstantiationException | IllegalAccessException
				| IllegalArgumentException | NoSuchMethodException
				| SecurityException e) {
			throw new RuntimeException(BAD_EXCEPTION, e);
		}
	}
}
