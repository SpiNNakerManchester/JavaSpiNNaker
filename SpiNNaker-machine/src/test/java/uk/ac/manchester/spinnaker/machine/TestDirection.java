/*
 * Copyright (c) 2018 The University of Manchester
 */
package uk.ac.manchester.spinnaker.machine;

import java.util.HashSet;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

/**
 *
 * @author Christian-B
 */
public class TestDirection {

    public TestDirection() {
    }

    /**
     * Test of values method, of class Direction.
     */
    @Test
    public void testValues() {
        Direction[] result = Direction.values();
        assertEquals(6, result.length);
    }

    /**
     * Test of valueOf method, of class Direction.
     */
    @Test
    public void testValueOf() {
        String name = "NORTH";
        Direction expResult = Direction.NORTH;
        Direction result = Direction.valueOf(name);
        assertEquals(expResult, result);
    }

    /**
     * Test of byId method, of class Direction.
     */
    @Test
    public void testById() {
        int id = 2;
        Direction expResult = Direction.NORTH;
        Direction result = Direction.byId(id);
        assertEquals(expResult, result);
    }

    /**
     * Test of typicalMove method, of class Direction.
     */
    @Test
    public void testTypicalMove() {
        HasChipLocation source = new ChipLocation(0,0);
        Direction instance = Direction.NORTH;
        HasChipLocation expResult = new ChipLocation(0, 1);
        HasChipLocation result = instance.typicalMove(source);
        assertEquals(expResult, result);
    }

    /**
     * Test the inverse and that they all return unique values.
     *
     * This makes sure two values do not use the same default.
     */
    @Test
    public void testInverse() {
        HashSet<Direction> inverses = new HashSet<>();
        for (Direction direction: Direction.values()) {
            inverses.add(direction.inverse());
        }
        assertEquals(Direction.values().length, inverses.size());
    }

}
