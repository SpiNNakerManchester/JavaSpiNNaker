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
import static java.util.Objects.isNull;
import static uk.ac.manchester.spinnaker.data_spec.Constants.APP_PTR_TABLE_BYTE_SIZE;

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
import uk.ac.manchester.spinnaker.data_spec.Reference;
import uk.ac.manchester.spinnaker.machine.CoreLocation;
import uk.ac.manchester.spinnaker.machine.HasCoreLocation;
import uk.ac.manchester.spinnaker.machine.MemoryLocation;
import uk.ac.manchester.spinnaker.transceiver.ProcessException;
import uk.ac.manchester.spinnaker.transceiver.TransceiverInterface;

/**
 * A context for the execution of multiple data specifications with
 * cross-references.
 */
class ExecutionContext implements AutoCloseable {

	private final TransceiverInterface txrx;

	private final Map<Reference, RegionToRef> regionsToRef = new HashMap<>();

	private final List<CoreToFill> regionsToFill = new ArrayList<>();

	ExecutionContext(TransceiverInterface txrx) {
		this.txrx = txrx;
	}

	/**
	 * Execute a data spec and arrange for the header to be written. This is
	 * complex because we may need to postpone writing the header until another
	 * core on the chip has had its memory written, due to memory region
	 * sharing.
	 *
	 * @param executor
	 *            How the execute.
	 * @param core
	 *            What core are we executing for.
	 * @param start
	 *            Where does the core's memory start.
	 * @throws DataSpecificationException
	 *             If something is wrong with the data specification.
	 * @throws ProcessException
	 *             If something goes wrong SpiNNaker-side with the write.
	 * @throws IOException
	 *             If there's a problem with I/O.
	 */
	void execute(Executor executor, CoreLocation core, MemoryLocation start)
			throws DataSpecificationException, ProcessException, IOException {
		executor.execute();
		executor.setBaseAddress(start);

		CoreToFill coreToFill = linkRegionReferences(executor, core, start);
		if (coreToFill.refs.isEmpty()) {
			writeHeader(core, executor, start);
		} else {
			regionsToFill.add(coreToFill);
		}
	}

	private CoreToFill linkRegionReferences(Executor executor,
			CoreLocation core, MemoryLocation start)
			throws DataSpecificationException {
		for (int region : executor.getReferenceableRegions()) {
			MemoryRegionReal r = (MemoryRegionReal) executor.getRegion(region);
			Reference ref = r.getReference();
			if (regionsToRef.containsKey(ref)) {
				RegionToRef reg = regionsToRef.get(ref);
				throw new DataSpecificationException(
						"Reference " + ref + " from " + core + ", " + region
								+ " already exists from " + reg);
			}
			regionsToRef.put(ref, new RegionToRef(core, r.getRegionBase()));
		}

		CoreToFill coreToFill = new CoreToFill(executor, start, core);
		for (int region : executor.getRegionsToFill()) {
			MemoryRegionReference r =
					(MemoryRegionReference) executor.getRegion(region);
			Reference ref = r.getReference();
			if (regionsToRef.containsKey(ref)) {
				RegionToRef reg = regionsToRef.get(ref);
				if (!reg.core.onSameChipAs(core)) {
					throw new DanglingReferenceException(ref, reg, core,
							region);
				}
				r.setRegionBase(reg.pointer);
			} else {
				coreToFill.refs.add(r);
			}
		}
		return coreToFill;
	}

	private void writeHeader(HasCoreLocation core, Executor executor,
			MemoryLocation startAddress) throws IOException, ProcessException {
		ByteBuffer b = allocate(APP_PTR_TABLE_BYTE_SIZE)
				.order(LITTLE_ENDIAN);

		executor.addHeader(b);
		executor.addPointerTable(b);

		b.flip();
		txrx.writeMemory(core.getScampCore(), startAddress, b);
	}

	@Override
	public void close() throws DataSpecificationException, ProcessException,
			IOException {
		// Check for missing
		List<String> errors = new ArrayList<>();
		for (CoreToFill toFill : regionsToFill) {
			for (MemoryRegionReference ref : toFill.refs) {
				checkForCrossReferenceError(errors, toFill, ref);
			}
		}
		if (!errors.isEmpty()) {
			throw new DataSpecificationException(errors.toString());
		}

		// Finish filling things in and write header
		for (CoreToFill toFill : regionsToFill) {
			for (MemoryRegionReference ref : toFill.refs) {
				RegionToRef reg = regionsToRef.get(ref.getReference());
				ref.setRegionBase(reg.pointer);
			}
			writeHeader(toFill.core, toFill.executor, toFill.start);
		}
	}

	private void checkForCrossReferenceError(List<String> errors,
			CoreToFill toFill, MemoryRegionReference ref) {
		Reference reference = ref.getReference();
		RegionToRef reg = regionsToRef.get(reference);

		if (isNull(reg)) {
			StringBuilder potentialRefs = new StringBuilder("Reference ")
					.append(reference).append(" from ").append(toFill)
					.append(" not found from ");
			regionsToRef.values().forEach(region -> {
				if (region.core.onSameChipAs(toFill.core)) {
					potentialRefs.append(ref).append(" (from core ")
							.append(region.core).append("); ");
				}
			});
			errors.add(potentialRefs.toString().trim());
		} else {
			if (!reg.core.onSameChipAs(toFill.core)) {
				errors.add("Region " + ref + " on " + reg
						+ " cannot be referenced from " + toFill);
			}
		}
	}

	static class DanglingReferenceException
			extends DataSpecificationException {
		private static final long serialVersionUID = -5070252348099737363L;

		DanglingReferenceException(Reference ref, RegionToRef reg,
				CoreLocation core, int region) {
			super("Region " + ref + " on " + reg + " cannot be"
					+ " referenced from " + core + ", " + region);
		}

		DanglingReferenceException(MemoryRegionReference ref, RegionToRef reg,
				CoreToFill toFill) {
			super("Region " + ref + " on " + reg + " cannot be"
					+ " referenced from " + toFill);
		}
	}

	// Migrate to record in new enough Java
	private static class RegionToRef {
		final CoreLocation core;

		final MemoryLocation pointer;

		RegionToRef(CoreLocation core, MemoryLocation pointer) {
			this.core = core;
			this.pointer = pointer;
		}

		@Override
		public String toString() {
			return "RegionToRef(" + core + ", " + pointer + ")";
		}
	}

	// Migrate to record in new enough Java
	private static class CoreToFill {
		final Executor executor;

		final MemoryLocation start;

		final CoreLocation core;

		final List<MemoryRegionReference> refs = new ArrayList<>();

		CoreToFill(Executor executor, MemoryLocation start, CoreLocation core) {
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
