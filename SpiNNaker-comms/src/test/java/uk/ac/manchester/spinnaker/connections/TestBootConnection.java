package uk.ac.manchester.spinnaker.connections;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;

import org.junit.jupiter.api.Test;

class TestBootConnection {

	@Test
	void testCreateConnection() throws IOException {
		BootConnection udpConnect = new BootConnection(null, null, null, null);
		// this is a very stupid test!
		assertNotNull(udpConnect);
	}

}
