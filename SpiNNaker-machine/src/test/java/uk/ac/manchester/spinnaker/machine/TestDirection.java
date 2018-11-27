/*
 * Copyright (c) 2018 The University of Manchester
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
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

    /**
     * Test that json labels can be converted to direction
     */
    @Test
    public void testByLabel() {
        assertEquals(Direction.NORTHEAST, Direction.byLabel("north_east"));
        assertEquals(Direction.WEST, Direction.byLabel("west"));
        assertThrows(Exception.class, () -> {
            Direction.byLabel("foo");
        });
    }
}
