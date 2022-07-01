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
package uk.ac.manchester.spinnaker.alloc;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

/**
 * Utility wrappers for I/O.
 *
 * @author Donal Fellows
 */
public abstract class IOUtils {
	private IOUtils() {
	}

	/**
	 * Convert a serializable object into its serialized form.
	 *
	 * @param obj
	 *            The object to serialize.
	 * @return The serialized form.
	 * @throws IOException
	 *             If the object isn't serializable.
	 */
	public static byte[] serialize(Object obj) throws IOException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		try (ObjectOutputStream oos = new ObjectOutputStream(baos)) {
			oos.writeObject(obj);
		}
		return baos.toByteArray();
	}

	/**
	 * Convert a serialized object into its normal form. <strong>Only call on
	 * data that is trusted!</strong>
	 *
	 * @param <T>
	 *            The type of the response.
	 * @param bytes
	 *            The serialized data.
	 * @param cls
	 *            The class that the object is expected to conform to.
	 * @return The deserialized object.
	 * @throws IOException
	 *             If the object isn't deserializable.
	 * @throws ClassNotFoundException
	 *             If the data doesn't represent a known class.
	 * @throws ClassCastException
	 *             If the data is not an object of the expected class.
	 */
	public static <T> T deserialize(byte[] bytes, Class<T> cls)
			throws ClassNotFoundException, IOException {
		ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
		try (ObjectInputStream ois = new ObjectInputStream(bais)) {
			return cls.cast(ois.readObject());
		}
	}
}
