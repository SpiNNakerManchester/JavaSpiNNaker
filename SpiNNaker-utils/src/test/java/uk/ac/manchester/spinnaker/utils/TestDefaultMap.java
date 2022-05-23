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

    @SuppressWarnings({
    	"unchecked", "rawtypes"
	})
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
                new DefaultMap<>(ArrayList<Integer>::new);
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
                new DefaultMap<>(new ArrayList<Integer>());
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
                DefaultMap.newAdvancedDefaultMap(i -> i*2);
        Integer two = instance.get(1);
        assertEquals(2, two.intValue());
    }

	public static class Doubler
			implements DefaultMap.KeyAwareFactory<Integer, Integer> {
		@Override
		public Integer createValue(Integer key) {
			return key * 2;
		}
	}
}
