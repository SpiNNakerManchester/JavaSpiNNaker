/*
 * Copyright (c) 2018 The University of Manchester
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

    @Test
    public void testMultiple() {
        Map<Double, Map<String, Integer>> aMap = new HashMap();

        Map<String, Integer> inner = new HashMap();
        inner.put("One", 1);
        inner.put("Two", 2);
        inner.put("Three", 3);
        aMap.put(23.2, inner);

        Map<String, Integer> inner2 = new HashMap();
        inner2.put("Ten", 10);
        inner2.put("Eleven", 11);
        inner2.put("Twelve", 12);
        aMap.put(43.6, inner2);

        DoubleMapIterable<Integer> instance;
        instance = new DoubleMapIterable(aMap);
        int count = 0;
        for (Integer value: instance) {
            count += 1;
        }
        assertEquals(6, count);
        for (Integer value: instance) {
            count += 1;
        }
        assertEquals(12, count);
    }


}
