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
import static uk.ac.manchester.spinnaker.py2json.PythonUtils.getattr;
import static uk.ac.manchester.spinnaker.py2json.PythonUtils.item;
import static uk.ac.manchester.spinnaker.py2json.PythonUtils.toCollectingMap;
import static uk.ac.manchester.spinnaker.py2json.PythonUtils.toList;
import static uk.ac.manchester.spinnaker.py2json.PythonUtils.toMap;
import static uk.ac.manchester.spinnaker.py2json.PythonUtils.toSet;

import java.io.File;
import java.io.IOException;
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

public class MachineDefinitionConverter implements AutoCloseable {
	public static class XYZ {
		public final int x;
		public final int y;
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

	public static class CF {
		public final int c;
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

	public static class CFB {
		public final int c;
		public final int f;
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

	public enum Link {
		east, northEast, north, west, southWest, south
	}

	public static class Machine {
		@JsonProperty
		final String name;
		@JsonProperty
		final Set<String> tags;
		@JsonProperty
		final int width;
		@JsonProperty
		final int height;
		@JsonProperty
		final Set<XYZ> deadBoards;
		@JsonProperty
		final Map<XYZ, EnumSet<Link>> deadLinks;
		@JsonProperty
		final Map<XYZ, CFB> boardLocations;
		@JsonProperty("bmp-ips")
		final Map<CF, String> bmpIPs;
		@JsonProperty("spinnaker-ips")
		final Map<XYZ, String> spinnakerIPs;

		private Machine(PyObject machine) {
			name = getattr(machine, "name").asString();
			tags = toSet(getattr(machine, "tags"), PyObject::asString);
			width = getattr(machine, "width").asInt();
			height = getattr(machine, "height").asInt();
			deadBoards = toSet(getattr(machine, "dead_boards"), XYZ::new);
			deadLinks = toCollectingMap(getattr(machine, "dead_links"),
					XYZ::new, ignored -> EnumSet.noneOf(Link.class),
					key -> Link.values()[item(key, 3).asInt()]);
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

	public static class Configuration {
		@JsonProperty
		final List<Machine> machines;
		@JsonProperty
		final int port;
		@JsonProperty
		final String ip;
		@JsonProperty
		final double timeoutCheckInterval;
		@JsonProperty
		final int maxRetiredJobs;
		@JsonProperty
		final int secondsBeforeFree;

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

	public Configuration loadClassicConfigurationDefinition(
			File definitionFile) {
		try (PythonInterpreter py = new PythonInterpreter(null, sys)) {
			py.execfile(definitionFile.getAbsolutePath());
			return new Configuration(py.get("configuration"));
		}
	}

	public static ObjectWriter getJsonWriter() {
		return JsonMapper.builder().findAndAddModules()
				.disable(WRITE_DATES_AS_TIMESTAMPS)
				.propertyNamingStrategy(KEBAB_CASE).build().writer()
				.without(FAIL_ON_EMPTY_BEANS);
	}

	public static void main(String... args) throws IOException {
		if (args.length != 2) {
			System.err.println("takes two args: <source.py> <target.json>");
			System.exit(1);
		}
		File configFile = new File(args[0]);
		File destination = new File(args[1]);
		try (MachineDefinitionConverter loader =
				new MachineDefinitionConverter()) {
			Configuration config =
					loader.loadClassicConfigurationDefinition(configFile);
			getJsonWriter().writeValue(destination, config);
		}
	}
}
