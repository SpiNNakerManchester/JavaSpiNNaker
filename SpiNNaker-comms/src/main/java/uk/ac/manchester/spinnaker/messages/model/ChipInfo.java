package uk.ac.manchester.spinnaker.messages.model;

import static java.lang.String.format;
import static java.util.Collections.sort;
import static java.util.Collections.unmodifiableList;
import static uk.ac.manchester.spinnaker.messages.model.DataType.BYTE_ARRAY;
import static uk.ac.manchester.spinnaker.messages.model.DataType.LONG;
import static uk.ac.manchester.spinnaker.messages.model.SystemVariableDefinition.allocated_tag_table_address;
import static uk.ac.manchester.spinnaker.messages.model.SystemVariableDefinition.app_data_table_address;
import static uk.ac.manchester.spinnaker.messages.model.SystemVariableDefinition.board_info;
import static uk.ac.manchester.spinnaker.messages.model.SystemVariableDefinition.board_test_flags;
import static uk.ac.manchester.spinnaker.messages.model.SystemVariableDefinition.boot_signature;
import static uk.ac.manchester.spinnaker.messages.model.SystemVariableDefinition.clock_divisor;
import static uk.ac.manchester.spinnaker.messages.model.SystemVariableDefinition.clock_milliseconds;
import static uk.ac.manchester.spinnaker.messages.model.SystemVariableDefinition.cpu_clock_mhz;
import static uk.ac.manchester.spinnaker.messages.model.SystemVariableDefinition.cpu_information_base_address;
import static uk.ac.manchester.spinnaker.messages.model.SystemVariableDefinition.debug_x;
import static uk.ac.manchester.spinnaker.messages.model.SystemVariableDefinition.debug_y;
import static uk.ac.manchester.spinnaker.messages.model.SystemVariableDefinition.ethernet_ip_address;
import static uk.ac.manchester.spinnaker.messages.model.SystemVariableDefinition.first_free_router_entry;
import static uk.ac.manchester.spinnaker.messages.model.SystemVariableDefinition.fixed_route_copy;
import static uk.ac.manchester.spinnaker.messages.model.SystemVariableDefinition.hardware_version;
import static uk.ac.manchester.spinnaker.messages.model.SystemVariableDefinition.iobuf_size;
import static uk.ac.manchester.spinnaker.messages.model.SystemVariableDefinition.is_ethernet_available;
import static uk.ac.manchester.spinnaker.messages.model.SystemVariableDefinition.is_peer_to_peer_available;
import static uk.ac.manchester.spinnaker.messages.model.SystemVariableDefinition.is_root_chip;
import static uk.ac.manchester.spinnaker.messages.model.SystemVariableDefinition.last_biff_id;
import static uk.ac.manchester.spinnaker.messages.model.SystemVariableDefinition.led_0;
import static uk.ac.manchester.spinnaker.messages.model.SystemVariableDefinition.led_1;
import static uk.ac.manchester.spinnaker.messages.model.SystemVariableDefinition.led_half_period_10_ms;
import static uk.ac.manchester.spinnaker.messages.model.SystemVariableDefinition.link_peek_timeout_microseconds;
import static uk.ac.manchester.spinnaker.messages.model.SystemVariableDefinition.links_available;
import static uk.ac.manchester.spinnaker.messages.model.SystemVariableDefinition.lock;
import static uk.ac.manchester.spinnaker.messages.model.SystemVariableDefinition.log_peer_to_peer_sequence_length;
import static uk.ac.manchester.spinnaker.messages.model.SystemVariableDefinition.monitor_mailbox_flags;
import static uk.ac.manchester.spinnaker.messages.model.SystemVariableDefinition.n_active_peer_to_peer_addresses;
import static uk.ac.manchester.spinnaker.messages.model.SystemVariableDefinition.n_scamp_working_cores;
import static uk.ac.manchester.spinnaker.messages.model.SystemVariableDefinition.n_shared_message_buffers;
import static uk.ac.manchester.spinnaker.messages.model.SystemVariableDefinition.n_working_cores;
import static uk.ac.manchester.spinnaker.messages.model.SystemVariableDefinition.nearest_ethernet_x;
import static uk.ac.manchester.spinnaker.messages.model.SystemVariableDefinition.nearest_ethernet_y;
import static uk.ac.manchester.spinnaker.messages.model.SystemVariableDefinition.nearest_neighbour_delay_us;
import static uk.ac.manchester.spinnaker.messages.model.SystemVariableDefinition.nearest_neighbour_forward;
import static uk.ac.manchester.spinnaker.messages.model.SystemVariableDefinition.nearest_neighbour_last_id;
import static uk.ac.manchester.spinnaker.messages.model.SystemVariableDefinition.nearest_neighbour_memory_pointer;
import static uk.ac.manchester.spinnaker.messages.model.SystemVariableDefinition.nearest_neighbour_retry;
import static uk.ac.manchester.spinnaker.messages.model.SystemVariableDefinition.netinit_bc_wait_time;
import static uk.ac.manchester.spinnaker.messages.model.SystemVariableDefinition.netinit_phase;
import static uk.ac.manchester.spinnaker.messages.model.SystemVariableDefinition.p2p_b_repeats;
import static uk.ac.manchester.spinnaker.messages.model.SystemVariableDefinition.p2p_root_x;
import static uk.ac.manchester.spinnaker.messages.model.SystemVariableDefinition.p2p_root_y;
import static uk.ac.manchester.spinnaker.messages.model.SystemVariableDefinition.peer_to_peer_hop_table_address;
import static uk.ac.manchester.spinnaker.messages.model.SystemVariableDefinition.physical_to_virtual_core_map;
import static uk.ac.manchester.spinnaker.messages.model.SystemVariableDefinition.random_seed;
import static uk.ac.manchester.spinnaker.messages.model.SystemVariableDefinition.router_table_copy_address;
import static uk.ac.manchester.spinnaker.messages.model.SystemVariableDefinition.router_time_phase_timer;
import static uk.ac.manchester.spinnaker.messages.model.SystemVariableDefinition.sdram_base_address;
import static uk.ac.manchester.spinnaker.messages.model.SystemVariableDefinition.sdram_clock_frequency_mhz;
import static uk.ac.manchester.spinnaker.messages.model.SystemVariableDefinition.sdram_heap_address;
import static uk.ac.manchester.spinnaker.messages.model.SystemVariableDefinition.shared_message_buffer_address;
import static uk.ac.manchester.spinnaker.messages.model.SystemVariableDefinition.shared_message_count_in_use;
import static uk.ac.manchester.spinnaker.messages.model.SystemVariableDefinition.shared_message_first_free_address;
import static uk.ac.manchester.spinnaker.messages.model.SystemVariableDefinition.shared_message_maximum_used;
import static uk.ac.manchester.spinnaker.messages.model.SystemVariableDefinition.software_watchdog_count;
import static uk.ac.manchester.spinnaker.messages.model.SystemVariableDefinition.status_map;
import static uk.ac.manchester.spinnaker.messages.model.SystemVariableDefinition.system_buffer_words;
import static uk.ac.manchester.spinnaker.messages.model.SystemVariableDefinition.system_ram_base_address;
import static uk.ac.manchester.spinnaker.messages.model.SystemVariableDefinition.system_ram_heap_address;
import static uk.ac.manchester.spinnaker.messages.model.SystemVariableDefinition.system_sdram_base_address;
import static uk.ac.manchester.spinnaker.messages.model.SystemVariableDefinition.system_sdram_bytes;
import static uk.ac.manchester.spinnaker.messages.model.SystemVariableDefinition.system_sdram_heap_address;
import static uk.ac.manchester.spinnaker.messages.model.SystemVariableDefinition.time_milliseconds;
import static uk.ac.manchester.spinnaker.messages.model.SystemVariableDefinition.time_phase_scale;
import static uk.ac.manchester.spinnaker.messages.model.SystemVariableDefinition.unix_timestamp;
import static uk.ac.manchester.spinnaker.messages.model.SystemVariableDefinition.user_temp_0;
import static uk.ac.manchester.spinnaker.messages.model.SystemVariableDefinition.user_temp_1;
import static uk.ac.manchester.spinnaker.messages.model.SystemVariableDefinition.user_temp_2;
import static uk.ac.manchester.spinnaker.messages.model.SystemVariableDefinition.user_temp_4;
import static uk.ac.manchester.spinnaker.messages.model.SystemVariableDefinition.virtual_to_physical_core_map;
import static uk.ac.manchester.spinnaker.messages.model.SystemVariableDefinition.x;
import static uk.ac.manchester.spinnaker.messages.model.SystemVariableDefinition.x_size;
import static uk.ac.manchester.spinnaker.messages.model.SystemVariableDefinition.y;
import static uk.ac.manchester.spinnaker.messages.model.SystemVariableDefinition.y_size;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collection;
import java.util.List;

