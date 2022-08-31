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

import static com.fasterxml.jackson.databind.PropertyNamingStrategies.KEBAB_CASE;
import static com.fasterxml.jackson.databind.SerializationFeature.FAIL_ON_EMPTY_BEANS;
import static com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS;
import static java.lang.System.err;
import static java.lang.System.exit;
import static org.python.core.Py.getSystemState;
import static org.python.core.Py.newString;
import static org.python.core.PySystemState.initialize;
import static picocli.CommandLine.populateCommand;

import java.io.File;

import org.python.core.PySystemState;
import org.python.util.PythonInterpreter;

import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.json.JsonMapper;

import picocli.CommandLine.ITypeConverter;
import picocli.CommandLine.ParameterException;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.TypeConversionException;

/**
 * Converts Python configurations for classic Spalloc Server into JSON
 * descriptions.
 *
 * @author Donal Fellows
 */
public class MachineDefinitionConverter implements AutoCloseable {
	private PySystemState sys;

	/**
	 * Create a converter.
	 */
	public MachineDefinitionConverter() {
		initialize(null, null);
		sys = getSystemState();
		var enumPy = locateEnumPy();
		sys.path.append(newString(enumPy.getParent()));
	}

	private static File locateEnumPy() {
		var cl = MachineDefinitionConverter.class.getClassLoader();
		return new File(cl.getResource("enum.py").getFile());
	}

	/** {@inheritDoc} */
	@Override
	public void close() {
		sys.close();
	}

	/**
	 * Get the configuration from a Python file.
	 * <p>
	 * <strong>WARNING!</strong> This changes the current working directory of
	 * the process (if {@code doCd} is true).
	 *
	 * @param definitionFile
	 *            The file to load from.
	 * @param doCd
	 *            Whether to force the change of the working directory for the
	 *            duration. Some scripts (especially test cases) need this.
	 * @return The converted configuration.
	 */
	public Configuration loadClassicConfigurationDefinition(File definitionFile,
			boolean doCd) {
		var what = definitionFile.getAbsolutePath();
		try (var py = new PythonInterpreter(null, sys);
				var cd = new WithCurrentDirectory(py, doCd)) {
			py.execfile(what);
			return new Configuration(py.get("configuration"));
		}
	}

	/**
	 * How we write JSON.
	 *
	 * @return A service for writing objects as JSON.
	 */
	protected static ObjectWriter getJsonWriter() {
		return JsonMapper.builder().findAndAddModules()
				.disable(WRITE_DATES_AS_TIMESTAMPS)
				.propertyNamingStrategy(KEBAB_CASE).build().writer()
				.without(FAIL_ON_EMPTY_BEANS);
	}

	/**
	 * The command line arguments of
	 * {@link MachineDefinitionConverter#main(String[])}.
	 */
	private static class Arguments {
		@Parameters(index = "0", paramLabel = "source.py",
				description = "The file to load the configuration Python from.",
				converter = ExistingFileConverter.class)
		private File configFile;

		@Parameters(index = "1", paramLabel = "target.json",
				description = "The file to write the configuration JSON into.")
		private File destination;
	}

	/**
	 * Requires that an argument be an existing plain file.
	 */
	private static class ExistingFileConverter implements ITypeConverter<File> {
		@Override
		public File convert(String value) throws Exception {
			var f = new File(value);
			if (!f.isFile() || !f.canRead()) {
				throw new TypeConversionException("file must be readable");
			}
			return f;
		}
	}

	/**
	 * Main entry point.
	 *
	 * @param args
	 *            Takes two arguments: {@code <source.py>} and
	 *            {@code <target.json>}.
	 * @throws Exception
	 *             If things go wrong
	 */
	public static void main(String... args) throws Exception {
		try (var loader = new MachineDefinitionConverter()) {
			var a = populateCommand(new Arguments(), args);
			var config = loader.loadClassicConfigurationDefinition(a.configFile,
					false);
			getJsonWriter().writeValue(a.destination, config);
		} catch (ParameterException e) {
			err.println(e.getMessage());
			e.getCommandLine().usage(err);
			exit(1);
		}
	}
}
