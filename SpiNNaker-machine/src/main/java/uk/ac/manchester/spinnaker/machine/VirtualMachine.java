/*
 * Copyright (c) 2018 The University of Manchester
 */
package uk.ac.manchester.spinnaker.machine;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import uk.ac.manchester.spinnaker.utils.InetFactory;


/**
 * A representation of a SpiNNaker Machine with a number of Chips.
 * <p>
 * Machine is also iterable, providing ((x, y), chip) where:
 * x is the x-coordinate of a chip.
 * y is the y-coordinate of a chip.
 * chip is the chip with the given x, y coordinates.
 *
 * <p>
 * @see <a
 * href="https://github.com/SpiNNakerManchester/SpiNNMachine/blob/master/spinn_machine/machine.py">
 * Python Version</a>

 * @author Christian-B
 */
public class VirtualMachine extends Machine {

     /**
     * Creates a virtual machine to fill the machine dimensions.
     *
     * @param machineDimensions
     *      Size of the machine along the x and y axes in Chips.
     * @param ignoreChips A set of chips to ignore in the machine.
     *      Requests for a "machine" will have these chips excluded,
     *      as if they never existed.
     *      The processor IDs of the specified chips are ignored.
     * @param ignoreCores A map of cores to ignore in the machine.
     *      Requests for a "machine" will have these cores excluded,
     *      as if they never existed.
     * @param ignoreLinks A set of links to ignore in the machine.
     *      Requests for a "machine" will have these links excluded,
     *      as if they never existed.
     */
    public VirtualMachine(MachineDimensions machineDimensions,
            Collection<ChipLocation> ignoreChips,
            Map<ChipLocation, Collection<Integer>> ignoreCores,
            Map<ChipLocation, Collection<Direction>> ignoreLinks) {
        super(machineDimensions, ChipLocation.ZERO_ZERO);

        if (ignoreChips == null) {
            ignoreChips = Collections.emptySet();
        }
        if (ignoreCores == null) {
            ignoreCores = Collections.emptyMap();
        }
        if (ignoreLinks == null) {
            ignoreLinks = new HashMap<>();
        }

        addVerionIgnores(ignoreLinks);

        SpiNNakerTriadGeometry geometry =
                SpiNNakerTriadGeometry.getSpinn5Geometry();

        // Get all the root and therefor ethernet locations
        ArrayList<ChipLocation> roots =
                geometry.getPotentialRootChips(machineDimensions);

        // Get all the valid locations
        HashMap<ChipLocation, ChipLocation> allChips = new HashMap<>();
        for (ChipLocation root: roots) {
            for (ChipLocation local: geometry.singleBoard()) {
                ChipLocation normalized = normalizedLocation(
                        root.getX() + local.getX(), root.getY() + local.getY());
                if (!ignoreChips.contains(normalized)) {
                    allChips.put(normalized, root);
                }
            }
        }
        //System.out.println(allChips.keySet());
        for (ChipLocation location: allChips.keySet()) {
            Router router = getRouter(location, allChips, ignoreLinks);
            InetAddress ipAddress = getIpaddress(location, roots);
            addChip(getChip(location, router, ipAddress,
                    allChips.get(location), ignoreCores));
        }
    }

     /**
     * Creates a virtual machine to fill the machine dimensions with no ignores.
     *
     * @param machineDimensions
     *      Size of the machine along the x and y axes in Chips.
     */
    public VirtualMachine(MachineDimensions machineDimensions) {
        this(machineDimensions, null, null, null);
    }

     /**
     * Creates a virtual machine based on the MachineVersion.
     *
     * @param version
     *      A version which specifies fixed size.
     */
    public VirtualMachine(MachineVersion version) {
        this(version.machineDimensions, null, null, null);
    }

    private void addVerionIgnores(
            Map<ChipLocation, Collection<Direction>> ignoreLinks) {
        if (version.isFourChip) {
            ignoreLinks.putAll(MachineDefaults.FOUR_CHIP_DOWN_LINKS);
        }
    }

