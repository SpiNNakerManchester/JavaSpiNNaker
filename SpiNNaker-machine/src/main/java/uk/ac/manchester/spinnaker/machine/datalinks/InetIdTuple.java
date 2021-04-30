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
package uk.ac.manchester.spinnaker.machine.datalinks;

import java.net.InetAddress;
import java.util.Objects;

/**
 *
 * @author Christian-B
 */
public class InetIdTuple {
	/** The InetAddress of this tuple which may be {@code null}. */
	public final InetAddress address;

	/** The ID of this tuple. */
	public final int id;

	/**
	 * The main Constructor which sets all values.
	 *
	 * @param address
	 *            The InetAddress of this tuple which may be {@code null}.
	 * @param id
	 *            The ID of this tuple.
	 */
	public InetIdTuple(InetAddress address, int id) {
		this.address = address;
		this.id = id;
	}

	@Override
	public int hashCode() {
		int hash = 5;
		hash = 29 * hash + Objects.hashCode(this.address);
		hash = 29 * hash + this.id;
		return hash;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		final InetIdTuple other = (InetIdTuple) obj;
		if (this.id != other.id) {
			return false;
		}
		return Objects.equals(this.address, other.address);
	}
}
