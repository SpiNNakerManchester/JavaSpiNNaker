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
        Link link1 = new Link(chip00, Direction.EAST, chip01);
        Link link2 = new Link(chip00, Direction.EAST, chip01);
        assertEquals(link1, link2);
        assertEquals(link1.hashCode(), link2.hashCode());
        assertEquals(link1.toString(), link2.toString());
        assertEquals(link1, link1);
        assertNotEquals(link1, "link1");
        assertNotEquals(link1, null);
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
        Link link1 =  new Link(chip00, Direction.EAST, chip01);
        Link link2 =  new Link(chip11, Direction.EAST, chip01);
        checkDifferent(link1, link2);

        link2 =  new Link(chip00, Direction.NORTH, chip01);
        checkDifferent(link1, link2);

        link2 =  new Link(chip00, Direction.EAST, chip11);
        checkDifferent(link1, link2);
    }

    public void testById() {
        Link link1 =  new Link(chip00, Direction.NORTH, chip01);
        Link link2 =  new Link(chip00, 2, chip01);
        assertEquals(link1, link2);
    }

 }
