/*
 * Copyright (c) 2018 The University of Manchester
 */
package uk.ac.manchester.spinnaker.machine.datalinks;

import java.net.InetAddress;
import java.net.UnknownHostException;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import uk.ac.manchester.spinnaker.machine.ChipLocation;
import uk.ac.manchester.spinnaker.machine.Direction;

/**
 *
 * @author Christian-B
 */
public class TestAbstractDataLink {

    ChipLocation location00 = new ChipLocation(0,0);
    ChipLocation location01 = new ChipLocation(0,1);

    private InetAddress createInetAddress() throws UnknownHostException {
        byte[] bytes = {127,0,0,0};
        return InetAddress.getByAddress(bytes);
    }


    public TestAbstractDataLink() {
    }


    @Test
    public void testEquals() throws UnknownHostException {
        AbstractDataLink link1 = new AbstractDataLink(location00,
                Direction.NORTHEAST, createInetAddress());
        AbstractDataLink link2= new AbstractDataLink(location00,
                Direction.NORTHEAST, createInetAddress());
        assertEquals(link1, link2);
        assertEquals(link1, link1);
        assertNotEquals(link1, null);
        assertNotEquals(link1, "link1");
    }

}
