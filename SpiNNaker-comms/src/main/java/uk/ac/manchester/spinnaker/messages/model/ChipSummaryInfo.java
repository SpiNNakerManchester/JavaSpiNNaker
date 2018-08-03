package uk.ac.manchester.spinnaker.messages.model;

import static java.net.InetAddress.getByAddress;
import static java.util.Collections.unmodifiableList;
import static java.util.Collections.unmodifiableSet;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import uk.ac.manchester.spinnaker.machine.ChipLocation;
import uk.ac.manchester.spinnaker.machine.Direction;
import uk.ac.manchester.spinnaker.machine.HasChipLocation;

/** Represents the chip summary information read via an SCP command. */
public final class ChipSummaryInfo {
    /** The state of the cores on the chip (list of one per core). */
    public final List<CPUState> coreStates;
    /** The IP address of the Ethernet if up, or <tt>null</tt> if not. */
    public final InetAddress ethernetIPAddress;
    /** Determines if the Ethernet connection is available on this chip. */
    public final boolean isEthernetAvailable;
    /** The size of the largest block of free SDRAM in bytes. */
    public final int largestFreeSDRAMBlock;
    /** The size of the largest block of free SRAM in bytes. */
    public final int largestFreeSRAMBlock;
    /** The number of cores working on the chip (including monitors). */
    public final int numCores;
    /** The number of multicast routing entries free on this chip. */
    public final int numFreeMulticastRoutingEntries;
    /** The location of the nearest Ethernet chip. */
    public final ChipLocation nearestEthernetChip;
    /** The IDs of the working links outgoing from this chip. */
    public final Set<Direction> workingLinks;
    /** The chip that this data is from. */
    public final HasChipLocation chip;

    private static final int ADDRESS_SIZE = 4;
    private static final byte[] NO_ADDRESS = new byte[ADDRESS_SIZE];
    private static final int NUM_CORES = 18;
    private static final int NUM_LINKS = 6;
    private static final int LINKS_FIELD_SHIFT = 8;
    private static final int NUM_CORES_FIELD_MASK = 0b00011111;
    private static final int FREE_ENTRIES_FIELD_SHIFT = 14;
    private static final int FREE_ENTRIES_FIELD_MASK = 0x7FF;
    private static final int ETH_AVAIL_FIELD_BIT = 25;

    private static boolean bitset(int value, int bitnum) {
        return ((value >>> bitnum) & 1) != 0;
    }

    private static Set<Direction> parseWorkingLinks(int flags) {
        Set<Direction> wl = new LinkedHashSet<>();
        for (int link = 0; link < NUM_LINKS; link++) {
            if (bitset(flags, LINKS_FIELD_SHIFT + link)) {
                wl.add(Direction.byId(link));
            }
        }
        return unmodifiableSet(wl);
    }

    private static List<CPUState> parseStates(byte[] stateBytes) {
        List<CPUState> states = new ArrayList<>();
        for (byte b : stateBytes) {
            states.add(CPUState.get(b));
        }
        return unmodifiableList(states);
    }

    private static InetAddress parseEthernetAddress(byte[] addr) {
        try {
            if (!Arrays.equals(addr, NO_ADDRESS)) {
                return getByAddress(addr);
            }
        } catch (UnknownHostException e) {
            // should be unreachable
        }
        return null;
    }

    /**
     * @param buffer
     *            The data from the SCP response
     * @param source
     *            The coordinates of the chip that this data is from
     */
    public ChipSummaryInfo(ByteBuffer buffer, HasChipLocation source) {
        int flags = buffer.getInt();
        numCores = flags & NUM_CORES_FIELD_MASK;
        workingLinks = parseWorkingLinks(flags);
        numFreeMulticastRoutingEntries = (flags >>> FREE_ENTRIES_FIELD_SHIFT)
                & FREE_ENTRIES_FIELD_MASK;
        isEthernetAvailable = bitset(flags, ETH_AVAIL_FIELD_BIT);

        largestFreeSDRAMBlock = buffer.getInt();
        largestFreeSRAMBlock = buffer.getInt();

        byte[] states = new byte[NUM_CORES];
        buffer.get(states);
        coreStates = parseStates(states);

        chip = source;
        int neY = Byte.toUnsignedInt(buffer.get());
        int neX = Byte.toUnsignedInt(buffer.get());
        nearestEthernetChip = new ChipLocation(neX, neY);

        byte[] ia = new byte[ADDRESS_SIZE];
        buffer.get(ia);
        ethernetIPAddress = parseEthernetAddress(ia);
    }
}
