/*
 * Copyright (c) 2018-2022 The University of Manchester
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
package uk.ac.manchester.spinnaker.utils;

import static java.lang.Boolean.parseBoolean;
import static java.lang.Integer.parseInt;
import static java.lang.String.format;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNull;
import static java.util.regex.Pattern.compile;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import com.google.errorprone.annotations.ForOverride;

/**
 * A cut-down limited version of the parser used in Python.
 *
 * @author Donal Fellows
 */
public class RawConfigParser {
	// Regular expressions for parsing section headers and options
	private static final String SECT_TMPL = "\\[(?<name>[^]]+)\\]";

	@SuppressWarnings("InlineFormatString")
	private static final String OPT_TMPL =
			"(?<key>.*?)\\s*[%s]\\s*(?<value>.*)$";

	@SuppressWarnings("InlineFormatString")
	private static final String COMMENT_TMPL = "^(?<keep>.*?)[%s].*$";

	private final Pattern sectRE;

	private final Pattern optRE;

	private final Pattern commentRE;

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
	 * @throws RuntimeException
	 *             If the file can't be read. (NB: <i>not</i>
	 *             {@link IOException}.)
	 */
	public RawConfigParser(URL resource) {
		this();
		try {
			if (nonNull(resource)) {
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
	 * @throws RuntimeException
	 *             If the file can't be read. (NB: <i>not</i>
	 *             {@link IOException}.)
	 */
	public RawConfigParser(File file) {
		this();
		try {
			if (nonNull(file)) {
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
	@ForOverride
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
	@ForOverride
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
		try (var lines = new ReaderLineIterable(resource.openStream())) {
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
		try (var lines = new ReaderLineIterable(new FileInputStream(file))) {
			read(lines);
		}
	}

	private void read(ReaderLineIterable lines) {
		int ln = 0;
		String sect = null;
		for (var line : lines) {
			ln++;
			line = clean(line);
			if (line.isBlank()) {
				continue;
			}
			var m = sectRE.matcher(line);
			if (m.matches()) {
				sect = normaliseSectionName(m.group("name"));
				map.computeIfAbsent(sect, __ -> new HashMap<>());
				continue;
			} else if (isNull(sect)) {
				throw new IllegalArgumentException(
						"content before first section starts, at line " + ln);
			}
			m = optRE.matcher(line);
			if (!m.matches()) {
				throw new IllegalArgumentException(
						"unknown line format, at line " + ln);
			}
			var key = normaliseOptionName(m.group("key"));
			var value = m.group("value");
			map.get(requireNonNull(sect)).put(key, value);
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
		var m = commentRE.matcher(line);
		if (m.matches()) {
			line = m.group("keep");
		}
		return line.strip();
	}

	/**
	 * How to decide if a value is to be treated as {@code null}.
	 *
	 * @param value
	 *            The value to examine
	 * @return True iff the value is a {@code null}-equivalent.
	 */
	protected boolean isNone(String value) {
		return isNull(value) || "None".equalsIgnoreCase(value);
	}

	/**
	 * Get a value from the config.
	 *
	 * @param section
	 *            The section to look in.
	 * @param option
	 *            The option to look at.
	 * @return The option value, or {@code null} if it is absent.
	 */
	public Integer getInt(String section, String option) {
		var value = get(section, option);
		if (isNone(value)) {
			return null;
		}
		return parseInt(value);
	}

	/**
	 * Get a value from the config.
	 *
	 * @param section
	 *            The section to look in.
	 * @param option
	 *            The option to look at.
	 * @return The option value, or {@code null} if it is absent.
	 */
	public Boolean getBoolean(String section, String option) {
		var value = get(section, option);
		if (isNone(value)) {
			return null;
		}
		return parseBoolean(value);
	}

	/**
	 * Get a value from the config.
	 *
	 * @param section
	 *            The section to look in.
	 * @param option
	 *            The option to look at.
	 * @return The option value, or {@code null} if it is absent.
	 */
	public String get(String section, String option) {
		var sect = map.get(normaliseSectionName(section));
		if (nonNull(sect)) {
			return sect.get(normaliseOptionName(option));
		}
		// TODO should this fail with an exception?
		return null;
	}
}
