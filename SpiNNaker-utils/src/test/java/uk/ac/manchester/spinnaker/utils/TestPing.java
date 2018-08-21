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

    public TestPing() {
    }

    @Test
    public void testTravis() throws UnknownHostException {
        InetAddress travis = InetAddress.getByName("travis-ci.org");
        Ping.ping(travis);
    }
}
