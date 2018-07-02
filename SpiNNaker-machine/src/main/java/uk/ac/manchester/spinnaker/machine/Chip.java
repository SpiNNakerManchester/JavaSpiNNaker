/*
 * Copyright (c) 2018 The University of Manchester
 */
package uk.ac.manchester.spinnaker.machine;

import java.net.InetAddress;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

/**
 * A Description of a Spinnaker Chip.
 *
 * @see <a
 * href="https://github.com/SpiNNakerManchester/SpiNNMachine/blob/master/spinn_machine/chip.py">
 * Python Version</a>
 *
 * @author Christian-B
 */
public class Chip implements HasChipLocation {

    // This is private to force use of HasChipLocation methods
    private final int x;
    // This is private to force use of HasChipLocation methods
    private final int y;

    // This is private as mutable and implementation could change
    private final TreeMap<Integer, Processor> processors;

    // This is not final as will chane as processors become monitors
    private int nUserProssors;

    /** A router for the chip. */
    public final Router router;

    // Changed from an Object to just an int as Object only had a single value
    /** The size of the sdram. */
    public final int sdram;

    /** The IP address of the chip or None if no Ethernet attached. */
    public final InetAddress ipAddress;

    /** boolean which defines if this chip is a virtual one. */
    public final boolean virtual;

    // Changed from a list of tags to just the number of tags at agr suggestion
    /** Number of SDP identifers available. */
    public final int nTagIds;

    /** The nearest Ethernet coordinates or null if none known. */
    public final HasChipLocation nearestEthernet;

    /**
     * Main Constructor which sets all parameters.
     *
     * @param x The x-coordinate of the chip's position in the two-dimensional
     *      grid of chips.
     * @param y The y-coordinate of the chip's position in the two-dimensional
     *      grid of chips
     * @param processors An iterable of processor objects.
     * @param router a router for the chip.
     * @param sdram The size of the sdram.
     * @param ipAddress The IP address of the chip or None if no Ethernet
     *      attached.
     * @param virtual boolean which defines if this chip is a virtual one
     * @param nTagIds Number of SDP identifers available.
     * @param nearestEthernet The nearest Ethernet coordinates
     *      or null if none known.
     */
    @SuppressWarnings("checkstyle:parameternumber")
    public Chip(int x, int y, Iterable<Processor> processors, Router router,
            int sdram, InetAddress ipAddress, boolean virtual, int nTagIds,
            HasChipLocation nearestEthernet) {
        MachineDefaults.validateChipLocation(x, y);
        this.x = x;
        this.y = y;
        this.processors = new TreeMap<>();
        nUserProssors = 0;
        processors.forEach((processor) -> {
            if (this.processors.containsKey(processor.processorId)) {
                throw new IllegalArgumentException();
            } else {
                this.processors.put(processor.processorId, processor);
                if (!processor.isMonitor) {
                    nUserProssors += 1;
                }

            }
        });
        this.router = router;
        this.sdram = sdram;
        this.ipAddress = ipAddress;
        this.virtual = virtual;
        this.nTagIds = nTagIds;
        this.nearestEthernet = nearestEthernet;
    }

    @Override
    public int getX() {
        return x;
    }

    @Override
    public int getY() {
        return y;
    }

    /**
     * Determines if a processor with the given ID exists in the chip.
     * @param processorId Id of the potential processor.
     * @return True if and only if there is a processor for this ID.
     */
    public boolean hasProcessor(int processorId) {
        return this.processors.containsKey(processorId);
    }

    /**
     * Obtains the Processor with this ID or returns null.
     *
     * @param processorId Id of the potential processor.
     * @return The Processor or null if noe is found.
     * @return
     */
    public Processor getProcessor(int processorId) {
        return this.processors.get(processorId);
    }

    /**
     * Return a view over the Processors on this Chip
     * <p>
     * The Processors will be ordered by ProcessorID
     *      which are guaranteed to all be different.
     *
     * @return A unmodifiable View over the processors.
     */
    public Collection<Processor> processors() {
        return Collections.unmodifiableCollection(this.processors.values());
    }

    /**
     * The total number of processors.
     *
     * @return The size of the Processor Collection
     */
    public int nProcessors() {
        return this.processors.size();
    }

    /**
     * The total number of processors that are not monitors.
     *
     * @return The total number of processors that are not monitors.
     */
    public int nUserProcessors() {
        return this.nUserProssors;
    }

    /**
     * Get the first processor in the list which is not a monitor core.
     *
     * @return A Processor
     * @throws IllegalStateException If all the Processor(s) are monitors.
     */
    public Processor getFirstNoneMonitorProcessor()
            throws IllegalStateException {
        for (Processor processor:processors.values()) {
            if (!processor.isMonitor) {
                return processor;
            }
        }
        throw new IllegalStateException("No None monitor processor found!");
    }

    // TODO: Work out if we can guarantee:
    // This method should ONLY be called via\
    //        :py:meth:`spinn_machine.Machine.reserve_system_processors`
    /**
     * Sets one of the none monitor processors as a system processor.
     * <p>
     * This will reduce by one the result of nUserProcessors()
     *
     * @return ID of the processor converted to a monitor
     * @throws IllegalStateException If all the Processor(s) are monitors.
     */
    public int reserveASystemProcessor() throws IllegalStateException {
        for (Map.Entry<Integer, Processor> entry : processors.entrySet()) {
            if (!entry.getValue().isMonitor) {
                Processor monitor = entry.getValue().cloneAsSystemProcessor();
                processors.replace(entry.getKey(), monitor);
                nUserProssors-= 1;
                return entry.getKey();
            }
        }
        throw new IllegalStateException("No None monitor processor found!");
    }

    @Override
    public String toString() {
        return "[Chip: x=" + getX() + ", y=" + getY() + ", sdram=" + sdram
            + ", ip_address=" + this.ipAddress + ", router=" + router
            + ", processors=" + processors.keySet()
            + ", nearest_ethernet=" + this.nearestEthernet + "]";
    }
}
