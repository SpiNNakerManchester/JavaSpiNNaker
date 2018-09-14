package uk.ac.manchester.spinnaker.messages.model;

import static java.nio.ByteOrder.LITTLE_ENDIAN;
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

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collection;
import java.util.List;

import uk.ac.manchester.spinnaker.machine.ChipLocation;
import uk.ac.manchester.spinnaker.machine.HasChipLocation;
import uk.ac.manchester.spinnaker.machine.MachineDimensions;

/**
 * Represents the system variables for a chip, received from the chip SDRAM.
 */
public class ChipInfo implements HasChipLocation {
	private static final byte[] NO_IP = {
			0, 0, 0, 0
	};
	private static final int UNMAPPED = 0xFF;
	private static final int TEN_MS = 10;
	private final ByteBuffer systemData;

	private InetAddress ipAddress;
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
		this.systemData = systemData.asReadOnlyBuffer().order(LITTLE_ENDIAN);

		int links = read(links_available);
		linksAvailable = BitSet.valueOf(new byte[] {
				(byte) links
		});

		ledFlashPeriod = read(led_half_period_10_ms) * TEN_MS;
		leds = new int[] {
				read(led_0), read(led_1)
		};
		statusMap = readBytes(status_map);
		physicalToVirtualCoreMap = readBytes(physical_to_virtual_core_map);
		virtualToPhysicalCoreMap = readBytes(virtual_to_physical_core_map);

