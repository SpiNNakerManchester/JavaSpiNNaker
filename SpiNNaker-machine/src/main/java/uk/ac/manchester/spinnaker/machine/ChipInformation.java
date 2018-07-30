package uk.ac.manchester.spinnaker.machine;

import java.util.BitSet;
import java.util.Collection;

/**
 * The interface to information about a chip, as retrieved from the chip.
 *
 * @author Donal Fellows
 */
public interface ChipInformation extends HasChipLocation {

    /** @return The number of chips in the x- and y-dimensions. */
    MachineDimensions getSize();

    /** @return The location of the chip to send debug messages to. */
    HasChipLocation getDebugChip();

    /** @return Indicates if peer-to-peer is working on the chip. */
    boolean isPeerToPeerAvailable();

    /** @return The last ID used in nearest neighbour transaction. */
    int getNearestNeighbourLastID();

    /** @return The location of the nearest chip with Ethernet. */
    HasChipLocation getEthernetChip();

    /** @return The version of the hardware in use. */
    int getHardwareVersion();

    /** @return Indicates if Ethernet is available on this chip. */
    boolean isEthernetAvailable();

    /** @return Number of times to send out P2PB packets. */
    int getP2PBRepeats();

    /** @return Log (base 2) of the peer-to-peer sequence length. */
    int getLogP2PSequenceLength();

    /** @return The clock divisors for system &amp; router clocks. */
    int getClockDivisor();

    /** @return The time-phase scaling factor. */
    int getTimePhaseScale();

    /** @return The time since startup in milliseconds. */
    long getClockMilliseconds();

    /** @return The number of milliseconds in the current second. */
    int getTimeMilliseconds();

    /** @return The time in seconds since midnight, 1st January 1970. */
    int getUnixTimestamp();

    /** @return The router time-phase timer. */
    int getRouterTimePhaseTimer();

    /** @return The CPU clock frequency in MHz. */
    int getCPUClock();

    /** @return The SDRAM clock frequency in MHz. */
    int getSDRAMClock();

    /** @return Nearest-Neighbour forward parameter. */
    int getNearestNeighbourForward();

    /** @return Nearest-Neighbour retry parameter. */
    int getNearestNeighbourRetry();

    /** @return The link peek/poke timeout in microseconds. */
    int getLinkPeekTimeout();

    /** @return The LED period in millisecond units, or 10 to show load. */
    int getLEDFlashPeriod();

    /**
     * @return The time to wait after last BC during network initialisation in
     *         10 ms units.
     */
    int getNetInitBCWaitTime();

    /** @return The phase of boot process (see enum netinit_phase_e). */
    int getNetInitPhase();

    /** @return The location of the chip from which the system was booted. */
    HasChipLocation getBootChip();

    /** @return The LED definitions. */
    int[] getLEDs();

    /** @return The random seed. */
    int getRandomSeeed();

    /** @return Indicates if this is the root chip. */
    boolean isRootChip();

    /** @return The number of shared message buffers. */
    int getNumSharedMessageBuffers();

    /** @return The delay between nearest-neighbour packets in microseconds. */
    int getNearestNeighbourDelay();

    /** @return The number of watch dog timeouts before an error is raised. */
    int getSoftwareWatchdogCount();

    /** @return The base address of the system SDRAM heap. */
    int getSystemRAMHeapAddress();

    /** @return The base address of the user SDRAM heap. */
    int getSDRAMHeapAddress();

    /** @return The size of the iobuf buffer in bytes. */
    int getIOBUFSize();

    /** @return The size of the system SDRAM in bytes. */
    int getSystemSDRAMSize();

    /** @return The size of the system buffer <b>in words</b>. */
    int getSystemBufferSize();

    /** @return The boot signature. */
    int getBootSignature();

    /** @return The memory pointer for nearest neighbour global operations. */
    int getNearestNeighbourMemoryAddress();

    /** @return The lock. (??) */
    int getLock();

    /** @return Bit mask (6 bits) of links enabled. */
    BitSet getLinksAvailable();

    /** @return Last ID used in BIFF packet. */
    int getLastBiffID();

    /** @return Board testing flags. */
    int getBoardTestFlags();

    /** @return Pointer to the first free shared message buffer. */
    int getSharedMessageFirstFreeAddress();

    /** @return The number of shared message buffers in use. */
    int getSharedMessageCountInUse();

    /** @return The maximum number of shared message buffers used. */
    int getSharedMessageMaximumUsed();

    /** @return The first user variable. */
    int getUser0();

    /** @return The second user variable. */
    int getUser1();

    /** @return The third user variable. */
    int getUser2();

    /** @return The fourth user variable. */
    int getUser4();

    /** @return The status map set during SCAMP boot. */
    byte[] getStatusMap();

    /**
     * @return The physical core ID to virtual core ID map; entries with a value
     *         of 0xFF are non-operational cores.
     */
    byte[] getPhysicalToVirtualCoreMap();

    /** @return The virtual core ID to physical core ID map. */
    byte[] getVirtualToPhysicalCoreMap();

    /**
     * @return A list of available cores by virtual core ID (including the
     *         monitor).
     */
    Collection<Integer> getVirtualCoreIDs();

    /** @return The number of working cores. */
    int getNumWorkingCores();

    /** @return The number of SCAMP working cores. */
    int getNumSCAMPWorkingCores();

    /** @return The base address of SDRAM. */
    int getSDRAMBaseAddress();

    /** @return The base address of System RAM. */
    int getSystemRAMBaseAddress();

    /** @return The base address of System SDRAM. */
    int getSystemSDRAMBaseAddress();

    /** @return The base address of the CPU information blocks. */
    int getCPUInformationBaseAddress();

    /** @return The base address of the system SDRAM heap. */
    int getSystemSDRAMHeapAddress();

    /** @return The address of the copy of the routing tables. */
    int getRouterTableCopyAddress();

    /** @return The address of the peer-to-peer hop tables. */
    int getP2PHopTableAddress();

    /** @return The address of the allocated tag table. */
    int getAllocatedTagTableAddress();

    /** @return The ID of the first free router entry. */
    int getFirstFreeRouterEntry();

    /** @return The number of active peer-to-peer addresses. */
    int getNumActiveP2PAddresses();

    /** @return The address of the application data table. */
    int getAppDataTableAddress();

    /** @return The address of the shared message buffers. */
    int getSharedMessageBufferAddress();

    /** @return The monitor incoming mailbox flags. */
    int getMonitorMailboxFlags();

    /** @return The IP address of the chip. */
    String getIPAddress();

    /** @return A (virtual) copy of the router FR register. */
    int getFixedRoute();

    /** @return A pointer to the board information structure. */
    int getBoardInfoAddress();
}
