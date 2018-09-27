package uk.ac.manchester.spinnaker.spalloc;

import static java.util.Arrays.asList;
import static uk.ac.manchester.spinnaker.spalloc.JobConstants.HOSTNAME_PROPERTY;
import static uk.ac.manchester.spinnaker.spalloc.JobConstants.KEEPALIVE_DEFAULT;
import static uk.ac.manchester.spinnaker.spalloc.JobConstants.KEEPALIVE_PROPERTY;
import static uk.ac.manchester.spinnaker.spalloc.JobConstants.MACHINE_DEFAULT;
import static uk.ac.manchester.spinnaker.spalloc.JobConstants.MACHINE_PROPERTY;
import static uk.ac.manchester.spinnaker.spalloc.JobConstants.MAX_DEAD_BOARDS_DEFAULT;
import static uk.ac.manchester.spinnaker.spalloc.JobConstants.MAX_DEAD_BOARDS_PROPERTY;
import static uk.ac.manchester.spinnaker.spalloc.JobConstants.MAX_DEAD_LINKS_DEFAULT;
import static uk.ac.manchester.spinnaker.spalloc.JobConstants.MAX_DEAD_LINKS_PROPERTY;
import static uk.ac.manchester.spinnaker.spalloc.JobConstants.MIN_RATIO_DEFAULT;
import static uk.ac.manchester.spinnaker.spalloc.JobConstants.MIN_RATIO_PROPERTY;
import static uk.ac.manchester.spinnaker.spalloc.JobConstants.PORT_DEFAULT;
import static uk.ac.manchester.spinnaker.spalloc.JobConstants.PORT_PROPERTY;
import static uk.ac.manchester.spinnaker.spalloc.JobConstants.RECONNECT_DELAY_DEFAULT;
import static uk.ac.manchester.spinnaker.spalloc.JobConstants.RECONNECT_DELAY_PROPERTY;
import static uk.ac.manchester.spinnaker.spalloc.JobConstants.REQUIRE_TORUS_DEFAULT;
import static uk.ac.manchester.spinnaker.spalloc.JobConstants.REQUIRE_TORUS_PROPERTY;
import static uk.ac.manchester.spinnaker.spalloc.JobConstants.TAGS_DEFAULT;
import static uk.ac.manchester.spinnaker.spalloc.JobConstants.TAGS_PROPERTY;
import static uk.ac.manchester.spinnaker.spalloc.JobConstants.TIMEOUT_DEFAULT;
import static uk.ac.manchester.spinnaker.spalloc.JobConstants.TIMEOUT_PROPERTY;
import static uk.ac.manchester.spinnaker.spalloc.JobConstants.USER_PROPERTY;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.configuration2.INIConfiguration;
import org.apache.commons.configuration2.SubnodeConfiguration;
import org.apache.commons.configuration2.builder.FileBasedConfigurationBuilder;
import org.apache.commons.configuration2.builder.fluent.INIBuilderParameters;
import org.apache.commons.configuration2.builder.fluent.Parameters;
import org.apache.commons.configuration2.convert.DefaultListDelimiterHandler;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.commons.configuration2.io.ClasspathLocationStrategy;
import org.apache.commons.configuration2.io.CombinedLocationStrategy;
import org.apache.commons.configuration2.io.HomeDirectoryLocationStrategy;
import org.apache.commons.configuration2.io.ProvidedURLLocationStrategy;

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
	 */
	public Configuration(String configFilename) {
		configurationMap = initDefaultValues();
		try {
			// TODO is this the right way to set this up?
			/*
			 * By default, configuration files are read (in ascending order of
			 * priority) from a system-wide configuration directory (e.g.
			 * ``/etc/xdg/spalloc``), user configuration file (e.g.
			 * ``$HOME/.config/spalloc``) and finally the current working
			 * directory (in a file named ``.spalloc``).
			 */
			INIBuilderParameters params = new Parameters().ini()
					.setLocationStrategy(new CombinedLocationStrategy(
							asList(new ProvidedURLLocationStrategy(),
									new HomeDirectoryLocationStrategy(),
									new ClasspathLocationStrategy())))
					.setListDelimiterHandler(
							new DefaultListDelimiterHandler(LIST_SEPARATOR))
					.setThrowExceptionOnMissing(true);
			section =
					new FileBasedConfigurationBuilder<>(INIConfiguration.class)
							.configure(params.setFileName(configFilename))
							.getConfiguration().getSection(SECTION_NAME);
		} catch (ConfigurationException e) {
			throw new RuntimeException(
					"failed to load configuration from " + configFilename, e);
		}
		setValuesFromConfig(configurationMap);
	}

	/** @return a clone of the map of configurationMap. */
	public Map<String, Object> getDefaults() {
		return new HashMap<>(configurationMap);
	}

	public String getHost() {
		return (String) configurationMap.get(HOSTNAME_PROPERTY);
	}

	public int getPort() {
		return (Integer) configurationMap.get(PORT_PROPERTY);
	}

	public String getUser() {
		return (String) configurationMap.get(USER_PROPERTY);
	}

	public double getKeepalive() {
		return (Double) configurationMap.get(KEEPALIVE_PROPERTY);
	}

	public double getReconnectDelay() {
		return (Double) configurationMap.get(RECONNECT_DELAY_PROPERTY);
	}

	public double getTimeout() {
		return (Double) configurationMap.get(TIMEOUT_PROPERTY);
	}

	public String getMachine() {
		return (String) configurationMap.get(MACHINE_PROPERTY);
	}

	public String[] getTags() {
		return (String[]) configurationMap.get(TAGS_PROPERTY);
	}

	public double getMinRatio() {
		return (Double) configurationMap.get(MIN_RATIO_PROPERTY);
	}

	public Integer getMaxDeadBoards() {
		return (Integer) configurationMap.get(MAX_DEAD_BOARDS_PROPERTY);
	}

	public Integer getMaxDeadLinks() {
		return (Integer) configurationMap.get(MAX_DEAD_LINKS_PROPERTY);
	}

	public boolean getRequireTorus() {
		return (Boolean) configurationMap.get(REQUIRE_TORUS_PROPERTY);
	}

	/**
	 * Set up the default values in the map, so the configuration file doesn't
	 * need to have everything listed in it.
	 *
	 * @return a map loaded with default configuration values.
	 */
	private static Map<String, Object> initDefaultValues() {
		Map<String, Object> defaults = new HashMap<>();
		defaults.put(PORT_PROPERTY, PORT_DEFAULT);
		defaults.put(KEEPALIVE_PROPERTY, KEEPALIVE_DEFAULT);
		defaults.put(RECONNECT_DELAY_PROPERTY, RECONNECT_DELAY_DEFAULT);
		defaults.put(TIMEOUT_PROPERTY, TIMEOUT_DEFAULT);
		defaults.put(MACHINE_PROPERTY, MACHINE_DEFAULT);
		defaults.put(TAGS_PROPERTY, TAGS_DEFAULT);
		defaults.put(MIN_RATIO_PROPERTY, MIN_RATIO_DEFAULT);
		defaults.put(MAX_DEAD_BOARDS_PROPERTY, MAX_DEAD_BOARDS_DEFAULT);
		defaults.put(MAX_DEAD_LINKS_PROPERTY, MAX_DEAD_LINKS_DEFAULT);
		defaults.put(REQUIRE_TORUS_PROPERTY, REQUIRE_TORUS_DEFAULT);
		return defaults;
	}

	private static final String NULL_MARKER = "None";

	private Double readNoneOrFloat(String prop) {
		String val = section.getString(prop);
		if (NULL_MARKER.equals(val)) {
			return null;
		}
		return Double.parseDouble(val);
	}

	private Integer readNoneOrInt(String prop) {
		String val = section.getString(prop);
		if (NULL_MARKER.equals(val)) {
			return null;
		}
		return Integer.parseInt(val);
	}

	private String readNoneOrString(String prop) {
		String val = section.getString(prop);
		if (NULL_MARKER.equals(val)) {
			return null;
		}
		return val;
	}

	private void setValuesFromConfig(Map<String, Object> defaults) {
		defaults.put(HOSTNAME_PROPERTY,
				section.getString(HOSTNAME_PROPERTY, null));
		if (section.containsKey(USER_PROPERTY)) {
			defaults.put(USER_PROPERTY, section.getString(USER_PROPERTY));
		}
		if (section.containsKey(KEEPALIVE_PROPERTY)) {
			defaults.put(KEEPALIVE_PROPERTY,
					readNoneOrFloat(KEEPALIVE_PROPERTY));
		}
		if (section.containsKey(RECONNECT_DELAY_PROPERTY)) {
			defaults.put(RECONNECT_DELAY_PROPERTY,
					section.getDouble(RECONNECT_DELAY_PROPERTY));
		}
		if (section.containsKey(TIMEOUT_PROPERTY)) {
			defaults.put(TIMEOUT_PROPERTY, readNoneOrFloat(TIMEOUT_PROPERTY));
		}
		if (section.containsKey(MACHINE_PROPERTY)) {
			defaults.put(MACHINE_PROPERTY, readNoneOrString(MACHINE_PROPERTY));
		}
		if (section.containsKey(TAGS_PROPERTY)) {
			if (NULL_MARKER.equals(section.getString(TAGS_PROPERTY))) {
				defaults.put(TAGS_PROPERTY, null);
			} else {
				defaults.put(TAGS_PROPERTY,
						section.getArray(String.class, TAGS_PROPERTY));
			}
		}
		if (section.containsKey(MIN_RATIO_PROPERTY)) {
			defaults.put(MIN_RATIO_PROPERTY,
					readNoneOrFloat(MIN_RATIO_PROPERTY));
		}
		if (section.containsKey(MAX_DEAD_BOARDS_PROPERTY)) {
			defaults.put(MAX_DEAD_BOARDS_PROPERTY,
					readNoneOrInt(MAX_DEAD_BOARDS_PROPERTY));
		}
		if (section.containsKey(MAX_DEAD_LINKS_PROPERTY)) {
			defaults.put(MAX_DEAD_LINKS_PROPERTY,
					readNoneOrInt(MAX_DEAD_LINKS_PROPERTY));
		}
		if (section.containsKey(REQUIRE_TORUS_PROPERTY)) {
			defaults.put(REQUIRE_TORUS_PROPERTY,
					section.getBoolean(REQUIRE_TORUS_PROPERTY));
		}
	}
}
