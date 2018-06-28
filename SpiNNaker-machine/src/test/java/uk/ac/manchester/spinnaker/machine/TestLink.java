/*
 * Copyright (c) 2018 The University of Manchester
 */
package uk.ac.manchester.spinnaker.machine;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;


/**
 *
 * @author Christian-B
 */
public class TestLink {

    ChipLocation chip00 = new ChipLocation(0,0);
    ChipLocation chip01 = new ChipLocation(0,1);
    ChipLocation chip11 = new ChipLocation(1,1);

    /**
     * Test equal Links.
     */
    @Test
    public void testEquals() {
        System.out.println("equals");
        Link link1 = new Link(chip00, Direction.EAST, chip01);
        Link link2 = new Link(chip00, Direction.EAST, chip01);
        assertEquals(link1, link2);
        assertEquals(link1.hashCode(), link2.hashCode());
        assertEquals(link1.toString(), link2.toString());
    }

    private void checkDifferent(Link link1, Link link2){
        assertNotEquals(link1, link2);
        assertNotEquals(link1.hashCode(), link2.hashCode());
        assertNotEquals(link1.toString(), link2.toString());
    }

    /**
     * Test of different Links.
     */
    @Test
    public void testDifferent() {
        System.out.println("equals");
        Link link1 =  new Link(chip00, Direction.EAST, chip01);
        Link link2 =  new Link(chip11, Direction.EAST, chip01);
        checkDifferent(link1, link2);

        link2 =  new Link(chip00, Direction.NORTH, chip01);
        checkDifferent(link1, link2);

        link2 =  new Link(chip00, Direction.EAST, chip11);
        checkDifferent(link1, link2);
    }

}
