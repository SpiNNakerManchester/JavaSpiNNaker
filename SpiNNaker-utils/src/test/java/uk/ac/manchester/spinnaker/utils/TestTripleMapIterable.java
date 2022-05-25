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
@SuppressWarnings("unused")
public class TestTripleMapIterable {

    @Test
    public void testMultiple() {
        Map<Float, Map<Double, Map<String, Integer>>> bigMap = new HashMap<>();

        Map<Double, Map<String, Integer>> aMap = new HashMap<>();

        var inner = new HashMap<String, Integer>();
        inner.put("One", 1);
        inner.put("Two", 2);
        inner.put("Three", 3);
        aMap.put(23.2, inner);

        var inner2 = new HashMap<String, Integer>();
        inner2.put("Ten", 10);
        inner2.put("Eleven", 11);
        inner2.put("Twelve", 12);
        aMap.put(43.6, inner2);

        bigMap.put((float)0.5, aMap);

        Map<Double, Map<String, Integer>> aMap2 = new HashMap<>();

        var inner11 = new HashMap<String, Integer>();
        inner.put("Un", -1);
        inner.put("Duex", -2);
        inner.put("Trois", -3);
        aMap2.put(423.2, inner11);

        var inner12 = new HashMap<String, Integer>();
        inner2.put("Dix", -10);
        inner2.put("Onze", -11);
        inner2.put("Douze", -12);
        aMap2.put(4.6, inner12);

        bigMap.put((float)2.5, aMap2);

        var instance = new TripleMapIterable<>(bigMap);
        int count = 0;
        for (var value: instance) {
            count += 1;
        }
        assertEquals(12, count);
        for (var value: instance) {
            count += 1;
        }
        assertEquals(24, count);
    }

    @Test
    public void testEmpty() {
        Map<Float, Map<Double, Map<String, Integer>>> bigMap = new HashMap<>();

        var instance = new TripleMapIterable<>(bigMap);
        int count = 0;
        for (var value: instance) {
            count += 1;
        }
        assertEquals(0, count);
    }
}
