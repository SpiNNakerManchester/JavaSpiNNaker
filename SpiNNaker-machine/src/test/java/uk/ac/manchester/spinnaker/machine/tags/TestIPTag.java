/*
 * Copyright (c) 2018 The University of Manchester
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package uk.ac.manchester.spinnaker.machine.tags;

import java.io.IOException;
import uk.ac.manchester.spinnaker.machine.ChipLocation;
import uk.ac.manchester.spinnaker.machine.bean.MapperFactory;
import java.net.InetAddress;
import java.net.UnknownHostException;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

/**
 *
 * @author Christian-B
 */
public class TestIPTag {

	private InetAddress createInetAddress(byte lastByte)
			throws UnknownHostException {
		byte[] bytes = {127, 0, 0, lastByte};
		return InetAddress.getByAddress(bytes);
	}

	@Test
	public void testBasic() throws UnknownHostException {
		var boardAddress = createInetAddress((byte) 45);
		var destination = new ChipLocation(3, 3);
		int tagID = 10;
		var targetAddress = createInetAddress((byte) 55);
		Integer port = 20;
		boolean stripSDP = true;
		var trafficIdentifier = TrafficIdentifier.DEFAULT;
		var instance = new IPTag(boardAddress, destination, tagID,
				targetAddress, port, stripSDP, trafficIdentifier);
		assertEquals(boardAddress, instance.getBoardAddress());
		assertEquals(destination, instance.getDestination());
		assertEquals(tagID, instance.getTag());
		assertEquals(targetAddress, instance.getIPAddress());
		assertEquals(port, instance.getPort());
		assertEquals(stripSDP, instance.isStripSDP());
		assertEquals(trafficIdentifier, instance.getTrafficIdentifier());
	}

	@Test
	public void testDefaults1() throws UnknownHostException {
		var boardAddress = createInetAddress((byte) 45);
		var destination = new ChipLocation(3, 3);
		int tagID = 10;
		var targetAddress = createInetAddress((byte) 55);
		var instance =
				new IPTag(boardAddress, destination, tagID, targetAddress);
		assertEquals(boardAddress, instance.getBoardAddress());
		assertEquals(destination, instance.getDestination());
		assertEquals(tagID, instance.getTag());
		assertEquals(targetAddress, instance.getIPAddress());
		assertEquals(null, instance.getPort());
		assertFalse(instance.isStripSDP());
		assertEquals(IPTag.DEFAULT_TRAFFIC_IDENTIFIER,
				instance.getTrafficIdentifier());

		Integer port = 20;
		instance.setPort(port);
		assertEquals(port, instance.getPort());
		instance.setPort(port);
		assertThrows(Exception.class, () -> {
			instance.setPort(port + 1);
		});
	}

	@Test
	public void testDefaults2() throws UnknownHostException {
		var boardAddress = createInetAddress((byte) 45);
		var destination = new ChipLocation(3, 3);
		int tagID = 10;
		var targetAddress = createInetAddress((byte) 55);
		Integer port = 20;
		var instance = new IPTag(boardAddress, destination, tagID,
				targetAddress, port);
		assertEquals(boardAddress, instance.getBoardAddress());
		assertEquals(destination, instance.getDestination());
		assertEquals(tagID, instance.getTag());
		assertEquals(targetAddress, instance.getIPAddress());
		assertEquals(port, instance.getPort());
		assertFalse(instance.isStripSDP());
		assertEquals(IPTag.DEFAULT_TRAFFIC_IDENTIFIER,
				instance.getTrafficIdentifier());
	}

	@Test
	public void testDefaul3() throws UnknownHostException {
		var boardAddress = createInetAddress((byte) 45);
		var destination = new ChipLocation(3, 3);
		int tagID = 10;
		var targetAddress = createInetAddress((byte) 55);
		Integer port = 20;
		boolean stripSDP = true;
		var instance = new IPTag(boardAddress, destination, tagID,
				targetAddress, port, stripSDP);
		assertEquals(boardAddress, instance.getBoardAddress());
		assertEquals(destination, instance.getDestination());
		assertEquals(tagID, instance.getTag());
		assertEquals(targetAddress, instance.getIPAddress());
		assertEquals(port, instance.getPort());
		assertEquals(stripSDP, instance.isStripSDP());
		assertEquals(IPTag.DEFAULT_TRAFFIC_IDENTIFIER,
				instance.getTrafficIdentifier());
	}