import uk.ac.manchester.spinnaker.machine.ChipLocation;
import uk.ac.manchester.spinnaker.machine.HasChipLocation;

/**
 * Represents the system variables for a chip, received from the chip SDRAM
 */
public class ChipInfo implements HasChipLocation {
	private static final byte[] NO_IP = { 0, 0, 0, 0 };
	private final ByteBuffer system_data;

	private String ipAddress;
	private int ledFlashPeriod;
	private int[] leds;
	private BitSet linksAvailable;
	private byte[] physicalToVirtualCoreMap;
	private byte[] statusMap;
	private List<Integer> virtualCoreIDs;
	private byte[] virtualToPhysicalCoreMap;

	/**
	 * Parse a system data block.
	 *
	 * @param systemData
	 *            The data retrieved from SDRAM on the board.
	 */
	public ChipInfo(ByteBuffer systemData) {
		this.system_data = systemData.asReadOnlyBuffer();

		int links = read(links_available);
		linksAvailable = BitSet.valueOf(new byte[] { (byte) links });

		ledFlashPeriod = read(led_half_period_10_ms) * 10;
		leds = new int[] { read(led_0), read(led_1) };
		statusMap = readBytes(status_map);
		physicalToVirtualCoreMap = readBytes(physical_to_virtual_core_map);
		virtualToPhysicalCoreMap = readBytes(virtual_to_physical_core_map);

		virtualCoreIDs = new ArrayList<>();
		for (int vID : physicalToVirtualCoreMap) {
			if ((vID & 0xFF) != 0xFF) {
				virtualCoreIDs.add(vID);
			}
		}
		sort(virtualCoreIDs);
		byte[] ip = readBytes(ethernet_ip_address);
		if (Arrays.equals(ip, NO_IP)) {
			ipAddress = null;
		} else {
			ipAddress = format("%d.%d.%d.%d", ip[0], ip[1], ip[2], ip[3]);
		}
	}

