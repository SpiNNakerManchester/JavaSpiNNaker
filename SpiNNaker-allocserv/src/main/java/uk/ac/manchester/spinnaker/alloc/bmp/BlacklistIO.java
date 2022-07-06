/*
 * Copyright (c) 2022 The University of Manchester
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
package uk.ac.manchester.spinnaker.alloc.bmp;

import static java.util.EnumSet.noneOf;
import static java.util.Objects.requireNonNull;
import static uk.ac.manchester.spinnaker.alloc.db.Row.enumerate;
import static uk.ac.manchester.spinnaker.alloc.db.Row.integer;
import static uk.ac.manchester.spinnaker.alloc.model.Utils.chip;

import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Component;

import uk.ac.manchester.spinnaker.alloc.db.DatabaseAwareBean;
import uk.ac.manchester.spinnaker.alloc.db.DatabaseEngine.Connection;
import uk.ac.manchester.spinnaker.alloc.db.DatabaseEngine.Query;
import uk.ac.manchester.spinnaker.alloc.db.DatabaseEngine.Update;
import uk.ac.manchester.spinnaker.machine.ChipLocation;
import uk.ac.manchester.spinnaker.machine.Direction;
import uk.ac.manchester.spinnaker.messages.bmp.Blacklist;

/**
 * Read a blacklist from a string, a definition file or the database. Note that
 * the code does not examine the filename, which is often used to determine what
 * board the blacklist applies to; that is left as a problem for the caller to
 * handle. That information <em>is</em> required when talking to the DB.
 * <p>
 * Arguably everything in here really ought to be in {@link Blacklist} itself,
 * but that's necessarily in code that can't see into the database.
 *
 * @author Donal Fellows
 */
@Component
public class BlacklistIO extends DatabaseAwareBean {
	/**
	 * Read a blacklist from the database.
	 *
	 * @param boardId
	 *            The ID of the board.
	 * @return The blacklist, if one is defined.
	 * @throws DataAccessException
	 *             If database access fails.
	 */
	public Optional<Blacklist> readBlacklistFromDB(int boardId) {
		return executeRead(conn -> readBlacklistFromDB(conn, boardId));
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
	 * @deprecated Call via {@link #readBlacklistFromDB(int)} if not in test
	 *             code.
	 */
	@Deprecated
	final Optional<Blacklist> readBlacklistFromDB(Connection conn,
			int boardId) {
		try (Query blChips = conn.query(GET_BLACKLISTED_CHIPS);
				Query blCores = conn.query(GET_BLACKLISTED_CORES);
				Query blLinks = conn.query(GET_BLACKLISTED_LINKS)) {
			Set<ChipLocation> blacklistedChips = blChips.call(boardId)
					.map(chip("x", "y")).toSet();
			Map<ChipLocation, Set<Integer>> blacklistedCores = blCores
					.call(boardId).toCollectingMap(HashSet::new,
							chip("x", "y"), integer("p"));
			Map<ChipLocation, Set<Direction>> blacklistedLinks = blLinks
					.call(boardId).toCollectingMap(
							() -> noneOf(Direction.class), chip("x", "y"),
							enumerate("direction", Direction.class));

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
	public void writeBlacklistToDB(Integer boardId, Blacklist blacklist) {
		requireNonNull(blacklist);
		execute(conn -> {
			writeBlacklistToDB(conn, boardId, blacklist);
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
	 * @deprecated Call via {@link #writeBlacklistToDB(Integer, Blacklist)} if
	 *             not in test code.
	 */
	@Deprecated
	void writeBlacklistToDB(Connection conn, Integer boardId,
			Blacklist blacklist) {
		try (Update clearChips = conn.update(CLEAR_BLACKLISTED_CHIPS);
				Update clearCores = conn.update(CLEAR_BLACKLISTED_CORES);
				Update clearLinks = conn.update(CLEAR_BLACKLISTED_LINKS);
				Update addChip = conn.update(ADD_BLACKLISTED_CHIP);
				Update addCore = conn.update(ADD_BLACKLISTED_CORE);
				Update addLink = conn.update(ADD_BLACKLISTED_LINK)) {
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
}
