package uk.ac.manchester.spinnaker.messages;

import uk.ac.manchester.spinnaker.utils.UnitConstants;

/** Miscellaneous SpiNNaker constants. */
public abstract class Constants {
	/** The max size a UDP packet can be. */
	public static final int UDP_MESSAGE_MAX_SIZE = 256;
	/** The default port of the connection. */
	public static final int SCP_SCAMP_PORT = 17893;
	/** The default port of the connection. */
	public static final int UDP_BOOT_CONNECTION_DEFAULT_PORT = 54321;
	/** The base address of the system variable structure in system RAM. */
	public static final int SYSTEM_VARIABLE_BASE_ADDRESS = 0xf5007f00;
	/** The base address of a routers diagnostic filter controls. */
	public static final int ROUTER_REGISTER_BASE_ADDRESS = 0xe1000000;
	/** The base address of a routers p2p routing table. */
	public static final int ROUTER_REGISTER_P2P_ADDRESS =
			ROUTER_REGISTER_BASE_ADDRESS + 0x10000;
	/** Offset for the router filter controls first register (one word each). */
	public static final int ROUTER_FILTER_CONTROLS_OFFSET = 0x200;
	/**
	 * Point where default filters finish and user set-able ones are available.
	 */
	public static final int ROUTER_DEFAULT_FILTERS_MAX_POSITION = 11;
	/** The size of a router diagnostic filter control register in bytes. */
	public static final int ROUTER_DIAGNOSTIC_FILTER_SIZE = 4;
	/** Number of router diagnostic filters. */
	public static final int NO_ROUTER_DIAGNOSTIC_FILTERS = 16;
	/** The size of the system variable structure in bytes. */
	public static final int SYSTEM_VARIABLE_BYTES = 256;
	/** The amount of size in bytes that the EIEIO command header is. */
	public static final int EIEIO_COMMAND_HEADER_SIZE = 3;
	/** The amount of size in bytes the EIEIO data header is. */
	public static final int EIEIO_DATA_HEADER_SIZE = 2;
	/** The address of the start of the VCPU structure (copied from sark.h). */
	public static final int CPU_INFO_OFFSET = 0xe5007000;
	/** How many bytes the CPU info data takes up. */
	public static final int CPU_INFO_BYTES = 128;
	/** The address at which user0 register starts. */
	public static final int CPU_USER_0_START_ADDRESS = 112;
	/** The address at which user1 register starts. */
	public static final int CPU_USER_1_START_ADDRESS = 116;
	/** The address at which user2 register starts. */
	public static final int CPU_USER_2_START_ADDRESS = 120;
	/** The address at which the iobuf address starts. */
	public static final int CPU_IOBUF_ADDRESS_OFFSET = 88;
	/** default UDP tag. */
	public static final int DEFAULT_SDP_TAG = 0xFF;
	/** max user requested tag value. */
	public static final int MAX_TAG_ID = 7;
	private static final int BMP_ADC_RANGE_BITS = 12;
	/** The range of values the BMP's 12-bit ADCs can measure. */
	public static final int BMP_ADC_MAX = 1 << BMP_ADC_RANGE_BITS;
	/**
	 * Multiplier to convert from ADC value to volts for lines less than 2.5 V.
	 */
	public static final double BMP_V_SCALE_2_5 = 2.5 / BMP_ADC_MAX;
	/** Multiplier to convert from ADC value to volts for 3.3 V lines. */
	public static final double BMP_V_SCALE_3_3 = 3.75 / BMP_ADC_MAX;
	/** Multiplier to convert from ADC value to volts for 12 V lines. */
	public static final double BMP_V_SCALE_12 = 15.0 / BMP_ADC_MAX;
	/**
	 * Multiplier to convert from temperature probe values to degrees Celsius.
	 */
	public static final double BMP_TEMP_SCALE = 1.0 / 256.0;
	/** Temperature value returned when a probe is not connected. */
	public static final int BMP_MISSING_TEMP = -0x8000;
	/** Fan speed value returned when a fan is absent. */
	public static final int BMP_MISSING_FAN = -1;
	/** Timeout for BMP power-on commands to reply. */
	public static final double BMP_POWER_ON_TIMEOUT = 10.0;
	/** Timeout for other BMP commands to reply (in seconds). */
	public static final double BMP_TIMEOUT = 0.5;
	/** Time to sleep after powering on boards (in seconds). */
	public static final double BMP_POST_POWER_ON_SLEEP_TIME = 5.0;
	/** number of chips to check are booted fully from the middle. */
	public static final int NO_MIDDLE_CHIPS_TO_CHECK = 8;
	/** This is the default timeout when using SCP, in milliseconds. */
	public static final int SCP_TIMEOUT = 1000;

	/** Number of bytes in a SpiNNaker word. */
	public static final int WORD_SIZE = 4;
	/** Number of bytes in a SpiNNaker half-word. */
	public static final int SHORT_SIZE = 2;
	/** ms per second. */
	public static final double MS_PER_S = UnitConstants.MS_PER_S;
	/** Number of bytes in an IPv4 address. */
	public static final int IPV4_SIZE = 4;
}
