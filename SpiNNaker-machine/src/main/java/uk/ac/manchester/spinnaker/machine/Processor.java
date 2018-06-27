/*
 * Copyright (c) 2018 The University of Manchester
 */
package uk.ac.manchester.spinnaker.machine;

import uk.ac.manchester.spinnaker.utils.UnitConstants;

/**
 * A processor object included in a SpiNNaker chip.
 *
 * @see <a
 * href="https://github.com/SpiNNakerManchester/SpiNNMachine/blob/master/spinn_machine/processor.py">
 * Python Version</a>
 *
 * @author Christian-B
 */
public class Processor {
    private static final Processor[] NON_MONITOR =
        new Processor[MachineDefaults.PROCESSORS_PER_CHIP];
    private static final Processor[] MONITOR =
        new Processor[MachineDefaults.PROCESSORS_PER_CHIP];

    /** The ID of the processor. */
    public final int processorId;

    /** The clock speed of the processor in cycles per second. */
    public final int clockSpeed;

    /**
     * Determines if the processor is the monitor, and therefore not
     * to be allocated.
     */
    public final boolean isMonitor;

    /** The amount of DTCM available on this processor. */
    public final int dtcmAvailable;

    /**
     * Main Constructor for a chip with all values provided.
     *
     * @param processorId ID of the processor in the chip.
     * @param clockSpeed The number of CPU cycles per second of the processor.
     * @param dtcmAvailable Data Tightly Coupled Memory available.
     * @param isMonitor  Determines if the processor is considered the
     *      monitor processor, and so should not be otherwise allocated.
     */
    public Processor(int processorId, int clockSpeed, int dtcmAvailable,
            boolean isMonitor) throws SpinnMachineInvalidParameterException {
        this.processorId = processorId;
        if (clockSpeed <= 0) {
            throw new SpinnMachineInvalidParameterException(
                "clockSpeed parameter " + clockSpeed
                + " cannot be less than or equal to zero");
        }
        this.clockSpeed = clockSpeed;
        if (dtcmAvailable <= 0) {
            throw new SpinnMachineInvalidParameterException(
                "dtcmAvailable parameter " + dtcmAvailable
                + " cannot be less than or equal to zero");
        }
        this.dtcmAvailable = dtcmAvailable;
        this.isMonitor = isMonitor;
    }

    /**
     * Constructor for a None monitor chip using defaults for all but.
     *
     * processor Id
     *
     * @param processorId ID of the processor in the chip
     */
    public Processor(int processorId) {
        this(processorId, MachineDefaults.CLOCK_SPEED,
             MachineDefaults.DTCM_AVAILABLE, false);
    }

    /**
     * Constructor for a possible monitor chip using defaults for all but
     * processor Id and isMmonitor.
     *
     * @param processorId ID of the processor in the chip.
     * @param isMonitor  Determines if the processor is considered the
     *      monitor processor, and so should not be otherwise allocated.
     */
    public Processor(int processorId, boolean isMonitor) {
        this(processorId, MachineDefaults.CLOCK_SPEED,
             MachineDefaults.DTCM_AVAILABLE, isMonitor);
    }

    /**
     * The number of CPU cycles available from this processor per ms.
     *
     * @return The number of CPU cycles available from this processor per ms.
     */
    public int cpuCyclesAvailable() {
        return clockSpeed / UnitConstants.MEGAHERTZ_PRE_KILOHERTZ;
    }

    /**
     * Provides a clone of this processor but changing it to a system processor.
     *
     * @return A different Processor with all the same parameter values EXCEPT
     *      isMonitor which will always be true.
     */
    public final Processor cloneAsSystemProcessor() {
        if (this.clockSpeed == MachineDefaults.CLOCK_SPEED
            && this.dtcmAvailable == MachineDefaults.DTCM_AVAILABLE) {
                return factory(this.processorId, true);
        } else {
            return new Processor(this.processorId, this.clockSpeed,
                this.dtcmAvailable, true);
        }
    }

    @Override
    public String toString() {
        return "[CPU: id=" + this.processorId
            + ", clock_speed="
            + this.clockSpeed / UnitConstants.MEGAHERTZ_PRE_HERTZ
            + " MHz, monitor=" + this.isMonitor + "]";
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 47 * hash + this.processorId;
        hash = 47 * hash + this.clockSpeed;
        hash = 47 * hash + (this.isMonitor ? 1 : 0);
        hash = 47 * hash + this.dtcmAvailable;
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Processor other = (Processor) obj;
        if (this.processorId != other.processorId) {
            return false;
        }
        if (this.clockSpeed != other.clockSpeed) {
            return false;
        }
        if (this.isMonitor != other.isMonitor) {
            return false;
        }
        if (this.dtcmAvailable != other.dtcmAvailable) {
            return false;
        }
        return true;
    }

    /**
     * Obtain a Processor Object for this ID which could be a monitor.
     *
     * @param processorId ID of the processor in the chip.
     * @param isMonitor  Determines if the processor is considered the
     *      monitor processor, and so should not be otherwise allocated.
     * @return A Processor Object
     */
    public static final Processor factory(int processorId, boolean isMonitor) {
        if (isMonitor) {
            if (NON_MONITOR[processorId] == null) {
                NON_MONITOR[processorId] =
                    new Processor(processorId, isMonitor);
            }
            return NON_MONITOR[processorId];
        }
        if (NON_MONITOR[processorId] == null) {
            NON_MONITOR[processorId] = new Processor(processorId, isMonitor);
        }
        return NON_MONITOR[processorId];
    }
}
