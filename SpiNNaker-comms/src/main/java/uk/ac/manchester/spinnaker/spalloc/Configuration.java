/*
 * Copyright (c) 2018 The University of Manchester
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
package uk.ac.manchester.spinnaker.spalloc;

import static java.lang.Double.parseDouble;
import static java.lang.Integer.parseInt;
import static java.util.Map.entry;
import static uk.ac.manchester.spinnaker.spalloc.JobConstants.HOSTNAME_PROPERTY;
import static uk.ac.manchester.spinnaker.spalloc.JobConstants.KEEPALIVE_DEFAULT;
import static uk.ac.manchester.spinnaker.spalloc.JobConstants.KEEPALIVE_PROPERTY;
import static uk.ac.manchester.spinnaker.spalloc.JobConstants.MACHINE_PROPERTY;
import static uk.ac.manchester.spinnaker.spalloc.JobConstants.MAX_DEAD_BOARDS_DEFAULT;
import static uk.ac.manchester.spinnaker.spalloc.JobConstants.MAX_DEAD_BOARDS_PROPERTY;
import static uk.ac.manchester.spinnaker.spalloc.JobConstants.MAX_DEAD_LINKS_PROPERTY;
import static uk.ac.manchester.spinnaker.spalloc.JobConstants.MIN_RATIO_DEFAULT;
import static uk.ac.manchester.spinnaker.spalloc.JobConstants.MIN_RATIO_PROPERTY;
import static uk.ac.manchester.spinnaker.spalloc.JobConstants.PORT_DEFAULT;
import static uk.ac.manchester.spinnaker.spalloc.JobConstants.PORT_PROPERTY;
import static uk.ac.manchester.spinnaker.spalloc.JobConstants.RECONNECT_DELAY_DEFAULT;
import static uk.ac.manchester.spinnaker.spalloc.JobConstants.RECONNECT_DELAY_PROPERTY;
import static uk.ac.manchester.spinnaker.spalloc.JobConstants.REQUIRE_TORUS_DEFAULT;
import static uk.ac.manchester.spinnaker.spalloc.JobConstants.REQUIRE_TORUS_PROPERTY;
import static uk.ac.manchester.spinnaker.spalloc.JobConstants.TAGS_PROPERTY;
import static uk.ac.manchester.spinnaker.spalloc.JobConstants.TIMEOUT_DEFAULT;
import static uk.ac.manchester.spinnaker.spalloc.JobConstants.TIMEOUT_PROPERTY;
import static uk.ac.manchester.spinnaker.spalloc.JobConstants.USER_PROPERTY;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

import org.apache.commons.configuration2.INIConfiguration;
import org.apache.commons.configuration2.SubnodeConfiguration;
import org.apache.commons.configuration2.builder.FileBasedConfigurationBuilder;
import org.apache.commons.configuration2.builder.fluent.Parameters;
import org.apache.commons.configuration2.convert.DefaultListDelimiterHandler;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.commons.configuration2.io.ClasspathLocationStrategy;
import org.apache.commons.configuration2.io.CombinedLocationStrategy;
import org.apache.commons.configuration2.io.HomeDirectoryLocationStrategy;
import org.apache.commons.configuration2.io.ProvidedURLLocationStrategy;

import uk.ac.manchester.spinnaker.utils.validation.TCPPort;

/** A spalloc configuration loaded from a file. */
public class Configuration {
	private static final char LIST_SEPARATOR = ' ';

	private static final String SECTION_NAME = "spalloc";

	private Map<String, Object> configurationMap;

	private SubnodeConfiguration section;

	/**
	 * Load the configuration from a file. The configuration loader searches
	 * from the file in various "sensible" places.
	 *
	 * @param configFilename
	 *            The name of file to load from.
	 * @throws RuntimeException
	 *             If the configuration could not be loaded.
	 */
	public Configuration(String configFilename) {
		configurationMap = new HashMap<>(DEFAULTS);
		try {
			loadConfig(configFilename);
		} catch (ConfigurationException e) {
			throw new RuntimeException(
					"failed to load configuration from " + configFilename, e);
		}
		setValuesFromConfig();
	}

	private void loadConfig(String configFilename)
			throws ConfigurationException {
		// TODO is this the right way to set this up?
		/*
		 * By default, configuration files are read (in ascending order of
		 * priority) from a system-wide configuration directory (e.g.
		 * ``/etc/xdg/spalloc``), user configuration file (e.g.
		 * ``$HOME/.config/spalloc``) and finally the current working directory
		 * (in a file named ``.spalloc``).
		 */
		var params = new Parameters().ini()
				.setLocationStrategy(new CombinedLocationStrategy(
						List.of(new ProvidedURLLocationStrategy(),
								new HomeDirectoryLocationStrategy(),
								new ClasspathLocationStrategy())))
				.setListDelimiterHandler(
						new DefaultListDelimiterHandler(LIST_SEPARATOR))
				.setThrowExceptionOnMissing(true);
		section = new FileBasedConfigurationBuilder<>(INIConfiguration.class)
				.configure(params.setFileName(configFilename))
				.getConfiguration().getSection(SECTION_NAME);
	}

	/** @return a clone of the map of the configuration. */
	public Map<String, Object> getDefaults() {
		return new HashMap<String, Object>(configurationMap);
	}

	/** @return The spalloc TCP/IP host. */
	public String getHost() {
		return (String) configurationMap.get(HOSTNAME_PROPERTY);
	}

	/** @return The spalloc TCP/IP port. */
	@TCPPort
	public int getPort() {
		return (Integer) configurationMap.get(PORT_PROPERTY);
	}

