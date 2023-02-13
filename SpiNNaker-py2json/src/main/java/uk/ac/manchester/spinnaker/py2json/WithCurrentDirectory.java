/*
 * Copyright (c) 2021-2023 The University of Manchester
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

import static java.lang.String.format;
import static java.lang.System.getProperty;

import org.python.util.PythonInterpreter;

import com.google.errorprone.annotations.MustBeClosed;

/**
 * Hack for Java 11 and later, where just changing {@code user.dir} is no longer
 * enough. We force the change inside Jython as that's the environment that
 * cares. Outside... we shouldn't need to care.
 */
final class WithCurrentDirectory implements AutoCloseable {
	private final PythonInterpreter python;

	@MustBeClosed
	WithCurrentDirectory(PythonInterpreter python, boolean doCd) {
		var cwd = getProperty("user.dir");
		if (doCd) {
			this.python = python;
		} else {
			this.python = null;
		}
		run(format("import os; __saved=os.getcwd(); os.chdir(r'''%s''')", cwd));
	}

	private void run(String script) {
		if (python == null) {
			return;
		}
		python.exec(script);
	}

	@Override
	public void close() {
		run("os.chdir(__saved)");
	}
}
