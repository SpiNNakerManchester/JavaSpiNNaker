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
package uk.ac.manchester.spinnaker.front_end.dse;

import uk.ac.manchester.spinnaker.machine.CoreLocation;
import static uk.ac.manchester.spinnaker.front_end.Constants.CORE_DATA_SDRAM_BASE_TAG;

import java.io.IOException;
import java.nio.ByteBuffer;

import static java.nio.ByteBuffer.allocate;
import static java.nio.ByteOrder.LITTLE_ENDIAN;
import java.util.LinkedHashMap;

import uk.ac.manchester.spinnaker.machine.HasCoreLocation;
import uk.ac.manchester.spinnaker.machine.MemoryLocation;
import uk.ac.manchester.spinnaker.messages.model.AppID;
import uk.ac.manchester.spinnaker.storage.DSEStorage;
import uk.ac.manchester.spinnaker.storage.DSEStorage.Ethernet;
import uk.ac.manchester.spinnaker.storage.StorageException;
import uk.ac.manchester.spinnaker.transceiver.ProcessException;
import uk.ac.manchester.spinnaker.transceiver.TransceiverInterface;
import static uk.ac.manchester.spinnaker.utils.MathUtils.ceildiv;

abstract class BoardWorker {
	/** The transceiver for talking to the SpiNNaker machine. */
	protected final TransceiverInterface txrx;

	/** The Ethernet data for this board.*/
	protected final Ethernet board;

	/** The database holding the DS data.*/
	private final DSEStorage storage;

	/** The system wide app id.*/
	private final int appId;

	/** Application data magic number. */
	private static final int APPDATA_MAGIC_NUM = 0xAD130AD6;

	/** Version of the file produced by the DSE. */
	private static final int DSE_VERSION = 0x00010000;

	/** The number of memory regions in the DSE model. */
	private static final int MAX_MEM_REGIONS = 32;

	private static final long UNSIGNED_INT = 0xFFFFFFFFL;

	/** Bytes per int/word. */
	private static final int INT_SIZE = 4;

	/**
	 * The size of the Data Specification table header, in bytes.
	 * Note that the header consists of 2 uint32_t variables
	 * (magic number, version).
	 */
	private static final int APP_PTR_TABLE_HEADER_SIZE = INT_SIZE * 2;

	/**
	 * The size of a Data Specification region description, in bytes.
	 * Note that each description consists of a pointer and 2 uint32_t variables
	 * (pointer, checksum, n_words).
	 */
	private static final int APP_PTR_TABLE_REGION_SIZE = INT_SIZE * 3;

	/**
	 * The size of the Data Specification table, in bytes.
	 */
	private static final int APP_PTR_TABLE_BYTE_SIZE =
			APP_PTR_TABLE_HEADER_SIZE
			+ (MAX_MEM_REGIONS * APP_PTR_TABLE_REGION_SIZE);

	/**
	 * Create the base/Shared Board worker.
	 *
	 * @param txrx
	 *           The transceiver for talking to the SpiNNaker machine.
	 * @param board
	 *           The Ethernet data for this board
	 * @param storage
	 *           The database holding the DS data
	 * @throws StorageException
	 *             If the database access fails.
	 */
	protected BoardWorker(TransceiverInterface txrx, Ethernet board,
			DSEStorage storage) throws StorageException {
		this.board = board;
		this.storage = storage;
		this.appId = storage.getAppId();
		this.txrx = txrx;
	}

