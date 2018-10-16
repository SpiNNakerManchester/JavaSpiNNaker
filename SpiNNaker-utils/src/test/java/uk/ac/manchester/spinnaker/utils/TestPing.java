/*
 * Copyright (c) 2018 The University of Manchester
 */
package uk.ac.manchester.spinnaker.utils;

import java.net.InetAddress;
import java.net.UnknownHostException;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 *
 * @author Christian-B
 */
public class TestPing {
    @Test
    public void testPingSpalloc() throws UnknownHostException {
        InetAddress spalloc = InetAddress.getByName("spinnaker.cs.man.ac.uk");
        // Should be able to reach Spalloc...
        assertEquals(0, Ping.ping(spalloc));
    }

    @Test
    public void testPingGoogle() throws UnknownHostException {
        InetAddress travis = InetAddress.getByName("8.8.8.8");
        // *REALLY* should be able to reach Google's DNS...
        assertEquals(0, Ping.ping(travis));
    }

    @Test
    public void testPingLocalhost() throws UnknownHostException {
    	// Can't ping localhost? Network catastrophically bad!
        assertEquals(0, Ping.ping("localhost"));
    }

    @Test
    public void testPingDownHost() throws UnknownHostException {
    	// Definitely unpingable host
    	// http://answers.microsoft.com/en-us/windows/forum/windows_vista-networking/invalid-ip-address-169254xx/ce096728-e2b7-4d54-80cc-52a4ed342870
        assertNotEquals(0, Ping.ping("169.254.254.254"));
    }
}
