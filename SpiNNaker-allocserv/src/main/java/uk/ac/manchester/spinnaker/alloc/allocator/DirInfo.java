/*
 * Copyright (c) 2021 The University of Manchester
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
package uk.ac.manchester.spinnaker.alloc.allocator;

import static java.util.Objects.requireNonNull;
import static org.slf4j.LoggerFactory.getLogger;
import static uk.ac.manchester.spinnaker.alloc.DatabaseEngine.query;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;

import uk.ac.manchester.spinnaker.alloc.DatabaseEngine.Query;
import uk.ac.manchester.spinnaker.alloc.DatabaseEngine.Row;
import uk.ac.manchester.spinnaker.alloc.SQLQueries;

/**
 * A mapping that says how to go from one board's coordinates (only the Z
 * coordinate matters for this) to another when you move in a particular
 * direction.
 *
 * <pre>
 *  ___     ___     ___     ___
 * / . \___/ . \___/ . \___/ . \___
 * \___/ . \___/ . \___/ . \___/ . \
 * /0,1\___/1,1\___/2,1\___/3,1\___/
 * \___/ . \___/ . \___/ . \___/ . \___
 *     \_2_/ . \___/ . \___/ . \___/ . \
 *     /0,0\_1_/1,0\___/2,0\___/3,0\___/
 *     \_0_/   \___/   \___/   \___/
 * </pre>
 *
 * Bear in mind that 0,1,0 is <em>actually</em> 12 chips vertically and 0 chips
 * horizontally offset from 0,0,0; the hexagons are actually a distorted shape.
 * This is closer:
 *
 * <pre>
 *    __     __     __     __
 *   /  |   /  |   /  |   /  |
 *  /   |__/   |__/   |__/   |__
 *  | 2 /  | 2 /  | 2 /  | 2 /  |
 *  |__/   |__/   |__/   |__/   |
 *  /  | 1 /  | 1 /  | 1 /  | 1 /
 * /0,1|__/1,1|__/2,1|__/3,1|__/
 * | 0 /  | 0 /  | 0 /  | 0 /  |
 * |__/   |__/   |__/   |__/   |__
 *    | 2 /  | 2 /  | 2 /  | 2 /  |
 *    |__/   |__/   |__/   |__/   |
 *    /  | 1 /  | 1 /  | 1 /  | 1 /
 *   /0,0|__/1,0|__/2,0|__/3,0|__/
 *   | 0 /  | 0 /  | 0 /  | 0 /
 *   |__/   |__/   |__/   |__/
 * </pre>
 *
 * @author Donal Fellows
 */
public final class DirInfo extends SQLQueries {
	private static final Logger log = getLogger(DirInfo.class);

	private static final Map<Integer, Map<Direction, DirInfo>> MAP =
			new HashMap<>();

	/**
	 * When your Z coordinate is this.
	 */
	public final int z;

	/** When you are moving in this direction. */
	public final Direction dir;

	/** Change your X coordinate by this. */
	public final int dx;

	/** Change your Y coordinate by this. */
	public final int dy;

	/** Change your Z coordinate by this. */
	public final int dz;

	private DirInfo(int z, Direction d, int dx, int dy, int dz) {
		this.z = z;
		this.dir = requireNonNull(d);
		this.dx = dx;
		this.dy = dy;
		this.dz = dz;

		MAP.computeIfAbsent(z, key -> new HashMap<>()).put(d, this);
	}

	/**
	 * Obtain the correct motion information given a starting point and a
	 * direction.
	 *
	 * @param z
	 *            The starting Z coordinate. (Motions are independent of X and
	 *            Y.) Must be in range {@code 0..2}.
	 * @param direction
	 *            The direction to move in.
	 * @return How to move.
	 */
	public static DirInfo get(int z, Direction direction) {
		return MAP.get(z).get(direction);
	}

	// TODO provide a method that manipulates a triad coordinate tuple

	@Override
	public boolean equals(Object o) {
		if (!(o instanceof DirInfo)) {
			return false;
		}
		DirInfo di = (DirInfo) o;
		return z == di.z && dir == di.dir;
	}

	@Override
	public int hashCode() {
		return z ^ dir.hashCode();
	}

	static void load(Connection conn) throws SQLException {
		if (MAP.isEmpty()) {
			try (Query di = query(conn, LOAD_DIR_INFO)) {
				for (Row row : di.call()) {
					new DirInfo(row.getInt("z"),
							row.getEnum("direction", Direction.class),
							row.getInt("dx"), row.getInt("dy"),
							row.getInt("dz"));
				}
			}
			log.debug("created {} DirInfo instances",
					MAP.values().stream().mapToInt(Map::size).sum());
		}
	}
}
