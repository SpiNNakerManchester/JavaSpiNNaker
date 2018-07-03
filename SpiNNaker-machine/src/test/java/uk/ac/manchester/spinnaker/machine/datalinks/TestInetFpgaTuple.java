/*
 * Copyright (c) 2018 The University of Manchester
 */
package uk.ac.manchester.spinnaker.machine.datalinks;

import java.net.InetAddress;
import java.net.UnknownHostException;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

/**
 *
 * @author Christian-B
 */
public class TestInetFpgaTuple {

    public TestInetFpgaTuple() {
    }

    @Test
    public void testEquals() throws UnknownHostException {
        byte[] bytes1 = {127,0,0,0};
        InetAddress addr1 = InetAddress.getByAddress(bytes1);
        InetAddress addr2 = InetAddress.getByAddress(bytes1);
        InetFpgaTuple t1 = new InetFpgaTuple(addr1, 1, 23);
        InetFpgaTuple t2 = new InetFpgaTuple(addr2, 1, 23);

        assertEquals(t1, t1);
        assertEquals(t1, t2);
        assertEquals(t1.hashCode(), t2.hashCode());
    }

    @Test
    public void testEqualsWithNull() throws UnknownHostException {
        InetFpgaTuple t1 = new InetFpgaTuple(null, 1, 23);
        InetFpgaTuple t2 = new InetFpgaTuple(null, 1, 23);

        assertEquals(t1, t2);
        assertEquals(t1.hashCode(), t2.hashCode());
    }

    @Test
    public void testDifferent() throws UnknownHostException {
        byte[] bytes1 = {127,0,0,0};
        InetAddress addr1 = InetAddress.getByAddress(bytes1);
        byte[] bytes2 = {127,0,0,1};
        InetAddress addr2 = InetAddress.getByAddress(bytes2);

        InetFpgaTuple t1 = new InetFpgaTuple(addr1, 1, 23);
        InetFpgaTuple t2 = new InetFpgaTuple(addr2, 1, 23);
        InetFpgaTuple t3 = new InetFpgaTuple(null, 1, 23);
        InetFpgaTuple t4 = new InetFpgaTuple(addr1, 2, 23);
        InetFpgaTuple t5 = new InetFpgaTuple(addr1, 1, 24);

        assertNotEquals(t1, t2);
        assertNotEquals(t1.hashCode(), t2.hashCode());
        assertNotEquals(t1, t3);
        assertNotEquals(t1.hashCode(), t3.hashCode());
        assertNotEquals(t1, t4);
        assertNotEquals(t1.hashCode(), t4.hashCode());
        assertNotEquals(t1, t5);
        assertNotEquals(t1.hashCode(), t5.hashCode());

        assertNotEquals(t3, null);
        assertNotEquals(t1, "t1");

    }

}
