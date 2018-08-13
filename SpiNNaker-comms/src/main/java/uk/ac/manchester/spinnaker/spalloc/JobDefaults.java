package uk.ac.manchester.spinnaker.spalloc;

/**
 * Default values for various configuration file options.
 * @author Donal Fellows
 */
public abstract class JobDefaults {
	private JobDefaults() {
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
}
