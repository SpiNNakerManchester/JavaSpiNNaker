/*
 * Copyright (c) 2019 The University of Manchester
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
package uk.ac.manchester.spinnaker.front_end.iobuf;

import static java.lang.String.format;
import static java.nio.charset.StandardCharsets.ISO_8859_1;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.apache.commons.io.IOUtils.buffer;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.slf4j.Logger;

import com.google.errorprone.annotations.MustBeClosed;

import uk.ac.manchester.spinnaker.front_end.BasicExecutor;
import uk.ac.manchester.spinnaker.front_end.BoardLocalSupport;
import uk.ac.manchester.spinnaker.machine.CoreSubsets;
import uk.ac.manchester.spinnaker.machine.HasCoreLocation;
import uk.ac.manchester.spinnaker.machine.Machine;
import uk.ac.manchester.spinnaker.messages.model.IOBuffer;
import uk.ac.manchester.spinnaker.storage.ProxyAwareStorage;
import uk.ac.manchester.spinnaker.storage.StorageException;
import uk.ac.manchester.spinnaker.transceiver.ProcessException;
import uk.ac.manchester.spinnaker.transceiver.SpinnmanException;
import uk.ac.manchester.spinnaker.transceiver.TransceiverInterface;
import uk.ac.manchester.spinnaker.utils.DefaultMap;

/**
 * Retrieves and processes IOBUFs.
 *
 * @author Donal Fellows
 */
public class IobufRetriever extends BoardLocalSupport implements AutoCloseable {
	private static final Logger log = getLogger(IobufRetriever.class);

	private static final Pattern ERROR_ENTRY =
			Pattern.compile("\\[ERROR\\]\\s+\\((.*)\\):\\s+(.*)");

	private static final Pattern WARNING_ENTRY =
			Pattern.compile("\\[WARNING\\]\\s+\\((.*)\\):\\s+(.*)");

	private static final int ENTRY_FILE = 1;

	private static final int ENTRY_TEXT = 2;

	private final BasicExecutor executor;

	private final TransceiverInterface txrx;

	/**
	 * Create a IOBUF retriever.
	 *
	 * @param db
	 *            Where to get information about the proxy from.
	 * @param machine
	 *            Description of the machine being talked to.
	 * @param parallelSize
	 *            How many tasks to do at once (at most).
	 * @throws IOException
	 *            If we can't discover the machine details due to I/O problems
	 * @throws InterruptedException
	 *            If communications are interrupted.
	 * @throws SpinnmanException
	 *            If the there is an error creating a transceiver.
	 * @throws StorageException
	 *           If the database access fails.
	 */
	@MustBeClosed
	@SuppressWarnings("MustBeClosed")
	public IobufRetriever(ProxyAwareStorage db, Machine machine,
			int parallelSize) throws IOException, StorageException,
			SpinnmanException, InterruptedException {
		super(db, machine);
		executor = new BasicExecutor(parallelSize);
		txrx = getTransceiver();
	}

	@Override
	public void close() throws InterruptedException {
		executor.close();
	}

	/**
	 * Retrieve and translate some IOBUFs.
	 *
	 * @param request
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
	 * @throws InterruptedException
	 *             If communications are interrupted.
	 * @throws RuntimeException
	 *             If an unexpected exception happens.
	 */
	// see https://github.com/SpiNNakerManchester/JavaSpiNNaker/issues/1218
	public NotableMessages retrieveIobufContents(IobufRequest request,
			File provenanceDir)
			throws IOException, ProcessException, InterruptedException {
		validateProvenanceDirectory(provenanceDir);
		var errorEntries = new ArrayList<String>();
		var warnEntries = new ArrayList<String>();
		var mapping = request.getRequestDetails();
		try {
			executor.submitTasks(mapping.entrySet().stream()
					.map(this::partitionByBoard).flatMap(entry -> {
						var r = new Replacer(entry.getKey());
						return entry.getValue().stream().map(cs -> {
							return () -> retrieveIobufContents(cs, r,
									provenanceDir, errorEntries, warnEntries);
						});
					})).awaitAndCombineExceptions();
		} catch (Replacer.WrappedException e) {
			e.rethrow();
		} catch (IOException | ProcessException | InterruptedException
				| RuntimeException e) {
			throw e;
		} catch (Exception e) {
			throw new RuntimeException("unexpected exception", e);
		}
		return new NotableMessages(errorEntries, warnEntries);
	}

