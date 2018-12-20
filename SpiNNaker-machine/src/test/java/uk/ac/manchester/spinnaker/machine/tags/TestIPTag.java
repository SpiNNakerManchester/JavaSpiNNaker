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
package uk.ac.manchester.spinnaker.machine.tags;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import uk.ac.manchester.spinnaker.machine.*;
import java.net.InetAddress;
import java.net.UnknownHostException;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import uk.ac.manchester.spinnaker.machine.bean.ChipDetails;
import uk.ac.manchester.spinnaker.machine.bean.MapperFactory;


/**
 *
 * @author Christian-B
 */
public class TestIPTag {

    private InetAddress createInetAddress(byte lastByte) throws UnknownHostException {
        byte[] bytes = {127, 0, 0, lastByte};
        return InetAddress.getByAddress(bytes);
    }

    @Test
    public void testBasic() throws UnknownHostException {
        InetAddress boardAddress = createInetAddress((byte)45);
        ChipLocation destination = new ChipLocation(3, 3);
        int tagID = 10;
        InetAddress targetAddress = createInetAddress((byte)55);
        Integer port = 20;
        boolean stripSDP = true;
        TrafficIdentifier trafficIdentifier = TrafficIdentifier.DEFAULT;
        IPTag instance = new IPTag(boardAddress, destination, tagID,
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
        InetAddress boardAddress = createInetAddress((byte)45);
        ChipLocation destination = new ChipLocation(3, 3);
        int tagID = 10;
        InetAddress targetAddress = createInetAddress((byte)55);
        IPTag instance = new IPTag(boardAddress, destination, tagID,
            targetAddress);
        assertEquals(boardAddress, instance.getBoardAddress());
        assertEquals(destination, instance.getDestination());
        assertEquals(tagID, instance.getTag());
        assertEquals(targetAddress, instance.getIPAddress());
        assertEquals(null, instance.getPort());
        assertFalse(instance.isStripSDP());
        assertEquals(IPTag.DEFAULT_TRAFFIC_IDENTIFIER, instance.getTrafficIdentifier());

        Integer port = 20;
        instance.setPort(port);
        assertEquals(port, instance.getPort());
        instance.setPort(port);
        assertThrows(Exception.class, () -> {
            instance.setPort(port+1);
        });
    }

    @Test
    public void testDefaults2() throws UnknownHostException {
        InetAddress boardAddress = createInetAddress((byte)45);
        ChipLocation destination = new ChipLocation(3, 3);
        int tagID = 10;
        InetAddress targetAddress = createInetAddress((byte)55);
        Integer port = 20;
        IPTag instance = new IPTag(boardAddress, destination, tagID,
            targetAddress, port);
        assertEquals(boardAddress, instance.getBoardAddress());
        assertEquals(destination, instance.getDestination());
        assertEquals(tagID, instance.getTag());
        assertEquals(targetAddress, instance.getIPAddress());
        assertEquals(port, instance.getPort());
        assertFalse(instance.isStripSDP());
        assertEquals(IPTag.DEFAULT_TRAFFIC_IDENTIFIER, instance.getTrafficIdentifier());
    }

    @Test
    public void testDefaul3() throws UnknownHostException {
        InetAddress boardAddress = createInetAddress((byte)45);
        ChipLocation destination = new ChipLocation(3, 3);
        int tagID = 10;
        InetAddress targetAddress = createInetAddress((byte)55);
        Integer port = 20;
        boolean stripSDP = true;
        IPTag instance = new IPTag(boardAddress, destination, tagID,
            targetAddress, port, stripSDP);
        assertEquals(boardAddress, instance.getBoardAddress());
        assertEquals(destination, instance.getDestination());
        assertEquals(tagID, instance.getTag());
        assertEquals(targetAddress, instance.getIPAddress());
        assertEquals(port, instance.getPort());
        assertEquals(stripSDP, instance.isStripSDP());
        assertEquals(IPTag.DEFAULT_TRAFFIC_IDENTIFIER, instance.getTrafficIdentifier());
    }

        @Test
    public void testDefaults4() throws UnknownHostException {
        InetAddress boardAddress = createInetAddress((byte)45);
        ChipLocation destination = new ChipLocation(3, 3);
        int tagID = 10;
        InetAddress targetAddress = createInetAddress((byte)55);
        IPTag instance = new IPTag(boardAddress, destination, tagID,
            targetAddress, true);
        assertEquals(boardAddress, instance.getBoardAddress());
        assertEquals(destination, instance.getDestination());
        assertEquals(tagID, instance.getTag());
        assertEquals(targetAddress, instance.getIPAddress());
        assertEquals(null, instance.getPort());
        assertTrue(instance.isStripSDP());
        assertEquals(IPTag.DEFAULT_TRAFFIC_IDENTIFIER, instance.getTrafficIdentifier());
    }

    private void testNotEquals(IPTag tag1, IPTag tag2) {
        assertNotEquals(tag1, tag2);
        assertNotEquals(tag1.hashCode(), tag2.hashCode());
    }

    @Test
    public void testEquals() throws UnknownHostException {
        IPTag tag1 = new IPTag(
                createInetAddress((byte)45), new ChipLocation(3, 3), 10,
                createInetAddress((byte)55), 20, true, TrafficIdentifier.DEFAULT);
        IPTag tag1a = new IPTag(
                createInetAddress((byte)45), new ChipLocation(3, 3), 10,
                createInetAddress((byte)55), 20, true, TrafficIdentifier.DEFAULT);
        assertEquals(tag1, tag1a);
        assertEquals(tag1.hashCode(), tag1a.hashCode());
        testNotEquals(tag1, new IPTag(
                createInetAddress((byte)46), new ChipLocation(3, 3), 10,
                createInetAddress((byte)55), 20, true, TrafficIdentifier.DEFAULT));
        testNotEquals(tag1, new IPTag(
                createInetAddress((byte)45), new ChipLocation(3, 2), 10,
                createInetAddress((byte)55), 20, true, TrafficIdentifier.DEFAULT));
        testNotEquals(tag1, new IPTag(
                createInetAddress((byte)45), new ChipLocation(3, 3), 11,
                createInetAddress((byte)55), 20, true, TrafficIdentifier.DEFAULT));
        testNotEquals(tag1, new IPTag(
                createInetAddress((byte)45), new ChipLocation(3, 3), 10,
                createInetAddress((byte)56), 20, true, TrafficIdentifier.DEFAULT));
        testNotEquals(tag1, new IPTag(
                createInetAddress((byte)45), new ChipLocation(3, 3), 10,
                createInetAddress((byte)55), 21, true, TrafficIdentifier.DEFAULT));
        testNotEquals(tag1, new IPTag(
                createInetAddress((byte)45), new ChipLocation(3, 3), 10,
                createInetAddress((byte)55), 20, false, TrafficIdentifier.DEFAULT));
        testNotEquals(tag1, new IPTag(
                createInetAddress((byte)45), new ChipLocation(3, 3), 10,
                createInetAddress((byte)55), 20, true, TrafficIdentifier.BUFFERED));

        assertNotEquals(tag1, "tag1");
        assertFalse(tag1.equals(null));
        IPTag nullTag = null;
        assertFalse(tag1.equals(nullTag));
    }

        @Test
    public void testFromJson() throws IOException {
        String json = " {\"x\": 0, \"y\": 0, "
                + "\"boardAddress\": \"192.168.240.253\", "
                + "\"targetAddress\": \"localhost\", \"stripSDP\": true, "
                + "\"tagID\": 1, \"trafficIdentifier\": \"DATA_SPEED_UP\"}";
        ObjectMapper mapper = MapperFactory.createMapper();
        IPTag fromJson = mapper.readValue(json, IPTag.class);
        assertNotNull(fromJson);
    }

}
