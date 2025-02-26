/*
 * Copyright (c) 2018 The University of Manchester
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
package uk.ac.manchester.spinnaker.front_end.download;

import static difflib.DiffUtils.diff;
import static java.lang.System.getProperty;
import static java.nio.ByteBuffer.allocate;
import static java.util.stream.Collectors.toList;
import static org.slf4j.LoggerFactory.getLogger;
import static uk.ac.manchester.spinnaker.front_end.Constants.PARALLEL_SIZE;
import static uk.ac.manchester.spinnaker.utils.ByteBufferUtils.sliceUp;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.slf4j.Logger;

import com.google.errorprone.annotations.ForOverride;
import com.google.errorprone.annotations.MustBeClosed;
import com.google.errorprone.annotations.OverridingMethodsMustInvokeSuper;

import difflib.ChangeDelta;
import difflib.Chunk;
import difflib.DeleteDelta;
import difflib.InsertDelta;
import uk.ac.manchester.spinnaker.connections.MostDirectConnectionSelector;
import uk.ac.manchester.spinnaker.front_end.BasicExecutor;
import uk.ac.manchester.spinnaker.front_end.BasicExecutor.SimpleCallable;
import uk.ac.manchester.spinnaker.front_end.BoardLocalSupport;
import uk.ac.manchester.spinnaker.front_end.download.request.Gather;
import uk.ac.manchester.spinnaker.front_end.download.request.Monitor;
import uk.ac.manchester.spinnaker.front_end.download.request.Placement;
import uk.ac.manchester.spinnaker.machine.ChipLocation;
import uk.ac.manchester.spinnaker.machine.Machine;
import uk.ac.manchester.spinnaker.machine.tags.IPTag;
import uk.ac.manchester.spinnaker.protocols.NoDropPacketContext;
import uk.ac.manchester.spinnaker.protocols.SystemRouterTableContext;
import uk.ac.manchester.spinnaker.storage.BufferManagerStorage;
import uk.ac.manchester.spinnaker.storage.BufferManagerStorage.Region;
import uk.ac.manchester.spinnaker.storage.StorageException;
import uk.ac.manchester.spinnaker.transceiver.ProcessException;
import uk.ac.manchester.spinnaker.transceiver.TransceiverInterface;
import uk.ac.manchester.spinnaker.utils.MathUtils;
import uk.ac.manchester.spinnaker.utils.UsedInJavadocOnly;

/**
 * Implementation of the SpiNNaker Fast Data Download Protocol.
 *
 * @author Donal Fellows
 * @author Alan Stokes
 */