	/**
	 * Execute a data specification and load the results onto a core.
	 *
	 * @param xyp
	 *            The coordinates of the core to malloc on.
	 * @throws IOException
	 *             If anything goes wrong with I/O.
	 * @throws ProcessException
	 *             If SCAMP rejects the request.
	 * @throws DataSpecificationException
	 *             If the instructions to build the data are wrong.
	 * @throws StorageException
	 *             If the database access fails.
	 * @throws InterruptedException
	 *             If communications are interrupted.
	 */
	protected void mallocCore(CoreLocation xyp) throws
			IOException, ProcessException, StorageException,
			InterruptedException {
		LinkedHashMap<Integer, Integer> regionSizes =
				storage.getRegionSizes(xyp);
		int totalSize = regionSizes.values().stream().mapToInt(
				Integer::intValue).sum();
		var start = txrx.mallocSDRAM(xyp.getScampCore(),
				totalSize + APP_PTR_TABLE_BYTE_SIZE, new AppID(this.appId),
				xyp.getP() + CORE_DATA_SDRAM_BASE_TAG);

		txrx.writeUser0(xyp, start);
		storage.setStartAddress(xyp, start);

		int nextPointer = start.address() + APP_PTR_TABLE_BYTE_SIZE;
		for (var regionNum : regionSizes.keySet()) {
			var size = regionSizes.get(regionNum);
			storage.setRegionPointer(xyp, regionNum, nextPointer);
			nextPointer += size;
		}
	}

	/**
	 * Execute a data specification and load the results onto a core.
	 *
	 * @param xyp
	 *            the coordinates of the core to load.
	 * @throws IOException
	 *             If anything goes wrong with I/O.
	 * @throws ProcessException
	 *             If SCAMP rejects the request.
	 * @throws DataSpecificationException
	 *             If the instructions to build the data are wrong.
	 * @throws StorageException
	 *             If the database access fails.
	 * @throws InterruptedException
	 *             If communications are interrupted.
	 */
	protected void loadCore(CoreLocation xyp) throws IOException,
			ProcessException, StorageException, InterruptedException {
		var pointerTable =
				allocate(APP_PTR_TABLE_BYTE_SIZE).order(LITTLE_ENDIAN);
		//header
		pointerTable.putInt(APPDATA_MAGIC_NUM);
		pointerTable.putInt(DSE_VERSION);

		var regionInfos = storage.getRegionPointersAndContent(xyp);
		for (int region = 0; region < MAX_MEM_REGIONS; region++) {
			if (regionInfos.containsKey(region)) {
				var regionInfo = regionInfos.get(region);
				pointerTable.putInt(regionInfo.pointer.address());
				if (regionInfo.content != null) {
					var written = writeRegion(
							xyp, regionInfo.content, regionInfo.pointer);
					// Work out the checksum
					int nWords = ceildiv(written, INT_SIZE);
					var buf = regionInfo.content.duplicate()
							.order(LITTLE_ENDIAN).rewind().asIntBuffer();
					long sum = 0;
					for (int i = 0; i < nWords; i++) {
						sum = (sum + (buf.get() & UNSIGNED_INT)) & UNSIGNED_INT;
					}
					// Write the checksum and number of words
					pointerTable.putInt((int) (sum & UNSIGNED_INT));
					pointerTable.putInt(nWords);
				} else {
					// Don't checksum references
					pointerTable.putInt(0);
					pointerTable.putInt(0);
				}
			} else {
				// There is no data for non-regions
				pointerTable.putInt(0);
				pointerTable.putInt(0);
				pointerTable.putInt(0);
			}
		}

		var startAddress = storage.getStartAddress(xyp);
		pointerTable.flip();
		txrx.writeMemory(xyp.getScampCore(), startAddress, pointerTable);
	}

	/**
	 * Writes the contents of a region. Caller is responsible for ensuring
	 * this method has work to do.
	 *
	 * @param core
	 *            Which core to write to.
	 * @param content
	 *            Data to write
	 * @param baseAddress
	 *            Where to write the region.
	 * @return How many bytes were actually written.
	 * @throws IOException
	 *             If anything goes wrong with I/O.
	 * @throws ProcessException
	 *             If SCAMP rejects the request.
	 * @throws InterruptedException
	 *             If communications are interrupted.
	 */
	protected abstract int writeRegion(HasCoreLocation core,
			ByteBuffer content, MemoryLocation baseAddress)
			throws IOException, ProcessException, InterruptedException;
}
