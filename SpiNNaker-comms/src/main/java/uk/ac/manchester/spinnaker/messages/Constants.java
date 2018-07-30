package uk.ac.manchester.spinnaker.messages;

/** Miscellaneous SpiNNaker constants. */
public interface Constants {
	/** The max size a UDP packet can be. */
	int UDP_MESSAGE_MAX_SIZE = 256;
	/** The default port of the connection. */
	int SCP_SCAMP_PORT = 17893;
	/** The default port of the connection. */
	int UDP_BOOT_CONNECTION_DEFAULT_PORT = 54321;
	/** The base address of the system variable structure in system RAM. */
	int SYSTEM_VARIABLE_BASE_ADDRESS = 0xf5007f00;
	/** The base address of a routers diagnostic filter controls. */
	int ROUTER_REGISTER_BASE_ADDRESS = 0xe1000000;
	/** The base address of a routers p2p routing table. */
	int ROUTER_REGISTER_P2P_ADDRESS = ROUTER_REGISTER_BASE_ADDRESS + 0x10000;
	/** Offset for the router filter controls first register (one word each). */
	int ROUTER_FILTER_CONTROLS_OFFSET = 0x200;
	/**
	 * Point where default filters finish and user set-able ones are available.
	 */
	int ROUTER_DEFAULT_FILTERS_MAX_POSITION = 11;
	/** The size of a router diagnostic filter control register in bytes. */
	int ROUTER_DIAGNOSTIC_FILTER_SIZE = 4;
	/** Number of router diagnostic filters. */
	int NO_ROUTER_DIAGNOSTIC_FILTERS = 16;
	/** The size of the system variable structure in bytes. */
	int SYSTEM_VARIABLE_BYTES = 256;
	/** The amount of size in bytes that the EIEIO command header is. */
	int EIEIO_COMMAND_HEADER_SIZE = 3;
	/** The amount of size in bytes the EIEIO data header is. */
	int EIEIO_DATA_HEADER_SIZE = 2;
	/** The address of the start of the VCPU structure (copied from sark.h). */
	int CPU_INFO_OFFSET = 0xe5007000;
	/** How many bytes the CPU info data takes up. */
	int CPU_INFO_BYTES = 128;
	/** The address at which user0 register starts. */
	int CPU_USER_0_START_ADDRESS = 112;
	/** The address at which user1 register starts. */
	int CPU_USER_1_START_ADDRESS = 116;
	/** The address at which user2 register starts. */
	int CPU_USER_2_START_ADDRESS = 120;
	/** The address at which the iobuf address starts. */
	int CPU_IOBUF_ADDRESS_OFFSET = 88;
	/** default UDP tag. */
	int DEFAULT_SDP_TAG = 0xFF;
	/** max user requested tag value. */
	int MAX_TAG_ID = 7;
	/** The range of values the BMP's 12-bit ADCs can measure. */
	int BMP_ADC_MAX = 1 << 12;
	/**
	 * Multiplier to convert from ADC value to volts for lines less than 2.5 V.
	 */
	double BMP_V_SCALE_2_5 = 2.5 / BMP_ADC_MAX;
	/** Multiplier to convert from ADC value to volts for 3.3 V lines. */
	double BMP_V_SCALE_3_3 = 3.75 / BMP_ADC_MAX;
	/** Multiplier to convert from ADC value to volts for 12 V lines. */
	double BMP_V_SCALE_12 = 15.0 / BMP_ADC_MAX;
	/**
	 * Multiplier to convert from temperature probe values to degrees Celsius.
	 */
	double BMP_TEMP_SCALE = 1.0 / 256.0;
	/** Temperature value returned when a probe is not connected. */
	int BMP_MISSING_TEMP = -0x8000;
	/** Fan speed value returned when a fan is absent. */
	int BMP_MISSING_FAN = -1;
	/** Timeout for BMP power-on commands to reply. */
	double BMP_POWER_ON_TIMEOUT = 10.0;
	/** Timeout for other BMP commands to reply (in seconds). */
	double BMP_TIMEOUT = 0.5;
	/** Time to sleep after powering on boards (in seconds). */
	double BMP_POST_POWER_ON_SLEEP_TIME = 5.0;
	/** number of chips to check are booted fully from the middle. */
	int NO_MIDDLE_CHIPS_TO_CHECK = 8;
	/** This is the default timeout when using SCP, in milliseconds. */
	int SCP_TIMEOUT = 1000;
}
