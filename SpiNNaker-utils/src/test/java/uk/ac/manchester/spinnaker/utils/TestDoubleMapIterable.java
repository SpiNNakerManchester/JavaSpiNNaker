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

import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 *
 * @author Christian-B
 */
public class TestDoubleMapIterable {

    @SuppressWarnings("unused")
	@Test
    public void testMultiple() {
        Map<Double, Map<String, Integer>> aMap = new HashMap<>();

        Map<String, Integer> inner = new HashMap<>();
        inner.put("One", 1);
        inner.put("Two", 2);
        inner.put("Three", 3);
        aMap.put(23.2, inner);

        Map<String, Integer> inner2 = new HashMap<>();
        inner2.put("Ten", 10);
        inner2.put("Eleven", 11);
        inner2.put("Twelve", 12);
        aMap.put(43.6, inner2);

        DoubleMapIterable<Integer> instance;
        instance = new DoubleMapIterable<>(aMap);
        int count = 0;
        for (var value: instance) {
            count += 1;
        }
        assertEquals(6, count);
        for (var value: instance) {
            count += 1;
        }
        assertEquals(12, count);
    }
}
