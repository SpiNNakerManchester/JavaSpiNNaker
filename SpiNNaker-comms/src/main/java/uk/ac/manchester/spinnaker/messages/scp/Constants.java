package uk.ac.manchester.spinnaker.messages.scp;

/**
 * Constants used elsewhere in this package only.
 *
 * @author Donal Fellows
 */
abstract class Constants {
	private Constants() {
	}

	/**
	 * Indicates that all cores should receive a signal.
	 */
	static final int ALL_CORE_SIGNAL_MASK = 0xFFFF;
}
