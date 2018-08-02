package uk.ac.manchester.spinnaker.messages.model;

import static uk.ac.manchester.spinnaker.messages.model.DataType.BYTE;
import static uk.ac.manchester.spinnaker.messages.model.DataType.BYTE_ARRAY;
import static uk.ac.manchester.spinnaker.messages.model.DataType.INT;
import static uk.ac.manchester.spinnaker.messages.model.DataType.LONG;
import static uk.ac.manchester.spinnaker.messages.model.DataType.SHORT;
import static uk.ac.manchester.spinnaker.messages.model.SVDConstants.IP_ADDR_WIDTH;
import static uk.ac.manchester.spinnaker.messages.model.SVDConstants.PER_CORE_WIDTH;

import java.nio.ByteBuffer;

/** Defines the system variables available. */
public enum SystemVariableDefinition {
	/** The y-coordinate of the chip. */
	y(BYTE, 0),
	/** The x-coordinate of the chip. */
	x(BYTE, 0x01),
	/** The number of chips in the y-dimension. */
	y_size(BYTE, 0x02),
	/** The number of chips in the x-dimension. */
	x_size(BYTE, 0x03),
	/** The y-coordinate of the chip to send debug messages to. */
	debug_y(BYTE, 0x04),
	/** The x-coordinate of the chip to send debug messages to. */
	debug_x(BYTE, 0x05),
	/** Indicates if peer-to-peer is working on the chip. */
	is_peer_to_peer_available(BYTE, 0x06),
	/** The last ID used in nearest neighbour transaction. */
	nearest_neighbour_last_id(BYTE, 0x07),
	/** The x-coordinate of the nearest chip with Ethernet. */
	nearest_ethernet_y(BYTE, 0x08),
	/** The y-coordinate of the nearest chip with Ethernet. */
	nearest_ethernet_x(BYTE, 0x09),
	/** The version of the hardware in use. */
	hardware_version(BYTE, 0x0a),
	/** Indicates if Ethernet is available on this chip. */
	is_ethernet_available(BYTE, 0x0b),
	/** Number of times to send out P2PB packets. */
	p2p_b_repeats(BYTE, 0x0c, 4),
	/** Log (base 2) of the peer-to-peer sequence length. */
	log_peer_to_peer_sequence_length(BYTE, 0x0d, 4),
	/** The clock divisors for system &amp; router clocks. */
	clock_divisor(BYTE, 0x0e, 0x33),
	/** The time-phase scaling factor. */
	time_phase_scale(BYTE, 0x0f),
	/** The time since startup in milliseconds. */
	clock_milliseconds(LONG, 0x10),
	/** The number of milliseconds in the current second. */
	time_milliseconds(SHORT, 0x18),
	/**
	 * Local time phase control packet send period (in units of 10ms). Set to
	 * zero to disable.
	 *
	 * @deprecated UNTESTED part of SCAMP
	 */
	ltpc_period(SHORT, 0x1a),
	/** The time in seconds since midnight, 1st January 1970. */
	unix_timestamp(INT, 0x1c),
	/** The router time-phase timer. */
	router_time_phase_timer(INT, 0x20),
	/** The CPU clock frequency in MHz. */
	cpu_clock_mhz(SHORT, 0x24, 200),
	/** The SDRAM clock frequency in MHz. */
	sdram_clock_frequency_mhz(SHORT, 0x26, 130),
	/** Nearest-Neighbour forward parameter. */
	nearest_neighbour_forward(BYTE, 0x28, 0x3F),
	/** Nearest-Neighbour retry parameter. */
	nearest_neighbour_retry(BYTE, 0x29),
	/** The link peek/poke timeout in microseconds. */
	link_peek_timeout_microseconds(BYTE, 0x2a, 100),
	/** The LED half-period in 10 ms units, or 1 to show load. */
	led_half_period_10_ms(BYTE, 0x2b, 1),
	/**
	 * The time to wait after last BC during network initialisation, in 10 ms
	 * units.
	 */
	netinit_bc_wait_time(BYTE, 0x2c, 50),
	/** The phase of boot process (see enum netinit_phase_e). */
	netinit_phase(BYTE, 0x2d),
	/** The y-coordinate of the chip from which the system was booted. */
	p2p_root_y(BYTE, 0x2e),
	/** The x-coordinate of the chip from which the system was booted. */
	p2p_root_x(BYTE, 0x2f),
	/** The first part of the LED definitions. */
	led_0(INT, 0x30, 0x1),
	/** The last part of the LED definitions. */
	led_1(INT, 0x34),
	/** A word of padding. */
	@Deprecated
	padding_1(INT, 0x38),
	/** The random seed. */
	random_seed(INT, 0x3c),
	/** Indicates if this is the root chip. */
	is_root_chip(BYTE, 0x40),
	/** The number of shared message buffers. */
	n_shared_message_buffers(BYTE, 0x41, 7),
	/** The delay between nearest-neighbour packets in microseconds. */
	nearest_neighbour_delay_us(BYTE, 0x42, 20),
	/** The number of watch dog timeouts before an error is raised. */
	software_watchdog_count(BYTE, 0x43),
	/** A word of padding. */
	@Deprecated
	padding_2(INT, 0x44),
	/** The base address of the system SDRAM heap. */
	system_ram_heap_address(INT, 0x48, 1024),
	/** The base address of the user SDRAM heap. */
	sdram_heap_address(INT, 0x4c, 0),
	/** The size of the iobuf buffer in bytes. */
	iobuf_size(INT, 0x50, 16384),
	/** The size of the system SDRAM in bytes. */
	system_sdram_bytes(INT, 0x54, 8388608),
	/** The size of the system buffer in words. */
	system_buffer_words(INT, 0x58, 32768),
	/** The boot signature. */
	boot_signature(INT, 0x5c),
	/** The memory pointer for nearest neighbour global operations. */
	nearest_neighbour_memory_pointer(INT, 0x60),
	/** The lock. */
	lock(BYTE, 0x64),
	/** Bit mask (6 bits) of links enabled. */
	links_available(BYTE, 0x65, 0x3f),
	/** Last ID used in BIFF packet. */
	last_biff_id(BYTE, 0x66),
	/** Board testing flags. */
	board_test_flags(BYTE, 0x67),
	/** Pointer to the first free shared message buffer. */
	shared_message_first_free_address(INT, 0x68),
	/** The number of shared message buffers in use. */
	shared_message_count_in_use(SHORT, 0x6c),
	/** The maximum number of shared message buffers used. */
	shared_message_maximum_used(SHORT, 0x6e),
	/** The first user variable. */
	user_temp_0(INT, 0x70),
	/** The second user variable. */
	user_temp_1(INT, 0x74),
	/** The third user variable. */
	user_temp_2(INT, 0x78),
	/** The fourth user variable. */
	user_temp_4(INT, 0x7c),
	/** The status map set during SCAMP boot. */
	status_map(BYTE_ARRAY, 0x80, new byte[PER_CORE_WIDTH]),
	/** The physical core ID to virtual core ID map. */
	physical_to_virtual_core_map(BYTE_ARRAY, 0x94, new byte[PER_CORE_WIDTH]),
	/** The virtual core ID to physical core ID map. */
	virtual_to_physical_core_map(BYTE_ARRAY, 0xa8, new byte[PER_CORE_WIDTH]),
	/** The number of working cores. */
	n_working_cores(BYTE, 0xbc),
	/** The number of SCAMP working cores. */
	n_scamp_working_cores(BYTE, 0xbd),
	/** A short of padding. */
	@Deprecated
	padding_3(SHORT, 0xbe),
	/** The base address of SDRAM. */
	sdram_base_address(INT, 0xc0),
	/** The base address of System RAM. */
	system_ram_base_address(INT, 0xc4),
	/** The base address of System SDRAM. */
	system_sdram_base_address(INT, 0xc8),
	/** The base address of the CPU information blocks. */
	cpu_information_base_address(INT, 0xcc),
	/** The base address of the system SDRAM heap. */
	system_sdram_heap_address(INT, 0xd0),
	/** The address of the copy of the routing tables. */
	router_table_copy_address(INT, 0xd4),
	/** The address of the peer-to-peer hop tables. */
	peer_to_peer_hop_table_address(INT, 0xd8),
	/** The address of the allocated tag table. */
	allocated_tag_table_address(INT, 0xdc),
	/** The ID of the first free router entry. */
	first_free_router_entry(SHORT, 0xe0),
	/** The number of active peer-to-peer addresses. */
	n_active_peer_to_peer_addresses(SHORT, 0xe2),
	/** The address of the application data table. */
	app_data_table_address(INT, 0xe4),
	/** The address of the shared message buffers. */
	shared_message_buffer_address(INT, 0xe8),
	/** The monitor incoming mailbox flags. */
	monitor_mailbox_flags(INT, 0xec),
	/** The IP address of the chip. */
	ethernet_ip_address(BYTE_ARRAY, 0xf0, new byte[IP_ADDR_WIDTH]),
	/** A (virtual) copy of the router FR register. */
	fixed_route_copy(INT, 0xf4),
	/** A pointer to the board information structure. */
	board_info(INT, 0xf8),
	/** A word of padding. */
	@Deprecated
	padding_4(INT, 0xfc);