	private int read(SystemVariableDefinition var) {
		switch (var.type) {
		case BYTE:
			return system_data.get(system_data.position() + var.offset);
		case INT:
			return system_data.getInt(system_data.position() + var.offset);
		case SHORT:
			return system_data.getShort(system_data.position() + var.offset);
		case BYTE_ARRAY:
		case LONG:
		default:
			throw new IllegalArgumentException();
		}
	}

	private long readLong(SystemVariableDefinition var) {
		if (var.type != LONG) {
			throw new IllegalArgumentException();
		}
		return system_data.getLong(system_data.position() + var.offset);
	}

	private byte[] readBytes(SystemVariableDefinition var) {
		if (var.type != BYTE_ARRAY) {
			throw new IllegalArgumentException();
		}
		ByteBuffer b = system_data.duplicate();
		b.position(b.position() + var.offset);
		byte[] bytes = (byte[]) var.getDefault();
		b.get(bytes);
		return bytes;
	}

	@Override
	public int getX() {
		return read(x);
	}

	@Override
	public int getY() {
		return read(y);
	}

	/** The number of chips in the x-dimension */
	public int getXSize() {
		return read(x_size);
	}

	/** The number of chips in the y-dimension */
	public int getYSize() {
		return read(y_size);
	}

	/** The location of the chip to send debug messages to */
	public HasChipLocation getDebugChip() {
		return new ChipLocation(read(debug_x), read(debug_y));
	}

	/** Indicates if peer-to-peer is working on the chip */
	public boolean isPeerToPeerAvailable() {
		return read(is_peer_to_peer_available) != 0;
	}

	/** The last ID used in nearest neighbour transaction */
	public int getNearestNeighbourLastID() {
		return read(nearest_neighbour_last_id);
	}

	/** The location of the nearest chip with Ethernet */
	public HasChipLocation getEthernetChip() {
		return new ChipLocation(read(nearest_ethernet_x),
				read(nearest_ethernet_y));
	}

