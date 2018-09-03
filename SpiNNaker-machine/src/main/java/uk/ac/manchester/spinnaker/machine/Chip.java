/*
 * Copyright (c) 2018 The University of Manchester
 */
package uk.ac.manchester.spinnaker.machine;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.TreeMap;

/**
 * A Description of a Spinnaker Chip including its Router.
 *
 * @see <a href=
 * "https://github.com/SpiNNakerManchester/SpiNNMachine/blob/master/spinn_machine/chip.py">
 * Python Chip Version</a>
 * @see <a href=
 * "https://github.com/SpiNNakerManchester/SpiNNMachine/blob/master/spinn_machine/router.py">
 * Python Router Version</a>
 *
 * @author Christian-B
 */
public class Chip implements HasChipLocation {

    private final ChipLocation location;

    private TreeMap<Integer, Processor> monitorProcessors;

    private TreeMap<Integer, Processor> userProcessors;

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

    private static final TreeMap<Integer, Processor> DEFAULT_USER_PROCESSORS =
            defaultUserProcessors();

    private static final TreeMap<Integer, Processor>
            DEFAULT_MONITOR_PROCESSORS = defaultMonitorProcessors();

    // Note: emergency_routing_enabled not implemented as not used
    // TODO convert_routing_table_entry_to_spinnaker_route


    /**
     * Main Constructor which sets all parameters.
     *
     * @param location
     *            The x and y coordinates of the chip's position
     *            in the two-dimensional grid of chips.
     * @param processors
     *            An iterable of processor objects.
     * @param router
     *            A router for the chip.
     * @param sdram
     *            The size of the sdram.
     * @param ipAddress
     *            The IP address of the chip or None if no Ethernet attached.
     * @param virtual
     *            boolean which defines if this chip is a virtual one
     * @param nTagIds
     *            Number of SDP identifers available.
     * @param nearestEthernet
     *            The nearest Ethernet coordinates or null if none known.
     * @throws IllegalArgumentException
     *            Thrown if multiple chips share the same id.
     */
    @SuppressWarnings("checkstyle:parameternumber")
    public Chip(ChipLocation location, Iterable<Processor> processors,
            Router router, int sdram, InetAddress ipAddress, boolean virtual,
            int nTagIds, HasChipLocation nearestEthernet) {
        this.location = location;
        this.monitorProcessors = new TreeMap<>();
        this.userProcessors =  new TreeMap<>();
        processors.forEach((processor) -> {
            if (this.monitorProcessors.containsKey(processor.processorId)) {
                throw new IllegalArgumentException();
            }
            if (this.userProcessors.containsKey(processor.processorId)) {
                throw new IllegalArgumentException();
            }
            if (processor.isMonitor) {
                this.monitorProcessors.put(processor.processorId, processor);
            } else {
                this.userProcessors.put(processor.processorId, processor);
            }
        });
        this.router = router;
        this.sdram = sdram;
        this.ipAddress = ipAddress;
        this.virtual = virtual;
        this.nTagIds = nTagIds;

        // previous Router stuff
        this.nearestEthernet = nearestEthernet;
    }

    /**
     * Constructor which fills in some default values.
     *
     * @param location
     *            The x and y coordinates of the chip's position
     *            in the two-dimensional grid of chips.
     * @param processors
     *            An iterable of processor objects.
     * @param router
     *            A router for the chip.
     * @param sdram
     *            The size of the sdram.
     * @param ipAddress
     *            The IP address of the chip or None if no Ethernet attached.
     * @param nearestEthernet
     *            The nearest Ethernet coordinates or null if none known.
     * @throws IllegalArgumentException
     *            Thrown if multiple Links share the same sourceLinkDirection.
     *            Thrown if multiple chips share the same id.
     */
    public Chip(ChipLocation location, Iterable<Processor> processors,
            Router router, int sdram, InetAddress ipAddress,
            HasChipLocation nearestEthernet) {
        this(location, processors, router, sdram, ipAddress, false,
                MachineDefaults.N_IPTAGS_PER_CHIP, nearestEthernet);
    }

   /**
     * Constructor for a virtual Chip with the none default processors.
     * <p>
     * Creates the Router on the fly based on the links.
     *
     * @param location
     *            The x and y coordinates of the chip's position
     *            in the two-dimensional grid of chips.
     * @param processors
     *            An iterable of processor objects.
     * @param router
     *            A router for the chip.
     * @param ipAddress
     *            The IP address of the chip or None if no Ethernet attached.
     * @param nearestEthernet
     *            The nearest Ethernet coordinates or null if none known.
     * @throws IllegalArgumentException Indicates another Link with this
     *     sourceLinkDirection has already been added.
     */
     public Chip(ChipLocation location, Iterable<Processor> processors,
             Router router, InetAddress ipAddress,
             HasChipLocation nearestEthernet) {
        this(location, processors, router, MachineDefaults.SDRAM_PER_CHIP,
                ipAddress, false, MachineDefaults.N_IPTAGS_PER_CHIP,
                nearestEthernet);
    }

