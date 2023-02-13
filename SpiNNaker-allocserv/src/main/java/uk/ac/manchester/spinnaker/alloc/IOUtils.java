/*
 * Copyright (c) 2022-2023 The University of Manchester
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
		var baos = new ByteArrayOutputStream();
		try (var oos = new ObjectOutputStream(baos)) {
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
		var bais = new ByteArrayInputStream(bytes);
		try (var ois = new ObjectInputStream(bais)) {
			return cls.cast(ois.readObject());
		}
	}
}
