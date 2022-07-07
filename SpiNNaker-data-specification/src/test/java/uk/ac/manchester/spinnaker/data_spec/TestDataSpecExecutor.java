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
import static uk.ac.manchester.spinnaker.data_spec.Generator.*;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class TestDataSpecExecutor {

	@Test
	void testSimpleSpec() throws IOException, DataSpecificationException {
		ByteBuffer spec = makeSpec(s -> {
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
		Executor executor = new Executor(spec, 400);
		executor.execute();
		executor.close();

		// Test the size
		int header_and_table_size = ((MAX_MEM_REGIONS * 3) + 2) * 4;
		assertEquals(header_and_table_size + 100 + 200 + 4 + 12,
				executor.getConstructedDataSize());

		// Test the unused regions
		range(5, MAX_MEM_REGIONS)
				.forEach(r -> assertNull(executor.getRegion(r)));

		// Test region 0
		MemoryRegion reg_0 = executor.getRegion(0);
		assertTrue(reg_0 instanceof MemoryRegionReal);
		MemoryRegionReal region_0 = (MemoryRegionReal) reg_0;
		assertEquals(100, region_0.getAllocatedSize());
		assertEquals(24, region_0.getMaxWritePointer());
		assertFalse(region_0.isUnfilled());
		int[] expectedR0 = new int[] {
			0, 1, 2, 0, 0, 4
		};
		ByteBuffer r0data = region_0.getRegionData().asReadOnlyBuffer()
				.order(LITTLE_ENDIAN);
		r0data.flip();
		int[] dst = new int[expectedR0.length];
		r0data.asIntBuffer().get(dst);
		assertArrayEquals(expectedR0, dst);

		// Test region 1
		MemoryRegion reg_1 = executor.getRegion(1);
		assertTrue(reg_1 instanceof MemoryRegionReal);
		MemoryRegionReal region_1 = (MemoryRegionReal) reg_1;
		assertEquals(200, region_1.getAllocatedSize());
		assertTrue(region_1.isUnfilled());

		// Test region 2
		MemoryRegion reg_2 = executor.getRegion(2);
		assertTrue(reg_2 instanceof MemoryRegionReal);
		MemoryRegionReal region_2 = (MemoryRegionReal) reg_2;
		assertEquals(4, region_2.getAllocatedSize());
		assertEquals(10, region_2.getRegionData().getInt(0));

		// Test region 3
		MemoryRegion reg_3 = executor.getRegion(3);
		assertTrue(reg_3 instanceof MemoryRegionReal);
		MemoryRegionReal region_3 = (MemoryRegionReal) reg_3;
		assertEquals(12, region_3.getAllocatedSize());

		// Test region 4 (reference)
		MemoryRegion reg_4 = executor.getRegion(4);
		assertTrue(reg_4 instanceof MemoryRegionReference);
		MemoryRegionReference region_4 = (MemoryRegionReference) reg_4;
		assertEquals(region_4.getReference(), 2);

		// Test referencing
		assertArrayEquals(executor.getReferenceableRegions().toArray(),
				new Integer[] {
					3
				});
		assertArrayEquals(executor.getRegionsToFill().toArray(), new Integer[] {
			4
		});

		// Test the pointer table
		ByteBuffer buffer = ByteBuffer.allocate(4096).order(LITTLE_ENDIAN);
		executor.setBaseAddress(0);
		executor.addPointerTable(buffer);
		IntBuffer table = ((ByteBuffer) buffer.flip()).asIntBuffer();
		assertEquals(MAX_MEM_REGIONS * 3, table.limit());
		assertEquals(header_and_table_size, table.get(0));
		assertEquals(header_and_table_size + 100, table.get(3));
		assertEquals(header_and_table_size + 300, table.get(6));
		assertEquals(header_and_table_size + 304, table.get(9));
		range(4, MAX_MEM_REGIONS).forEach(r -> assertEquals(0, table.get(r * 3)));

		// Test the header
		buffer.clear();
		executor.addHeader(buffer);
		IntBuffer header = ((ByteBuffer) buffer.flip()).asIntBuffer();
		assertEquals(2, header.limit());
		assertEquals(APPDATA_MAGIC_NUM, header.get(0));
		assertEquals(DSE_VERSION, header.get(1));
	}

	@Test
	void testTrivialSpec() throws IOException, DataSpecificationException {
		ByteBuffer spec = makeSpec(s -> {
			s.nop();
			s.endSpecification();
		});

		// Execute the spec
		Executor executor = new Executor(spec, 400);
		executor.execute();
		executor.close();

		executor.regions().forEach(Assertions::assertNull);
	}

	@Test
	void testComplexSpec() throws IOException, DataSpecificationException {
		ByteBuffer spec = makeSpec(s -> {
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
		try (Executor executor = new Executor(spec, 400)) {
			executor.execute();

			MemoryRegion reg = executor.getRegion(0);
			assertTrue(reg instanceof MemoryRegionReal);
			MemoryRegionReal r = (MemoryRegionReal) reg;
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
		ByteArrayInputStream spec = makeSpecStream(s -> s.endSpecification());

		// Execute the spec
		try (Executor executor = new Executor(spec, 400)) {
			executor.execute();
			executor.regions().forEach(Assertions::assertNull);
		}
	}

	@Test
	void testTrivialSpecFromFile()
			throws IOException, DataSpecificationException {
		File f = createTempFile("dse", ".spec");
		try {
			makeSpec(f, s -> s.endSpecification());

			// Execute the spec
			try (Executor executor = new Executor(f, 400)) {
				executor.execute();
				executor.regions().forEach(Assertions::assertNull);
			}
		} finally {
			f.delete();
		}
	}

	@Test
	void testFailingSpec() throws IOException, DataSpecificationException {
		ByteBuffer spec = makeSpec(s -> {
			s.fail();
			s.endSpecification();
		});

		// Execute the spec
		try (Executor executor = new Executor(spec, 400)) {
			assertThrows(ExecuteBreakInstruction.class, executor::execute);
		}
	}
}