    /**
     * Constructor for a virtual Chip with the default processors.
     * <p>
     * Creates the Router on the fly based on the links.
     *
     * @param location
     *            The x and y coordinates of the chip's position in the
     *            two-dimensional grid of chips.
     * @param router
     *            A router for the chip.
     * @param ipAddress
     *            The IP address of the chip or None if no Ethernet attached.
     * @param nearestEthernet
     *            The nearest Ethernet coordinates or null if none known.
     * @throws IllegalArgumentException
     *             Indicates another Link with this sourceLinkDirection has
     *             already been added.
     */
    public Chip(ChipLocation location, Router router, InetAddress ipAddress,
            HasChipLocation nearestEthernet) {
        this.location = location;
        this.monitorProcessors = DEFAULT_MONITOR_PROCESSORS;
        this.userProcessors =  DEFAULT_USER_PROCESSORS;
        this.router = router;

        this.sdram = MachineDefaults.SDRAM_PER_CHIP;
        this.ipAddress = ipAddress;

        this.virtual = false;
        this.nTagIds = MachineDefaults.N_IPTAGS_PER_CHIP;

        this.nearestEthernet = nearestEthernet;
    }

    Chip(Chip chip, Router newRouter) {
        this.location = chip.location;
        this.monitorProcessors = chip.monitorProcessors;
        this.userProcessors =  chip.userProcessors;
        this.router = newRouter;

        this.sdram = chip.sdram;
        this.ipAddress = chip.ipAddress;

        this.virtual = chip.virtual;
        this.nTagIds = chip.nTagIds;

        this.nearestEthernet = chip.nearestEthernet;
    }

    private static TreeMap<Integer, Processor> defaultUserProcessors() {
        TreeMap<Integer, Processor> processors = new TreeMap<>();
        for (int i = 1; i < MachineDefaults.PROCESSORS_PER_CHIP; i++) {
           processors.put(i, Processor.factory(i, false));
        }
        return processors;
    }

    private static TreeMap<Integer, Processor> defaultMonitorProcessors() {
        TreeMap<Integer, Processor> processors = new TreeMap<>();
        processors.put(0, Processor.factory(0, true));
        return processors;
    }

    @Override
    public int getX() {
        return location.getX();
    }

    @Override
    public int getY() {
        return location.getY();
    }

    @Override
    public ChipLocation asChipLocation() {
        return location;
    }

    /**
     * Determines if a processor with the given ID exists in the chip.
     * <p>
     * This method will check both the user and monitor processors.
     *
     * @param processorId
     *            Id of the potential processor.
     * @return True if and only if there is a processor for this ID.
     * @deprecated
     *            Keeping track of the Monitor(s) processors may be removed
     *            unless a use case can be found.
     */
    public boolean hasAnyProcessor(int processorId) {
        return this.userProcessors.containsKey(processorId)
                || this.monitorProcessors.containsKey(processorId);
    }

    /**
     * Determines if a user processor with the given ID exists in the chip.
     * <p>
     * Warning: If a Monitor processor exists with this ID this method will
     *      return false. Use @see hasAnyProcessor()
     *
     * @param processorId
     *            Id of the potential processor.
     * @return True if and only if there is a user processor for this ID.
     */
    public boolean hasUserProcessor(int processorId) {
        return this.userProcessors.containsKey(processorId);
    }

    /**
     * Determines if a monitor processor with the given ID exists in the chip.
     * <p>
     * Warning: If a User processor exists with this ID this method will
     *      return false. Use @see hasAnyProcessor()
     *
     * @param processorId
     *            Id of the potential processor.
     * @return True if and only if there is a processor for this ID.
     * @deprecated
     *            Keeping track of the Monitor(s) processors may be removed
     *            unless a use case can be found.
     */
    public boolean hasMonitorProcessor(int processorId) {
        return this.monitorProcessors.containsKey(processorId);
    }

    /**
     * Obtains the Processor with this ID or returns null.
     * <p>
     * This method will check both the user and monitor processors.
     *
     * @param processorId
     *            Id of the potential processor.
     * @return The Processor or null if not is found.
     * @deprecated
     *            Keeping track of the Monitor(s) processors may be removed
     *            unless a use case can be found.
     */
    public Processor getAnyProcessor(int processorId) {
        if (this.userProcessors.containsKey(processorId)) {
            return this.userProcessors.get(processorId);
        } else {
            // This also covers the return null if neither have it.
            return this.monitorProcessors.get(processorId);
        }
    }

    /**
     * Obtains the User Processor with this ID or returns null.
     * <p>
     * This method will only check user processors
     * so will return null even if a monitor processor exists with this id.
     *
     * @param processorId
     *            Id of the potential processor.
     * @return The Processor or null if not is found.
     */
    public Processor getUserProcessor(int processorId) {
        return this.userProcessors.get(processorId);
    }

