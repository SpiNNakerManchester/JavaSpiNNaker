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

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;

import org.apache.commons.io.IOUtils;

/**
 * Loads resources. In particular, it knows which is the correct class loader to
 * handle looking them up.
 *
 * @author Donal Fellows
 */
public abstract class ResourceLoader {
	private ResourceLoader() {
	}

	/**
	 * Get the contents of a (text) resource. The resource <em>must</em> be in
	 * this package.
	 *
	 * @param name
	 *            The name of the resource.
	 * @return The contents of the file.
	 * @throws UncheckedIOException
	 *             If the file can't be read.
	 */
	public static String resourceToString(String name) {
		try (InputStream is = ResourceLoader.class.getResourceAsStream(name)) {
			return IOUtils.toString(is, UTF_8);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}
}
