/*
 * Copyright (c) 2018 The University of Manchester
 */
package uk.ac.manchester.spinnaker.machine;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;


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
            Set<ChipLocation> ignoreChips,
            Map<ChipLocation, Collection<Integer>> ignoreCores,
            Map<ChipLocation, Collection<Direction>> ignoreLinks) {
        super(machineDimensions, ChipLocation.ZERO_ZERO);
        addVerionIgnores(ignoreLinks);

        SpiNNakerTriadGeometry geometry =
                SpiNNakerTriadGeometry.getSpinn5Geometry();

        // Get all the root and therefor ethernet locations
        ArrayList<ChipLocation> roots =
                geometry.getPotentialRootChips(machineDimensions);

        // Get all the valid locations
        HashMap<ChipLocation, ChipLocation> allChips = new HashMap();
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
            InetAddress ipAddress = getIpaddress (location, roots);
            addChip(getChip(location, router, ipAddress, allChips.get(location), ignoreCores));
        }
    }

    private void addVerionIgnores(
            Map<ChipLocation, Collection<Direction>> ignoreLinks) {
        switch (version) {
            case TWO:
            case THREE:
                ignoreLinks.putAll(MachineDefaults.FOUR_CHIP_DOWN_LINKS);
                break;
            case FOUR:
            case FIVE:
            case TRIAD_WITH_WRAPAROUND:
            case TRIAD_NO_WRAPAROUND:
                break;
            case NONE_TRIAD_LARGE:
                break;
            case INVALID:
                throw new IllegalStateException(
                        "Based on current maxX:" + machineDimensions.width
                        + " and maxY:" + machineDimensions.height
                        + " no valid board version available.");
            default:
                throw new Error("Unexpected BoardVersion Enum: " + version
                        + " Please reraise an issue.");
        }
    }

    private Router getRouter(ChipLocation location,
            HashMap<ChipLocation, ChipLocation> allChips,
            Map<ChipLocation, Collection<Direction>> ignoreLinks) {
        if (ignoreLinks.containsKey(location)) {
            return getRouter(location, allChips, ignoreLinks.get(location));
        } else {
            return getRouter(location, allChips);
        }
    }

    private Router getRouter(ChipLocation location,
            HashMap<ChipLocation, ChipLocation> allChips) {
        Router router = new Router();
        for (Direction direction: Direction.values()) {
            ChipLocation destination = normalizedLocation(
                    location.getX() + direction.xChange,
                    location.getY() + direction.yChange);
            if (allChips.containsKey(destination)) {
                router.addLink(new Link(location, direction, destination));
            }
        }
        return router;
    }

    private Router getRouter(ChipLocation location,
            HashMap<ChipLocation, ChipLocation> allChips,
            Collection<Direction> ignoreLinks) {
        Router router = new Router();
        for (Direction direction: Direction.values()) {
            if (!ignoreLinks.contains(direction)) {
                ChipLocation destination = normalizedLocation(
                        location.getX() + direction.xChange,
                        location.getY() + direction.yChange);
                if (allChips.containsKey(destination)) {
                    router.addLink(new Link(location, direction, destination));
                }
            }
        }
        return router;
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
                    processors.add(Processor.factory(i, true));
                }
            }
            return new Chip(location, processors, router, ipAddress, ethernet);
        } else {
            return new Chip(location, router, ipAddress, ethernet);
        }
    }

    private InetAddress getIpaddress (
            ChipLocation location, Collection<ChipLocation> roots) {
        if (roots.contains(location)){
            byte[] bytes = new byte[4];
            bytes[0] = 127;
            bytes[1] = 0;
            bytes[2] = (byte)location.getX();
            bytes[3] = (byte)location.getY();
            try {
                return InetAddress.getByAddress(bytes);
            } catch (UnknownHostException ex) {
                //Should never happen so convert to none catchable
                throw new Error(ex);
            }
        } else {
            return null;
        }
    }

}