public abstract class DataGatherer extends BoardLocalSupport
		implements AutoCloseable {
	/**
	 * Logger for the gatherer.
	 */
	protected static final Logger log = getLogger(DataGatherer.class);

	private static final String SPINNAKER_COMPARE_DOWNLOAD =
			getProperty("spinnaker.compare.download");

	private final BasicExecutor pool;

	private int missCount;

	/**
	 * Create an instance of the protocol implementation. (Subclasses handle
	 * where to put it afterwards.)
	 *
	 * @param transceiver
	 *            How to talk to the SpiNNaker system via SCP. Where the system
	 *            is located.
	 * @param machine
	 *            The description of the SpiNNaker machine being talked to.
	 * @throws ProcessException
	 *             If we can't discover the machine details due to SpiNNaker
	 *             rejecting messages
	 * @throws IOException
	 *             If we can't discover the machine details due to I/O problems
	 */
	@MustBeClosed
	@SuppressWarnings("MustBeClosed")
	public DataGatherer(TransceiverInterface transceiver, Machine machine)
			throws IOException, ProcessException {
		super(transceiver, machine);
		this.pool = new BasicExecutor(PARALLEL_SIZE);
		this.missCount = 0;
	}

	@Override
	@OverridingMethodsMustInvokeSuper
	public void close() throws InterruptedException {
		pool.close();
	}

	/**
	 * Download he contents of the regions that are described through the data
	 * gatherers.
	 *
	 * @param gatherers
	 *            The data gatherer information for the boards.
	 * @return The total number of missed packets. Misses are retried, so this
	 *         is just an assessment of data transfer quality.
	 * @throws IOException
	 *             If IO fails.
	 * @throws ProcessException
	 *             If SpiNNaker rejects a message.
	 * @throws StorageException
	 *             If DB access goes wrong.
	 * @throws InterruptedException
	 *             If communications are interrupted.
	 */
	public int gather(List<Gather> gatherers) throws IOException,
			ProcessException, StorageException, InterruptedException {
		sanityCheck(gatherers);
		var work = discoverActualWork(gatherers);
		try (var s = new SystemRouterTableContext(txrx,
				gatherers.stream().flatMap(g -> g.getMonitors().stream()));
				var p = new NoDropPacketContext(txrx,
						gatherers.stream()
								.flatMap(g -> g.getMonitors().stream()),
						gatherers.stream())) {
			log.info("launching {} parallel high-speed download tasks",
					work.size());
			parallel(work.keySet().stream().map(gather -> {
				return () -> fastDownload(work.get(gather),
						gather.asChipLocation(), gather.getIptag());
			}));
		}
		return missCount;
	}

	/**
	 * Trivial POJO holding the pairing of monitor and list of lists of memory
	 * blocks.
	 *
	 * @author Donal Fellows
	 */
	private static final class WorkItems {
		/**
		 * Monitor that is used to download the regions.
		 */
		private final Monitor monitor;

		/**
		 * List of information about where to download. The inner sub-lists are
		 * ordered, and are either one or two items long to represent what
		 * pieces of memory should really be downloaded. The outer list could
		 * theoretically be done in any order... but needs to be processed
		 * single-threaded anyway.
		 */
		private final List<Region> regions;

		WorkItems(Monitor m, List<Region> region) {
			this.monitor = m;
			this.regions = region;
		}
	}

	/**
	 * Query the machine to discover what actual pieces of memory the recording
	 * region IDs of the placements of the vertices attached to the monitors
	 * associated with the data speed up packet gatherers are.
	 *
	 * @param gatherers
	 *            The gatherer information.
	 * @return What each board (as represented by the chip location of its data
	 *         speed up packet gatherer) has to be downloaded.
	 * @throws IOException
	 *             If IO fails.
	 * @throws ProcessException
	 *             If SpiNNaker rejects a message.
	 * @throws StorageException
	 *             If DB access goes wrong.
	 * @throws InterruptedException
	 *             If communications are interrupted.
	 */
	private Map<Gather, List<WorkItems>> discoverActualWork(
			List<Gather> gatherers) throws IOException, ProcessException,
			StorageException, InterruptedException {
		log.info("discovering regions to download");
		var work = new HashMap<Gather, List<WorkItems>>();
		int count = 0;
		for (var g : gatherers) {
			var workitems = new ArrayList<WorkItems>();
			for (var m : g.getMonitors()) {

				for (var p : m.getPlacements()) {
					var regions = new ArrayList<Region>();
					for (int id : p.getVertex().getRecordedRegionIds()) {
						var r = getRecordingRegion(p, id);
						if (r.size > 0) {
							regions.add(r);
							count += 1;
						} else {
							storeData(r, allocate(0));
						}
					}
					for (var dr : p.getVertex().getDownloadRegions()) {
						regions.add(new Region(p, dr.getIndex(),
								dr.getAddress(), dr.getSize(), false));
						count++;
					}
					workitems.add(new WorkItems(m, regions));
				}

			}
			// Totally empty boards can be ignored
			work.put(g, workitems);
		}
		log.info("found {} regions to download", count);
		return work;
	}

	/**
	 * Wrapper around the thread pool to sanitise the exceptions.
	 *
	 * @param tasks
	 *            The tasks to run in the pool.
	 * @throws IOException
	 *             If IO fails.
	 * @throws ProcessException
	 *             If SpiNNaker rejects a message.
	 * @throws StorageException
	 *             If DB access goes wrong.
	 * @throws InterruptedException
	 *             If communications are interrupted.
	 */
	private void parallel(Stream<SimpleCallable> tasks) throws IOException,
			ProcessException, StorageException, InterruptedException {
		try {
			pool.submitTasks(tasks).awaitAndCombineExceptions();
		} catch (IOException | StorageException | ProcessException
				| InterruptedException | RuntimeException e) {
			throw e;
		} catch (Exception e) {
			throw new RuntimeException("unexpected exception", e);
		}
	}

	/**
	 * Do the fast downloads for a particular board.
	 *
	 * @param work
	 *            The items to be downloaded for that board.
	 * @param conn
	 *            The connection for talking to the board.
	 * @throws IOException
	 *             If IO fails.
	 * @throws StorageException
	 *             If DB access goes wrong.
	 * @throws TimeoutException
	 *             If a download times out unrecoverably.
	 * @throws ProcessException
	 *             If anything unexpected goes wrong.
	 * @throws InterruptedException
	 *             If communications are interrupted.
	 */
	private void fastDownload(List<WorkItems> work,
			ChipLocation gathererChip, IPTag ipTag)
			throws IOException, StorageException,
			ProcessException, InterruptedException {
		try (var c = new BoardLocal(gathererChip)) {
			log.info("processing fast downloads for {}", gathererChip);
			for (var item : work) {
				for (var region : item.regions) {
					/*
					 * Once there's something too small, all subsequent
					 * retrieves for that recording region have to be done the
					 * same way to get the data in the DB in the right order.
					 */
					var data = txrx.readMemoryFast(gathererChip, ipTag,
							item.monitor, region.startAddress, region.size);
					if (SPINNAKER_COMPARE_DOWNLOAD != null) {
						compareDownloadWithSCP(region, data);
					}
					storeData(region, data);
				}
			}
		}
	}

	private void sanityCheck(List<Gather> gatherers) {
		var sel = txrx.getScampConnectionSelector();
		MostDirectConnectionSelector<?> s = null;
		if (sel instanceof MostDirectConnectionSelector) {
			s = (MostDirectConnectionSelector<?>) sel;
		}

		// Sanity check the inputs
		for (var g : gatherers) {
			if (machine.getChipAt(g).ipAddress == null) {
				throw new IllegalStateException(
						"gatherer on chip without IP address: "
								+ g.asChipLocation());
			}
			if (s != null && !s.hasDirectConnectionFor(machine.getChipAt(g))) {
				throw new IllegalStateException(
						"gatherer at " + g.asCoreLocation()
								+ " without direct route in transceiver");
			}
		}
	}

	private void compareDownloadWithSCP(Region r, ByteBuffer data)
			throws IOException, ProcessException, InterruptedException {
		var data2 = txrx.readMemory(r.core.asChipLocation(), r.startAddress,
				r.size);
		if (data.remaining() != data2.remaining()) {
			log.error("different buffer sizes: {} with gatherer, {} with SCP",
					data.remaining(), data2.remaining());
		}
		for (int i = 0; i < data.remaining(); i++) {
			if (data.get(i) != data2.get(i)) {
				log.error("downloaded buffer contents different");
				for (var delta : diff(list(data2), list(data)).getDeltas()) {
					if (delta instanceof ChangeDelta) {
						var delete = delta.getOriginal();
						var insert = delta.getRevised();
						log.warn(
								"swapped {} bytes (SCP) for {} (gather) "
										+ "at {}->{}",
								delete.getLines().size(),
								insert.getLines().size(), delete.getPosition(),
								insert.getPosition());
						log.info("change {} -> {}", describeChunk(delete),
								describeChunk(insert));
					} else if (delta instanceof DeleteDelta) {
						var delete = delta.getOriginal();
						log.warn("gather deleted {} bytes at {}",
								delete.getLines().size(), delete.getPosition());
						log.info("delete {}", describeChunk(delete));
					} else if (delta instanceof InsertDelta) {
						var insert = delta.getRevised();
						log.warn("gather inserted {} bytes at {}",
								insert.getLines().size(), insert.getPosition());
						log.info("insert {}", describeChunk(insert));
					}
				}
				break;
			}
		}
	}

	private static List<Byte> list(ByteBuffer buffer) {
		return sliceUp(buffer, 1).map(ByteBuffer::get).toList();
	}

	private static List<String> describeChunk(Chunk<Byte> chunk) {
		return chunk.getLines().stream().map(MathUtils::hexbyte)
				.collect(toList());
	}

	/**
	 * Work out exactly where is going to be downloaded. The elements of the
	 * list this method returns will end up directing what calls to
	 * {@link #storeData(BufferManagerStorage.Region,ByteBuffer) storeData(...)}
	 * are done, and the order in which they are done.
	 * <p>
	 * The recording region memory management scheme effectively requires this
	 * to be a list of zero, one or two elements, but the {@link DataGatherer}
	 * class does not care.
	 *
	 * @param placement
	 *            The placement information.
	 * @param regionID
	 *            The region ID.
	 * @return The region descriptors that are the actual download instructions.
	 *         May be unmodifiable.
	 * @throws IOException
	 *             If communication fails.
	 * @throws ProcessException
	 *             If SpiNNaker rejects a message.
	 * @throws StorageException
	 *             If the database doesn't like something.
	 * @throws InterruptedException
	 *             If communications are interrupted.
	 */
	@UsedInJavadocOnly(BufferManagerStorage.class)
	@ForOverride
	protected abstract Region getRecordingRegion(
			Placement placement, int regionID)
			throws IOException, ProcessException, StorageException,
			InterruptedException;

	/**
	 * Store the data retrieved from a region
	 *
	 * No guarantee is made about which thread will call this method.
	 *
	 * @param r
	 *            Where the data came from.
	 * @param data
	 *            The data that was retrieved.
	 * @throws StorageException
	 *             If the database refuses to do the store.
	 */
	@ForOverride
	protected abstract void storeData(Region r, ByteBuffer data)
			throws StorageException;


}

