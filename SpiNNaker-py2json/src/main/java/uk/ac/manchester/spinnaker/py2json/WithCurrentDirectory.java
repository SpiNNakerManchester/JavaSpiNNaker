/*
 * Copyright (c) 2021 The University of Manchester
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
