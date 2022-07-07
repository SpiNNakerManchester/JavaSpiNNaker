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

import static java.lang.String.format;
import static java.lang.System.getProperty;

import org.python.util.PythonInterpreter;

/**
 * Hack for Java 11 and later, where just changing {@code user.dir} is no longer
 * enough. We force the change inside Jython as that's the environment that
 * cares. Outside... we shouldn't need to care.
 */
final class WithCurrentDirectory implements AutoCloseable {
	private final PythonInterpreter python;

	WithCurrentDirectory(PythonInterpreter python, boolean doCd) {
		String cwd = getProperty("user.dir");
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
