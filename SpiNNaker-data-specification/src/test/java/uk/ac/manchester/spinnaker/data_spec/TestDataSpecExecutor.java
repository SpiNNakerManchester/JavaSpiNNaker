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

import uk.ac.manchester.spinnaker.data_spec.exceptions.DataSpecificationException;
import uk.ac.manchester.spinnaker.data_spec.exceptions.ExecuteBreakInstruction;

public class TestDataSpecExecutor {

	@Test
	void testSimpleSpec() throws IOException, DataSpecificationException {
		ByteBuffer spec = makeSpec(s -> {
			s.reserveMemoryRegion(0, 100);
			s.reserveMemoryRegion(1, 200, true);
			s.reserveMemoryRegion(2, 4);
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
		int header_and_table_size = (MAX_MEM_REGIONS + 2) * 4;
		assertEquals(header_and_table_size + 100 + 200 + 4,
				executor.getConstructedDataSize());

		// Test the unused regions
		range(3, MAX_MEM_REGIONS)
				.forEach(r -> assertNull(executor.getRegion(r)));

		// Test region 0
		MemoryRegion region_0 = executor.getRegion(0);
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
		MemoryRegion region_1 = executor.getRegion(1);
		assertEquals(200, region_1.getAllocatedSize());
		assertTrue(region_1.isUnfilled());

		// Test region 2
		MemoryRegion region_2 = executor.getRegion(2);
		assertEquals(4, region_2.getAllocatedSize());
		assertEquals(10, region_2.getRegionData().getInt(0));

		// Test the pointer table
		ByteBuffer buffer = ByteBuffer.allocate(4096).order(LITTLE_ENDIAN);
		executor.addPointerTable(buffer, 0);
		IntBuffer table = ((ByteBuffer) buffer.flip()).asIntBuffer();
		assertEquals(MAX_MEM_REGIONS, table.limit());
		assertEquals(header_and_table_size, table.get(0));
		assertEquals(header_and_table_size + 100, table.get(1));
		assertEquals(header_and_table_size + 300, table.get(2));
		range(3, MAX_MEM_REGIONS).forEach(r -> assertEquals(0, table.get(r)));

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

			MemoryRegion r = executor.getRegion(0);
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