	/** The version of the hardware in use */
	public int getHardwareVersion() {
		return read(hardware_version);
	}

	/** Indicates if Ethernet is available on this chip */
	public boolean isEthernetAvailable() {
		return read(is_ethernet_available) != 0;
	}

	/** Number of times to send out P2PB packets */
	public int getP2PBRepeats() {
		return read(p2p_b_repeats);
	}

	/** Log (base 2) of the peer-to-peer sequence length */
	public int getLogP2PSequenceLength() {
		return read(log_peer_to_peer_sequence_length);
	}

	/** The clock divisors for system & router clocks */
	public int getClockDivisor() {
		return read(clock_divisor);
	}

	/** The time-phase scaling factor */
	public int getTimePhaseScale() {
		return read(time_phase_scale);
	}

	/** The time since startup in milliseconds */
	public long getClockMilliseconds() {
		return readLong(clock_milliseconds);
	}

	/** The number of milliseconds in the current second */
	public int getTimeMilliseconds() {
		return read(time_milliseconds);
	}

	/** The time in seconds since midnight, 1st January 1970 */
	public int getUnixTimestamp() {
		return read(unix_timestamp);
	}

	/** The router time-phase timer */
	public int getRouterTimePhaseTimer() {
		return read(router_time_phase_timer);
	}

	/** The CPU clock frequency in MHz */
	public int getCPUClock() {
		return read(cpu_clock_mhz);
	}

	/** The SDRAM clock frequency in MHz */
	public int getSDRAMClock() {
		return read(sdram_clock_frequency_mhz);
	}

	/** Nearest-Neighbour forward parameter */
	public int getNearestNeighbourForward() {
		return read(nearest_neighbour_forward);
	}

	/** Nearest-Neighbour retry parameter */
	public int getNearestNeighbourRetry() {
		return read(nearest_neighbour_retry);
	}

	/** The link peek/poke timeout in microseconds */
	public int getLinkPeekTimeout() {
		return read(link_peek_timeout_microseconds);
	}

	/** The LED period in millisecond units, or 10 to show load */
	public int getLEDFlashPeriod() {
		return ledFlashPeriod;
	}

	/**
	 * The time to wait after last BC during network initialisation in 10 ms
	 * units
	 */
	public int getNetInitBCWaitTime() {
		return read(netinit_bc_wait_time);
	}

	/** The phase of boot process (see enum netinit_phase_e) */
	public int getNetInitPhase() {
		return read(netinit_phase);
	}

	/** The location of the chip from which the system was booted */
	public HasChipLocation getBootChip() {
		return new ChipLocation(read(p2p_root_x), read(p2p_root_y));
	}

	/** The LED definitions */
	public int[] getLEDs() {
		return leds;
	}

	/** The random seed */
	public int getRandomSeeed() {
		return read(random_seed);
	}

	/** Indicates if this is the root chip */
	public boolean isRootChip() {
		return read(is_root_chip) != 0;
	}

	/** The number of shared message buffers */
	public int getNumSharedMessageBuffers() {
		return read(n_shared_message_buffers);
	}

	/** The delay between nearest-neighbour packets in microseconds */
	public int getNearestNeighbourDelay() {
		return read(nearest_neighbour_delay_us);
	}

	/** The number of watch dog timeouts before an error is raised */
	public int getSoftwareWatchdogCount() {
		return read(software_watchdog_count);
	}

	/** The base address of the system SDRAM heap */
	public int getSystemRAMHeapAddress() {
		return read(system_ram_heap_address);
	}

	/** The base address of the user SDRAM heap */
	public int getSDRAMHeapAddress() {
		return read(sdram_heap_address);
	}

	/** The size of the iobuf buffer in bytes */
	public int getIOBUFSize() {
		return read(iobuf_size);
	}

	/** The size of the system SDRAM in bytes */
	public int getSystemSDRAMSize() {
		return read(system_sdram_bytes);
	}

	/** The size of the system buffer <b>in words</b> */
	public int getSystemBufferSize() {
		return read(system_buffer_words);
	}

	/** The boot signature */
	public int getBootSignature() {
		return read(boot_signature);
	}

	/** The memory pointer for nearest neighbour global operations */
	public int getNearestNeighbourMemoryAddress() {
		return read(nearest_neighbour_memory_pointer);
	}

