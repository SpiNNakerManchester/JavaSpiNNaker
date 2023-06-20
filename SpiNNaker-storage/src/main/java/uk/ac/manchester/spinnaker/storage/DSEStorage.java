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

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.validation.Valid;

import uk.ac.manchester.spinnaker.machine.ChipLocation;
import uk.ac.manchester.spinnaker.machine.CoreLocation;
import uk.ac.manchester.spinnaker.machine.MemoryLocation;
import uk.ac.manchester.spinnaker.utils.validation.IPAddress;

/**
 * The interface supported by the DSE part of the storage system.
 *
 * @author Donal Fellows
 */
public interface DSEStorage extends DatabaseAPI {

	/**
	 * See how many DSE loading actions have to be done.
	 *
	 * @param loadSystemCores
	 *            If {@code true}, just count system cores. If {@code false},
	 *            just count application (non-system) cores.
	 * @return The count of the cores which match the is loadSystemCores
	 * @throws StorageException
	 *             If the database access fails.
	 */
	int countCores(boolean loadSystemCores) throws StorageException;

	/**
	 * Get a list of all ethernets that need to have DSE loading done on them.
	 *
	 * @return The list of ethernets.
	 * @throws StorageException
	 *             If the database access fails.
	 */
	List<Ethernet> listEthernetsToLoad() throws StorageException;

	/**
	 * Get a list of all cores that need to be done for a particular ethernet.
	 *
	 * @param ethernet
	 *            The ethernet we're loading onto.
	 * @param loadSystemCores
	 *            If {@code true}, just list system cores. If {@code false},
	 *            just list application (non-system) cores.
	 * @return The list of core locations.
	 * @throws StorageException
	 *             If the database access fails.
	 */
	List<CoreLocation> listCoresToLoad(Ethernet ethernet,
			boolean loadSystemCores) throws StorageException;

	/**
	 *
	 * Get a map of region id to size for regions with a none zero size.
	 *
	 * @param xyp
	 *      Coordinates to get the region sizes for
	 * @return Sorted Map of Region number to size.
	 *           For the regions with a none zero size
	 * @throws StorageException
	 *             If the database access fails.
	 */
	LinkedHashMap<Integer, Integer> getRegionSizes(CoreLocation xyp)
			throws StorageException;

	/**
	 * Record the start address for the metadata on this core.
	 *
	 * @param xyp
	 *            Coordinates for the core
	 * @param start
	 *            Where the load metadata starts.
	 * @throws StorageException
	 *             If the database access fails.
	 */
	void setStartAddress(CoreLocation xyp, MemoryLocation start)
			throws StorageException;

	/**
	 * Gets the start address for the metadata on this core.
	 *
	 * @param xyp
	 *            Coordinates for the core
	 * @return The location of the start of the metadata region
	 * @throws StorageException
	 *             If the database access fails.
	 */
	MemoryLocation getStartAddress(CoreLocation xyp) throws StorageException;

	/**
	 * Get the system wide app id.
	 *
	 * @return the app id
	 * @throws StorageException
	 *             If the database access fails.
	 */
	int getAppId() throws StorageException;

	/**
	 * Set the pointer for where to write the region data to.
	 *
	 * @param xyp
	 *            Coordinates for the core
	 * @param regionNum
	 *            region number for this pointer
	 * @param pointer
	 *            start address for this regions metadata
	 * @throws StorageException
	 *             If the database access fails.
	 */
	void setRegionPointer(CoreLocation xyp, int regionNum, int pointer)
			throws StorageException;

	/**
	 * Gets a map of region ids to pointers and content
	 *
	 * Maps only regions with a none zero size.
	 *
	 * The content may be null is no data added
	 *
	 * @param xyp
	 *            Coordinates for the core
	 * @return map of region number to object holding pointer and content
	 * @throws StorageException
	 *             If the database access fails.
	 */
	Map<Integer, RegionInfo> getRegionPointersAndContent(CoreLocation xyp)
			throws StorageException;

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
}
