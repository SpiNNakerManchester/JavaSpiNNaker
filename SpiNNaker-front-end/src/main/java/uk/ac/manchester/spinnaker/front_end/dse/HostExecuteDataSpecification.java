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

import static org.slf4j.LoggerFactory.getLogger;
import uk.ac.manchester.spinnaker.machine.CoreLocation;
import static uk.ac.manchester.spinnaker.front_end.Constants.CORE_DATA_SDRAM_BASE_TAG;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;

import org.slf4j.Logger;

import com.google.errorprone.annotations.MustBeClosed;
import static java.nio.ByteBuffer.allocate;
import static java.nio.ByteOrder.LITTLE_ENDIAN;
import java.util.LinkedHashMap;
import static uk.ac.manchester.spinnaker.data_spec.Constants.APPDATA_MAGIC_NUM;
import static uk.ac.manchester.spinnaker.data_spec.Constants.APP_PTR_TABLE_BYTE_SIZE;
import static uk.ac.manchester.spinnaker.data_spec.Constants.DSE_VERSION;
import static uk.ac.manchester.spinnaker.data_spec.Constants.INT_SIZE;
import static uk.ac.manchester.spinnaker.data_spec.Constants.MAX_MEM_REGIONS;

import uk.ac.manchester.spinnaker.data_spec.DataSpecificationException;
import uk.ac.manchester.spinnaker.front_end.Progress;
import uk.ac.manchester.spinnaker.machine.HasCoreLocation;
import uk.ac.manchester.spinnaker.machine.Machine;
import uk.ac.manchester.spinnaker.machine.MemoryLocation;
import uk.ac.manchester.spinnaker.messages.model.AppID;
import uk.ac.manchester.spinnaker.storage.DSEDatabaseEngine;
import uk.ac.manchester.spinnaker.storage.DSEStorage;
import uk.ac.manchester.spinnaker.storage.DSEStorage.Ethernet;
import uk.ac.manchester.spinnaker.storage.StorageException;
import uk.ac.manchester.spinnaker.transceiver.ProcessException;
import uk.ac.manchester.spinnaker.transceiver.TransceiverInterface;
import static uk.ac.manchester.spinnaker.utils.MathUtils.ceildiv;

/**
 * Executes the host based data specification.
 *
 * @author Donal Fellows
 */
public class HostExecuteDataSpecification extends ExecuteDataSpecification {
	private static final String LOADING_MSG =
			"loading data specifications onto SpiNNaker";

	private static final Logger log =
			getLogger(HostExecuteDataSpecification.class);

	private static final long UNSIGNED_INT = 0xFFFFFFFFL;

	/**
	 * Create a high-level DSE interface.
	 *
	 * @param txrx
	 *            The transceiver for talking to the SpiNNaker machine.
	 * @param machine
	 *            The description of the SpiNNaker machine.
	 * @param db
	 *            The DSE database.
	 * @throws IOException
	 *             If the transceiver can't talk to its sockets.
	 * @throws ProcessException
	 *             If SpiNNaker rejects a message.
	 * @throws InterruptedException
	 *             If communications are interrupted.
	 * @throws URISyntaxException
	 *             If the URI is not valid.
	 * @throws StorageException
	 *             If the database cannot be read.
	 * @throws IllegalStateException
	 *             If something really strange occurs with talking to the BMP;
	 *             this constructor should not be doing that!
	 */
	@MustBeClosed
	public HostExecuteDataSpecification(TransceiverInterface txrx,
			Machine machine, DSEDatabaseEngine db)
			throws IOException, ProcessException, InterruptedException,
			StorageException, URISyntaxException {
		super(txrx, machine, db);
	}

	/**
	 * Execute all application data specifications that a particular connection
	 * knows about, storing back in the database the information collected about
	 * those executions.

	 * @throws StorageException
	 *             If the database can't be talked to.
	 * @throws IOException
	 *             If the transceiver can't talk to its sockets.
	 * @throws ProcessException
	 *             If SpiNNaker rejects a message.
	 * @throws DataSpecificationException
	 *             If a data specification in the database is invalid.
	 * @throws InterruptedException
	 *             If communications are interrupted.
	 * @throws IllegalStateException
	 *             If an unexpected exception occurs in any of the parallel
	 *             tasks.
	 */
	public void loadApplicationCores()
			throws StorageException, IOException, ProcessException,
			DataSpecificationException, InterruptedException {
		var storage = db.getStorageInterface();
		var ethernets = storage.listEthernetsToLoad();
		int opsToRun = storage.countCores(false);
		try (var bar = new Progress(opsToRun, LOADING_MSG)) {
			processTasksInParallel(ethernets, board -> {
				return () -> loadBoard(board, storage, bar, false);
			});
		}
	}

	/**
	 * Execute all system data specifications that a particular connection knows
	 * about, storing back in the database the information collected about those
	 * executions.

	 * @throws StorageException
	 *             If the database can't be talked to.
	 * @throws IOException
	 *             If the transceiver can't talk to its sockets.
	 * @throws ProcessException
	 *             If SpiNNaker rejects a message.
	 * @throws DataSpecificationException
	 *             If a data specification in the database is invalid.
	 * @throws InterruptedException
	 *             If communications are interrupted.
	 * @throws IllegalStateException
	 *             If an unexpected exception occurs in any of the parallel
	 *             tasks.
	 */
	public void loadSystemCores()
			throws StorageException, IOException, ProcessException,
			DataSpecificationException, InterruptedException {
		var storage = db.getStorageInterface();
		var ethernets = storage.listEthernetsToLoad();
		int opsToRun = storage.countCores(true);
		try (var bar = new Progress(opsToRun, LOADING_MSG)) {
			processTasksInParallel(ethernets, board -> {
				return () -> loadBoard(board, storage, bar, true);
			});
		}
	}

