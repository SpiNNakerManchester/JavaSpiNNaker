/*
 * Copyright (c) 2018 The University of Manchester
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
package uk.ac.manchester.spinnaker.data_spec;

import static java.io.File.createTempFile;
import static java.nio.ByteOrder.LITTLE_ENDIAN;
import static java.util.stream.IntStream.range;
import static org.junit.jupiter.api.Assertions.*;
import static uk.ac.manchester.spinnaker.data_spec.Constants.APPDATA_MAGIC_NUM;
import static uk.ac.manchester.spinnaker.data_spec.Constants.DSE_VERSION;
import static uk.ac.manchester.spinnaker.data_spec.Constants.MAX_MEM_REGIONS;
import static uk.ac.manchester.spinnaker.data_spec.generator.Generator.makeSpec;
import static uk.ac.manchester.spinnaker.data_spec.generator.Generator.makeSpecStream;
import static uk.ac.manchester.spinnaker.machine.MemoryLocation.NULL;

import java.io.IOException;
import java.nio.ByteBuffer;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import uk.ac.manchester.spinnaker.data_spec.generator.Generator;
import uk.ac.manchester.spinnaker.data_spec.impl.ExecuteBreakInstruction;

public class TestDataSpecExecutor {
	@Test
	void testSimpleSpec() throws IOException, DataSpecificationException {
		var spec = makeSpec(s -> {
			s.reserveMemoryRegion(0, 100);
			s.reserveMemoryRegion(1, 200, true);
			s.reserveMemoryRegion(2, 4);
			s.reserveMemoryRegion(3, 12, false, 5);
			s.referenceMemoryRegion(4, 2);
			s.switchWriteFocus(0);
			s.writeArray(0, 1, 2);
			s.setWritePointer(20);
			s.writeValue(4);
			s.switchWriteFocus(2);
			s.writeValue(3);
			s.setWritePointer(0);
			s.writeValue(10);
			s.endSpecification();
		});

		// Execute the spec
		@SuppressWarnings("resource")
		var executor = new Executor(spec, 400);
		executor.execute();
		executor.close();

		// Test the size
		int headerAndTableSize = ((MAX_MEM_REGIONS * 3) + 2) * 4;
		assertEquals(headerAndTableSize + 100 + 200 + 4 + 12,
				executor.getConstructedDataSize());

		// Test the unused regions
		range(5, MAX_MEM_REGIONS)
				.forEach(r -> assertNull(executor.getRegion(r)));

		// Test region 0
		var reg0 = executor.getRegion(0);
		assertTrue(reg0 instanceof MemoryRegionReal);
		var region0 = (MemoryRegionReal) reg0;
		assertEquals(100, region0.getAllocatedSize());
		assertEquals(24, region0.getMaxWritePointer());
		assertFalse(region0.isUnfilled());
		var expectedR0 = new int[] {
			0, 1, 2, 0, 0, 4
		};
		var r0data = region0.getRegionData().asReadOnlyBuffer()
				.order(LITTLE_ENDIAN);
		r0data.flip();
		var dst = new int[expectedR0.length];
		r0data.asIntBuffer().get(dst);
		assertArrayEquals(expectedR0, dst);

		// Test region 1
		var reg1 = executor.getRegion(1);
		assertTrue(reg1 instanceof MemoryRegionReal);
		var region1 = (MemoryRegionReal) reg1;
		assertEquals(200, region1.getAllocatedSize());
		assertTrue(region1.isUnfilled());

		// Test region 2
		var reg2 = executor.getRegion(2);
		assertTrue(reg2 instanceof MemoryRegionReal);
		var region2 = (MemoryRegionReal) reg2;
		assertEquals(4, region2.getAllocatedSize());
		assertEquals(10, region2.getRegionData().getInt(0));

		// Test region 3
		var reg3 = executor.getRegion(3);
		assertTrue(reg3 instanceof MemoryRegionReal);
		var region3 = (MemoryRegionReal) reg3;
		assertEquals(12, region3.getAllocatedSize());

		// Test region 4 (reference)
		var reg4 = executor.getRegion(4);
		assertTrue(reg4 instanceof MemoryRegionReference);
		var region4 = (MemoryRegionReference) reg4;
		assertEquals(region4.getReference(), new Reference(2));

		// Test referencing
		assertArrayEquals(executor.getReferenceableRegions().toArray(),
				new Integer[] {
					3
				});
		assertArrayEquals(executor.getRegionsToFill().toArray(), new Integer[] {
			4
		});

		// Test the pointer table
		var buffer = ByteBuffer.allocate(4096).order(LITTLE_ENDIAN);
		executor.setBaseAddress(NULL);
		executor.addPointerTable(buffer);
		var table = buffer.flip().asIntBuffer();
		assertEquals(MAX_MEM_REGIONS * 3, table.limit());
		assertEquals(headerAndTableSize, table.get(0));
		assertEquals(headerAndTableSize + 100, table.get(3));
		assertEquals(headerAndTableSize + 300, table.get(6));
		assertEquals(headerAndTableSize + 304, table.get(9));
		range(4, MAX_MEM_REGIONS)
				.forEach(r -> assertEquals(0, table.get(r * 3)));

		// Test the header
		buffer.clear();
		executor.addHeader(buffer);
		var header = buffer.flip().asIntBuffer();
		assertEquals(2, header.limit());
		assertEquals(APPDATA_MAGIC_NUM, header.get(0));
		assertEquals(DSE_VERSION, header.get(1));
	}

	@Test
	void testTrivialSpec() throws IOException, DataSpecificationException {
		var spec = makeSpec(s -> {
			s.nop();
			s.endSpecification();
		});

		// Execute the spec
		@SuppressWarnings("resource")
		var executor = new Executor(spec, 400);
		executor.execute();
		executor.close();

		executor.regions().forEach(Assertions::assertNull);
	}

	@Test
	void testComplexSpec() throws IOException, DataSpecificationException {
		var spec = makeSpec(s -> {
			s.reserveMemoryRegion(0, 44);
			s.switchWriteFocus(0);
			s.setRegisterValue(3, 0x31323341);
			s.writeValueFromRegister(3);
			s.setRegisterValue(3, 0x31323342);
			s.writeValueFromRegister(3);
			s.setRegisterValue(3, 0x31323344);
			s.writeValueFromRegister(3);
			s.setRegisterValue(3, 0x31323347);
			s.writeValueFromRegister(3);
			s.setRegisterValue(3, 0x3132334B);
			s.writeValueFromRegister(3);
			s.setRegisterValue(2, 24);
			s.setWritePointerFromRegister(2);
			s.writeArray(new Number[] {
				0x61, 0x62, 0x63, 0x64
			}, Generator.DataType.INT8);
			s.setRegisterValue(5, 4);
			s.writeRepeatedValue(0x70, 5, Generator.DataType.INT8);
			s.writeValue(0x7d, Generator.DataType.INT64);
			s.endSpecification();
		});

		// Execute the spec
		try (var executor = new Executor(spec, 400)) {
			executor.execute();

			var reg = executor.getRegion(0);
			assertTrue(reg instanceof MemoryRegionReal);
			var r = (MemoryRegionReal) reg;
			assertEquals(44, r.getAllocatedSize());
			assertEquals(40, r.getMaxWritePointer());
			assertFalse(r.isUnfilled());
			assertArrayEquals(
					("A321" + "B321" + "D321" + "G321" + "K321" + "\0\0\0\0"
							+ "abcd" + "pppp" + "}\0\0\0" + "\0\0\0\0"
							+ "\0\0\0\0").getBytes("ASCII"),
					r.getRegionData().array());
			assertEquals(44, executor.getTotalSpaceAllocated());
		}
	}

	@Test
	void testTrivialSpecFromStream()
			throws IOException, DataSpecificationException {
		var spec = makeSpecStream(s -> s.endSpecification());

		// Execute the spec
		try (var executor = new Executor(spec, 400)) {
			executor.execute();
			executor.regions().forEach(Assertions::assertNull);
		}
	}

	@Test
	void testTrivialSpecFromFile()
			throws IOException, DataSpecificationException {
		var f = createTempFile("dse", ".spec");
		try {
			makeSpec(f, s -> s.endSpecification());

			// Execute the spec
			try (var executor = new Executor(f, 400)) {
				executor.execute();
				executor.regions().forEach(Assertions::assertNull);
			}
		} finally {
			f.delete();
		}
	}

	@Test
	void testFailingSpec() throws IOException {
		var spec = makeSpec(s -> {
			s.fail();
			s.endSpecification();
		});

		// Execute the spec
		try (var executor = new Executor(spec, 400)) {
			assertThrows(ExecuteBreakInstruction.class, executor::execute);
		}
	}
}
