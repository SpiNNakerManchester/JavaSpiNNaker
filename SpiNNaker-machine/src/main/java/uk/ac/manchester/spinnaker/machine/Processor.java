/*
 * Copyright (c) 2018 The University of Manchester
 */
package uk.ac.manchester.spinnaker.machine;

import uk.ac.manchester.spinnaker.utils.UnitConstants;

/**
 * A processor object included in a SpiNNaker chip.
 * <p>
 * Note: There is No public Constructor instead use a static factory method.
 *
 * @see <a
 * href="https://github.com/SpiNNakerManchester/SpiNNMachine/blob/master/spinn_machine/processor.py">
 * Python Version</a>
 *
 * @author Christian-B
 */
public final class Processor implements Comparable<Processor> {
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
    private Processor(int processorId, int clockSpeed, int dtcmAvailable,
            boolean isMonitor) {
        this.processorId = processorId;
        this.clockSpeed = clockSpeed;
        this.dtcmAvailable = dtcmAvailable;
        this.isMonitor = isMonitor;
    }

    /**
     * The number of CPU cycles available from this processor per millisecond.
     *
     * @return The number of CPU cycles available from this processor per ms.
     */
    public int cpuCyclesAvailable() {
        return clockSpeed / UnitConstants.MEGAHERTZ_PER_KILOHERTZ;
    }

    /**
     * Provides a clone of this processor but changing it to a system processor.
     *
     * @return A different Processor with all the same parameter values EXCEPT
     *      isMonitor which will always be true.
     */
    public Processor cloneAsSystemProcessor() {
        if (this.clockSpeed == MachineDefaults.PROCESSOR_CLOCK_SPEED
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
            + this.clockSpeed / UnitConstants.MEGAHERTZ_PER_HERTZ
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

    @Override
    public int compareTo(Processor other) {
        if (this.processorId < other.processorId) {
            return -1;
        }
        if (this.processorId > other.processorId) {
            return 1;
        }
        // Check the other parameters for consistentcy with equals.
        if (this.isMonitor) {
            if (!other.isMonitor) {
                return 1;
            }
        } else {
            if (other.isMonitor) {
                return -1;
            }
        }
        if (this.dtcmAvailable < other.dtcmAvailable) {
           return -1;
        }
        if (this.dtcmAvailable > other.dtcmAvailable) {
           return 1;
        }
        if (this.clockSpeed < other.clockSpeed) {
           return -1;
        }
        if (this.clockSpeed > other.clockSpeed) {
           return 1;
        }
        return 0;
    }

    /**
     * Obtain a Processor Object for this ID and with these properties.
     *
     * @param processorId ID of the processor in the chip.
     * @param clockSpeed The number of CPU cycles per second of the processor.
     * @param dtcmAvailable Data Tightly Coupled Memory available.
     * @param isMonitor  Determines if the processor is considered the
     *      monitor processor, and so should not be otherwise allocated.
     *
     * @return A Processor Object with these properties
     */
    public static Processor factory(
                int processorId, int clockSpeed, int dtcmAvailable,
                boolean isMonitor)
            throws IllegalArgumentException {
        if (clockSpeed == MachineDefaults.PROCESSOR_CLOCK_SPEED
                && dtcmAvailable == MachineDefaults.DTCM_AVAILABLE) {
            return factory(processorId, isMonitor);
        }
        if (clockSpeed <= 0) {
            throw new IllegalArgumentException(
                    "clockSpeed parameter " + clockSpeed
                    + " cannot be less than or equal to zero");
        }
        if (dtcmAvailable <= 0) {
            throw new IllegalArgumentException(
                    "dtcmAvailable parameter " + dtcmAvailable
                    + " cannot be less than or equal to zero");
        }
        return new Processor(processorId, clockSpeed, dtcmAvailable, isMonitor);
    }

    /**
     * Obtain a Processor Object for this ID which could be a monitor.
     *
     * @param processorId ID of the processor in the chip.
     * @param isMonitor  Determines if the processor is considered the
     *      monitor processor, and so should not be otherwise allocated.
     * @return A default Processor Object with this ID and monitor state
     */
    public static Processor factory(int processorId, boolean isMonitor) {
        if (isMonitor) {
            if (MONITOR[processorId] == null) {
                MONITOR[processorId] =
                    new Processor(processorId,
                        MachineDefaults.PROCESSOR_CLOCK_SPEED,
                        MachineDefaults.DTCM_AVAILABLE, isMonitor);
            }
            return MONITOR[processorId];
        }
        if (NON_MONITOR[processorId] == null) {
            NON_MONITOR[processorId] = new Processor(
                processorId, MachineDefaults.PROCESSOR_CLOCK_SPEED,
                MachineDefaults.DTCM_AVAILABLE, isMonitor);
        }
        return NON_MONITOR[processorId];
    }

    /**
     * Obtain a non-monitor Processor Object for this ID.
     *
     * @param processorId ID of the processor in the chip.
     * @return A default Processor Object with this ID and monitor state
     */
    public static Processor factory(int processorId) {
        return factory(processorId, false);
    }

}
