package uk.ac.manchester.spinnaker.utils;

import static java.lang.String.format;
import static java.util.regex.Pattern.compile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A cut-down limited version of the parser used in Python.
 *
 * @author Donal Fellows
 */
public class RawConfigParser {
	// Regular expressions for parsing section headers and options
	private static final String SECT_TMPL = "\\[([^]]+)\\]";
	private static final String OPT_TMPL = "(.*?)\\s*[%s]\\s*(.*)$";
	private static final String COMMENT_TMPL = "^(.*?)\\s*[#;].*$";

	private final Pattern sectRE, optRE, commentRE;
	Map<String, Map<String, String>> map = new HashMap<>();

	/**
	 * Create a basic configuration parser.
	 *
	 * @param delimiters
	 *            the option/value delimiter characters.
	 */
	protected RawConfigParser(String delimiters) {
		this.sectRE = compile(SECT_TMPL);
		this.optRE = compile(format(OPT_TMPL, delimiters));
		this.commentRE = compile(COMMENT_TMPL);
	}

	/**
	 * Create a configuration from the given configuration file. This is
	 * designed to be used with {@link Class#getResource(String)}.
	 *
	 * @param resource
	 *            The handle to the configuration file.
	 */
	public RawConfigParser(URL resource) {
		this("=:");
		try {
			if (resource != null) {
				read(resource);
			}
		} catch (IOException e) {
			throw new RuntimeException("failed to read config from " + resource,
					e);
		}
	}

	/**
	 * How to convert a section name into canonical form.
	 *
	 * @param name
	 *            The raw section name.
	 * @return The canonicalised section name.
	 */
	protected String normaliseSectionName(String name) {
		return name;
	}

	/**
	 * How to convert an option name into canonical form.
	 *
	 * @param name
	 *            The raw option name.
	 * @return The canonicalised option name.
	 */
	protected String normaliseOptionName(String name) {
		return name;
	}

	/**
	 * Read a configuration file.
	 *
	 * @param resource
	 *            Where the file is.
	 * @throws IOException
	 *             if the reading fails.
	 */
	@SuppressWarnings("checkstyle:InnerAssignment")
	public void read(URL resource) throws IOException {
		try (InputStream s = resource.openStream();
				BufferedReader r = new BufferedReader(
						new InputStreamReader(s))) {
			int ln = 0;
			String sect = null;
			for (String line; (line = r.readLine()) != null;) {
				ln++;
				Matcher m = commentRE.matcher(line);
				if (m.matches()) {
					line = m.group(1);
				}
				line = line.trim();
				if (line.isEmpty()) {
					continue;
				}
				m = sectRE.matcher(line);
				if (m.matches()) {
					sect = normaliseSectionName(m.group(1));
					if (!map.containsKey(sect)) {
						map.put(sect, new HashMap<>());
					}
					continue;
				} else if (sect == null) {
					throw new IllegalArgumentException(
							"content before first section starts, at line "
									+ ln);
				}
				m = optRE.matcher(line);
				if (m.matches()) {
					String key = normaliseOptionName(m.group(1));
					String value = m.group(2);
					map.get(sect).put(key, value);
					continue;
				}
				throw new IllegalArgumentException(
						"unknown line format, at line " + ln);
			}
		}
	}

	/**
	 * How to decide if a value is to be treated as <tt>null</tt>
	 *
	 * @param value
	 *            The value to examine
	 * @return True iff the value is a <tt>null</tt>-equivalent.
	 */
	protected boolean isNone(String value) {
		return value == null || "None".equalsIgnoreCase(value);
	}

	/**
	 * Get a value from the config.
	 *
	 * @param section
	 *            The section to look in.
	 * @param option
	 *            The option to look at.
	 * @return The option value, or <tt>null</tt> if it is absent.
	 */
	public Integer getint(String section, String option) {
		String value = get(section, option);
		if (isNone(value)) {
			return null;
		}
		return Integer.parseInt(value);
	}

	/**
	 * Get a value from the config.
	 *
	 * @param section
	 *            The section to look in.
	 * @param option
	 *            The option to look at.
	 * @return The option value, or <tt>null</tt> if it is absent.
	 */
	public Boolean getboolean(String section, String option) {
		String value = get(section, option);
		if (isNone(value)) {
			return null;
		}
		return Boolean.parseBoolean(value);
	}

	/**
	 * Get a value from the config.
	 *
	 * @param section
	 *            The section to look in.
	 * @param option
	 *            The option to look at.
	 * @return The option value, or <tt>null</tt> if it is absent.
	 */
	public String get(String section, String option) {
		Map<String, String> sect = map.get(normaliseSectionName(section));
		if (sect != null) {
			String value = sect.get(normaliseOptionName(option));
			if (value != null) {
				return value;
			}
		}
		// TODO should this fail with an exception?
		return null;
	}
}
