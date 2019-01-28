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

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Replacement management engine for compacted IOBUFs.
 *
 * @author Donal Fellows
 */
class Replacer {
	private static final Pattern FORMAT_SEQUENCE =
			Pattern.compile("%\\d*(?:\\.\\d+)?[cdfiksuxR]");
	/** ASCII RS (record separator) token. */
	private static final String RS_TOKEN = "\u001e";
	private static final int NUM_PARTS = 3;

	private Map<String, Replacement> messages = new HashMap<>();

	Replacer(String aplxFile) throws Replacer.WrappedException {
		this(new File(aplxFile));
	}

	Replacer(File aplxFile) throws Replacer.WrappedException {
		Path dictPath = Paths
				.get(removeExtension(aplxFile.getAbsolutePath()) + ".dict");
		if (dictPath.toFile().isFile()) {
			try (Stream<String> lines = Files.lines(dictPath)) {
				lines.forEachOrdered(this::parseLine);
			} catch (IOException e) {
				throw new WrappedException(e);
			}
		} else {
			IobufRetriever.log.error("Unable to find a dictionary file at {}",
					dictPath);
		}
	}

	private void parseLine(String line) {
		String[] parts = line.trim().split(",", NUM_PARTS);
		if (parts.length == NUM_PARTS) {
			try {
				Replacement r = new Replacement(parts);
				messages.put(r.key, r);
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
		Replacement r = messages.get(parts[0]);
		StringBuilder replaced = r.getReplacementBuffer();

		if (parts.length > 1) {
			List<Pair> matches = r.getMatches();
			if (matches.size() != parts.length - 1) {
				// try removing any blanks due to double spacing
				matches.removeIf(x -> x.start == x.end);
			}
			if (matches.size() != parts.length - 1) {
				// wrong number of elements so not short after all
				return shortLine;
			}
			for (int i = parts.length - 1; i >= 0; i--) {
				Pair match = matches.get(i);
				replaced.replace(match.start, match.end, parts[i + 1]);
			}
		}
		return r.preface + replaced;
	}

	private static final class Replacement {
		final String key;
		final String preface;
		final String original;
		final List<Pair> matches;

		Replacement(String[] parts) throws NumberFormatException {
			key = parts[0];
			preface = parts[1];
			original = parts[2];
			parseUnsignedInt(key); // throws if fails
			Matcher m = FORMAT_SEQUENCE.matcher(original);
			matches = new ArrayList<>();
			while (m.find()) {
				matches.add(new Pair(m));
			}
		}

		StringBuilder getReplacementBuffer() {
			return new StringBuilder(unescapeJava(original));
		}

		List<Pair> getMatches() {
			return new ArrayList<>(matches);
		}
	}

	private static final class Pair {
		final int start;
		final int end;

		private Pair(Matcher m) {
			start = m.start();
			end = m.end();
		}
	}

	/**
	 * An exception that wraps an {@link IOException} so it can pass out of a
	 * spliterator.
	 *
	 * @author Donal Fellows
	 */
	static final class WrappedException extends RuntimeException {
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
