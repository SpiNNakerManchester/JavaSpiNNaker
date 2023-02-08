/*
 * Copyright (c) 2018 The University of Manchester
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
package uk.ac.manchester.spinnaker.machine.tags;

import uk.ac.manchester.spinnaker.machine.CoreLocation;
import java.net.InetAddress;
import java.net.UnknownHostException;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

/**
 *
 * @author Christian-B
 */
public class TestReverseIPTag {

	private InetAddress createInetAddress(byte lastByte)
			throws UnknownHostException {
		byte[] bytes = {127, 0, 0, lastByte};
		return InetAddress.getByAddress(bytes);
	}

	@Test
	public void testBasic() throws UnknownHostException {
		var boardAddress = createInetAddress((byte) 45);
		int tagID = 10;
		Integer udpPort = 20;
		int sdpPort = 30;
		var destination = new CoreLocation(2, 3, 4);

		var instance = new ReverseIPTag(boardAddress, tagID, udpPort,
				destination, sdpPort);

		assertEquals(boardAddress, instance.getBoardAddress());
		assertEquals(destination, instance.getDestination());
		assertEquals(tagID, instance.getTag());
		assertEquals(udpPort, instance.getPort());
		assertEquals(sdpPort, instance.getSdpPort());
	}

	@Test
	public void testDefaults() throws UnknownHostException {
		var boardAddress = createInetAddress((byte) 45);
		int tagID = 10;
		Integer udpPort = 20;
		var destination = new CoreLocation(2, 3, 4);

		var instance =
				new ReverseIPTag(boardAddress, tagID, udpPort, destination);

		assertEquals(boardAddress, instance.getBoardAddress());
		assertEquals(destination, instance.getDestination());
		assertEquals(tagID, instance.getTag());
		assertEquals(udpPort, instance.getPort());
		assertEquals(1, instance.getSdpPort());
	}

	private void testNotEquals(ReverseIPTag tag1, ReverseIPTag tag2) {
		assertNotEquals(tag1, tag2);
		assertNotEquals(tag1.hashCode(), tag2.hashCode());
	}

	@Test
	@SuppressWarnings("unused")
	public void testEquals() throws UnknownHostException {
		var boardAddress = createInetAddress((byte) 45);
		int tagID = 10;
		Integer udpPort = 20;
		int sdpPort = 30;
		var destination = new CoreLocation(2, 3, 4);

		var tag = new ReverseIPTag(createInetAddress((byte) 45), 10,
				20, new CoreLocation(2, 3, 4), 30);
		var tag2 = new ReverseIPTag(createInetAddress((byte) 45), 10,
				20, new CoreLocation(2, 3, 4), 30);
		assertEquals(tag, tag2);
		assertEquals(tag.hashCode(), tag2.hashCode());

		testNotEquals(tag, new ReverseIPTag(createInetAddress((byte) 46), 10,
				20, new CoreLocation(2, 3, 4), 30));
		testNotEquals(tag, new ReverseIPTag(createInetAddress((byte) 45), 11,
				20, new CoreLocation(2, 3, 4), 30));
		testNotEquals(tag, new ReverseIPTag(createInetAddress((byte) 45), 10,
				21, new CoreLocation(2, 3, 4), 30));
		testNotEquals(tag, new ReverseIPTag(createInetAddress((byte) 45), 10,
				20, new CoreLocation(2, 1, 4), 30));
		testNotEquals(tag, new ReverseIPTag(createInetAddress((byte) 45), 10,
				20, new CoreLocation(2, 3, 4), 31));

		ReverseIPTag nullTag = null;
		assertFalse(tag.equals(nullTag));
	}

}