	@Test
	public void testDefaults4() throws UnknownHostException {
		var boardAddress = createInetAddress((byte) 45);
		var destination = new ChipLocation(3, 3);
		int tagID = 10;
		var targetAddress = createInetAddress((byte) 55);
		var instance = new IPTag(boardAddress, destination, tagID,
				targetAddress, true);
		assertEquals(boardAddress, instance.getBoardAddress());
		assertEquals(destination, instance.getDestination());
		assertEquals(tagID, instance.getTag());
		assertEquals(targetAddress, instance.getIPAddress());
		assertEquals(null, instance.getPort());
		assertTrue(instance.isStripSDP());
		assertEquals(IPTag.DEFAULT_TRAFFIC_IDENTIFIER,
				instance.getTrafficIdentifier());
	}

	private void testNotEquals(IPTag tag1, IPTag tag2) {
		assertNotEquals(tag1, tag2);
		assertNotEquals(tag1.hashCode(), tag2.hashCode());
	}

	@Test
	public void testEquals() throws UnknownHostException {
		var tag1 = new IPTag(createInetAddress((byte) 45),
				new ChipLocation(3, 3), 10, createInetAddress((byte) 55), 20,
				true, TrafficIdentifier.DEFAULT);
		var tag1a = new IPTag(createInetAddress((byte) 45),
				new ChipLocation(3, 3), 10, createInetAddress((byte) 55), 20,
				true, TrafficIdentifier.DEFAULT);
		assertEquals(tag1, tag1a);
		assertEquals(tag1.hashCode(), tag1a.hashCode());
		testNotEquals(tag1,
				new IPTag(createInetAddress((byte) 46), new ChipLocation(3, 3),
						10, createInetAddress((byte) 55), 20, true,
						TrafficIdentifier.DEFAULT));
		testNotEquals(tag1,
				new IPTag(createInetAddress((byte) 45), new ChipLocation(3, 2),
						10, createInetAddress((byte) 55), 20, true,
						TrafficIdentifier.DEFAULT));
		testNotEquals(tag1,
				new IPTag(createInetAddress((byte) 45), new ChipLocation(3, 3),
						11, createInetAddress((byte) 55), 20, true,
						TrafficIdentifier.DEFAULT));
		testNotEquals(tag1,
				new IPTag(createInetAddress((byte) 45), new ChipLocation(3, 3),
						10, createInetAddress((byte) 56), 20, true,
						TrafficIdentifier.DEFAULT));
		testNotEquals(tag1,
				new IPTag(createInetAddress((byte) 45), new ChipLocation(3, 3),
						10, createInetAddress((byte) 55), 21, true,
						TrafficIdentifier.DEFAULT));
		testNotEquals(tag1,
				new IPTag(createInetAddress((byte) 45), new ChipLocation(3, 3),
						10, createInetAddress((byte) 55), 20, false,
						TrafficIdentifier.DEFAULT));

		assertNotEquals(tag1, "tag1");
		assertFalse(tag1.equals(null));
		IPTag nullTag = null;
		assertFalse(tag1.equals(nullTag));
	}

	@Test
	public void testFromJson() throws IOException {
		var json = " {\"x\": 0, \"y\": 0, "
				+ "\"boardAddress\": \"192.168.240.253\", "
				+ "\"targetAddress\": \"localhost\", \"stripSDP\": true, "
				+ "\"tagID\": 1, \"trafficIdentifier\": \"DATA_SPEED_UP\"}";
		var mapper = MapperFactory.createMapper();
		var fromJson = mapper.readValue(json, IPTag.class);
		assertNotNull(fromJson);
	}

}
