/*
 * Copyright (c) 2018 The University of Manchester
 */
package uk.ac.manchester.spinnaker.utils;

import java.io.StringReader;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 *
 * @author Christian-B
 */
public class TestReaderLineIterable {

    public TestReaderLineIterable() {
    }

    @Test
    public void testSimple() {
        StringReader reader = new StringReader("First\nSecond\nThird");
        ReaderLineIterable iterable = new ReaderLineIterable(reader);
        int count = 0;
        for (String line:iterable) {
            count += 1;
        }
        assertEquals(3, count);
    }

}
