/*
 * Copyright (c) 2018 The University of Manchester
 */
package uk.ac.manchester.spinnaker.machine;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import uk.ac.manchester.spinnaker.machine.datalinks.FPGALinkData;
import uk.ac.manchester.spinnaker.machine.datalinks.FpgaId;
import uk.ac.manchester.spinnaker.machine.datalinks.FpgaEnum;
import uk.ac.manchester.spinnaker.machine.datalinks.InetIdTuple;
import uk.ac.manchester.spinnaker.machine.datalinks.SpinnakerLinkData;
import uk.ac.manchester.spinnaker.utils.Counter;

/**
 *
 * @author Christian-B
 */
public class Machine {
    /*
        __slots__ = (
        "_boot_x",
        "_boot_y",
        "_boot_ethernet_address",
        "_chips",
        "_ethernet_connected_chips",
        "_fpga_links",
        "_spinnaker_links",
    )
    */

    /** Size of the machine along the x axis in Chips */
    public final int width;

    /** Size of the machine along the y axis in Chips */
    public final int height;

    // This is not final as will change as processors become monitors
    private int maxUserProssorsOnAChip;

    ArrayList<Chip> ethernetConnectedChips;

    private final HashMap<InetIdTuple, SpinnakerLinkData> spinnakerLinks;

    private final HashMap<InetAddress, Map<FpgaId, Map<Integer, FPGALinkData>>>
            fpgaLinks;

    public final ChipLocation boot;

    private InetAddress bootEthernetAddress;

    private final TreeMap<ChipLocation, Chip> chips;
    //private final Chip[][] chipArray;

    // Stats
    private MachineVersion version;

    public Machine(int width, int height, Iterable<Chip> chips, HasChipLocation boot) {
        this.width = width;
        this.height = height;
        version = SpiNNakerTriadGeometry.getSpinn5Geometry().versionBySize(width, height);

        maxUserProssorsOnAChip = 0;

        ethernetConnectedChips = new ArrayList<>();
        spinnakerLinks = new HashMap();
        fpgaLinks = new HashMap();

        this.boot = boot.asChipLocation();
        bootEthernetAddress = null;

        this.chips = new TreeMap();
        //this.chipArray = new Chip[width][height];
        addChips(chips);
    }


    public void addChip(Chip chip) {
        ChipLocation location = chip.asChipLocation();
        if (chips.containsKey(location)) {
            throw new IllegalArgumentException(
                    "There is already a Chip at location: " + location);
        }

        if (chip.getX() >= width) {
            throw new IllegalArgumentException("Chip x: " + chip.getX()
                    + " is too high for a machine with width " + width);
        }
        if (chip.getY() >= height) {
           throw new IllegalArgumentException("Chip y: " + chip.getY()
                    + " is too high for a machine with height " + width);
        }

        chips.put(location, chip);
        if (chip.ipAddress != null) {
            ethernetConnectedChips.add(chip);
            if (boot.onSameChipAs(chip)) {
                bootEthernetAddress = chip.ipAddress;
            }
        }
        if (chip.nUserProcessors() > maxUserProssorsOnAChip) {
            maxUserProssorsOnAChip = chip.nUserProcessors();
        }
    }

    public void addChips(Iterable<Chip> chips) {
        for (Chip chip:chips) {
            addChip(chip);
        }
    }

    public Collection<Chip> chips() {
        return Collections.unmodifiableCollection(this.chips.values());
    }

    public int nChips() {
        return chips.size();
    }

    public Set<ChipLocation> chipCoordinates() {
        return Collections.unmodifiableSet(this.chips.keySet());
    }

    public SortedMap<ChipLocation, Chip> chipsMap() {
        return Collections.unmodifiableSortedMap(chips);
    }

    public Chip getChipAt(ChipLocation location) {
        return chips.get(location);
    }

    public boolean hasChipAt(ChipLocation location) {
        return chips.containsKey(location);
    }

    //public Chip getChipAt(int x, int y) {
    //    return this.chipArray[x][y];
    //}

    //public boolean hasChipAt(int x, int y) {
    //    return this.chipArray[x][y] != null;
    //}

    public boolean hasLinkAt(ChipLocation location, Direction link) {
        Chip chip = chips.get(location);
        if (chip == null) {
            return false;
        } else {
            return chip.router.hasLink(link);
        }
    }

    public int maxChipX() {
        return width;
    }

    public int maxChipY() {
        return height;
    }

