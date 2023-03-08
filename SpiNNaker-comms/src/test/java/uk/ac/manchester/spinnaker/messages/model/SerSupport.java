/*
 * Copyright (c) 2022 The University of Manchester
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
package uk.ac.manchester.spinnaker.messages.model;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

abstract class SerSupport {
	private SerSupport() {
	}

	static byte[] serialize(Object obj) throws IOException {
		var baos = new ByteArrayOutputStream();
		try (var oos = new ObjectOutputStream(baos)) {
			oos.writeObject(obj);
		}
		return baos.toByteArray();
	}

	static <T> T deserialize(byte[] bytes, Class<T> cls)
			throws ClassNotFoundException, IOException {
		var bais = new ByteArrayInputStream(bytes);
		try (var ois = new ObjectInputStream(bais)) {
			return cls.cast(ois.readObject());
		}
	}
}
