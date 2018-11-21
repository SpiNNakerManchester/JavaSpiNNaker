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
public class TestMachineDimensions {

    public TestMachineDimensions() {
    }

    @Test
    public void testEquals() {
       MachineDimensions m1 = new MachineDimensions(2, 3);
       MachineDimensions m2 = new MachineDimensions(2, 3);
       assertEquals(m1, m2);
       assertEquals(m1.hashCode(), m2.hashCode());
       assertEquals(m1.toString(), m2.toString());
    }

    @Test
    public void testNullEquals() {
       MachineDimensions m1 = new MachineDimensions(2, 3);
       MachineDimensions m2 = null;
       assertNotEquals(m1, m2);
       assertFalse(m1.equals(m2));
    }
}