    public List<Chip> ethernetConnectedChips() {
        return Collections.unmodifiableList(this.ethernetConnectedChips);
    }

    public Map<InetIdTuple, SpinnakerLinkData> spinnakerLinks() {
        return Collections.unmodifiableMap(spinnakerLinks);
    }

    public SpinnakerLinkData getSpinnakerLink(InetIdTuple key) {
        if (key.address == null) {
            key = new InetIdTuple(bootEthernetAddress, key.id);
        }
        return spinnakerLinks.get(key);
    }

    public void addSpinnakerLinks() {
        switch (version) {
            case TWO:
            case THREE:
                Chip chip00 = getChipAt(ChipLocation.ZERO_ZERO);
                if (!chip00.router.hasLink(Direction.WEST)) {
                    spinnakerLinks.put(new InetIdTuple(chip00.ipAddress, 0),
                            new SpinnakerLinkData(0, chip00,
                                    Direction.WEST, chip00.ipAddress));
                }
                Chip chip10 = getChipAt(ChipLocation.ONE_ZERO);
                if (!chip10.router.hasLink(Direction.EAST)) {
                    spinnakerLinks.put(new InetIdTuple(chip10.ipAddress, 0),
                            new SpinnakerLinkData(1, chip00,
                                    Direction.WEST, chip10.ipAddress));
                }
                break;
            case FOUR:
            case FIVE:
            case TRIAD_WITH_WRAPAROUND:
            case TRIAD_NO_WRAPAROUND:
            case NONE_TRIAD_LARGE:
                for (Chip chip: ethernetConnectedChips) {
                    if (!chip.router.hasLink(Direction.SOUTHWEST)) {
                        spinnakerLinks.put(new InetIdTuple(chip.ipAddress, 0),
                            new SpinnakerLinkData(0, chip,
                                    Direction.SOUTHWEST, chip.ipAddress));
                    }
                }
                break;
            case INVALID:
                throw new IllegalStateException(
                        "Based on current maxX:" + width + " and maxY:"
                        + height + " no valid board version available.");
            default:
                throw new Error("Unexpected BoardVersion Enum: " + version
                        + " Please reraise an issue.");
        }
    }

    /**
     * Converts x and y to a chip location.
     *
     * If required (and applicable) adjusting for wrap around.
     * <p>
     * There is NO check that the location is valid or
     *      that there is a Chip at this location.
     * @param x X coordinate
     * @param y Y coordinate
     * @return A ChipLocation based on X and Y with possible wrap around.
     */
    private ChipLocation normalizedLocation(int x, int y) {
        if (version.wrapAround) {
            x = x % width;
            y = y % height;
        }
        return new ChipLocation(x, y);
    }

    public FPGALinkData getFpgaLink(
            FpgaId fpgaId, int fpgaLinkId, InetAddress address) {
        Map<FpgaId, Map<Integer, FPGALinkData>> byAddress;
        byAddress = fpgaLinks.get(address);
        if (byAddress == null) {
            return null;
        }
        Map<Integer, FPGALinkData> byId = byAddress.get(fpgaId);
        if (byId == null) {
            return null;
        }
        return byId.get(fpgaLinkId);
    }

    private void addFpgaLinks(int rootX, int rootY, InetAddress address) {
        for (FpgaEnum fpgaEnum:FpgaEnum.values()){
            ChipLocation location = normalizedLocation(
                    rootX + fpgaEnum.getX(), rootY + fpgaEnum.getY());
            if (hasChipAt(location) && !hasLinkAt(location, fpgaEnum.direction)) {
                FPGALinkData fpgaLinkData = new FPGALinkData(
                        fpgaEnum.id, fpgaEnum.fpgaId, location,
                        fpgaEnum.direction, address);
                Map<FpgaId, Map<Integer, FPGALinkData>> byAddress;
                byAddress = fpgaLinks.get(address);
                if (byAddress == null) {
                    byAddress = new HashMap();
                    fpgaLinks.put(address, byAddress);
                }
                Map<Integer, FPGALinkData> byId;
                byId = byAddress.get(fpgaEnum.fpgaId);
                if (byId == null) {
                    byId = new HashMap();
                    byAddress.put(fpgaEnum.fpgaId, byId);
                }
                byId.put(fpgaEnum.id, fpgaLinkData);
            }
        }
    }

