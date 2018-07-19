/*
 * Copyright (c) 2018 The University of Manchester
 */
package uk.ac.manchester.spinnaker.utils;

import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 *
 * @author Christian-B
 */
public class TestDoubleMapIterator {

    @Test
    public void testSingle() {
        Map<Double, Map<String, Integer>> aMap = new HashMap();

        Map<String, Integer> inner = new HashMap();
        inner.put("One", 1);
        inner.put("Two", 2);
        inner.put("Three", 3);
        aMap.put(23.2, inner);

        DoubleMapIterator<Integer> instance;
        instance = new DoubleMapIterator(aMap);
        int count = 0;
        while (instance.hasNext()) {
            int value = instance.next();
            count += 1;
        }
        assertEquals(3, count);
    }

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

        DoubleMapIterator<Integer> instance;
        instance = new DoubleMapIterator(aMap);
        int count = 0;
        while (instance.hasNext()) {
            int value = instance.next();
            count += 1;
        }
        assertEquals(6, count);

        assertThrows(NoSuchElementException.class, () -> {
            instance.next();
        });

    }

    @Test
    public void testEmptyWhole() {
        Map<Double, Map<String, Integer>> aMap = new HashMap();
        DoubleMapIterator<Integer> instance;
        instance = new DoubleMapIterator(aMap);
        int count = 0;
        while (instance.hasNext()) {
            int value = instance.next();
            System.out.println(value);
            count += 1;
        }
        assertEquals(0, count);
    }

    @Test
    public void testOneEmpty() {
        Map<Double, Map<String, Integer>> aMap = new HashMap();

        Map<String, Integer> inner0 = new HashMap();
        aMap.put(343.2, inner0);

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

        DoubleMapIterator<Integer> instance;
        instance = new DoubleMapIterator(aMap);
        int count = 0;
        while (instance.hasNext()) {
            int value = instance.next();
            count += 1;
        }
        assertEquals(6, count);
    }

}
