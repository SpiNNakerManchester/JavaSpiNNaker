/*
 * Copyright (c) 2021 The University of Manchester
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
package uk.ac.manchester.spinnaker.alloc.admin;

import static java.util.Objects.requireNonNull;
import static org.slf4j.LoggerFactory.getLogger;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;

import uk.ac.manchester.spinnaker.alloc.db.DatabaseAPI.Connection;
import uk.ac.manchester.spinnaker.alloc.db.SQLQueries;
import uk.ac.manchester.spinnaker.alloc.model.Direction;
import uk.ac.manchester.spinnaker.machine.board.ValidTriadZ;

/**
 * A mapping that says how to go from one board's coordinates (only the Z
 * coordinate matters for this) to another when you move in a particular
 * direction. Comes from the {@code movement_directions} table in the database.
 * <p>
 * Consider this board layout (a classic 24 board machine, with wrap-arounds not
 * shown):
 * <p>
 * <img src="doc-files/DirInfo1.png" width="450" alt="24-board layout">
 * <p>
 * Bear in mind that 0,1,0 is <em>actually</em> 12 chips vertically and 0 chips
 * horizontally offset from 0,0,0. (Also, the real boards are slightly offset
 * from this layout.)
 *
 * @author Donal Fellows
 * @see Direction
 * @see MachineDefinitionLoader
 */
public final class DirInfo extends SQLQueries {
	private static final Logger log = getLogger(DirInfo.class);

	private static final Map<Integer, EnumMap<Direction, DirInfo>> MAP =
			new HashMap<>();

	/**
	 * When your Z coordinate is this.
	 */
	@ValidTriadZ
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

		MAP.computeIfAbsent(z, __ -> new EnumMap<>(Direction.class)).put(d,
				this);
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

	@Override
	public boolean equals(Object o) {
		return (o instanceof DirInfo di) && (z == di.z) && (dir == di.dir);
	}

	@Override
	public int hashCode() {
		return z ^ dir.hashCode();
	}

	static void load(Connection conn) {
		if (MAP.isEmpty()) {
			conn.transaction(false, () -> {
				try (var di = conn.query(LOAD_DIR_INFO)) {
					di.call(row -> new DirInfo(row.getInt("z"),
							row.getEnum("direction", Direction.class),
							row.getInt("dx"), row.getInt("dy"),
							row.getInt("dz")));
				}
			});
			log.debug("created {} DirInfo instances",
					MAP.values().stream().mapToInt(Map::size).sum());
		}
	}
}