	private void loadBoard(Ethernet board, DSEStorage storage, Progress bar,
			boolean system)
			throws IOException, ProcessException, DataSpecificationException,
			StorageException, InterruptedException {
		try (var c = new BoardLocal(board.location)) {
			var worker = new BoardWorker(board, storage, bar);
			for (var xyp : storage.listCoresToLoad(board, system)) {
				worker.mallocCore(storage, xyp);
			}
			for (var ctl : storage.listCoresToLoad(board, system)) {
				worker.loadCore(storage, ctl);
			}
		}
	}

	private class BoardWorker {
		private final Ethernet board;

		private final DSEStorage storage;

		private final Progress bar;

        private final int appId;

		BoardWorker(Ethernet board, DSEStorage storage, Progress bar)
                throws StorageException {
			this.board = board;
			this.storage = storage;
			this.bar = bar;
            this.appId = storage.getAppId();
		}

		/**
		 * Execute a data specification and load the results onto a core.
		 *
		 * @param ctl
		 *            The definition of what to run and where to send the
		 *            results.
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
		void mallocCore(DSEStorage storage, CoreLocation xyp) throws 
                IOException, ProcessException,
				DataSpecificationException, StorageException,
				InterruptedException {
            LinkedHashMap<Integer, Integer> region_sizes = 
                    storage.getRegionSizes(xyp);
            int total_size = region_sizes.values().stream().mapToInt(
                    Integer::intValue).sum();
 			var start = malloc(xyp, total_size + APP_PTR_TABLE_BYTE_SIZE);
            storage.setStartAddress(xyp, start);
            
            int next_pointer = start.address + + APP_PTR_TABLE_BYTE_SIZE;
            for (var region_num : region_sizes.keySet()) {
                var size = region_sizes.get(region_num);
                storage.setRegionPointer(xyp, region_num, next_pointer);
                next_pointer += size;
            }
		}

		/**
		 * Execute a data specification and load the results onto a core.
		 *
		 * @param ctl
		 *            The definition of what to run and where to send the
		 *            results.
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
		void loadCore(DSEStorage storage, CoreLocation xyp) 
                throws IOException, ProcessException,
				DataSpecificationException, StorageException,
				InterruptedException {
            int totalWritten = APP_PTR_TABLE_BYTE_SIZE;
            var pointer_table = allocate(APP_PTR_TABLE_BYTE_SIZE).order(LITTLE_ENDIAN);
            //header
            pointer_table.putInt(APPDATA_MAGIC_NUM);
            //log.info("APPDATA_MAGIC_NUM " + APPDATA_MAGIC_NUM);
            pointer_table.putInt(DSE_VERSION);
            //log.info("DSE_VERSION" + DSE_VERSION);
            
            //log.info("xyp: " + xyp.toString());
            var regionInfos = storage.getRegionPointersAndContent(xyp);
            for (int region = 0; region < MAX_MEM_REGIONS; region++) {
                 if (regionInfos.containsKey(region)){
                    var regionInfo = regionInfos.get(region);
                    pointer_table.putInt(regionInfo.pointer.address);
                    if (regionInfo.content != null) {
                        //log.info(""  +regionInfo.content.position() + " " + regionInfo.content.limit() + " " +  regionInfo.content.capacity());
                        var written = writeRegion(
                                xyp, regionInfo.content, regionInfo.pointer);
                        // Work out the checksum
                        int nWords =
                                ceildiv(written, INT_SIZE);
                        //log.info("Wrote " + written + " to " + regionInfo.pointer + " in " + nWords + " words");
                        var buf = regionInfo.content.duplicate()
                                .order(LITTLE_ENDIAN).rewind().asIntBuffer();
                        long sum = 0;
                        for (int i = 0; i < nWords; i++) {
                            sum = (sum + (buf.get() & UNSIGNED_INT)) 
                                    & UNSIGNED_INT;
                        }
                        // Write the checksum and number of words
                        pointer_table.putInt((int) (sum & UNSIGNED_INT));
                        pointer_table.putInt(nWords);
                    } else {
                        // Don't checksum references
                        pointer_table.putInt(0);
                        pointer_table.putInt(0);
                    }
                } else {
                    // There is no data for non-regions
                    pointer_table.putInt(0);
                    pointer_table.putInt(0);
                    pointer_table.putInt(0);
                }
            }
            
            var startAddress = storage.getStartAddress(xyp);
            pointer_table.flip();
            txrx.writeMemory(xyp.getScampCore(), startAddress, pointer_table);
            txrx.writeUser0(xyp, startAddress);
          }
        
        private MemoryLocation malloc(CoreLocation xyp, int bytesUsed)
				throws IOException, ProcessException, InterruptedException {
			return txrx.mallocSDRAM(xyp.getScampCore(), bytesUsed,
					new AppID(this.appId),
					xyp.getP() + CORE_DATA_SDRAM_BASE_TAG);
		}
            
		/**
		 * Writes the contents of a region. Caller is responsible for ensuring
		 * this method has work to do.
		 *
		 * @param core
		 *            Which core to write to.
		 * @param region
		 *            The region to write.
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
		private int writeRegion(HasCoreLocation core, ByteBuffer content,
				MemoryLocation baseAddress)
				throws IOException, ProcessException, InterruptedException {
			var data = content.duplicate();

			int written = data.remaining();
			txrx.writeMemory(core.getScampCore(), baseAddress, data);
			return written;
		}
    }
        
}
