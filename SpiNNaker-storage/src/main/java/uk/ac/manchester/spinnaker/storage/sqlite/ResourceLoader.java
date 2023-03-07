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
package uk.ac.manchester.spinnaker.storage.sqlite;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Objects.isNull;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.IOUtils;

import com.google.errorprone.annotations.MustBeClosed;

/**
 * Factoring out of correct resource loading pattern.
 */
public abstract class ResourceLoader {
	private ResourceLoader() {
	}

	@MustBeClosed
	private static InputStream open(String name) throws FileNotFoundException {
		var stream = ResourceLoader.class.getResourceAsStream(name);
		if (isNull(stream)) {
			throw new FileNotFoundException(name);
		}
		return stream;
	}

	/**
	 * Load a text resource from the given name using this class's class loader.
	 *
	 * @param name
	 *            The name of the resource to load.
	 * @return The content of the resource.
	 * @throws RuntimeException
	 *             If things don't work. Shouldn't happen if build is correct.
	 */
	public static String loadResource(String name) {
		try (var stream = open(name)) {
			return IOUtils.toString(stream, UTF_8);
		} catch (IOException e) {
			throw new RuntimeException("failed to read resource", e);
		}
	}
}
