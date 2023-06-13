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
package uk.ac.manchester.spinnaker.storage;

import java.nio.ByteBuffer;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.validation.Valid;
import javax.validation.constraints.Positive;

import uk.ac.manchester.spinnaker.machine.ChipLocation;
import uk.ac.manchester.spinnaker.machine.CoreLocation;
import uk.ac.manchester.spinnaker.machine.MemoryLocation;
import uk.ac.manchester.spinnaker.utils.validation.IPAddress;

/**
 * The interface supported by the DSE part of the storage system.
 *
 * @author Donal Fellows
 */
public interface DSEStorage extends ProxyAwareStorage {
	/**
	 * See how many DSE loading actions have to be done.
	 *
	 * @return The number of data specifications remaining to be executed.
	 * @throws StorageException
	 *             If the database access fails.
	 */
	int countCores(boolean loadSystemCores) throws StorageException;

	/**
	 * Get a list of all ethernets that need to have DSE loading done on them.
	 *
	 * @param loadSystemCores
	 *            If {@code true}, just count system cores. If {@code false},
	 *            just count application (non-system) cores.
	 * @return The list of ethernets.
	 * @throws StorageException
	 *             If the database access fails.
	 */
	List<Ethernet> listEthernetsToLoad() throws StorageException;

	/**
	 * Get a list of all DSE loading actions that need to be done for a
	 * particular ethernet.
	 *
	 * @param ethernet
	 *            The ethernet we're loading onto.
	 * @param loadSystemCores
	 *            If {@code true}, just list system cores. If {@code false},
	 *            just list application (non-system) cores.
	 * @return The list of actions.
	 * @throws StorageException
	 *             If the database access fails.
	 */
	List<CoreLocation> listCoresToLoad(Ethernet ethernet, boolean loadSystemCores)
			throws StorageException;

    /**
     * 
     * @param cores
	 *            cores to get the region sizes for
     * @return Map of Region number to size. 
*           For the regions with a none zero size
     * @throws StorageException 
	 *             If the database access fails.
     */
    LinkedHashMap<Integer, Integer> getRegionSizes(CoreLocation core) 
            throws StorageException;
    
	/**
	 * Record the results of loading a core.
	 *
	 * @param coreToLoad
	 *            The instruction to load a particular core.
	 * @param startAddress
	 *            Where the load metadata starts.
	 * @throws StorageException
	 *             If the database access fails.
	 */
	void setStartAddress(CoreLocation xyp, MemoryLocation start)
            throws StorageException;

    MemoryLocation getStartAddress(CoreLocation xyp) throws StorageException;
       
    int getAppId() throws StorageException;
 
    void setRegionPointer(CoreLocation xyp, int region_num, int next_pointer)
            throws StorageException;
    
    Map<Integer,RegionInfo> getRegionPointersAndContent(CoreLocation xyp)
           throws StorageException;
    
    /**
	 * Record the results of loading a core.
	 *
	 * @param coreToLoad
	 *            The instruction to load a particular core.
	 * @param startAddress
	 *            Where the load metadata starts.
	 * @param memoryUsed
	 *            How much memory was allocated by loading.
	 * @param memoryWritten
	 *            How much memory was written by loading.
	 * @throws StorageException
	 *             If the database access fails.
	 */
	void saveLoadingMetadata(CoreToLoad coreToLoad, MemoryLocation startAddress,
			int memoryUsed, int memoryWritten) throws StorageException;

	/**
	 * A ethernet which allows data specifications to be loaded.
	 *
	 * @author Donal Fellows
	 */
	abstract class Ethernet {
		/**
		 * The virtual location of this ethernet.
		 */
		@Valid
		public final ChipLocation location;

		/**
		 * The network address of this ethernet.
		 */
		@IPAddress
		public final String ethernetAddress;

		/**
		 * Create an instance.
		 *
		 * @param ethernetX
		 *            The X coordinate of the ethernet chip.
		 * @param ethernetY
		 *            The Y coordinate of the ethernet chip.
		 * @param address
		 *            The IP address of the ethernet.
		 */
		protected Ethernet(int ethernetX, int ethernetY, String address) {
			this.location = new ChipLocation(ethernetX, ethernetY);
			this.ethernetAddress = address;
		}
	}

	/**
	 * A core with a data specification to load.
	 *
	 * @author Donal Fellows
	 */
	abstract class CoreToLoad {
		/**
		 * The core that the load is to be done on.
		 */
		@Valid
		public final CoreLocation core;

		/**
		 * The size of region to allocate and write into.
		 */
		@Positive
		public final int sizeToWrite;

		/**
		 * Create an instance.
		 *
		 * @param x
		 *            The X coordinate of the core.
		 * @param y
		 *            The Y coordinate of the core.
		 * @param p
		 *            The application identifier.
		 * @param sizeToWrite
		 *            Number of bytes to be written, as computed by DSG.
		 */
		protected CoreToLoad(int x, int y, int p, int sizeToWrite) {
			this.core = new CoreLocation(x, y, p);
			this.sizeToWrite = sizeToWrite;
		}
	}
    
}