	/** The data type of the variable. */
	public final DataType type;
	/**
	 * The offset from the start of the system variable structure where the
	 * variable is found.
	 */
	public final int offset;
	/**
	 * The default value assigned to the variable if not overridden; this can be
	 * an integer or a byte array.
	 */
	private final Object def;

	SystemVariableDefinition(DataType type, int offset) {
		this.type = type;
		this.offset = offset;
		this.def = 0;
	}

	SystemVariableDefinition(DataType type, int offset, Object def) {
		this.type = type;
		this.offset = offset;
		this.def = def;
	}

	/**
	 * The default value assigned to the variable if not overridden; this can be
	 * an integer or a byte array.
	 *
	 * @return The default value, or a copy of it if the type of the value is an
	 *         array.
	 */
	public Object getDefault() {
		if (type == BYTE_ARRAY) {
			return ((byte[]) def).clone();
		}
		return def;
	}

	/**
	 * Writes an object described by this field into the given buffer at the
	 * <i>position</i> as a contiguous range of bytes. This assumes that the
	 * buffer has been configured to be
	 * {@linkplain java.nio.ByteOrder#LITTLE_ENDIAN little-endian} and that its
	 * <i>position</i> is at the point where this method should begin writing.
	 * Once it has finished, the <i>position</i> will be immediately after the
	 * last byte written by this method.
	 *
	 * @param value
	 *            The value to write.
	 * @param buffer
	 *            The buffer to write into.
	 */
	public void addToBuffer(Object value, ByteBuffer buffer) {
		type.addToBuffer(value, buffer);
	}
}

/** Just some constants for {@link SystemVariableDefinition}. */
abstract class SVDConstants {
	private SVDConstants() {
	}

	/**
	 * Width of arrays that have an element per core.
	 */
	static final int PER_CORE_WIDTH = 20;
	/**
	 * Width of arrays that hold an IPv4 address.
	 */
	static final int IP_ADDR_WIDTH = 4;
}
