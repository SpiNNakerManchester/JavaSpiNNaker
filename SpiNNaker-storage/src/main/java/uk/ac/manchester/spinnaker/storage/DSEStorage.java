/*
 * Copyright (c) 2018 The University of Manchester
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

import uk.ac.manchester.spinnaker.machine.ChipLocation;
import uk.ac.manchester.spinnaker.machine.CoreLocation;

/**
 * The interface supported by the DSE part of the storage system.
 *
 * @author Donal Fellows
 */
public interface DSEStorage {
	/**
	 * Get a list of all boards that need to have DSE loading done on them.
	 *
	 * @return The list of boards.
	 * @throws StorageException
	 *             If the database access fails.
	 */
	List<Board> listBoardsToLoad() throws StorageException;

	/**
	 * Get a list of all DSE loading actions that need to be done for a
	 * particular board.
	 *
	 * @param board
	 *            The SpiNNaker board we're loading onto.
	 * @return The list of actions.
	 * @throws StorageException
	 *             If the database access fails.
	 */
	List<CoreToLoad> listCoresToLoad(Board board) throws StorageException;

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
	void saveLoadingMetadata(CoreToLoad coreToLoad, int startAddress,
			int memoryUsed, int memoryWritten) throws StorageException;

	/**
	 * A board which has data specifications to be loaded onto it.
	 *
	 * @author Donal Fellows
	 */
	abstract class Board {
		/**
		 * The virtual location of this board.
		 */
		public final ChipLocation ethernet;

		/**
		 * The network address of this board.
		 */
		public final String ethernetAddress;

		/**
		 * Create an instance.
		 *
		 * @param ethernetX
		 *            The X coordinate of the board's ethernet chip.
		 * @param ethernetY
		 *            The Y coordinate of the board's ethernet chip.
		 * @param address
		 *            The IP address of the board.
		 */
		protected Board(int ethernetX, int ethernetY, String address) {
			this.ethernet = new ChipLocation(ethernetX, ethernetY);
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
		public final CoreLocation core;

		/**
		 * The data specification to execute for this core.
		 */
		public final ByteBuffer dataSpec;

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
		 * @param dataSpec
		 *            The data specification to execute.
		 */
		protected CoreToLoad(int x, int y, int p, int appID, byte[] dataSpec) {
			this.core = new CoreLocation(x, y, p);
			this.dataSpec = ByteBuffer.wrap(dataSpec);
			this.appID = appID;
		}
	}
}
