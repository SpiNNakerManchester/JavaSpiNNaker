package uk.ac.manchester.spinnaker.messages.model;

import static org.junit.jupiter.api.Assertions.*;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import org.junit.jupiter.api.Test;

import uk.ac.manchester.spinnaker.machine.CoreLocation;

class TestVersionInfo {
	private ByteBuffer packVersionData(int arg1, int arg2, int arg3,
			byte[] data) {
		ByteBuffer buffer = ByteBuffer.allocate(25).order(ByteOrder.LITTLE_ENDIAN);
        buffer.putInt(arg1).putInt(arg2).putInt(arg3).put(data).flip();
		return buffer;
	}

	@Test
    void testRetrievingBitsFromVersionData() throws UnsupportedEncodingException {
        int p2p_adr = 0xf0a1;
        int phys_cpu = 0xff;
        int virt_cpu = 0x0b;
        int ver_number = 0xff;
        int arg1 = (p2p_adr << 16) | (phys_cpu << 8) | virt_cpu;
        int buffer_size = 0x10;
        int arg2 = (ver_number << 16) | buffer_size;
        int build_date = 0x1000;
        int arg3 = build_date;
        byte[] data = "my/spinnaker".getBytes("ASCII");

        ByteBuffer version_data = packVersionData(arg1, arg2, arg3, data);

		VersionInfo vi = new VersionInfo(version_data);
		assertEquals("my", vi.name);
		assertEquals(new Version(2, 55, 0), vi.versionNumber);
		assertEquals("spinnaker", vi.hardware);
		assertEquals(new CoreLocation(0xf0, 0xa1, 0x0b), vi.core);
        assertEquals(build_date, vi.buildDate);
        assertEquals("my/spinnaker", vi.versionString);
	}

	@Test
    void testInvalidVersionDataFormat() throws UnsupportedEncodingException {
		int p2p_adr = 0xf0a1;
		int phys_cpu = 0xff;
		int virt_cpu = 0x0b;
		int ver_number = 0xff;
		int arg1 = (p2p_adr << 16) | (phys_cpu << 8) | virt_cpu;
		int buffer_size = 0x10;
		int arg2 = (ver_number << 16) | buffer_size;
		int build_date = 0x1000;
		int arg3 = build_date;
		byte[] data = "my.spinnaker".getBytes("ASCII");

		ByteBuffer version_data = packVersionData(arg1, arg2, arg3, data);

		assertThrows(IllegalArgumentException.class, ()->{
            new VersionInfo(version_data);
        });
    }

	@Test
   void testInvalidSizedVersionData() throws UnsupportedEncodingException {
		int p2p_adr = 0xf0a1;
		int phys_cpu = 0xff;
		int virt_cpu = 0x0b;
		int ver_number = 0xff;
		int arg1 = (p2p_adr << 16) | (phys_cpu << 8) | virt_cpu;
		int buffer_size = 0x10;
		int arg2 = ((ver_number << 16) | buffer_size);
		// int build_date = 0x1000;
		// int arg3 = build_date;
        byte[] data = "my/spinnaker".getBytes("ASCII");

        // Oh arg3, where art thou?
        ByteBuffer version_data = ByteBuffer.allocate(21).order(ByteOrder.LITTLE_ENDIAN);
        version_data.putInt(arg1).putInt(arg2)/*.putInt(arg3)*/.put(data).flip();

        assertThrows(IllegalArgumentException.class, ()->{
            new VersionInfo(version_data);
        });
    }
}
