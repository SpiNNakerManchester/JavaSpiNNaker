/*
 * Copyright (c) 2018 The University of Manchester
 */
package uk.ac.manchester.spinnaker.machine.tags;

import uk.ac.manchester.spinnaker.machine.*;
import java.net.InetAddress;
import java.net.UnknownHostException;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;


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
        TrafficIdentifer trafficIdentifier = TrafficIdentifer.DEFAULT;
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
                createInetAddress((byte)55), 20, true, TrafficIdentifer.DEFAULT);
        IPTag tag1a = new IPTag(
                createInetAddress((byte)45), new ChipLocation(3, 3), 10,
                createInetAddress((byte)55), 20, true, TrafficIdentifer.DEFAULT);
        assertEquals(tag1, tag1a);
        assertEquals(tag1.hashCode(), tag1a.hashCode());
        testNotEquals(tag1, new IPTag(
                createInetAddress((byte)46), new ChipLocation(3, 3), 10,
                createInetAddress((byte)55), 20, true, TrafficIdentifer.DEFAULT));
        testNotEquals(tag1, new IPTag(
                createInetAddress((byte)45), new ChipLocation(3, 2), 10,
                createInetAddress((byte)55), 20, true, TrafficIdentifer.DEFAULT));
        testNotEquals(tag1, new IPTag(
                createInetAddress((byte)45), new ChipLocation(3, 3), 11,
                createInetAddress((byte)55), 20, true, TrafficIdentifer.DEFAULT));
        testNotEquals(tag1, new IPTag(
                createInetAddress((byte)45), new ChipLocation(3, 3), 10,
                createInetAddress((byte)56), 20, true, TrafficIdentifer.DEFAULT));
        testNotEquals(tag1, new IPTag(
                createInetAddress((byte)45), new ChipLocation(3, 3), 10,
                createInetAddress((byte)55), 21, true, TrafficIdentifer.DEFAULT));
        testNotEquals(tag1, new IPTag(
                createInetAddress((byte)45), new ChipLocation(3, 3), 10,
                createInetAddress((byte)55), 20, false, TrafficIdentifer.DEFAULT));
        testNotEquals(tag1, new IPTag(
                createInetAddress((byte)45), new ChipLocation(3, 3), 10,
                createInetAddress((byte)55), 20, true, TrafficIdentifer.BUFFERED));

        assertNotEquals(tag1, "tag1");
        assertFalse(tag1.equals(null));
        IPTag nullTag = null;
        assertFalse(tag1.equals(nullTag));
    }

}
