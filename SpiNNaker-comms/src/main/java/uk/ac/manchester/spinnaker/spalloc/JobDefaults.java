package uk.ac.manchester.spinnaker.spalloc;

abstract class JobDefaults {
	private JobDefaults() {
	}

	static final int PORT_DEFAULT = 22244;
	static final double KEEPALIVE_DEFAULT = 60.0;
	static final double RECONNECT_DELAY_DEFAULT = 5.0;
	static final double TIMEOUT_DEFAULT = 5.0;
	static final String MACHINE_DEFAULT = null;
	static final String TAGS_DEFAULT = null;
	static final double MIN_RATIO_DEFAULT = 0.333;
	static final int MAX_DEAD_BOARDS_DEFAULT = 0;
	static final Integer MAX_DEAD_LINKS_DEFAULT = null;
	static final boolean REQUIRE_TORUS_DEFAULT = false;
}