    /**
     * Add FPGA links that are on a given machine depending on the version
     *      of the board.
     * <p>
     * Note: This implementation assumes the Ethernet Chip is the 0, 0 chip
     *      on each board
     */
    //TODO Better to get version in constructor I think!
    public void addFpgaLinks() {
        switch (version) {
            case TWO:
            case THREE:
                break;  // NO fpga links
            case FOUR:
            case FIVE:
            case TRIAD_WITH_WRAPAROUND:
            case TRIAD_NO_WRAPAROUND:
            case NONE_TRIAD_LARGE:
                for (Chip ethernetConnectedChip: ethernetConnectedChips) {
                     addFpgaLinks(
                            ethernetConnectedChip.getX(),
                            ethernetConnectedChip.getY(),
                            ethernetConnectedChip.ipAddress);
                }
                break;
            case INVALID:
                throw new IllegalStateException(
                        "Based on current maxX:" + width + " and maxY:"
                        + height + " no valid board version available.");
            default:
                throw new Error("Unexpected BoardVersion Enum: " + version
                        + " Please reraise an issue.");
        }
    }

    //TODO Check assumption that every link is created exactly twice
    //    (Backwards and Forwards) is correct.
    public String coresAndLinkOutputString() {
        int cores = 0;
        int everyLink = 0;
        for (Chip chip:chips.values()) {
            cores += chip.nProcessors();
            everyLink += chip.router.size();
        }
        return cores + " cores and " + (everyLink / 2.0) + " links";
    }

    public HasChipLocation bootChip() {
        return this.boot;
    }

    /**
     * Iterable over the destinations of each link.
     * <p>
     * There will be exactly one destination for each Link.
     * While normally all destinations will be unique the is no guarantee.
     *
     * @param chip x and y coordinates for any chip on the board
     * @return A Stream over the destination locations.
     */
    public Iterable<Chip> iterChipsOnBoard(Chip chip) {
        return new Iterable<Chip>() {
            @Override
            public Iterator<Chip> iterator() {
                return new ChipOnBoardIterator(chip.nearestEthernet);
            }
        };
    }

    public CoreSubsetsFailedChipsTuple reserveSystemProcessors() {
        maxUserProssorsOnAChip = 0;
        CoreSubsetsFailedChipsTuple result = new CoreSubsetsFailedChipsTuple();

        this.chips.forEach((location, chip) -> {
            int p = chip.reserveASystemProcessor();
            if (p == -1) {
                result.addFailedChip(chip);
            } else {
                result.addCore(location, p);
            }
            if (chip.nUserProcessors() > maxUserProssorsOnAChip) {
                maxUserProssorsOnAChip = chip.nUserProcessors();
            }
        });
        return result;
    }

    public int maximumUserCoresOnChip() {
        return maxUserProssorsOnAChip;
    }

    private int totalAvailableUserCores1() {
        Counter count = new Counter();
        this.chips.forEach((location, chip) -> {
            count.add(chip.nUserProcessors());
        });
        return count.get();
    }

    private int totalAvailableUserCores2() {
        return chips.values().stream().map(Chip::nUserProcessors).
                mapToInt(Integer::intValue).sum();

    }

    public int totalAvailableUserCores() {
        int count = 0;
        for (Chip chip :chips.values()) {
            count += chip.nUserProcessors();
        }
        return count;
    }

    public int totalCores() {
        int count = 0;
        for (Chip chip :chips.values()) {
            count += chip.nProcessors();
        }
        return count;
    }

    public MachineVersion version() {
        return version;
    }

    private class ChipOnBoardIterator implements Iterator<Chip> {

        private HasChipLocation root;
        private Chip nextChip;
        private Iterator<ChipLocation> singleBoardIterator;

        ChipOnBoardIterator(HasChipLocation root) {
            this.root = root;
            SpiNNakerTriadGeometry geometry =
                    SpiNNakerTriadGeometry.getSpinn5Geometry();
            singleBoardIterator = geometry.singleBoardIterator();
            prepareNextChip();
        }

        @Override
        public boolean hasNext() {
            return nextChip != null;
        }

        @Override
        public Chip next() {
            if (nextChip == null) {
                throw new NoSuchElementException ("No more chips available.");
            }
            Chip result = nextChip;
            prepareNextChip();
            return result;
        }

        private void prepareNextChip() {
            while (singleBoardIterator.hasNext()) {
                ChipLocation local = singleBoardIterator.next();
                ChipLocation global = normalizedLocation(
                        root.getX() + local.getX(), root.getY() + root.getX());
                nextChip = getChipAt(global);
                if (nextChip != null) {
                    return;
                }
            }
            nextChip = null;
        }

    }


}
