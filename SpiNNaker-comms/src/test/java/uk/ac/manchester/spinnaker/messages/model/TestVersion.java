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
package uk.ac.manchester.spinnaker.messages.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;

/**
 *
 * @author micro
 */
public class TestVersion {

    @Test
    public void testThreeUnquoted() {
        Version version = new Version("1.2.3");
        assertEquals(1, version.majorVersion);
        assertEquals(2, version.minorVersion);
        assertEquals(3, version.revision);
    }

    @Test
    public void testThreeQuoted() {
        Version version = new Version("\"1.2.3\"");
        assertEquals(1, version.majorVersion);
        assertEquals(2, version.minorVersion);
        assertEquals(3, version.revision);
    }

    @Test
    public void testTwoUnquoted() {
        Version version = new Version("1.2");
        assertEquals(1, version.majorVersion);
        assertEquals(2, version.minorVersion);
        assertEquals(0, version.revision);
    }

    @Test
    public void testOneQuoted() {
        Version version = new Version("\"1\"");
        assertEquals(1, version.majorVersion);
        assertEquals(0, version.minorVersion);
        assertEquals(0, version.revision);
    }
}
