/*
 * Copyright (c) 2021 The University of Manchester
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package uk.ac.manchester.spinnaker.front_end.dse;

import static java.lang.String.format;
import static java.nio.ByteBuffer.allocate;
import static java.nio.ByteOrder.LITTLE_ENDIAN;
import static java.util.Objects.isNull;
import static uk.ac.manchester.spinnaker.data_spec.Constants.APP_PTR_TABLE_BYTE_SIZE;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.errorprone.annotations.Immutable;
import com.google.errorprone.annotations.MustBeClosed;

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
 * A context for the execution of multiple data specifications that handles
 * linking cross-references.
 */
class ExecutionContext implements AutoCloseable {
	private final TransceiverInterface txrx;

	private final Map<Reference, RegionToRef> regionsToRef = new HashMap<>();

	private final List<CoreToFill> regionsToFill = new ArrayList<>();

	@MustBeClosed
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
	 * @throws InterruptedException
	 *             If communications are interrupted.
	 */
	void execute(Executor executor, CoreLocation core, MemoryLocation start)
			throws DataSpecificationException, ProcessException, IOException,
			InterruptedException {
		executor.execute();
		executor.setBaseAddress(start);

		var coreToFill = linkRegionReferences(executor, core, start);
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
			var r = (MemoryRegionReal) executor.getRegion(region);
			var ref = r.getReference();
			if (regionsToRef.containsKey(ref)) {
				var reg = regionsToRef.get(ref);
				throw new DataSpecificationException(format(
						"Reference %s from %s, %d already exists from %s", ref,
						core, region, reg));
			}
			regionsToRef.put(ref, new RegionToRef(core, r.getRegionBase()));
		}

		var coreToFill = new CoreToFill(executor, start, core);
		for (int region : executor.getRegionsToFill()) {
			var r = (MemoryRegionReference) executor.getRegion(region);
			var ref = r.getReference();
			if (regionsToRef.containsKey(ref)) {
				var reg = regionsToRef.get(ref);
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
			MemoryLocation startAddress)
			throws IOException, ProcessException, InterruptedException {
		var b = allocate(APP_PTR_TABLE_BYTE_SIZE).order(LITTLE_ENDIAN);

		executor.addHeader(b);
		executor.addPointerTable(b);

		b.flip();
		txrx.writeMemory(core.getScampCore(), startAddress, b);
	}

	@Override
	public void close() throws DataSpecificationException, ProcessException,
			IOException, InterruptedException {
		// Check for missing
		var errors = new ArrayList<String>();
		for (var toFill : regionsToFill) {
			for (var ref : toFill.refs) {
				checkForCrossReferenceError(errors, toFill, ref);
			}
		}
		if (!errors.isEmpty()) {
			throw new DataSpecificationException(errors.toString());
		}

		// Finish filling things in and write header
		for (var toFill : regionsToFill) {
			for (var ref : toFill.refs) {
				var reg = regionsToRef.get(ref.getReference());
				ref.setRegionBase(reg.pointer);
			}
			writeHeader(toFill.core, toFill.executor, toFill.start);
		}
	}

	private void checkForCrossReferenceError(List<String> errors,
			CoreToFill toFill, MemoryRegionReference ref) {
		var reference = ref.getReference();
		var reg = regionsToRef.get(reference);

		if (isNull(reg)) {
			var potentialRefs = new StringBuilder("Reference ")
					.append(reference).append(" from ").append(toFill)
					.append(" not found from ");
			regionsToRef.values().forEach(region -> {
				if (region.core.onSameChipAs(toFill.core)) {
					potentialRefs.append(ref).append(" (from core ")
							.append(region.core).append("); ");
				}
			});
			errors.add(potentialRefs.toString().strip());
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
			super(format("Region %s on %s cannot be referenced from %s, %d",
					ref, reg, core, region));
		}

		DanglingReferenceException(MemoryRegionReference ref, RegionToRef reg,
				CoreToFill toFill) {
			super(format("Region %s on %s cannot be referenced from %s", ref,
					reg, toFill));
		}
	}

	@Immutable
	private record RegionToRef(CoreLocation core, MemoryLocation pointer) {
	}

	private record CoreToFill(Executor executor, MemoryLocation start,
			CoreLocation core, List<MemoryRegionReference> refs) {
		CoreToFill(Executor executor, MemoryLocation start, CoreLocation core) {
			this(executor, start, core, new ArrayList<>());
		}

		@Override
		public String toString() {
			return "CoreToFill(" + core + ", " + refs + ")";
		}
	}
}
