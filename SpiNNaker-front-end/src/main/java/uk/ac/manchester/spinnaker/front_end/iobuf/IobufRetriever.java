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
import static java.nio.charset.StandardCharsets.ISO_8859_1;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.apache.commons.text.StringEscapeUtils.unescapeJava;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;

import uk.ac.manchester.spinnaker.front_end.BasicExecutor;
import uk.ac.manchester.spinnaker.front_end.BasicExecutor.Tasks;
import uk.ac.manchester.spinnaker.machine.ChipLocation;
import uk.ac.manchester.spinnaker.machine.CoreLocation;
import uk.ac.manchester.spinnaker.machine.CoreSubsets;
import uk.ac.manchester.spinnaker.machine.HasCoreLocation;
import uk.ac.manchester.spinnaker.machine.Machine;
import uk.ac.manchester.spinnaker.messages.model.IOBuffer;
import uk.ac.manchester.spinnaker.transceiver.Transceiver;
import uk.ac.manchester.spinnaker.transceiver.processes.ProcessException;

/**
 * Retrieves and processes IOBUFs.
 *
 * @author Donal Fellows
 */
public class IobufRetriever {
	protected static final Logger log = getLogger(IobufRetriever.class);
	private static final Pattern ERROR_ENTRY =
			Pattern.compile("\\[ERROR\\]\\s+\\((.*)\\):\\s+(.*)");
	private static final Pattern WARNING_ENTRY =
			Pattern.compile("\\[WARNING\\]\\s+\\((.*)\\):\\s+(.*)");
	private static final int ENTRY_FILE = 1;
	private static final int ENTRY_TEXT = 2;

	private Transceiver txrx;
	private Machine machine;
	private BasicExecutor executor;

	/**
	 * Create a IOBUF retriever.
	 *
	 * @param transceiver
	 *            How to talk to the machine.
	 * @param machine
	 *            Description of the machine being talked to.
	 * @param parallelSize
	 *            How many tasks to do at once (at most).
	 */
	public IobufRetriever(Transceiver transceiver, Machine machine,
			int parallelSize) {
		txrx = transceiver;
		this.machine = machine;
		executor = new BasicExecutor(parallelSize);
	}

	/**
	 * Retrieve and translate some IOBUFs.
	 *
	 * @param coreSubsets
	 *            The cores from which the IOBUFs are to be extracted. They must
	 *            be running the executable contained in {@code binaryFile} or
	 *            the buffers will contain the wrong information.
	 * @param binaryFile
	 *            The APLX file being executed. There must be a {@code .dict}
	 *            file as a sibling to it.
	 * @param provenanceDir
	 *            The directory in which provenance data is written.
	 * @param errorEntries
	 *            Accumulate errors here. They are also written to the file on
	 *            disk.
	 * @param warnEntries
	 *            Accumulate warnings here. They are also written to the file on
	 *            disk.
	 * @throws IOException
	 *             If network IO fails or the mapping dictionary is absent.
	 * @throws ProcessException
	 *             If SpiNNaker rejects a message.
	 */
	public void retrieveIobufContents(CoreSubsets coreSubsets, File binaryFile,
			File provenanceDir, List<String> errorEntries,
			List<String> warnEntries) throws IOException, ProcessException {
		Replacer replacer = new Replacer(binaryFile);
		Tasks tasks =
				executor.submitTasks(partitionByBoard(coreSubsets).stream()
						.map(boardSubset -> () -> retrieveIobufContents(
								boardSubset, replacer, provenanceDir,
								errorEntries, warnEntries)));
		try {
			tasks.awaitAndCombineExceptions();
		} catch (IOException | ProcessException | RuntimeException e) {
			throw e;
		} catch (Exception e) {
			throw new RuntimeException("unexpected exception", e);
		}
	}

	private Collection<CoreSubsets> partitionByBoard(CoreSubsets coreSubsets) {
		Map<ChipLocation, CoreSubsets> map = new HashMap<>();
		for (CoreLocation core : coreSubsets) {
			map.computeIfAbsent(
					machine.getChipAt(core).nearestEthernet.asChipLocation(),
					cl -> new CoreSubsets()).addCore(core);
		}
		return map.values();
	}

