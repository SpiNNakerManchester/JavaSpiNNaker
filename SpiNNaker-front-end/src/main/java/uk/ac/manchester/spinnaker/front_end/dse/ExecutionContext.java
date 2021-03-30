/*
 * Copyright (c) 2021 The University of Manchester
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package uk.ac.manchester.spinnaker.front_end.dse;

import static java.nio.ByteBuffer.allocate;
import static java.nio.ByteOrder.LITTLE_ENDIAN;
import static uk.ac.manchester.spinnaker.data_spec.Constants.APP_PTR_TABLE_HEADER_SIZE;
import static uk.ac.manchester.spinnaker.data_spec.Constants.MAX_MEM_REGIONS;
import static uk.ac.manchester.spinnaker.messages.Constants.WORD_SIZE;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import uk.ac.manchester.spinnaker.data_spec.DataSpecificationException;
import uk.ac.manchester.spinnaker.data_spec.Executor;
import uk.ac.manchester.spinnaker.data_spec.MemoryRegionReal;
import uk.ac.manchester.spinnaker.data_spec.MemoryRegionReference;
import uk.ac.manchester.spinnaker.machine.CoreLocation;
import uk.ac.manchester.spinnaker.machine.HasCoreLocation;
import uk.ac.manchester.spinnaker.transceiver.ProcessException;
import uk.ac.manchester.spinnaker.transceiver.Transceiver;

/**
 * A context for the execution of multiple data specifications with
 * cross-references.
 */
class ExecutionContext implements AutoCloseable {

    /** The size of the region table. **/
    private static final int REGION_TABLE_SIZE = MAX_MEM_REGIONS * WORD_SIZE;

    /** The size of everything before the first region starts. */
    public static final int TOTAL_HEADER_SIZE =
            APP_PTR_TABLE_HEADER_SIZE + REGION_TABLE_SIZE;

    private final Transceiver txrx;
    private final Map<Integer, RegionToRef> regionsToReference = new HashMap<>();
    private final List<CoreToFill> regionsToFill = new ArrayList<>();

    ExecutionContext(Transceiver txrx) {
        this.txrx = txrx;
    }

    void execute(Executor executor, CoreLocation core, int start)
            throws DataSpecificationException, ProcessException, IOException {
        executor.execute();
        executor.setBaseAddress(start);

        for (int region : executor.getReferenceableRegions()) {
            MemoryRegionReal r = (MemoryRegionReal) executor.getRegion(region);
            int ref = r.getReference();
            if (regionsToReference.containsKey(ref)) {
                RegionToRef reg = regionsToReference.get(ref);
                throw new DataSpecificationException("Reference " + ref +
                        " from " + core + ", " + region + " already exists from "
                        + reg);
            }
            regionsToReference.put(ref, new RegionToRef(core, r.getRegionBase()));
        }

        CoreToFill coreToFill = new CoreToFill(executor, start, core);
        for (int region : executor.getRegionsToFill()) {
            MemoryRegionReference r = (MemoryRegionReference) executor.getRegion(region);
            int ref = r.getReference();
            if (regionsToReference.containsKey(ref)) {
                RegionToRef reg = regionsToReference.get(ref);
                if (!reg.core.onSameChipAs(core)) {
                    throw new DataSpecificationException(
                            "Region " + ref + " on " + reg + " cannot be"
                            + " referenced from " + core + ", " + region);
                }
                r.setRegionBase(reg.pointer);
            } else {
                coreToFill.refs.add(r);
            }
        }

        if (coreToFill.refs.isEmpty()) {
            writeHeader(core, executor, start);
        } else {
            regionsToFill.add(coreToFill);
        }
    }

    private void writeHeader(HasCoreLocation core, Executor executor,
            int startAddress) throws IOException, ProcessException {
        ByteBuffer b =
                allocate(APP_PTR_TABLE_HEADER_SIZE + REGION_TABLE_SIZE)
                        .order(LITTLE_ENDIAN);

        executor.addHeader(b);
        executor.addPointerTable(b);

        b.flip();
        txrx.writeMemory(core.getScampCore(), startAddress, b);
    }

    @Override
    public void close() throws Exception {
        for (CoreToFill toFill : regionsToFill) {
            for (MemoryRegionReference ref : toFill.refs) {
                int reference = ref.getReference();
                if (!regionsToReference.containsKey(reference)) {
                    throw new DataSpecificationException(
                            "Reference " + reference + " from " + toFill
                            + " not found");
                }
                RegionToRef reg = regionsToReference.get(reference);
                if (!reg.core.onSameChipAs(toFill.core)) {
                    throw new DataSpecificationException(
                            "Region " + ref + " on " + reg + " cannot be"
                            + " referenced from " + toFill);
                }
                ref.setRegionBase(reg.pointer);
            }
            writeHeader(toFill.core, toFill.executor, toFill.start);
        }
    }

    private class RegionToRef  {
        final CoreLocation core;
        final int pointer;

        RegionToRef(CoreLocation core, int pointer) {
            this.core = core;
            this.pointer = pointer;
        }

        @Override
        public String toString() {
            return "RegionToRef(" + core + ", " + pointer + ")";
        }
    }

    private class CoreToFill {
        final Executor executor;
        final int start;
        final CoreLocation core;
        final List<MemoryRegionReference> refs = new ArrayList<>();

        CoreToFill(Executor executor, int start, CoreLocation core) {
            this.executor = executor;
            this.start = start;
            this.core = core;
        }

        @Override
        public String toString() {
            return "CoreToFill(" + core + ", " + refs + ")";
        }
    }
}
