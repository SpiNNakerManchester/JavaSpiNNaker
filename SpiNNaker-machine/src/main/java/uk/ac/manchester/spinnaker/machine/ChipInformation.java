package uk.ac.manchester.spinnaker.machine;

import java.util.BitSet;
import java.util.Collection;

/**
 * The interface to information about a chip, as retrieved from the chip.
 *
 * @author Donal Fellows
 */
public interface ChipInformation extends HasChipLocation {

    /** The number of chips in the x- and y-dimensions. */
    MachineDimensions getSize();

    /** The location of the chip to send debug messages to. */
    HasChipLocation getDebugChip();

    /** Indicates if peer-to-peer is working on the chip. */
    boolean isPeerToPeerAvailable();

    /** The last ID used in nearest neighbour transaction. */
    int getNearestNeighbourLastID();

    /** The location of the nearest chip with Ethernet. */
    HasChipLocation getEthernetChip();

    /** The version of the hardware in use. */
    int getHardwareVersion();

    /** Indicates if Ethernet is available on this chip. */
    boolean isEthernetAvailable();

    /** Number of times to send out P2PB packets. */
    int getP2PBRepeats();

    /** Log (base 2) of the peer-to-peer sequence length. */
    int getLogP2PSequenceLength();

    /** The clock divisors for system &amp; router clocks. */
    int getClockDivisor();

    /** The time-phase scaling factor. */
    int getTimePhaseScale();

    /** The time since startup in milliseconds. */
    long getClockMilliseconds();

    /** The number of milliseconds in the current second. */
    int getTimeMilliseconds();

    /** The time in seconds since midnight, 1st January 1970. */
    int getUnixTimestamp();

    /** The router time-phase timer. */
    int getRouterTimePhaseTimer();

    /** The CPU clock frequency in MHz. */
    int getCPUClock();

    /** The SDRAM clock frequency in MHz. */
    int getSDRAMClock();

    /** Nearest-Neighbour forward parameter. */
    int getNearestNeighbourForward();

    /** Nearest-Neighbour retry parameter. */
    int getNearestNeighbourRetry();

    /** The link peek/poke timeout in microseconds. */
    int getLinkPeekTimeout();

    /** The LED period in millisecond units, or 10 to show load. */
    int getLEDFlashPeriod();

    /**
     * The time to wait after last BC during network initialisation in 10 ms
     * units.
     */
    int getNetInitBCWaitTime();

    /** The phase of boot process (see enum netinit_phase_e). */
    int getNetInitPhase();

    /** The location of the chip from which the system was booted. */
    HasChipLocation getBootChip();

    /** The LED definitions. */
    int[] getLEDs();

    /** The random seed. */
    int getRandomSeeed();

    /** Indicates if this is the root chip. */
    boolean isRootChip();

    /** The number of shared message buffers. */
    int getNumSharedMessageBuffers();

    /** The delay between nearest-neighbour packets in microseconds. */
    int getNearestNeighbourDelay();

    /** The number of watch dog timeouts before an error is raised. */
    int getSoftwareWatchdogCount();

    /** The base address of the system SDRAM heap. */
    int getSystemRAMHeapAddress();

    /** The base address of the user SDRAM heap. */
    int getSDRAMHeapAddress();

    /** The size of the iobuf buffer in bytes. */
    int getIOBUFSize();

    /** The size of the system SDRAM in bytes. */
    int getSystemSDRAMSize();

    /** The size of the system buffer <b>in words</b>. */
    int getSystemBufferSize();

    /** The boot signature. */
    int getBootSignature();

    /** The memory pointer for nearest neighbour global operations. */
    int getNearestNeighbourMemoryAddress();

    /** The lock. (??) */
    int getLock();

    /** Bit mask (6 bits) of links enabled. */
    BitSet getLinksAvailable();

    /** Last ID used in BIFF packet. */
    int getLastBiffID();

    /** Board testing flags. */
    int getBoardTestFlags();

    /** Pointer to the first free shared message buffer. */
    int getSharedMessageFirstFreeAddress();

    /** The number of shared message buffers in use. */
    int getSharedMessageCountInUse();

    /** The maximum number of shared message buffers used. */
    int getSharedMessageMaximumUsed();

    /** The first user variable. */
    int getUser0();

    /** The second user variable. */
    int getUser1();

    /** The third user variable. */
    int getUser2();

    /** The fourth user variable. */
    int getUser4();

    /** The status map set during SCAMP boot. */
    byte[] getStatusMap();

    /**
     * The physical core ID to virtual core ID map; entries with a value of 0xFF
     * are non-operational cores.
     */
    byte[] getPhysicalToVirtualCoreMap();

    /** The virtual core ID to physical core ID map. */
    byte[] getVirtualToPhysicalCoreMap();

    /**
     * A list of available cores by virtual core ID (including the monitor).
     */
    Collection<Integer> getVirtualCoreIDs();

    /** The number of working cores. */
    int getNumWorkingCores();

    /** The number of SCAMP working cores. */
    int getNumSCAMPWorkingCores();

    /** The base address of SDRAM. */
    int getSDRAMBaseAddress();

    /** The base address of System RAM. */
    int getSystemRAMBaseAddress();

    /** The base address of System SDRAM. */
    int getSystemSDRAMBaseAddress();

    /** The base address of the CPU information blocks. */
    int getCPUInformationBaseAddress();

    /** The base address of the system SDRAM heap. */
    int getSystemSDRAMHeapAddress();

    /** The address of the copy of the routing tables. */
    int getRouterTableCopyAddress();

    /** The address of the peer-to-peer hop tables. */
    int getP2PHopTableAddress();

    /** The address of the allocated tag table. */
    int getAllocatedTagTableAddress();

    /** The ID of the first free router entry. */
    int getFirstFreeRouterEntry();

    /** The number of active peer-to-peer addresses. */
    int getNumActiveP2PAddresses();

    /** The address of the application data table. */
    int getAppDataTableAddress();

    /** The address of the shared message buffers. */
    int getSharedMessageBufferAddress();

    /** The monitor incoming mailbox flags. */
    int getMonitorMailboxFlags();

    /** The IP address of the chip. */
    String getIPAddress();

    /** A (virtual) copy of the router FR register. */
    int getFixedRoute();

    /** A pointer to the board information structure. */
    int getBoardInfoAddress();
}
