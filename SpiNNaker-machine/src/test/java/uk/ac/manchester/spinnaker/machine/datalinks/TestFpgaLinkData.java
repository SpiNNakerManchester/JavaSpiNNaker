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
public class TestFpgaLinkData {

    ChipLocation location00 = new ChipLocation(0,0);
    ChipLocation location01 = new ChipLocation(0,1);

    private InetAddress createInetAddress() throws UnknownHostException {
        byte[] bytes = {127,0,0,0};
        return InetAddress.getByAddress(bytes);
    }


    public TestFpgaLinkData() {
    }

    private void checkDifferent(
            FPGALinkData link1, FPGALinkData link2) {
        assertNotEquals(link1, link2);
        assertNotEquals(link1.hashCode(), link2.hashCode());
        assertNotEquals(link1.toString(), link2.toString());
    }

    private void checkSame(
            FPGALinkData link1, FPGALinkData link2) {
        assertEquals(link1, link2);
        assertEquals(link1.hashCode(), link2.hashCode());
        assertEquals(link1.toString(), link2.toString());
    }

    @Test
    public void testEquals() throws UnknownHostException {
        FPGALinkData link1 = new FPGALinkData(34, FpgaId.TOP_RIGHT,
                location00, Direction.NORTHEAST, createInetAddress());
        FPGALinkData link2= new FPGALinkData(34, FpgaId.byId(2),
                location00, Direction.NORTHEAST, createInetAddress());
        assertTrue(link1.sameAs(link2));
        checkSame(link1, link2);
        assertEquals(link1, link1);
        assertEquals(ChipLocation.ZERO_ZERO, link1.asChipLocation());
    }

    @Test
    public void testDifferent() throws UnknownHostException {
        FPGALinkData link1 = new FPGALinkData(34, FpgaId.TOP_RIGHT,
                location00, Direction.NORTHEAST, createInetAddress());
        FPGALinkData link2= new FPGALinkData(33, FpgaId.TOP_RIGHT,
                location00, Direction.NORTHEAST, createInetAddress());
        FPGALinkData link3 = new FPGALinkData(34, FpgaId.TOP_RIGHT,
                location01, Direction.NORTHEAST, createInetAddress());
        FPGALinkData link4 = new FPGALinkData(34, FpgaId.TOP_RIGHT,
                location00, Direction.NORTH, createInetAddress());
        byte[] bytes = {127,0,0,1};
        InetAddress address2 =  InetAddress.getByAddress(bytes);
        FPGALinkData link5 = new FPGALinkData(34, FpgaId.TOP_RIGHT,
                location00, Direction.NORTHEAST, address2);
        FPGALinkData link6 = new FPGALinkData(34, FpgaId.LEFT, location00,
                Direction.NORTHEAST, createInetAddress());

        checkDifferent(link1, link2);
        checkDifferent(link1, link3);
        checkDifferent(link1, link4);
        checkDifferent(link1, link5);
        checkDifferent(link1, link6);

        assertNotEquals(link1, null);
        assertNotEquals(link1, "link1");
    }

}
