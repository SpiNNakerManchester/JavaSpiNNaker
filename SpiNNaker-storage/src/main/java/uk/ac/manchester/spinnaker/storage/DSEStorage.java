/*
 * Copyright (c) 2018-2019 The University of Manchester
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
package uk.ac.manchester.spinnaker.storage;

import java.nio.ByteBuffer;
import java.util.List;

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
	int countWorkRequired() throws StorageException;

	/**
	 * Get a list of all ethernets that need to have DSE loading done on them.
	 *
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
	 * @return The list of actions.
	 * @throws StorageException
	 *             If the database access fails.
	 */
	List<CoreToLoad> listCoresToLoad(Ethernet ethernet) throws StorageException;

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
	List<CoreToLoad> listCoresToLoad(Ethernet ethernet, boolean loadSystemCores)
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
		 * @return The data specification to execute for this core.
		 * @throws StorageException
		 *             If anything goes wrong when reading the data spec itself.
		 */
		public abstract ByteBuffer getDataSpec() throws StorageException;

		/**
		 * The application identifier associated with a core.
		 */
		public final int appID;

		/**
		 * Create an instance.
		 *
		 * @param x
		 *            The X coordinate of the core.
		 * @param y
		 *            The Y coordinate of the core.
		 * @param p
		 *            The P coordinate of the core.
		 * @param appID
		 *            The application identifier.
		 * @param sizeToWrite
		 *            Number of bytes to be written, as computed by DSG.
		 */
		protected CoreToLoad(int x, int y, int p, int appID, int sizeToWrite) {
			this.core = new CoreLocation(x, y, p);
			this.appID = appID;
			this.sizeToWrite = sizeToWrite;
		}
	}
}
