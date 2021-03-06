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
package uk.ac.manchester.spinnaker.utils;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 *
 * @author Christian-B
 */
public class TestUnits {

    public TestUnits() {
    }

    @Test
    public void testMilli() {
        assertEquals("25 ms", UnitConstants.formatDuration(25));
    }

    @Test
    public void testSecond() {
        assertEquals("2.003 s", UnitConstants.formatDuration(1000 * 2 + 3));
    }

    @Test
    public void testSecond2() {
        assertEquals("25.003 s", UnitConstants.formatDuration(1000 * 25 + 3));
    }

    @Test
    public void testMinutes() {
        assertEquals("2:00.003 m", UnitConstants.formatDuration(60000 * 2 + 3));
    }

    @Test
    public void testMinutes2() {
        assertEquals("23:00.003 m", UnitConstants.formatDuration(60000 * 23 + 3));
    }

    @Test
    public void testFewwHours() {
        long ms = 6 * 60 * 60 * 1000 + 34 * 60 * 1000 + 7 * 1000 + 45;
        assertEquals("6:34:07.045 h", UnitConstants.formatDuration(ms));
    }

    @Test
    public void testDuration() {
        long ms = 14 * 60 * 60 * 1000 + 34 * 60 * 1000 + 7 * 1000 + 45;
        assertEquals("14:34:07.045 h", UnitConstants.formatDuration(ms));
    }

    @Test
    public void testMoreThanaDay() {
        long ms = 87 * 60 * 60 * 1000 + 34 * 60 * 1000 + 7 * 1000 + 45;
        assertEquals("87:34:07.045 h", UnitConstants.formatDuration(ms));
    }
}