    private Router getRouter(ChipLocation location,
            HashMap<ChipLocation, ChipLocation> allChips,
            Map<ChipLocation, Collection<Direction>> ignoreLinks) {
        ArrayList<Link> links;
        if (ignoreLinks.containsKey(location)) {
            links = getLinks(location, allChips, ignoreLinks.get(location));
        } else {
            links = getLinks(location, allChips);
        }
        return new Router(links);
    }

    private ArrayList<Link> getLinks(ChipLocation location,
            HashMap<ChipLocation, ChipLocation> allChips) {
        ArrayList<Link> links = new ArrayList<>();
        for (Direction direction: Direction.values()) {
            ChipLocation destination = normalizedLocation(
                    location.getX() + direction.xChange,
                    location.getY() + direction.yChange);
            if (allChips.containsKey(destination)) {
                links.add(new Link(location, direction, destination));
            }
        }
        return links;
    }

    private ArrayList<Link> getLinks(ChipLocation location,
            HashMap<ChipLocation, ChipLocation> allChips,
            Collection<Direction> ignoreLinks) {
        ArrayList<Link> links = new ArrayList<>();
        for (Direction direction: Direction.values()) {
            if (!ignoreLinks.contains(direction)) {
                ChipLocation destination = normalizedLocation(
                        location.getX() + direction.xChange,
                        location.getY() + direction.yChange);
                if (allChips.containsKey(destination)) {
                    links.add(new Link(location, direction, destination));
                }
            }
        }
        return links;
    }

    private Chip getChip(ChipLocation location, Router router,
            InetAddress ipAddress, ChipLocation ethernet,
            Map<ChipLocation, Collection<Integer>> ignoreCores) {

        if (ignoreCores.containsKey(location)) {
            Collection<Integer> ignoreProcessors = ignoreCores.get(location);
            ArrayList<Processor> processors = new ArrayList<>();
            if (!ignoreProcessors.contains(0)) {
                processors.add(Processor.factory(0, true));
            }
            for (int i = 1; i < MachineDefaults.PROCESSORS_PER_CHIP; i++) {
                if (!ignoreProcessors.contains(i)) {
                    processors.add(Processor.factory(i, false));
                }
            }
            return new Chip(location, processors, router, ipAddress, ethernet);
        } else {
            return new Chip(location, router, ipAddress, ethernet);
        }
    }

    // Hide magic numbers
    private static final int BYTES_PER_IP_ADDRESS = 4;
    private static final int LOCAL_HOST_ONE = 127;
    private static final int FIRST_BYTE = 0;
    private static final int SECOND_BYTE = 1;
    private static final int THIRD_BYTE = 2;
    private static final int FOURTH_BYTE = 3;

    private Inet4Address getIpaddress(
            ChipLocation location, Collection<ChipLocation> roots) {
        if (roots.contains(location)) {
            byte[] bytes = new byte[BYTES_PER_IP_ADDRESS];
            bytes[FIRST_BYTE] = LOCAL_HOST_ONE;
            bytes[SECOND_BYTE] = 0;
            bytes[THIRD_BYTE] = (byte) location.getX();
            bytes[FOURTH_BYTE] = (byte) location.getY();
            try {
                return InetFactory.getByAddress(bytes);
            } catch (UnknownHostException ex) {
                //Should never happen so convert to none catchable
                throw new Error(ex);
            }
        } else {
            return null;
        }
    }

    /**
     * Converts a byte array to an InetAddress.
     * <p>
     * Hides the thrown UnknownHostException in an Runtime error.
     * <p>
     * Hackery to get 100% coverage.
     *
     * @param bytes The raw IP address in network byte order.
     * @return The InetAddress for these bytes
     * @throws Error if InetAddress.getByAddress throws UnknownHostException
     */
    static final InetAddress addressFromBytes(byte[] bytes) {
        try {
            return InetAddress.getByAddress(bytes);
        } catch (UnknownHostException ex) {
            //Should never happen so convert to none catchable
            throw new Error(ex);
        }
    }
}
