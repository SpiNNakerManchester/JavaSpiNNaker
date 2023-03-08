/*
 * Copyright (c) 2021 The University of Manchester
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
package uk.ac.manchester.spinnaker.py2json;

import static com.fasterxml.jackson.databind.PropertyNamingStrategies.KEBAB_CASE;
import static com.fasterxml.jackson.databind.SerializationFeature.FAIL_ON_EMPTY_BEANS;
import static com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS;
import static jakarta.validation.Validation.byDefaultProvider;
import static java.lang.System.exit;
import static org.python.core.Py.getSystemState;
import static org.python.core.Py.newString;
import static org.python.core.PySystemState.initialize;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.Callable;

import org.hibernate.validator.messageinterpolation.ParameterMessageInterpolator;
import org.python.core.PySystemState;
import org.python.util.PythonInterpreter;

import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.google.errorprone.annotations.MustBeClosed;

import jakarta.validation.ValidatorFactory;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.ITypeConverter;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.TypeConversionException;

/**
 * Converts Python configurations for classic Spalloc Server into JSON
 * descriptions.
 *
 * @author Donal Fellows
 */
public class MachineDefinitionConverter implements AutoCloseable {
	private static final ValidatorFactory VALIDATOR_FACTORY =
			byDefaultProvider().configure()
					.messageInterpolator(new ParameterMessageInterpolator())
					.buildValidatorFactory();

	private PySystemState sys;

	/**
	 * Create a converter.
	 */
	@MustBeClosed
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
	 * Validate a configuration, writing failures to {@link System#err}.
	 *
	 * @param config
	 *            The configuration to validate.
	 */
	public void validate(Configuration config) {
		var failures = VALIDATOR_FACTORY.getValidator().validate(config);
		failures.forEach(c -> {
			System.err.println(
					"WARNING: validation failure: " + c.getMessage());
		});
		if (!failures.isEmpty()) {
			System.err.println("validation failed; JSON may be unusable");
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
	 * Command line definition.
	 */
	@Command(name = "py2json", mixinStandardHelpOptions = true, version = {
		"version 0.1", "NB: this tool is only partially supported; it "
				+ "exists to support University of Manchester staff only"})
	private class CmdImpl implements Callable<Integer> {
		@Parameters(index = "0", paramLabel = "source.py",
				description = "The file to load the configuration Python from.",
				converter = ExistingFileConverter.class)
		private File configFile;

		@Parameters(index = "1", paramLabel = "target.json",
				description = "The file to write the configuration JSON into.")
		private File destination;

		/**
		 * Load a configuration from a Python config file and write it to a JSON
		 * file. Which files to use are defined by command line arguments.
		 *
		 * @return The exit code.
		 * @throws IOException
		 *             If anything goes wrong.
		 */
		@Override
		public Integer call() throws IOException {
			var config = loadClassicConfigurationDefinition(configFile, false);
			validate(config);
			getJsonWriter().writeValue(destination, config);
			return CommandLine.ExitCode.OK;
		}
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
		int code;
		try (var loader = new MachineDefinitionConverter()) {
			code = new CommandLine(loader.new CmdImpl()).execute(args);
		}
		exit(code);
	}
}
