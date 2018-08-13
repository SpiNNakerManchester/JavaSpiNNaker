package uk.ac.manchester.spinnaker.utils;

import static java.lang.String.format;
import static java.util.regex.Pattern.compile;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
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
	private static final String SECT_TMPL = "\\[(?<name>[^]]+)\\]";
	private static final String OPT_TMPL =
			"(?<key>.*?)\\s*[%s]\\s*(?<value>.*)$";
	private static final String COMMENT_TMPL = "^(?<keep>.*?)[%s].*$";

	private final Pattern sectRE, optRE, commentRE;
	private Map<String, Map<String, String>> map = new HashMap<>();

	/**
	 * Create a basic configuration parser.
	 *
	 * @param delimiters
	 *            the option/value delimiter characters.
	 * @param comments
	 *            the comment delimiter characters.
	 */
	protected RawConfigParser(String delimiters, String comments) {
		this.sectRE = compile(SECT_TMPL);
		this.optRE = compile(format(OPT_TMPL, delimiters));
		this.commentRE = compile(format(COMMENT_TMPL, comments));
	}

	/**
	 * Create a configuration with no values defined in it.
	 */
	public RawConfigParser() {
		this("=:", "#;");
	}

	/**
	 * Create a configuration from the given configuration file. This is
	 * designed to be used with {@link Class#getResource(String)}.
	 *
	 * @param resource
	 *            The handle to the configuration file.
	 */
	public RawConfigParser(URL resource) {
		this();
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
	 * Create a configuration from the given configuration file.
	 *
	 * @param file
	 *            The handle to the configuration file.
	 */
	public RawConfigParser(File file) {
		this();
		try {
			if (file != null) {
				read(file);
			}
		} catch (IOException e) {
			throw new RuntimeException("failed to read config from " + file, e);
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
	public void read(URL resource) throws IOException {
		try (ReaderLineIterable lines =
				new ReaderLineIterable(resource.openStream())) {
			read(lines);
		}
	}

	/**
	 * Read a configuration file.
	 *
	 * @param file
	 *            Where the file is.
	 * @throws IOException
	 *             if the reading fails.
	 */
	public void read(File file) throws IOException {
		try (ReaderLineIterable lines =
				new ReaderLineIterable(new FileInputStream(file))) {
			read(lines);
		}
	}

	private void read(ReaderLineIterable lines) {
		int ln = 0;
		String sect = null;
		for (String line : lines) {
			ln++;
			line = clean(line);
			if (line.isEmpty()) {
				continue;
			}
			Matcher m = sectRE.matcher(line);
			if (m.matches()) {
				sect = normaliseSectionName(m.group("name"));
				if (!map.containsKey(sect)) {
					map.put(sect, new HashMap<>());
				}
				continue;
			} else if (sect == null) {
				throw new IllegalArgumentException(
						"content before first section starts, at line " + ln);
			}
			m = optRE.matcher(line);
			if (m.matches()) {
				String key = normaliseOptionName(m.group("key"));
				String value = m.group("value");
				map.get(sect).put(key, value);
				continue;
			}
			throw new IllegalArgumentException(
					"unknown line format, at line " + ln);
		}
	}

	/**
	 * Remove any comment and dead space from a line.
	 *
	 * @param line
	 *            The line to clean up.
	 * @return The cleaned line.
	 */
	private String clean(String line) {
		Matcher m = commentRE.matcher(line);
		if (m.matches()) {
			line = m.group("keep");
		}
		return line.trim();
	}

	/**
	 * How to decide if a value is to be treated as <tt>null</tt>.
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
