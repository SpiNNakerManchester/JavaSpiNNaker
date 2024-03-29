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

	/** Name of property/parameter. */
	public static final String GROUP_PROPERTY = "group";

	/** Name of property/parameter. */
	public static final String COLLAB_PROPERTY = "nmpi_collab";

	/** Name of property/parameter. */
	public static final String NMPI_JOB_PROPERTY = "nmpi_job_id";
}