		virtualCoreIDs = new ArrayList<>();
		for (byte vID : physicalToVirtualCoreMap) {
			if (Byte.toUnsignedInt(vID) != UNMAPPED) {
				virtualCoreIDs.add((int) vID);
			}
		}
		sort(virtualCoreIDs);
		byte[] ip = readBytes(ethernet_ip_address);
		if (Arrays.equals(ip, NO_IP)) {
			ipAddress = null;
		} else {
			try {
				ipAddress = InetAddress.getByAddress(ip);
			} catch (UnknownHostException e) {
				ipAddress = null;
			}
		}
	}

	private int read(SystemVariableDefinition var) {
		switch (var.type) {
		case BYTE:
			return systemData.get(systemData.position() + var.offset);
		case INT:
			return systemData.getInt(systemData.position() + var.offset);
		case SHORT:
			return systemData.getShort(systemData.position() + var.offset);
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
		return systemData.getLong(systemData.position() + var.offset);
	}

	private byte[] readBytes(SystemVariableDefinition var) {
		if (var.type != BYTE_ARRAY) {
			throw new IllegalArgumentException();
		}
		ByteBuffer b = systemData.duplicate();
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

	/** @return The number of chips in the x- and y-dimensions. */
	public MachineDimensions getSize() {
		return new MachineDimensions(read(x_size), read(y_size));
	}

	/** @return The location of the chip to send debug messages to. */
	public HasChipLocation getDebugChip() {
		return new ChipLocation(read(debug_x), read(debug_y));
	}

	/** @return Indicates if peer-to-peer is working on the chip. */
	public boolean isPeerToPeerAvailable() {
		return read(is_peer_to_peer_available) != 0;
	}

	/** @return The last ID used in nearest neighbour transaction. */
	public int getNearestNeighbourLastID() {
		return read(nearest_neighbour_last_id);
	}

	/** @return The location of the nearest chip with Ethernet. */
	public HasChipLocation getEthernetChip() {
		return new ChipLocation(read(nearest_ethernet_x),
				read(nearest_ethernet_y));
	}

	/** @return The version of the hardware in use. */
	public int getHardwareVersion() {
		return read(hardware_version);
	}

	/** @return Indicates if Ethernet is available on this chip. */
	public boolean isEthernetAvailable() {
		return read(is_ethernet_available) != 0;
	}

	/** @return Number of times to send out P2PB packets. */
	public int getP2PBRepeats() {
		return read(p2p_b_repeats);
	}

	/** @return Log (base 2) of the peer-to-peer sequence length. */
	public int getLogP2PSequenceLength() {
		return read(log_peer_to_peer_sequence_length);
	}

	/** @return The clock divisors for system &amp; router clocks. */
	public int getClockDivisor() {
		return read(clock_divisor);
	}

	/** @return The time-phase scaling factor. */
	public int getTimePhaseScale() {
		return read(time_phase_scale);
	}

	/** @return The time since startup in milliseconds. */
	public long getClockMilliseconds() {
		return readLong(clock_milliseconds);
	}

	/** @return The number of milliseconds in the current second. */
	public int getTimeMilliseconds() {
		return read(time_milliseconds);
	}

	/** @return The time in seconds since midnight, 1st January 1970. */
	public int getUnixTimestamp() {
		return read(unix_timestamp);
	}

	/** @return The router time-phase timer. */
	public int getRouterTimePhaseTimer() {
		return read(router_time_phase_timer);
	}

	/** @return The CPU clock frequency in MHz. */
	public int getCPUClock() {
		return read(cpu_clock_mhz);
	}

	/** @return The SDRAM clock frequency in MHz. */
	public int getSDRAMClock() {
		return read(sdram_clock_frequency_mhz);
	}

	/** @return Nearest-Neighbour forward parameter. */
	public int getNearestNeighbourForward() {
		return read(nearest_neighbour_forward);
	}

	/** @return Nearest-Neighbour retry parameter. */
	public int getNearestNeighbourRetry() {
		return read(nearest_neighbour_retry);
	}

	/** @return The link peek/poke timeout in microseconds. */
	public int getLinkPeekTimeout() {
		return read(link_peek_timeout_microseconds);
	}

	/** @return The LED period in millisecond units, or 10 to show load. */
	public int getLEDFlashPeriod() {
		return ledFlashPeriod;
	}

	/**
	 * @return The time to wait after last BC during network initialisation in
	 *         10 ms units.
	 */
	public int getNetInitBCWaitTime() {
		return read(netinit_bc_wait_time);
	}

	/** @return The phase of boot process (see enum netinit_phase_e). */
	public int getNetInitPhase() {
		return read(netinit_phase);
	}

	/** @return The location of the chip from which the system was booted. */
	public HasChipLocation getBootChip() {
		return new ChipLocation(read(p2p_root_x), read(p2p_root_y));
	}

	/** @return The LED definitions. */
	public int[] getLEDs() {
		return leds;
	}

	/** @return The random seed. */
	public int getRandomSeeed() {
		return read(random_seed);
	}

	/** @return Indicates if this is the root chip. */
	public boolean isRootChip() {
		return read(is_root_chip) != 0;
	}

	/** @return The number of shared message buffers. */
	public int getNumSharedMessageBuffers() {
		return read(n_shared_message_buffers);
	}

	/** @return The delay between nearest-neighbour packets in microseconds. */
	public int getNearestNeighbourDelay() {
		return read(nearest_neighbour_delay_us);
	}

	/** @return The number of watch dog timeouts before an error is raised. */
	public int getSoftwareWatchdogCount() {
		return read(software_watchdog_count);
	}

	/** @return The base address of the system SDRAM heap. */
	public int getSystemRAMHeapAddress() {
		return read(system_ram_heap_address);
	}

	/** @return The base address of the user SDRAM heap. */
	public int getSDRAMHeapAddress() {
		return read(sdram_heap_address);
	}

	/** @return The size of the iobuf buffer in bytes. */
	public int getIOBUFSize() {
		return read(iobuf_size);
	}

	/** @return The size of the system SDRAM in bytes. */
	public int getSystemSDRAMSize() {
		return read(system_sdram_bytes);
	}

	/** @return The size of the system buffer <b>in words</b>. */
	public int getSystemBufferSize() {
		return read(system_buffer_words);
	}

	/** @return The boot signature. */
	public int getBootSignature() {
		return read(boot_signature);
	}

	/** @return The memory pointer for nearest neighbour global operations. */
	public int getNearestNeighbourMemoryAddress() {
		return read(nearest_neighbour_memory_pointer);
	}

	/** @return The lock. (??) */
	public int getLock() {
		return read(lock);
	}

	/** @return Bit mask (6 bits) of links enabled. */
	public BitSet getLinksAvailable() {
		return this.linksAvailable;
	}

	/** @return Last ID used in BIFF packet. */
	public int getLastBiffID() {
		return read(last_biff_id);
	}

	/** @return Board testing flags. */
	public int getBoardTestFlags() {
		return read(board_test_flags);
	}

	/** @return Pointer to the first free shared message buffer. */
	public int getSharedMessageFirstFreeAddress() {
		return read(shared_message_first_free_address);
	}

	/** @return The number of shared message buffers in use. */
	public int getSharedMessageCountInUse() {
		return read(shared_message_count_in_use);
	}

	/** @return The maximum number of shared message buffers used. */
	public int getSharedMessageMaximumUsed() {
		return read(shared_message_maximum_used);
	}

	/** @return The first user variable. */
	public int getUser0() {
		return read(user_temp_0);
	}

	/** @return The second user variable. */
	public int getUser1() {
		return read(user_temp_1);
	}

	/** @return The third user variable. */
	public int getUser2() {
		return read(user_temp_2);
	}

	/** @return The fourth user variable. */
	public int getUser4() {
		return read(user_temp_4);
	}

	/** @return The status map set during SCAMP boot. */
	public byte[] getStatusMap() {
		return statusMap;
	}

	/**
	 * @return The physical core ID to virtual core ID map; entries with a value
	 *         of 0xFF are non-operational cores.
	 */
	public byte[] getPhysicalToVirtualCoreMap() {
		return physicalToVirtualCoreMap;
	}

	/** @return The virtual core ID to physical core ID map. */
	public byte[] getVirtualToPhysicalCoreMap() {
		return virtualToPhysicalCoreMap;
	}

	/**
	 * @return A list of available cores by virtual core ID (including the
	 *         monitor).
	 */
	public Collection<Integer> getVirtualCoreIDs() {
		return unmodifiableList(virtualCoreIDs);
	}

	/** @return The number of working cores. */
	public int getNumWorkingCores() {
		return read(n_working_cores);
	}

	/** @return The number of SCAMP working cores. */
	public int getNumSCAMPWorkingCores() {
		return read(n_scamp_working_cores);
	}

	/** @return The base address of SDRAM. */
	public int getSDRAMBaseAddress() {
		return read(sdram_base_address);
	}

	/** @return The base address of System RAM. */
	public int getSystemRAMBaseAddress() {
		return read(system_ram_base_address);
	}

	/** @return The base address of System SDRAM. */
	public int getSystemSDRAMBaseAddress() {
		return read(system_sdram_base_address);
	}

	/** @return The base address of the CPU information blocks. */
	public int getCPUInformationBaseAddress() {
		return read(cpu_information_base_address);
	}

	/** @return The base address of the system SDRAM heap. */
	public int getSystemSDRAMHeapAddress() {
		return read(system_sdram_heap_address);
	}

	/** @return The address of the copy of the routing tables. */
	public int getRouterTableCopyAddress() {
		return read(router_table_copy_address);
	}

	/** @return The address of the peer-to-peer hop tables. */
	public int getP2PHopTableAddress() {
		return read(peer_to_peer_hop_table_address);
	}

	/** @return The address of the allocated tag table. */
	public int getAllocatedTagTableAddress() {
		return read(allocated_tag_table_address);
	}

	/** @return The ID of the first free router entry. */
	public int getFirstFreeRouterEntry() {
		return read(first_free_router_entry);
	}

	/** @return The number of active peer-to-peer addresses. */
	public int getNumActiveP2PAddresses() {
		return read(n_active_peer_to_peer_addresses);
	}

	/** @return The address of the application data table. */
	public int getAppDataTableAddress() {
		return read(app_data_table_address);
	}

	/** @return The address of the shared message buffers. */
	public int getSharedMessageBufferAddress() {
		return read(shared_message_buffer_address);
	}

	/** @return The monitor incoming mailbox flags. */
	public int getMonitorMailboxFlags() {
		return read(monitor_mailbox_flags);
	}

	/** @return The IP address of the chip. */
	public InetAddress getIPAddress() {
		return ipAddress;
	}

	/** @return A (virtual) copy of the router FR register. */
	public int getFixedRoute() {
		return read(fixed_route_copy);
	}

	/** @return A pointer to the board information structure. */
	public int getBoardInfoAddress() {
		return read(board_info);
	}
}
