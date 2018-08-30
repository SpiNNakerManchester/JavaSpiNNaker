package uk.ac.manchester.spinnaker.data_spec;

import static java.util.stream.IntStream.range;
import static org.junit.jupiter.api.Assertions.*;
import static uk.ac.manchester.spinnaker.data_spec.Constants.APPDATA_MAGIC_NUM;
import static uk.ac.manchester.spinnaker.data_spec.Constants.DSE_VERSION;
import static uk.ac.manchester.spinnaker.data_spec.Constants.MAX_MEM_REGIONS;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import org.junit.jupiter.api.Test;

import uk.ac.manchester.spinnaker.data_spec.exceptions.DataSpecificationException;

class TestDSE {

	@Test
	void testSimpleSpec() throws IOException, DataSpecificationException {
		Generator specGen = new Generator();
		specGen.reserve_memory_region(0, 100);
		specGen.reserve_memory_region(1, 200, true);
		specGen.reserve_memory_region(2, 4);
		specGen.switch_write_focus(0);
		specGen.write_array(0, 1, 2);
		specGen.set_write_pointer(20);
		specGen.write_value(4);
		specGen.switch_write_focus(2);
		specGen.write_value(3);
		specGen.set_write_pointer(0);
		specGen.write_value(10);
		specGen.end_specification();

		ByteBuffer spec = specGen.getSpecification();

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
		IntBuffer r0data = region_0.getRegionData().asIntBuffer();
		r0data.rewind();
		int[] dst = new int[6];
		r0data.get(dst);
		assertEquals(new int[] {
				0, 1, 2, 0, 0, 4
		}, dst);

		// Test region 1
		MemoryRegion region_1 = executor.getRegion(1);
		assertEquals(200, region_1.getAllocatedSize());
		assertTrue(region_1.isUnfilled());

		// Test region 2
		MemoryRegion region_2 = executor.getRegion(2);
		assertEquals(4, region_2.getAllocatedSize());
		assertEquals(10, region_2.getRegionData().getInt(0));

		// Test the pointer table
		ByteBuffer buffer = ByteBuffer.allocate(4096);
		executor.addPointerTable(buffer, 0);
		IntBuffer table = buffer.asIntBuffer();
		assertEquals(MAX_MEM_REGIONS, table.position());
		table.rewind();
		assertEquals(header_and_table_size, table.get(0));
		assertEquals(header_and_table_size + 100, table.get(1));
		assertEquals(header_and_table_size + 300, table.get(2));
		range(3, MAX_MEM_REGIONS).forEach(r -> assertEquals(0, table.get(r)));

		// Test the header
		buffer.clear();
		executor.addHeader(buffer);
		IntBuffer header = buffer.asIntBuffer();
		assertEquals(2, header.position());
		header.rewind();
		assertEquals(APPDATA_MAGIC_NUM, header.get(0));
		assertEquals(DSE_VERSION, header.get(1));
	}

	@Test
	void testTrivialSpec() throws IOException, DataSpecificationException {
		Generator specGen = new Generator();
		specGen.end_specification();
		ByteBuffer spec = specGen.getSpecification();

		// Execute the spec
		Executor executor = new Executor(spec, 400);
		executor.execute();
		executor.close();

		range(0, MAX_MEM_REGIONS)
				.forEach(r -> assertNull(executor.getRegion(r)));
	}

	@Test
	void testComplexSpec() throws IOException, DataSpecificationException {
		Generator specGen = new Generator();
		specGen.reserve_memory_region(0, 44);
		specGen.switch_write_focus(0);
		specGen.set_register_value(3, 0x31323341);
		specGen.write_value_from_register(3);
		specGen.set_register_value(3, 0x31323342);
		specGen.write_value_from_register(3);
		specGen.set_register_value(3, 0x31323344);
		specGen.write_value_from_register(3);
		specGen.set_register_value(3, 0x31323347);
		specGen.write_value_from_register(3);
		specGen.set_register_value(3, 0x3132334B);
		specGen.write_value_from_register(3);
		specGen.set_register_value(2, 24);
		specGen.set_write_pointer_from_register(2);
		specGen.write_array(new Number[] {
				0x61, 0x62, 0x63, 0x64
		}, Generator.DataType.INT8);
		specGen.set_register_value(5, 4);
		specGen.write_repeated_value(0x70, 5, Generator.DataType.INT8);
		specGen.write_value(0x7d, Generator.DataType.INT64);
		specGen.end_specification();
		ByteBuffer spec = specGen.getSpecification();

		// Execute the spec
		try (Executor executor = new Executor(spec, 400)) {
			executor.execute();

			MemoryRegion r = executor.getRegion(0);
			assertEquals(44, r.getAllocatedSize());
			assertEquals(40, r.getMaxWritePointer());
			assertFalse(r.isUnfilled());
			assertEquals(
					"A321" + "B321" + "D321" + "G321" + "K321" + "\0\0\0\0"
							+ "abcd" + "pppp" + "}\0\0\0" + "\0\0\0\0"
							+ "\0\0\0\0".getBytes("ISO 8859-1"),
					r.getRegionData().array());

		}
	}
}
