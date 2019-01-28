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

import static java.nio.charset.StandardCharsets.ISO_8859_1;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

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
	 * @param coresForBinaries
	 *            Mapping from the full paths to the APLX files executing, to
	 *            the cores on which those executables are running and which are
	 *            to have their IOBUFs extracted. There must be a {@code .dict}
	 *            file as a sibling to each APLX file.
	 * @param provenanceDir
	 *            The directory in which provenance data is written.
	 * @return The errors and warnings that have been detected. The order of the
	 *         messages is not determined.
	 * @throws IOException
	 *             If network IO fails or the mapping dictionary is absent.
	 * @throws ProcessException
	 *             If SpiNNaker rejects a message.
	 */
	public NotableMessages retrieveIobufContents(
			Map<String, CoreSubsets> coresForBinaries, String provenanceDir)
			throws IOException, ProcessException {
		File provDir = new File(provenanceDir);
		if (!provDir.isDirectory() || !provDir.canWrite()) {
			throw new IOException(
					"provenance location must be writable directory");
		}
		List<String> errorEntries = new ArrayList<>();
		List<String> warnEntries = new ArrayList<>();
		try {
			Tasks tasks = executor.submitTasks(
					coresForBinaries.keySet().stream().flatMap(binary -> {
						Replacer r = new Replacer(binary);
						return partitionByBoard(coresForBinaries.get(binary))
								.map(cores -> () -> retrieveIobufContents(cores,
										r, provDir, errorEntries, warnEntries));
					}));
			try {
				tasks.awaitAndCombineExceptions();
			} catch (IOException | ProcessException | RuntimeException e) {
				throw e;
			} catch (Exception e) {
				throw new RuntimeException("unexpected exception", e);
			}
		} catch (Replacer.WrappedException e) {
			e.rethrow();
		}
		return new NotableMessages(errorEntries, warnEntries);
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
	 * @return The errors and warnings that have been detected. The order of the
	 *         messages is not determined.
	 * @throws IOException
	 *             If network IO fails or the mapping dictionary is absent.
	 * @throws ProcessException
	 *             If SpiNNaker rejects a message.
	 */
	public NotableMessages retrieveIobufContents(CoreSubsets coreSubsets,
			File binaryFile, File provenanceDir)
			throws IOException, ProcessException {
		if (!provenanceDir.isDirectory() || !provenanceDir.canWrite()) {
			throw new IOException(
					"provenance location must be writable directory");
		}
		List<String> errorEntries = new ArrayList<>();
		List<String> warnEntries = new ArrayList<>();
		try {
			Replacer replacer = new Replacer(binaryFile);
			Tasks tasks = executor.submitTasks(partitionByBoard(coreSubsets)
					.map(boardSubset -> () -> retrieveIobufContents(boardSubset,
							replacer, provenanceDir, errorEntries,
							warnEntries)));
			try {
				tasks.awaitAndCombineExceptions();
			} catch (IOException | ProcessException | RuntimeException e) {
				throw e;
			} catch (Exception e) {
				throw new RuntimeException("unexpected exception", e);
			}
		} catch (Replacer.WrappedException e) {
			e.rethrow();
		}
		return new NotableMessages(errorEntries, warnEntries);
	}

	private Stream<CoreSubsets> partitionByBoard(CoreSubsets coreSubsets) {
		Map<ChipLocation, CoreSubsets> map = new HashMap<>();
		for (CoreLocation core : coreSubsets) {
			map.computeIfAbsent(
					machine.getChipAt(core).nearestEthernet.asChipLocation(),
					cl -> new CoreSubsets()).addCore(core);
		}
		return map.values().stream();
	}

	private void retrieveIobufContents(CoreSubsets cores, Replacer replacer,
			File provenanceDir, List<String> errorEntries,
			List<String> warnEntries) throws IOException, ProcessException {
		// extract iobuf
		Iterable<IOBuffer> ioBuffers = txrx.getIobuf(cores);

		// write iobuf to file and check for errors for provenance
		for (IOBuffer iobuf : ioBuffers) {
			File file = getProvenanceFile(provenanceDir, iobuf);
			try (BufferedWriter w = openFileForAppending(file)) {
				for (String originalLine : iobuf.getContentsString(ISO_8859_1)
						.split("\n")) {
					String line = replacer.replace(originalLine);
					w.write(line);
					w.newLine();
					addValueIfMatch(ERROR_ENTRY, line, errorEntries, iobuf);
					addValueIfMatch(WARNING_ENTRY, line, warnEntries, iobuf);
				}
			}
		}
	}

	private static File getProvenanceFile(File provenanceDir, IOBuffer iobuf) {
		return new File(provenanceDir,
				String.format("iobuf_for_chip_%d_%d_processor_id_%d.txt",
						iobuf.getX(), iobuf.getY(), iobuf.getP()));
	}

	private static BufferedWriter openFileForAppending(File file)
			throws IOException {
		return new BufferedWriter(new OutputStreamWriter(
				new FileOutputStream(file, true), UTF_8));
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
}