	private void retrieveIobufContents(CoreSubsets cores, Replacer replacer,
			File provenanceDir, List<String> errorEntries,
			List<String> warnEntries) throws IOException, ProcessException {
		// extract iobuf
		Iterable<IOBuffer> ioBuffers = txrx.getIobuf(cores);

		// write iobuf to file and check for errors for provenance
		for (IOBuffer iobuf : ioBuffers) {
			File file = getProvenanceFile(provenanceDir, iobuf);
			try (PrintWriter w =
					new PrintWriter(new BufferedWriter(new OutputStreamWriter(
							new FileOutputStream(file, true), UTF_8)))) {
				for (String line : getLines(iobuf)) {
					w.println(replacer.replace(line));
				}
			}
			checkIobufForError(iobuf, errorEntries, warnEntries);
		}
	}

	private File getProvenanceFile(File provenanceDir, IOBuffer iobuf) {
		return new File(provenanceDir,
				String.format("iobuf_for_chip_%d_%d_processor_id_%d.txt",
						iobuf.getX(), iobuf.getY(), iobuf.getP()));
	}

	private static String[] getLines(IOBuffer iobuf) {
		return iobuf.getContentsString(ISO_8859_1).split("\n");
	}

	private void checkIobufForError(IOBuffer iobuf, List<String> errorEntries,
			List<String> warnEntries) {
		for (String line : getLines(iobuf)) {
			addValueIfMatch(ERROR_ENTRY, line, errorEntries, iobuf);
			addValueIfMatch(WARNING_ENTRY, line, warnEntries, iobuf);
		}
	}

	private static void addValueIfMatch(Pattern regex, String line,
			List<String> entries, HasCoreLocation core) {
		Matcher match = regex.matcher(line);
		if (match.matches()) {
			synchronized (entries) {
				entries.add(String.format("%d, %d, %d: %s (%s)", core.getX(),
						core.getY(), core.getP(), match.group(ENTRY_TEXT),
						match.group(ENTRY_FILE)));
			}
		}
	}

	private static class Replacer {
		private static final Pattern FORMAT_SEQUENCE =
				Pattern.compile("%\\d*(?:\\.\\d+)?[cdfiksuxR]");
		private static final String RS_TOKEN = "\u001e";
		private static final int NUM_PARTS = 3;

		private Map<String, Replacement> messages = new HashMap<>();

		Replacer(File dictPointer) throws FileNotFoundException, IOException {
			String base = dictPointer.getAbsolutePath()
					.replaceFirst("[.][^.\\/]+$", "");
			File dictPath = new File(base + ".dict");
			if (dictPath.isFile()) {
				try (BufferedReader f =
						new BufferedReader(new FileReader(dictPath))) {
					String line;
					while ((line = f.readLine()) != null) {
						String[] parts = line.trim().split(",", NUM_PARTS);
						if (parts.length != NUM_PARTS) {
							continue;
						}
						try {
							Replacement r = new Replacement(parts);
							messages.put(r.key, r);
						} catch (NumberFormatException ignore) {
						}
					}
				}
			} else {
				log.error("Unable to find a dictionary file at {}", dictPath);
			}
		}

		public String replace(String shortLine) {
			String[] parts = shortLine.split(RS_TOKEN);
			if (!messages.containsKey(parts[0])) {
				return shortLine;
			}
			Replacement r = messages.get(parts[0]);
			StringBuilder replaced = r.getReplacementBuffer();

			if (parts.length > 1) {
				List<int[]> matches = r.getMatches();
				if (matches.size() != parts.length - 1) {
					// try removing any blanks due to double spacing
					matches.removeIf(x -> x[0] == x[1]);
				}
				if (matches.size() != parts.length - 1) {
					// wrong number of elements so not short after all
					return shortLine;
				}
				for (int i = 0; i < matches.size(); i++) {
					int[] match = matches.get(i);
					replaced.replace(match[0], match[1], parts[i + 1]);
				}
			}
			return r.preface + replaced;
		}

		private static final class Replacement {
			final String key;
			final String preface;
			final String original;
			final List<int[]> matches;

			Replacement(String[] parts) throws NumberFormatException {
				key = parts[0];
				preface = parts[1];
				original = parts[2];
				parseUnsignedInt(key); // throws if fails
				Matcher m = FORMAT_SEQUENCE.matcher(original);
				matches = new ArrayList<>();
				while (m.find()) {
					matches.add(new int[] {
						m.start(), m.end()
					});
				}
			}

			StringBuilder getReplacementBuffer() {
				return new StringBuilder(unescapeJava(original));
			}

			List<int[]> getMatches() {
				return new ArrayList<>(matches);
			}
		}
	}
}
