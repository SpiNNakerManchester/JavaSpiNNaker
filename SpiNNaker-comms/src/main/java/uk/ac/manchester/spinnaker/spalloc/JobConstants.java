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
package uk.ac.manchester.spinnaker.spalloc;

/**
 * Default values for various configuration file options.
 *
 * @author Donal Fellows
 */
public abstract class JobConstants {
	private JobConstants() {
	}

	/** Default value for "port". */
	public static final int PORT_DEFAULT = 22244;
	/** Default value for "keepalive". */
	public static final double KEEPALIVE_DEFAULT = 60.0;
	/** Default value for "reconnect_delay". */
	public static final double RECONNECT_DELAY_DEFAULT = 5.0;
	/** Default value for "timeout". */
	public static final double TIMEOUT_DEFAULT = 5.0;
	/** Default value for "machine". */
	public static final String MACHINE_DEFAULT = null;
	/** Default value for "tags". */
	public static final String TAGS_DEFAULT = null;
	/** Default value for "min_ratio". */
	public static final double MIN_RATIO_DEFAULT = 0.333;
	/** Default value for "max_dead_boards". */
	public static final int MAX_DEAD_BOARDS_DEFAULT = 0;
	/** Default value for "max_dead_links". */
	public static final Integer MAX_DEAD_LINKS_DEFAULT = null;
	/** Default value for "require_torus". */
	static final boolean REQUIRE_TORUS_DEFAULT = false;

	/** Name of property/parameter. */
	public static final String HOSTNAME_PROPERTY = "hostname";
	/** Name of property/parameter. */
	public static final String PORT_PROPERTY = "port";
	/** Name of property/parameter. */
	public static final String USER_PROPERTY = "owner";
	/** Name of property/parameter. */
	public static final String KEEPALIVE_PROPERTY = "keepalive";
	/** Name of property/parameter. */
	public static final String RECONNECT_DELAY_PROPERTY = "reconnect_delay";
	/** Name of property/parameter. */
	public static final String MACHINE_PROPERTY = "machine";
	/** Name of property/parameter. */
	public static final String TAGS_PROPERTY = "tags";
	/** Name of property/parameter. */
	public static final String MIN_RATIO_PROPERTY = "min_ratio";
	/** Name of property/parameter. */
	public static final String MAX_DEAD_BOARDS_PROPERTY = "max_dead_boards";
	/** Name of property/parameter. */
	public static final String MAX_DEAD_LINKS_PROPERTY = "max_dead_links";
	/** Name of property/parameter. */
	public static final String REQUIRE_TORUS_PROPERTY = "require_torus";
	/** Name of property/parameter. */
	public static final String TIMEOUT_PROPERTY = "timeout";
}
