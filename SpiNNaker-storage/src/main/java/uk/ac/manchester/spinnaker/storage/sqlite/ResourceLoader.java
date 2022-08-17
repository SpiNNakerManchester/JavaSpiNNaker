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
package uk.ac.manchester.spinnaker.storage.sqlite;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Objects.isNull;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.IOUtils;

/**
 * Factoring out of correct resource loading pattern.
 */
public abstract class ResourceLoader {
	private ResourceLoader() {
	}

	private static InputStream open(String name) throws FileNotFoundException {
		InputStream stream = ResourceLoader.class.getResourceAsStream(name);
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
		try (InputStream stream = open(name)) {
			return IOUtils.toString(stream, UTF_8);
		} catch (IOException e) {
			throw new RuntimeException("failed to read resource", e);
		}
	}
}
