/*
 * Copyright (c) 2019 The University of Manchester
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
package uk.ac.manchester.spinnaker.front_end.iobuf;

import static java.lang.Integer.parseUnsignedInt;
import static org.apache.commons.io.FilenameUtils.removeExtension;
import static org.apache.commons.text.StringEscapeUtils.unescapeJava;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.slf4j.Logger;

/**
 * Replacement management engine for compacted IOBUFs.
 *
 * @author Donal Fellows
 */
class Replacer {
	private static final Logger log = getLogger(Replacer.class);

	private static final Pattern FORMAT_SEQUENCE =
			Pattern.compile("%\\d*(?:\\.\\d+)?([cdfiksuxR])");

	/** ASCII RS (record separator) token. */
	private static final String RS_TOKEN = "\u001e";

	private static final int NUM_PARTS = 3;

	private Map<String, Template> messages = new HashMap<>();

	/** Where the APLX file is that this replacer is working on. */
	final File origin;

	Replacer(String aplxFile) throws Replacer.WrappedException {
		this(new File(aplxFile));
	}

	Replacer(File aplxFile) throws Replacer.WrappedException {
		origin = aplxFile.getAbsoluteFile();
		Path dictPath = Paths
				.get(removeExtension(aplxFile.getAbsolutePath()) + ".dict");
		if (dictPath.toFile().isFile()) {
			try (Stream<String> lines = Files.lines(dictPath)) {
				lines.forEachOrdered(this::parseLine);
			} catch (IOException e) {
				throw new WrappedException(e);
			}
		} else {
			log.error("Unable to find a dictionary file at {}", dictPath);
		}
	}

	private void parseLine(String line) {
		String[] parts = line.trim().split(",", NUM_PARTS);
		if (parts.length == NUM_PARTS) {
			try {
				Template tmpl = new Template(parts);
				messages.put(tmpl.key, tmpl);
			} catch (NumberFormatException ignore) {
			}
		}
	}

	/**
	 * Given the short string read from SpiNNaker, convert it to what it should
	 * have been if the dictionary-based contraction system had not been
	 * applied.
	 *
	 * @param shortLine
	 *            The line of data from SpiNNaker.
	 * @return The fully expanded line.
	 */
	public String replace(String shortLine) {
		String[] parts = shortLine.split(RS_TOKEN);
		if (!messages.containsKey(parts[0])) {
			return shortLine;
		}
		Template tmpl = messages.get(parts[0]);
		StringBuilder replaced = tmpl.getReplacementBuffer();

		if (parts.length > 1) {
			if (tmpl.matches.size() != parts.length - 1) {
				// wrong number of elements so not short after all
				return shortLine;
			}
			tmpl.applyReplacements(replaced, parts);
		}
		return tmpl.prefix + replaced;
	}

	private static final class Template {
		private final String key;

		private final String prefix;

		private final String unescaped;

		private final List<Replacement> matches;

		private Template(String[] parts) throws NumberFormatException {
			key = parts[0];
			prefix = parts[1];
			String original = parts[2];
			unescaped = unescapeJava(original);
			parseUnsignedInt(key); // throws if fails

			// Get the regions to replace
			Matcher m = FORMAT_SEQUENCE.matcher(original);
			matches = new ArrayList<>();
			int index = 0;
			while (m.find()) {
				matches.add(new Replacement(m.toMatchResult(), ++index));
			}
		}

		private StringBuilder getReplacementBuffer() {
			return new StringBuilder(unescaped);
		}

		private void applyReplacements(StringBuilder buffer, String[] parts) {
			matches.forEach(match -> match.replace(buffer, parts));
		}
	}

	private static final class Replacement {
		private final String match;

		private final int index;

		private Replacement(MatchResult m, int index) {
			match = m.group();
			this.index = index;
		}

		void replace(StringBuilder buffer, String[] parts) {
			int from = buffer.indexOf(match);
			buffer.replace(from, from + match.length(), parts[index]);
		}
	}

	/**
	 * An exception that wraps an {@link IOException} so it can pass out of a
	 * spliterator.
	 *
	 * @author Donal Fellows
	 */
	public static final class WrappedException extends RuntimeException {
		private static final long serialVersionUID = 1L;

		private final IOException e;

		private WrappedException(IOException e) {
			this.e = e;
		}

		/**
		 * Throws the exception contained in this exception.
		 *
		 * @throws IOException
		 *             Always
		 */
		void rethrow() throws IOException {
			throw new IOException(e.getMessage(), e);
		}
	}
}
