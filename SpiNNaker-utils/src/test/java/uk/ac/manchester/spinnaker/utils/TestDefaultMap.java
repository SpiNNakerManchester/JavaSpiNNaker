/*
 * Copyright (c) 2018 The University of Manchester
 */
package uk.ac.manchester.spinnaker.utils;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 *
 * @author Christian-B
 */
public class TestDefaultMap {

    public TestDefaultMap() {
    }

    @Test
    public void testUntyped() {
        DefaultMap instance = new DefaultMap(ArrayList::new);
        Object foo = instance.get("foo");
        assertTrue(foo instanceof ArrayList);
        ArrayList fooList = (ArrayList)foo;
        fooList.add("a");
        fooList.add(1);
    }

    @Test
    public void testTyped() {
        DefaultMap<String, List<Integer>> instance =
                new DefaultMap(ArrayList<Integer>::new);
        List<Integer> foo = instance.get("foo");
        assertTrue(foo instanceof ArrayList);
        //foo.add("a");
        foo.add(1);
    }

    /**
     * Instead it demonstrates the if you pass in an Object the same
     *      instance of this Object is used every time.
     */
    @Test
    public void testBad() {
        DefaultMap<String, List<Integer>> instance =
                new DefaultMap(new ArrayList<Integer>());
        List<Integer> foo = instance.get("one");
        foo.add(11);
        List<Integer> bar = instance.get("two");
        bar.add(12);
        assertEquals(2, bar.size());
        assertTrue(foo == bar);
    }

    @Test
    public void testKeyAware() {
        DefaultMap<Integer, Integer> instance =
                DefaultMap.newAdvancedDefaultMap(new Doubler());
        Integer two = instance.get(1);
        assertEquals(2, two.intValue());
    }

    @Test
    public void testKeyAware2() {
         DefaultMap<Integer, Integer> instance =
                DefaultMap.newAdvancedDefaultMap(i->i*2);
        Integer two = instance.get(1);
        assertEquals(2, two.intValue());
    }

    public class Doubler implements DefaultMap.KeyAwareFactory<Integer, Integer> {

        @Override
        public Integer createValue(Integer key) {
            return new Integer(key.intValue() * 2);
        }

    }

}
