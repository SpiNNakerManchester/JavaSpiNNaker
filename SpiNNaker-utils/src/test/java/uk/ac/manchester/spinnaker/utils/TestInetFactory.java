/*
 * Copyright (c) 2018 The University of Manchester
 */
package uk.ac.manchester.spinnaker.utils;

import java.net.UnknownHostException;
import java.util.Arrays;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import uk.ac.manchester.spinnaker.utils.InetFactory.Inet6NotSupportedException;

/**
 *
 * @author Christian-B
 */
public class TestInetFactory {

    public TestInetFactory() {
    }

    @Test
    public void testByBytes() throws UnknownHostException {
        byte[] bytes = {1, 2, 3, 4};
        InetFactory.getByAddress(bytes);
    }

    public void testByName() throws UnknownHostException {
        InetFactory.getByName("apt.cs.manchester.ac.uk");
    }

    @Test
    public void testByBytes6() {
        byte[] bytes = {1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16};
        assertThrows(Inet6NotSupportedException.class, () -> {
            InetFactory.getByAddress(bytes);
        });
    }

    @Test
    public void testByName6() {
        String bytes = "3731:54:65fe:2::a7";
        assertThrows(Inet6NotSupportedException.class, () -> {
            InetFactory.getByName(bytes);
        });
    }

}
