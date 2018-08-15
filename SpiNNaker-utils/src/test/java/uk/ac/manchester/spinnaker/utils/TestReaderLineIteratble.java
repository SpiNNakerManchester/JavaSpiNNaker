/*
 * Copyright (c) 2018 The University of Manchester
 */
package uk.ac.manchester.spinnaker.utils;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.net.URL;
import java.util.Iterator;
import java.util.NoSuchElementException;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 *
 * @author Christian-B
 */
public class TestReaderLineIteratble {

    public TestReaderLineIteratble() {
    }

    @Test
    public void testSimple() {
        ClassLoader classLoader = getClass().getClassLoader();
        URL resource = classLoader.getResource("afewlines.txt");
        Reader reader = new InputStreamReader(this.getClass().
                getResourceAsStream("/afewlines.txt"));
        ReaderLineIterable iterable = new ReaderLineIterable(reader);
        int count = 0;
        for (String line:iterable) {
            count += 1;
        }
        assertEquals(3, count);
        assertThrows(IllegalStateException.class, () -> {
            for (String line:iterable) {
            }
        });
        try {
            iterable.close();
        } catch (IOException ex) {
            assertTrue(false, "Unexpexceted exception in closed");
        }
    }


    @Test
    public void testHasNextFalse() {
        ClassLoader classLoader = getClass().getClassLoader();
        URL resource = classLoader.getResource("afewlines.txt");
        Reader reader = new InputStreamReader(this.getClass().
                getResourceAsStream("/afewlines.txt"));
        ReaderLineIterable iterable = new ReaderLineIterable(reader);
        Iterator<String> iterator = iterable.iterator();
        int count = 0;
        while (iterator.hasNext()) {
            iterator.next();
            count += 1;
        }
        assertFalse(iterator.hasNext());
        try {
            iterable.close();
        } catch (IOException ex) {
            assertTrue(false, "Unexpexceted exception in closed");
        }
    }

    @Test
    public void testAbnormal() {
        StringReader reader = new StringReader(
                "Line one\nLine two\nLine three\n");
        ReaderLineIterable iterable = new ReaderLineIterable(reader);
        Iterator<String> iterator = iterable.iterator();
        // Call with a hasNext();
        assertEquals("Line one", iterator.next());
        // Multiple hasNext()
        assertTrue(iterator.hasNext());
        assertTrue(iterator.hasNext());
        assertTrue(iterator.hasNext());
        assertEquals("Line two", iterator.next());
        assertEquals("Line three", iterator.next());
        assertThrows(NoSuchElementException.class, () -> {
            iterator.next();
        });
        assertFalse(iterator.hasNext());
        try {
            iterable.close();
        } catch (IOException ex) {
            assertTrue(false, "Unexpexceted exception in closed");
        }
    }

    @Test
    public void testException() throws IOException {
        Reader reader = new WeirdReader();
        ReaderLineIterable iterable = new ReaderLineIterable(reader);
        Iterator<String> iterator = iterable.iterator();
        int count = 0;
        while (iterator.hasNext()) {
            iterator.next();
            count += 1;
        }
        assertFalse(iterator.hasNext());
        try {
            iterable.close();
            assertFalse(true, "Expected IOException not raise");
        } catch (IOException ex) {
            assertEquals("WEIRD!", ex.getMessage());
        }
    }

    @Test
    public void testCloseException() throws IOException {
        Reader reader = new EmptyReader();
        ReaderLineIterable iterable = new ReaderLineIterable(reader);
        for (String line:iterable) {
            //do nothing
        }
        assertTrue(iterable.isClosed());
        try (ReaderLineIterable iterable2 = new ReaderLineIterable(reader)) {
            for (String line:iterable2) {
                //do nothing
            }
        } catch (IOException ex) {
            assertEquals ("bad close", ex.getMessage());
        }
    }

    @Test
    public void testCloseException2() throws IOException {
        Reader reader = new EmptyReader();
        try (ReaderLineIterable iterable = new ReaderLineIterable(reader)) {
            for (String line:iterable) {
                //do nothing
            }
        } catch (IOException ex) {
            assertEquals ("bad close", ex.getMessage());
        }
    }

    @Test
    public void testCloseException3() throws IOException {
        Reader reader = new WeirdReader();
        try (ReaderLineIterable iterable = new ReaderLineIterable(reader)) {
            for (String line:iterable) {
                //do nothing
            }
        } catch (IOException ex) {
            assertEquals ("WEIRD!", ex.getMessage());
        }
    }

    private class WeirdReader extends Reader {

        @Override
        public int read(char[] cbuf, int off, int len) throws IOException {
            throw new IOException("WEIRD!");
        }

        @Override
        public void close() throws IOException {
            // Do nothing
        }
    }

    private class EmptyReader extends Reader {

        @Override
        public int read(char[] cbuf, int off, int len) throws IOException {
            return -1;
        }

        @Override
        public void close() throws IOException {
            throw new IOException("bad close");
        }
    }
}
