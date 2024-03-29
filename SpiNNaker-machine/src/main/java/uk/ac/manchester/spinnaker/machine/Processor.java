/*
 * Copyright (c) 2018 The University of Manchester
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package uk.ac.manchester.spinnaker.machine;

import static java.lang.Integer.compare;
import static uk.ac.manchester.spinnaker.machine.MachineDefaults.DTCM_AVAILABLE;
import static uk.ac.manchester.spinnaker.machine.MachineDefaults.PROCESSORS_PER_CHIP;
import static uk.ac.manchester.spinnaker.machine.MachineDefaults.PROCESSOR_CLOCK_SPEED;
import static uk.ac.manchester.spinnaker.utils.UnitConstants.MEGAHERTZ_PER_HERTZ;
import static uk.ac.manchester.spinnaker.utils.UnitConstants.MEGAHERTZ_PER_KILOHERTZ;

import java.util.Objects;

import com.google.errorprone.annotations.Immutable;

/**
 * A processor object included in a SpiNNaker chip.
 * <p>
 * <strong>Note:</strong> There is no public constructor; instead use a static
 * factory method.
 *
 * @see <a href=
 *      "https://github.com/SpiNNakerManchester/SpiNNMachine/blob/master/spinn_machine/processor.py">
 *      Python Version</a>
 *
 * @author Christian-B
 */
@Immutable
public final class Processor implements Comparable<Processor> {
	private static final Processor[] NON_MONITOR =
			new Processor[PROCESSORS_PER_CHIP];

	private static final Processor[] MONITOR =
			new Processor[PROCESSORS_PER_CHIP];

	/** The ID of the processor. */
	@ValidP
	public final int processorId;

	/** The clock speed of the processor in cycles per second. */
	public final int clockSpeed;

	/**
	 * Determines if the processor is the monitor, and therefore not to be
	 * allocated.
	 */
	public final boolean isMonitor;

	/** The amount of DTCM available on this processor. */
	public final int dtcmAvailable;

	/**
	 * Main constructor for a chip with all values provided.
	 *
	 * @param processorId
	 *            ID of the processor in the chip.
	 * @param clockSpeed
	 *            The number of CPU cycles per second of the processor.
	 * @param dtcmAvailable
	 *            Data Tightly Coupled Memory available.
	 * @param isMonitor
	 *            Determines if the processor is considered the monitor
	 *            processor, and so should not be otherwise allocated.
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
		return clockSpeed / MEGAHERTZ_PER_KILOHERTZ;
	}

	/**
	 * Provides a clone of this processor but changing it to a system processor.
	 *
	 * @return A different Processor with all the same parameter values EXCEPT
	 *         {@code isMonitor} which will always be true.
	 */
	public Processor cloneAsSystemProcessor() {
		if (isStandard(clockSpeed, dtcmAvailable)) {
			return factory(processorId, true);
		} else {
			return new Processor(processorId, clockSpeed, dtcmAvailable, true);
		}
	}

	@Override
	public String toString() {
		return "[CPU: id=" + processorId + ", clock_speed="
				+ (clockSpeed / MEGAHERTZ_PER_HERTZ) + " MHz, monitor="
				+ isMonitor + "]";
	}

	@Override
	public int hashCode() {
		return Objects.hash(processorId, clockSpeed, isMonitor, dtcmAvailable);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof Processor)) {
			return false;
		}
		var other = (Processor) obj;
		return (processorId == other.processorId)
				&& (clockSpeed == other.clockSpeed)
				&& (isMonitor == other.isMonitor)
				&& (dtcmAvailable == other.dtcmAvailable);
	}

	@Override
	public int compareTo(Processor other) {
		int cmp = compare(this.processorId, other.processorId);
		if (cmp != 0) {
			return cmp;
		}
		// Check the other parameters for consistency with equals.
		cmp = Boolean.compare(this.isMonitor, other.isMonitor);
		if (cmp != 0) {
			return cmp;
		}
		cmp = compare(this.dtcmAvailable, other.dtcmAvailable);
		if (cmp != 0) {
			return cmp;
		}
		return compare(this.clockSpeed, other.clockSpeed);
	}

	/**
	 * Do these parameters conform to standard values? Standard values mean that
	 * the processor can be handled by a cached/memoized instance.
	 *
	 * @param clockSpeed
	 *            The number of CPU cycles per second of the processor.
	 * @param dtcmAvailable
	 *            Data Tightly Coupled Memory available.
	 * @return True if they're both equal to expected values.
	 */
	private static boolean isStandard(int clockSpeed, int dtcmAvailable) {
		return (clockSpeed == PROCESSOR_CLOCK_SPEED)
				&& (dtcmAvailable == DTCM_AVAILABLE);
	}

	/**
	 * Obtain a processor for this ID and with these properties.
	 *
	 * @param processorId
	 *            ID of the processor in the chip.
	 * @param clockSpeed
	 *            The number of CPU cycles per second of the processor.
	 * @param dtcmAvailable
	 *            Data Tightly Coupled Memory (DTCM) available.
	 * @param isMonitor
	 *            Determines if the processor is considered the monitor
	 *            processor, and so should not be otherwise allocated.
	 * @return A Processor object with these properties
	 * @throws IllegalArgumentException
	 *             If a nonsense parameter is given.
	 */
	public static Processor factory(int processorId, int clockSpeed,
			int dtcmAvailable, boolean isMonitor)
			throws IllegalArgumentException {
		if (isStandard(clockSpeed, dtcmAvailable)) {
			return factory(processorId, isMonitor);
		}
		if (clockSpeed <= 0) {
			throw new IllegalArgumentException("clockSpeed parameter, "
					+ clockSpeed + ", cannot be less than or equal to zero");
		}
		if (dtcmAvailable <= 0) {
			throw new IllegalArgumentException("dtcmAvailable parameter, "
					+ dtcmAvailable + ", cannot be less than or equal to zero");
		}
		return new Processor(processorId, clockSpeed, dtcmAvailable, isMonitor);
	}

	/**
	 * Obtain a processor object for this ID, which could be a monitor.
	 *
	 * @param processorId
	 *            ID of the processor in the chip.
	 * @param isMonitor
	 *            Determines if the processor is considered the monitor
	 *            processor, and so should not be otherwise allocated.
	 * @return A default processor object with this ID and monitor state
	 */
	public static Processor factory(int processorId, boolean isMonitor) {
		try {
			if (isMonitor) {
				if (MONITOR[processorId] == null) {
					MONITOR[processorId] = new Processor(processorId,
							PROCESSOR_CLOCK_SPEED, DTCM_AVAILABLE, isMonitor);
				}
				return MONITOR[processorId];
			}
			if (NON_MONITOR[processorId] == null) {
				NON_MONITOR[processorId] = new Processor(processorId,
						PROCESSOR_CLOCK_SPEED, DTCM_AVAILABLE, isMonitor);
			}
			return NON_MONITOR[processorId];
		} catch (ArrayIndexOutOfBoundsException ex) {
			// Only happens in rare virtual chips
			return new Processor(processorId, PROCESSOR_CLOCK_SPEED,
					DTCM_AVAILABLE, isMonitor);
		}
	}

	/**
	 * Obtain a non-monitor processor object for this ID.
	 *
	 * @param processorId
	 *            ID of the processor in the chip.
	 * @return A default processor object with this ID
	 */
	public static Processor factory(int processorId) {
		return factory(processorId, false);
	}

}
