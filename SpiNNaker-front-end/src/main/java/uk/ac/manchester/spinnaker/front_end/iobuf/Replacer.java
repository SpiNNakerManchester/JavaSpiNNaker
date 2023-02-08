/*
 * Copyright (c) 2019 The University of Manchester
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
package uk.ac.manchester.spinnaker.front_end.iobuf;

import static java.lang.Integer.parseUnsignedInt;
import static org.apache.commons.io.FilenameUtils.removeExtension;
import static org.apache.commons.text.StringEscapeUtils.unescapeJava;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.MatchResult;
import java.util.regex.Pattern;

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
		var dictPath = Paths
				.get(removeExtension(aplxFile.getAbsolutePath()) + ".dict");
		if (dictPath.toFile().isFile()) {
			try (var lines = Files.lines(dictPath)) {
				lines.forEachOrdered(this::parseLine);
			} catch (UncheckedIOException e) {
				throw new WrappedException(e.getCause());
			} catch (IOException e) {
				throw new WrappedException(e);
			}
		} else {
			log.error("Unable to find a dictionary file at {}", dictPath);
		}
	}

	private void parseLine(String line) {
		var parts = line.split(",", NUM_PARTS);
		if (parts.length == NUM_PARTS) {
			try {
				var tmpl = new Template(parts);
				messages.put(tmpl.key, tmpl);
			} catch (NumberFormatException e) {
				log.trace("bad template ID: {}", parts[0], e);
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
		var parts = shortLine.split(RS_TOKEN);
		if (!messages.containsKey(parts[0])) {
			return shortLine;
		}
		var tmpl = messages.get(parts[0]);
		var replaced = tmpl.getReplacementBuffer();

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
			var original = parts[2];
			unescaped = unescapeJava(original);
			parseUnsignedInt(key); // throws if fails

			// Get the regions to replace
			var m = FORMAT_SEQUENCE.matcher(original);
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

	private record Replacement(String match, int index) {
		private Replacement(MatchResult m, int index) {
			this(m.group(), index);
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

		/** The wrapped exception. */
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
