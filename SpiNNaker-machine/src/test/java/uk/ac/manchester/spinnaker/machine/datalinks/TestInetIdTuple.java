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
public class TestInetIdTuple {

    public TestInetIdTuple() {
    }

    @Test
    public void testEquals() throws UnknownHostException {
        byte[] bytes1 = {127,0,0,0};
        InetAddress addr1 = InetAddress.getByAddress(bytes1);
        InetAddress addr2 = InetAddress.getByAddress(bytes1);
        InetIdTuple t1 = new InetIdTuple(addr1, 23);
        InetIdTuple t2 = new InetIdTuple(addr2, 23);

        assertEquals(t1, t1);
        assertEquals(t1, t2);
        assertEquals(t1.hashCode(), t2.hashCode());
    }

    @Test
    public void testEqualsWithNull() throws UnknownHostException {
        InetIdTuple t1 = new InetIdTuple(null, 23);
        InetIdTuple t2 = new InetIdTuple(null, 23);

        assertEquals(t1, t2);
        assertEquals(t1.hashCode(), t2.hashCode());
    }

    @Test
    public void testDifferent() throws UnknownHostException {
        byte[] bytes1 = {127,0,0,0};
        InetAddress addr1 = InetAddress.getByAddress(bytes1);
        byte[] bytes2 = {127,0,0,1};
        InetAddress addr2 = InetAddress.getByAddress(bytes2);

        InetIdTuple t1 = new InetIdTuple(addr1, 23);
        InetIdTuple t2 = new InetIdTuple(addr2, 23);
        InetIdTuple t3 = new InetIdTuple(null, 23);
        InetIdTuple t4 = new InetIdTuple(addr1, 24);

        assertNotEquals(t1, t2);
        assertNotEquals(t1.hashCode(), t2.hashCode());
        assertNotEquals(t1, t3);
        assertNotEquals(t1.hashCode(), t3.hashCode());
        assertNotEquals(t1, t4);
        assertNotEquals(t1.hashCode(), t4.hashCode());

        assertNotEquals(t3, null);
        assertNotEquals(t1, "t1");

    }

}
