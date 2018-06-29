/*
 * Copyright (c) 2018 The University of Manchester
 */
package uk.ac.manchester.spinnaker.machine;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Optional;
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

    public final Router router;

    public final int sdram;

    public final InetAddress ipAddress;

    public final boolean virtual;

    public final int nTagIds;

    public final HasChipLocation nearestEthernet;

    public Chip(int x, int y, Collection<Processor> processors, Router router,
            int sdram, InetAddress ipAddress, boolean virtual, int nTagIds,
            HasChipLocation nearestEthernet) {
        this.x = x;
        this.y = y;
        this.processors = new TreeMap<>();
        nUserProssors = 0;
        for (Processor processor:processors){
            if (this.processors.containsKey(processor.processorId)) {
                throw new IllegalArgumentException();
            } else {
                this.processors.put(processor.processorId, processor);
                if (!processor.isMonitor) {
                     nUserProssors += 1;
                }

            }
        }
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

    public boolean hasProcessor(int processorId) {
        return this.processors.containsKey(processorId);
    }

    public Processor getProcessor(int processorId) {
        return this.processors.get(processorId);
    }

    public Collection<Processor> processors() {
        return Collections.unmodifiableCollection(this.processors.values());
    }

    public int nProcessors() {
        return this.processors.size();
    }

    public int nUserProcessors() {
        return this.nUserProssors;
    }

        def get_first_none_monitor_processor(self):

           def reserve_a_system_processor(self):

    toString
}