    /**
     * Obtains the Monitor Processor with this ID or returns null.
     * <p>
     * This method will only check monitor processors
     * so will return null even if a user processor exists with this id.
     *
     * @param processorId
     *            Id of the potential processor.
     * @return The Processor or null if not is found.
     * @deprecated
     *            Keeping track of the Monitor(s) processors may be removed
     *            unless a use case can be found.
     */
    public Processor getMonitorProcessor(int processorId) {
        return this.monitorProcessors.get(processorId);
    }

    /**
     * Return a list off all the Processors on this Chip
     * <p>
     * This method will check both the user and monitor processors.
     * <p>
     * The Processors will be ordered by ProcessorID which are guaranteed to all
     *      be different.
     * <p>
     * The current implementation builds a new list on the fly so this list is
     *      mutable without affecting the Chip.
     * Future implementations could return an unmodifiable list.
     *
     * @return A list of all the processors including both monitor and user.
     * @deprecated
     *            Keeping track of the Monitor(s) processors may be removed
     *            unless a use case can be found.
     */
    public List<Processor> allProcessors() {
        ArrayList<Processor> all =
                new ArrayList<>(this.monitorProcessors.values());
        all.addAll(this.userProcessors.values());
        Collections.sort(all);
        return all;
    }

    /**
     * Return a view over the User Processors on this Chip
     * <p>
     * Monitor processors are not included so every Processor in the list is
     *      guaranteed to have the property isMonitor == false!
     * <p>
     * The Processors will be ordered by ProcessorID which are guaranteed to all
     * be different.
     *
     * @return A unmodifiable View over the processors.
     */
    public Collection<Processor> userProcessors() {
        return Collections.unmodifiableCollection(this.userProcessors.values());
    }

    /**
     * Return a view over the Monitor Processors on this Chip
     * <p>
     * User processors are not included so every Processor in the list is
     *      guaranteed to have the property isMonitor == true!
     * <p>
     * The Processors will be ordered by ProcessorID which are guaranteed to all
     * be different.
     *
     * @return A unmodifiable View over the processors.
     * @deprecated
     *            Keeping track of the Monitor(s) processors may be removed
     *            unless a use case can be found.
     */
    public Collection<Processor> monitorProcessors() {
        return Collections.unmodifiableCollection(
                this.monitorProcessors.values());
    }

    /**
     * The total number of processors.
     *
     * @return The size of the Processor Collection
     * @deprecated
     *            Keeping track of the Monitor(s) processors may be removed
     *            unless a use case can be found.
     */
    public int nProcessors() {
        return this.userProcessors.size() + this.monitorProcessors.size();
    }

    /**
     * The total number of user processors.
     * <p>
     * For just the user processors so ignores monitor processors.
     *
     * @return The size of the Processor Collection
     */
    public int nUserProcessors() {
        return this.userProcessors.size();
    }

    /**
     * Get the first processor in the list which is not a monitor core.
     *
     * @return A Processor
     * @throws NoSuchElementException
     *             If all the Processor(s) are monitors.
     */
    public Processor getFirstUserProcessor()
            throws NoSuchElementException {
        return this.userProcessors.get(this.userProcessors.firstKey());
    }

    // TODO: Work out if we can guarantee:
    /*
     * This method should ONLY be called via
     * :py:meth:`spinn_machine.Machine.reserve_system_processors`
     */
    /**
     * Sets one of the none monitor processors as a system processor.
     * <p>
     * This will reduce by one the result of nUserProcessors()
     *
     * @deprecated Will be removed if confirmed to never be called any more.
     * @return ID of the processor converted to a monitor or -1 to report a
     *         failure
     */
    @SuppressWarnings("unchecked")
    int reserveASystemProcessor() throws IllegalStateException {
        if (this.monitorProcessors == DEFAULT_MONITOR_PROCESSORS) {
            this.monitorProcessors = (TreeMap<Integer, Processor>)
                    DEFAULT_MONITOR_PROCESSORS.clone();
            this.userProcessors = (TreeMap<Integer, Processor>)
                    DEFAULT_USER_PROCESSORS.clone();
        }
        Integer firstKey;
        try {
           firstKey = this.userProcessors.firstKey();
        } catch (NoSuchElementException ex) {
            // This will very rarely happen so no need for pretime checking
            return -1;
        }
        Processor mover = this.userProcessors.get(firstKey);
        this.userProcessors.remove(firstKey);
        this.monitorProcessors.put(firstKey, mover);
        return mover.processorId;
    }

    @Override
    public String toString() {
        return "[Chip: x=" + getX() + ", y=" + getY() + ", sdram=" + sdram
                + ", ip_address=" + this.ipAddress + ", router=" + router
                + ", monitors=" + monitorProcessors.keySet()
                + ", users=" + userProcessors.keySet()
                + ", nearest_ethernet="
                + this.nearestEthernet + "]";
    }

}