	/** @return The spalloc user. */
	@NotBlank
	public String getUser() {
		return (String) configurationMap.get(USER_PROPERTY);
	}

	/** @return The keepalive interval, in seconds. */
	@Positive
	public double getKeepalive() {
		return (Double) configurationMap.get(KEEPALIVE_PROPERTY);
	}

	/** @return The reconnection delay, in seconds. */
	@Positive
	public double getReconnectDelay() {
		return (Double) configurationMap.getOrDefault(RECONNECT_DELAY_PROPERTY,
				RECONNECT_DELAY_DEFAULT);
	}

	/** @return The network timeout, in seconds. */
	@Positive
	public double getTimeout() {
		return (Double) configurationMap.get(TIMEOUT_PROPERTY);
	}

	/** @return The desired machine name. */
	public String getMachine() {
		return (String) configurationMap.get(MACHINE_PROPERTY);
	}

	/** @return The desired machine tags. */
	public String[] getTags() {
		return (String[]) configurationMap.get(TAGS_PROPERTY);
	}

	/** @return The minimum ratio for rectangular allocations. */
	@PositiveOrZero
	public double getMinRatio() {
		return (Double) configurationMap.get(MIN_RATIO_PROPERTY);
	}

	/** @return The maximum number of dead boards wanted. */
	@PositiveOrZero
	public Integer getMaxDeadBoards() {
		return (Integer) configurationMap.get(MAX_DEAD_BOARDS_PROPERTY);
	}

	/** @return The maximum number of dead links desired. Often ignored. */
	@PositiveOrZero
	public Integer getMaxDeadLinks() {
		return (Integer) configurationMap.get(MAX_DEAD_LINKS_PROPERTY);
	}

	/** @return Whether a torus is required. Not normally useful. */
	public boolean getRequireTorus() {
		return (Boolean) configurationMap.get(REQUIRE_TORUS_PROPERTY);
	}

	/**
	 * Set up the default values in the map, so the configuration file doesn't
	 * need to have everything listed in it.
	 */
	private static final Map<String, Object> DEFAULTS = Map.ofEntries(//
			entry(PORT_PROPERTY, PORT_DEFAULT),
			entry(KEEPALIVE_PROPERTY, KEEPALIVE_DEFAULT),
			entry(TIMEOUT_PROPERTY, TIMEOUT_DEFAULT),
			entry(MIN_RATIO_PROPERTY, MIN_RATIO_DEFAULT),
			entry(MAX_DEAD_BOARDS_PROPERTY, MAX_DEAD_BOARDS_DEFAULT),
			entry(REQUIRE_TORUS_PROPERTY, REQUIRE_TORUS_DEFAULT));

	private static final String NULL_MARKER = "None";

	private static boolean isNull(String value) {
		return NULL_MARKER.equals(value);
	}

	private Double readNoneOrFloat(String prop) {
		var val = section.getString(prop);
		if (isNull(val)) {
			return null;
		}
		return parseDouble(val);
	}

	private Integer readNoneOrInt(String prop) {
		var val = section.getString(prop);
		if (isNull(val)) {
			return null;
		}
		return parseInt(val);
	}

	private String readNoneOrString(String prop) {
		var val = section.getString(prop);
		if (isNull(val)) {
			return null;
		}
		return val;
	}

	private void setValuesFromConfig() {
		configurationMap.put(HOSTNAME_PROPERTY,
				section.getString(HOSTNAME_PROPERTY, null));
		if (section.containsKey(USER_PROPERTY)) {
			configurationMap.put(USER_PROPERTY,
					section.getString(USER_PROPERTY));
		}
		if (section.containsKey(KEEPALIVE_PROPERTY)) {
			configurationMap.put(KEEPALIVE_PROPERTY,
					readNoneOrFloat(KEEPALIVE_PROPERTY));
		}
		if (section.containsKey(RECONNECT_DELAY_PROPERTY)) {
			configurationMap.put(RECONNECT_DELAY_PROPERTY,
					section.getDouble(RECONNECT_DELAY_PROPERTY));
		}
		if (section.containsKey(TIMEOUT_PROPERTY)) {
			configurationMap.put(TIMEOUT_PROPERTY,
					readNoneOrFloat(TIMEOUT_PROPERTY));
		}
		if (section.containsKey(MACHINE_PROPERTY)) {
			configurationMap.put(MACHINE_PROPERTY,
					readNoneOrString(MACHINE_PROPERTY));
		}
		if (section.containsKey(TAGS_PROPERTY)) {
			if (isNull(section.getString(TAGS_PROPERTY))) {
				configurationMap.put(TAGS_PROPERTY, null);
			} else {
				configurationMap.put(TAGS_PROPERTY,
						section.getArray(String.class, TAGS_PROPERTY));
			}
		}
		if (section.containsKey(MIN_RATIO_PROPERTY)) {
			configurationMap.put(MIN_RATIO_PROPERTY,
					readNoneOrFloat(MIN_RATIO_PROPERTY));
		}
		if (section.containsKey(MAX_DEAD_BOARDS_PROPERTY)) {
			configurationMap.put(MAX_DEAD_BOARDS_PROPERTY,
					readNoneOrInt(MAX_DEAD_BOARDS_PROPERTY));
		}
		if (section.containsKey(MAX_DEAD_LINKS_PROPERTY)) {
			configurationMap.put(MAX_DEAD_LINKS_PROPERTY,
					readNoneOrInt(MAX_DEAD_LINKS_PROPERTY));
		}
		if (section.containsKey(REQUIRE_TORUS_PROPERTY)) {
			configurationMap.put(REQUIRE_TORUS_PROPERTY,
					section.getBoolean(REQUIRE_TORUS_PROPERTY));
		}
	}
}