	/** The lock (??) */
	public int getLock() {
		return read(lock);
	}

	/** Bit mask (6 bits) of links enabled */
	public BitSet getLinksAvailable() {
		return this.linksAvailable;
	}

	/** Last ID used in BIFF packet */
	public int getLastBiffID() {
		return read(last_biff_id);
	}

	/** Board testing flags */
	public int getBoardTestFlags() {
		return read(board_test_flags);
	}

	/** Pointer to the first free shared message buffer */
	public int getSharedMessageFirstFreeAddress() {
		return read(shared_message_first_free_address);
	}

	/** The number of shared message buffers in use */
	public int getSharedMessageCountInUse() {
		return read(shared_message_count_in_use);
	}

	/** The maximum number of shared message buffers used */
	public int getSharedMessageMaximumUsed() {
		return read(shared_message_maximum_used);
	}

	/** The first user variable */
	public int getUser0() {
		return read(user_temp_0);
	}

	/** The second user variable */
	public int getUser1() {
		return read(user_temp_1);
	}

	/** The third user variable */
	public int getUser2() {
		return read(user_temp_2);
	}

	/** The fourth user variable */
	public int getUser4() {
		return read(user_temp_4);
	}

	/** The status map set during SCAMP boot */
	public byte[] getStatusMap() {
		return statusMap;
	}

	/**
	 * The physical core ID to virtual core ID map; entries with a value of 0xFF
	 * are non-operational cores
	 */
	public byte[] getPhysicalToVirtualCoreMap() {
		return physicalToVirtualCoreMap;
	}

	/** The virtual core ID to physical core ID map */
	public byte[] getVirtualToPhysicalCoreMap() {
		return virtualToPhysicalCoreMap;
	}

	/**
	 * A list of available cores by virtual core ID (including the monitor)
	 */
	public Collection<Integer> getVirtualCoreIDs() {
		return unmodifiableList(virtualCoreIDs);
	}

	/** The number of working cores */
	public int getNumWorkingCores() {
		return read(n_working_cores);
	}

	/** The number of SCAMP working cores */
	public int getNumSCAMPWorkingCores() {
		return read(n_scamp_working_cores);
	}

	/** The base address of SDRAM */
	public int getSDRAMBaseAddress() {
		return read(sdram_base_address);
	}

	/** The base address of System RAM */
	public int getSystemRAMBaseAddress() {
		return read(system_ram_base_address);
	}

	/** The base address of System SDRAM */
	public int getSystemSDRAMBaseAddress() {
		return read(system_sdram_base_address);
	}

	/** The base address of the CPU information blocks */
	public int getCPUInformationBaseAddress() {
		return read(cpu_information_base_address);
	}

	/** The base address of the system SDRAM heap */
	public int getSystemSDRAMHeapAddress() {
		return read(system_sdram_heap_address);
	}

	/** The address of the copy of the routing tables */
	public int getRouterTableCopyAddress() {
		return read(router_table_copy_address);
	}

	/** The address of the peer-to-peer hop tables */
	public int getP2PHopTableAddress() {
		return read(peer_to_peer_hop_table_address);
	}

	/** The address of the allocated tag table */
	public int getAllocatedTagTableAddress() {
		return read(allocated_tag_table_address);
	}

	/** The ID of the first free router entry */
	public int getFirstFreeRouterEntry() {
		return read(first_free_router_entry);
	}

	/** The number of active peer-to-peer addresses */
	public int getNumActiveP2PAddresses() {
		return read(n_active_peer_to_peer_addresses);
	}

	/** The address of the application data table */
	public int getAppDataTableAddress() {
		return read(app_data_table_address);
	}

	/** The address of the shared message buffers */
	public int getSharedMessageBufferAddress() {
		return read(shared_message_buffer_address);
	}

	/** The monitor incoming mailbox flags */
	public int getMonitorMailboxFlags() {
		return read(monitor_mailbox_flags);
	}

	/** The IP address of the chip */
	public String getIPAddress() {
		return ipAddress;
	}

	/** A (virtual) copy of the router FR register */
	public int getFixedRoute() {
		return read(fixed_route_copy);
	}

	/** A pointer to the board information structure */
	public int getBoardInfoAddress() {
		return read(board_info);
	}
}
