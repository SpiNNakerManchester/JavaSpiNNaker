/*
 * Copyright (c) 2018-2023 The University of Manchester
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package uk.ac.manchester.spinnaker.machine.datalinks;

import java.net.InetAddress;
import java.util.Objects;

import com.google.errorprone.annotations.Immutable;

/**
 * A tuple of an IP address and a SpiNNaker link ID.
 *
 * @author Christian-B
 */
@Immutable
public final class InetIdTuple {
	/** The InetAddress of this tuple which may be {@code null}. */
	public final InetAddress address;

	/** The ID of this tuple. */
	public final int id;

	/**
	 * Make an instance.
	 *
	 * @param address
	 *            The IP address of this tuple, which may be {@code null}.
	 * @param id
	 *            The ID of this tuple.
	 */
	public InetIdTuple(InetAddress address, int id) {
		this.address = address;
		this.id = id;
	}

	@Override
	public int hashCode() {
		return Objects.hash(address, id);
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof InetIdTuple) {
			var other = (InetIdTuple) obj;
			return (id == other.id) && Objects.equals(address, other.address);
		}
		return false;
	}
}
