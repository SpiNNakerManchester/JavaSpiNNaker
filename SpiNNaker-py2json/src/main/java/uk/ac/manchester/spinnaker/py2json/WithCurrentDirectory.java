/*
 * Copyright (c) 2021 The University of Manchester
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
package uk.ac.manchester.spinnaker.py2json;

import static java.lang.System.getProperty;

import java.nio.file.Paths;

import org.python.core.PySystemState;
import org.python.util.PythonInterpreter;

import com.google.errorprone.annotations.MustBeClosed;

/**
 * Sets the Jython notion of what the working directory is for the duration of
 * the the context. This isn't properly the current working directory (Java code
 * really doesn't care!) but it is what the Python side expects.
 * <p>
 * @see PySystemState#setCurrentWorkingDir(String)
 */
final class WithCurrentDirectory implements AutoCloseable {
	private final PySystemState sys;

	private final String oldCwd;

	@MustBeClosed
	WithCurrentDirectory(PythonInterpreter python, boolean doCd) {
		if (doCd) {
			sys = python.getSystemState();
			oldCwd = sys.getCurrentWorkingDir();
			sys.setCurrentWorkingDir(Paths.get(getProperty("user.dir"))
					.toAbsolutePath().toString());
		} else {
			sys = null;
			oldCwd = null;
		}
	}

	@Override
	public void close() {
		if (sys != null) {
			sys.setCurrentWorkingDir(oldCwd);
		}
	}
}
