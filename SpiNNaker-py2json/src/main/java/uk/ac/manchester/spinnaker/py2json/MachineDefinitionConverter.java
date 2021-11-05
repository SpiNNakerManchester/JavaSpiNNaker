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
import static picocli.CommandLine.populateCommand;
import static uk.ac.manchester.spinnaker.py2json.PythonUtils.getattr;
import static uk.ac.manchester.spinnaker.py2json.PythonUtils.item;
import static uk.ac.manchester.spinnaker.py2json.PythonUtils.toCollectingMap;
import static uk.ac.manchester.spinnaker.py2json.PythonUtils.toList;
import static uk.ac.manchester.spinnaker.py2json.PythonUtils.toMap;
import static uk.ac.manchester.spinnaker.py2json.PythonUtils.toSet;

import java.io.File;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.python.core.PyObject;
import org.python.core.PyString;
import org.python.core.PySystemState;
import org.python.util.PythonInterpreter;

import com.fasterxml.jackson.annotation.JsonProperty;
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
	/** Triad coordinates. */
	public static final class XYZ {
		/** X coordinate. */
		public final int x;

		/** Y coordinate. */
		public final int y;

		/** Z coordinate. */
		public final int z;

		public XYZ(int x, int y, int z) {
			this.x = x;
			this.y = y;
			this.z = z;
		}

		private XYZ(PyObject tuple) {
			int index = 0;
			x = item(tuple, index++).asInt();
			y = item(tuple, index++).asInt();
			z = item(tuple, index++).asInt();
		}

		@Override
		public boolean equals(Object obj) {
			if (obj instanceof XYZ) {
				XYZ other = (XYZ) obj;
				return x == other.x && y == other.y && z == other.z;
			}
			return false;
		}

		@Override
		public int hashCode() {
			return (((x << 2 + x) ^ y) << 2 + y) ^ z;
		}

		@Override
		public String toString() {
			return "[x:" + x + ",y:" + y + ",z:" + z + "]";
		}
	}

	/** Frame/BMP coordinates. */
	public static final class CF {
		/** Cabinet number. */
		public final int c;

		/** Frame number. */
		public final int f;

		public CF(int c, int f) {
			this.c = c;
			this.f = f;
		}

		private CF(PyObject tuple) {
			int index = 0;
			c = item(tuple, index++).asInt();
			f = item(tuple, index++).asInt();
		}

		@Override
		public boolean equals(Object obj) {
			if (obj instanceof CF) {
				CF other = (CF) obj;
				return c == other.c && f == other.f;
			}
			return false;
		}

		@Override
		public int hashCode() {
			return ((c << 2 + c) ^ f) << 2 + f;
		}

		@Override
		public String toString() {
			return "[c:" + c + ",f:" + f + "]";
		}
	}

	/** Physical board coordinates. */
	public static final class CFB {
		/** Cabinet number. */
		public final int c;

		/** Frame number. */
		public final int f;

		/** Board number. */
		public final int b;

		public CFB(int c, int f, int b) {
			this.c = c;
			this.f = f;
			this.b = b;
		}

		private CFB(PyObject tuple) {
			int index = 0;
			c = item(tuple, index++).asInt();
			f = item(tuple, index++).asInt();
			b = item(tuple, index++).asInt();
		}

		@Override
		public boolean equals(Object obj) {
			if (obj instanceof CFB) {
				CFB other = (CFB) obj;
				return c == other.c && f == other.f && b == other.b;
			}
			return false;
		}

		@Override
		public int hashCode() {
			return (((c << 2 + c) ^ f) << 2 + f) ^ b;
		}

		@Override
		public String toString() {
			return "[c:" + c + ",f:" + f + ",b:" + b + "]";
		}
	}

	/**
	 * Enumeration of links from a SpiNNaker chip.
	 * <p>
	 * Note that the numbers chosen have two useful properties:
	 *
	 * <ul>
	 * <li>The integer values assigned are chosen to match the numbers used to
	 * identify the links in the low-level software API and hardware registers.
	 * <li>The links are ordered consecutively in anticlockwise order meaning
	 * the opposite link is {@code (link+3)%6}.
	 * </ul>
	 */
	public enum Link {
		/** East. */
		east,
		/** North-East. */
		northEast,
		/** North. */
		north,
		/** West. */
		west,
		/** South-West. */
		southWest,
		/** South. */
		south
	}

	/** A machine description. JSON-serializable. */
	public static final class Machine {
		/** The name of the machine. */
		public final String name;

		/** The tags of the machine. */
		public final Set<String> tags;

		/** The width of the machine, in triads. */
		public final int width;

		/** The height of the machine, in triads. */
		public final int height;

		/** The dead boards of the machine. */
		public final Set<XYZ> deadBoards;

		/**
		 * The extra dead links of the machine. Doesn't include links to dead
		 * boards.
		 */
		public final Map<XYZ, EnumSet<Link>> deadLinks;

		/** The logical-to-physical board location map. */
		public final Map<XYZ, CFB> boardLocations;

		/** The IP addresses of the BMPs. */
		@JsonProperty("bmp-ips")
		public final Map<CF, String> bmpIPs;

		/** The IP addresses of the boards. */
		@JsonProperty("spinnaker-ips")
		public final Map<XYZ, String> spinnakerIPs;

		private static final int IDX = 3;

		private Machine(PyObject machine) {
			name = getattr(machine, "name").asString();
			tags = toSet(getattr(machine, "tags"), PyObject::asString);
			width = getattr(machine, "width").asInt();
			height = getattr(machine, "height").asInt();
			deadBoards = toSet(getattr(machine, "dead_boards"), XYZ::new);
			deadLinks = toCollectingMap(getattr(machine, "dead_links"),
					XYZ::new, ignored -> EnumSet.noneOf(Link.class),
					key -> Link.values()[item(key, IDX).asInt()]);
			boardLocations = toMap(getattr(machine, "board_locations"),
					XYZ::new, CFB::new);
			bmpIPs = toMap(getattr(machine, "bmp_ips"), CF::new,
					PyObject::asString);
			spinnakerIPs = toMap(getattr(machine, "spinnaker_ips"), XYZ::new,
					PyObject::asString);
		}

		@Override
		public String toString() {
			return new StringBuilder("Machine(").append("name=").append(name)
					.append(",").append("tags=").append(tags).append(",")
					.append("width=").append(width).append(",")
					.append("height=").append(height).append(",")
					.append("deadBoards=").append(deadBoards).append(",")
					.append("deadLinks=").append(deadLinks).append(",")
					.append("boardLocations=").append(boardLocations)
					.append(",").append("bmpIPs=").append(bmpIPs).append(",")
					.append("spinnakerIPs=").append(spinnakerIPs).append(")")
					.toString();
		}
	}

	/** A configuration description. JSON-serializable. */
	public static final class Configuration {
		/** The machines to manage. */
		public final List<Machine> machines;

		/** The port for the service to listen on. */
		public final int port;

		/**
		 * The host address for the service to listen on. Empty = all
		 * interfaces.
		 */
		public final String ip;

		/** How often (in seconds) to check for timeouts. */
		public final double timeoutCheckInterval;

		/** How many retired jobs to retain. */
		public final int maxRetiredJobs;

		/** Time to wait before freeing. */
		public final int secondsBeforeFree;

		private Configuration(PyObject configuration) {
			machines = toList(getattr(configuration, "machines"), Machine::new);
			port = getattr(configuration, "port").asInt();
			ip = getattr(configuration, "ip").asString();
			timeoutCheckInterval =
					getattr(configuration, "timeout_check_interval").asDouble();
			maxRetiredJobs = getattr(configuration, "max_retired_jobs").asInt();
			secondsBeforeFree =
					getattr(configuration, "seconds_before_free").asInt();
		}

		@Override
		public String toString() {
			return new StringBuilder("Configuration(").append("machines=")
					.append(machines).append(",").append("port=").append(port)
					.append(",").append("ip=").append(ip).append(",")
					.append("timeoutCheckInterval=")
					.append(timeoutCheckInterval).append(",")
					.append("maxRetiredJobs=").append(maxRetiredJobs)
					.append(",").append("secondsBeforeFree=")
					.append(secondsBeforeFree).append(")").toString();
		}
	}

	private PySystemState sys;

	/**
	 * Create a converter.
	 */
	public MachineDefinitionConverter() {
		PySystemState.initialize(null, null);
		sys = new PySystemState();
		File enumPy = new File(
				getClass().getClassLoader().getResource("enum.py").getFile());
		sys.path.append(new PyString(enumPy.getParent()));
	}

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
		String what = definitionFile.getAbsolutePath();
		String cwd = System.getProperty("user.dir");
		try (PythonInterpreter py = new PythonInterpreter(null, sys)) {
			if (doCd) {
				/*
				 * Hack for Java 11 and later, where just changing user.dir is
				 * no longer enough. We force the change inside Jython as that's
				 * the environment that cares. Outside... we shouldn't need to
				 * care.
				 */
				py.exec(String.format(
						"import os; __saved=os.getcwd(); os.chdir(r'''%s''')",
						cwd));
			}
			try {
				py.execfile(what);
				return new Configuration(py.get("configuration"));
			} finally {
				if (doCd) {
					py.exec("os.chdir(__saved)");
				}
			}
		}
	}

	/**
	 * @return A service for writing objects as JSON.
	 */
	public static ObjectWriter getJsonWriter() {
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
			File f = new File(value);
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
		try (MachineDefinitionConverter loader =
				new MachineDefinitionConverter()) {
			Arguments a = populateCommand(new Arguments(), args);
			Configuration config = loader
					.loadClassicConfigurationDefinition(a.configFile, false);
			getJsonWriter().writeValue(a.destination, config);
		} catch (ParameterException e) {
			System.err.println(e.getMessage());
			e.getCommandLine().usage(System.err);
			System.exit(1);
		}
	}
}
