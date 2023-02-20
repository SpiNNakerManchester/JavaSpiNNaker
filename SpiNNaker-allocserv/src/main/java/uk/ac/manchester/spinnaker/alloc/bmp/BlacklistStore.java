/*
 * Copyright (c) 2022 The University of Manchester
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package uk.ac.manchester.spinnaker.alloc.bmp;

import static java.util.EnumSet.noneOf;
import static java.util.Objects.requireNonNull;
import static uk.ac.manchester.spinnaker.alloc.db.Row.chip;
import static uk.ac.manchester.spinnaker.alloc.db.Row.core;
import static uk.ac.manchester.spinnaker.alloc.db.Row.stream;

import java.util.HashSet;
import java.util.Optional;

import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Component;

import com.google.errorprone.annotations.RestrictedApi;

import uk.ac.manchester.spinnaker.alloc.ForTestingOnly;
import uk.ac.manchester.spinnaker.alloc.db.DatabaseAPI.Connection;
import uk.ac.manchester.spinnaker.alloc.db.DatabaseAwareBean;
import uk.ac.manchester.spinnaker.alloc.db.Row;
import uk.ac.manchester.spinnaker.machine.ChipLocation;
import uk.ac.manchester.spinnaker.machine.Direction;
import uk.ac.manchester.spinnaker.messages.model.Blacklist;

/**
 * Read a blacklist from the database or write it to the database. This works
 * with blacklists as a collection of records, not a serialised BLOB.
 * <p>
 * Arguably everything in here really ought to be in {@link Blacklist} itself,
 * but that's necessarily in code that can't see into the database.
 *
 * @author Donal Fellows
 */
@Component
public class BlacklistStore extends DatabaseAwareBean {
	/**
	 * Read a blacklist from the database.
	 *
	 * @param boardId
	 *            The ID of the board.
	 * @return The blacklist, if one is defined.
	 * @throws DataAccessException
	 *             If database access fails.
	 */
	public Optional<Blacklist> readBlacklist(int boardId) {
		return executeRead(conn -> readBlacklist(conn, boardId));
	}

	private class DeadLink {
		ChipLocation location;

		Direction direction;

		DeadLink(Row row) {
			location = new ChipLocation(row.getInt("x"), row.getInt("y"));
			direction = row.getEnum("direction", Direction.class);
		}
	}

	/**
	 * Read a blacklist from the database.
	 *
	 * @param conn
	 *            The database connection.
	 * @param boardId
	 *            The ID of the board.
	 * @return The blacklist, if one is defined.
	 * @throws DataAccessException
	 *             If database access fails.
	 */
	private Optional<Blacklist> readBlacklist(Connection conn, int boardId) {
		try (var blChips = conn.query(GET_BLACKLISTED_CHIPS);
				var blCores = conn.query(GET_BLACKLISTED_CORES);
				var blLinks = conn.query(GET_BLACKLISTED_LINKS)) {
			var blacklistedChips =
					stream(blChips.call(chip("x", "y"), boardId)).toSet();
			var blacklistedCores =
					stream(blCores.call(core("x", "y", "p"), boardId))
					.toCollectingMap(
							HashSet::new, c -> c.asChipLocation(),
							c -> c.getP());
			var blacklistedLinks =
					stream(blLinks.call(DeadLink::new, boardId))
					.toCollectingMap(
							() -> noneOf(Direction.class), d -> d.location,
							d -> d.direction);

			if (blacklistedChips.isEmpty() && blacklistedCores.isEmpty()
					&& blacklistedLinks.isEmpty()) {
				return Optional.empty();
			}
			return Optional.of(new Blacklist(blacklistedChips, blacklistedCores,
					blacklistedLinks));
		}
	}

	/**
	 * Save a blacklist in the database.
	 *
	 * @param boardId
	 *            What board is this a blacklist for?
	 * @param blacklist
	 *            The blacklist to save.
	 */
	public void writeBlacklist(int boardId, Blacklist blacklist) {
		requireNonNull(blacklist);
		execute(conn -> {
			writeBlacklist(conn, boardId, blacklist);
			return this; // dummy
		});
	}

	/**
	 * Save a blacklist in the database.
	 *
	 * @param conn
	 *            Which database?
	 * @param boardId
	 *            What board is this a blacklist for?
	 * @param blacklist
	 *            The blacklist to save.
	 */
	private void writeBlacklist(Connection conn, int boardId,
			Blacklist blacklist) {
		try (var clearChips = conn.update(CLEAR_BLACKLISTED_CHIPS);
				var clearCores = conn.update(CLEAR_BLACKLISTED_CORES);
				var clearLinks = conn.update(CLEAR_BLACKLISTED_LINKS);
				var addChip = conn.update(ADD_BLACKLISTED_CHIP);
				var addCore = conn.update(ADD_BLACKLISTED_CORE);
				var addLink = conn.update(ADD_BLACKLISTED_LINK)) {
			// TODO Keep the old blacklist data where it is the same as before
			// Remove the old information
			clearChips.call(boardId);
			clearCores.call(boardId);
			clearLinks.call(boardId);
			// Write the new information
			blacklist.getChips().forEach(chip -> {
				addChip.call(boardId, chip.getX(), chip.getY());
			});
			blacklist.getCores().forEach((chip, cores) -> cores.forEach(c -> {
				addCore.call(boardId, chip.getX(), chip.getY(), c);
			}));
			blacklist.getLinks().forEach((chip, links) -> links.forEach(l -> {
				addLink.call(boardId, chip.getX(), chip.getY(), l);
			}));
		}
	}

	/**
	 * API only exposed for testing purposes.
	 */
	@ForTestingOnly
	interface TestAPI {
		/**
		 * Read a blacklist from the database.
		 *
		 * @param conn
		 *            The database connection.
		 * @param boardId
		 *            The ID of the board.
		 * @return The blacklist, if one is defined.
		 * @throws DataAccessException
		 *             If database access fails.
		 */
		Optional<Blacklist> readBlacklist(Connection conn, int boardId);

		/**
		 * Save a blacklist in the database.
		 *
		 * @param conn
		 *            Which database?
		 * @param boardId
		 *            What board is this a blacklist for?
		 * @param blacklist
		 *            The blacklist to save.
		 */
		void writeBlacklist(Connection conn, int boardId, Blacklist blacklist);
	}

	/**
	 * @return The internal API only used for testing.
	 * @deprecated Only use for testing, as circumvents transaction management.
	 */
	@Deprecated
	@RestrictedApi(explanation = "just for testing", link = "index.html",
			allowedOnPath = ".*/src/test/java/.*")
	@ForTestingOnly
	TestAPI getTestAPI() {
		ForTestingOnly.Utils.checkForTestClassOnStack();
		return new TestAPI() {
			@Override
			public Optional<Blacklist> readBlacklist(Connection conn,
					int boardId) {
				return BlacklistStore.this.readBlacklist(conn, boardId);
			}

			@Override
			public void writeBlacklist(Connection conn, int boardId,
					Blacklist blacklist) {
				BlacklistStore.this.writeBlacklist(conn, boardId, blacklist);
			}
		};
	}
}