	/**
	 * Retrieve and translate some IOBUFs.
	 *
	 * @param cores
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
	 * @throws InterruptedException
	 *             If communications are interrupted.
	 * @throws RuntimeException
	 *             If an unexpected exception happens.
	 */
	// see https://github.com/SpiNNakerManchester/JavaSpiNNaker/issues/1218
	public NotableMessages retrieveIobufContents(CoreSubsets cores,
			File binaryFile, File provenanceDir)
			throws IOException, ProcessException, InterruptedException {
		validateProvenanceDirectory(provenanceDir);
		var errorEntries = new ArrayList<String>();
		var warnEntries = new ArrayList<String>();
		try {
			var replacer = new Replacer(binaryFile);
			executor.submitTasks(partitionByBoard(cores), boards -> {
				return () -> retrieveIobufContents(boards, replacer,
						provenanceDir, errorEntries, warnEntries);
			}).awaitAndCombineExceptions();
		} catch (Replacer.WrappedException e) {
			e.rethrow();
		} catch (IOException | ProcessException | InterruptedException
				| RuntimeException e) {
			throw e;
		} catch (Exception e) {
			throw new RuntimeException("unexpected exception", e);
		}
		return new NotableMessages(errorEntries, warnEntries);
	}

	private static void validateProvenanceDirectory(File provDir)
			throws IOException {
		if (!provDir.isDirectory() || !provDir.canWrite()) {
			throw new IOException(
					"provenance location must be writable directory");
		}
	}

	private <K> Map.Entry<K, Collection<CoreSubsets>> partitionByBoard(
			Map.Entry<K, CoreSubsets> entry) {
		var map = new DefaultMap<Object, CoreSubsets>(CoreSubsets::new);
		for (var core : entry.getValue()) {
			map.get(machine.getChipAt(core).nearestEthernet).addCore(core);
		}
		return Map.entry(entry.getKey(), map.values());
	}

	private Collection<CoreSubsets> partitionByBoard(CoreSubsets coreSubsets) {
		var map = new DefaultMap<Object, CoreSubsets>(CoreSubsets::new);
		for (var core : coreSubsets) {
			map.get(machine.getChipAt(core).nearestEthernet).addCore(core);
		}
		return map.values();
	}

	private void retrieveIobufContents(CoreSubsets cores, Replacer replacer,
			File provenanceDir, List<String> errorEntries,
			List<String> warnEntries)
			throws IOException, ProcessException, InterruptedException {
		try (var bl = new BoardLocal(cores.first().orElseThrow())) {
			// extract iobuf, write to file and check for errors for provenance
			for (var iobuf : txrx.getIobuf(cores)) {
				var file = getProvenanceFile(provenanceDir, iobuf);
				try (var w = buffer(new FileWriter(file, UTF_8, true))) {
					log.info("storing iobuf from {} (running {}) in {}",
							iobuf.asCoreLocation(), replacer.origin, file);
					// ISO 8859-1: bytes are zero-extended to chars
					for (var originalLine : iobuf.getContentsString(ISO_8859_1)
							.split("\n", -1)) {
						var line = replacer.replace(originalLine);
						w.write(line);
						w.newLine();
						addValueIfMatch(ERROR_ENTRY, line, errorEntries, iobuf);
						addValueIfMatch(WARNING_ENTRY, line, warnEntries,
								iobuf);
					}
				}
			}
		}
	}

	private static File getProvenanceFile(File provenanceDir, IOBuffer iobuf) {
		return new File(provenanceDir,
				format("iobuf_for_chip_%d_%d_processor_id_%d.txt", iobuf.getX(),
						iobuf.getY(), iobuf.getP()));
	}

	private static void addValueIfMatch(Pattern regex, String line,
			List<String> entries, HasCoreLocation core) {
		var match = regex.matcher(line);
		if (match.matches()) {
			synchronized (entries) {
				entries.add(format("%d, %d, %d: %s (%s)", core.getX(),
						core.getY(), core.getP(), match.group(ENTRY_TEXT),
						match.group(ENTRY_FILE)));
			}
		}
	}
}
