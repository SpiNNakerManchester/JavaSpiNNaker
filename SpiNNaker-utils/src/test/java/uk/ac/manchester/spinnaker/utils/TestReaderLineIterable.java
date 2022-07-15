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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.NoSuchElementException;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 *
 * @author Christian-B
 */
@SuppressWarnings("unused")
public class TestReaderLineIterable {

	public TestReaderLineIterable() {
	}

	@Test
	public void testSimple() {
		var reader = new StringReader("First\nSecond\nThird");
		var iterable = new ReaderLineIterable(reader);
		int count = 0;
		for (var line : iterable) {
			count += 1;
		}
		assertEquals(3, count);
		assertThrows(IllegalStateException.class, () -> {
			for (var line : iterable) {
				continue;
			}
		});
		assertEquals(3, count);
		try {
			iterable.close();
		} catch (IOException ex) {
			assertTrue(false, "Unexpected Exception");
		}
	}

	@Test
	public void testStream() {
		var inputStream =
				new ByteArrayInputStream("First\nSecond\nThird".getBytes());
		var iterable = new ReaderLineIterable(inputStream);
		int count = 0;
		for (var line : iterable) {
			count += 1;
		}
		assertEquals(3, count);
		assertThrows(IllegalStateException.class, () -> {
			for (var line : iterable) {
				continue;
			}
		});
		assertEquals(3, count);
		try {
			iterable.close();
		} catch (IOException ex) {
			assertTrue(false, "Unexpected Exception");
		}
	}

	@Test
	public void testEarlyClose() {
		var reader = new StringReader("First\nSecond\nThird");
		var iterable = new ReaderLineIterable(reader);
		try {
			iterable.close();
		} catch (IOException ex) {
			assertTrue(false, "Unexpected Exception");
		}
		assertThrows(IllegalStateException.class, () -> {
			for (var line : iterable) {
				continue;
			}
		});
	}

	/**
	 * test that close method is called if used in a try.
	 */
	@Test
	public void testClose() {
		var reader = new CloseError();
		try (var iterable = new ReaderLineIterable(reader)) {
			for (var line : iterable) {
				continue;
			}
		} catch (IOException ex) {
			assertEquals("Close marker", ex.getMessage());
			return;
		}
		assertFalse(true, "Exception not thrown");
	}

	/**
	 * test that close method is not called if used simply.
	 */
	@Test
	@SuppressWarnings("resource")
	public void testNoClose() {
		var reader = new CloseError();
		for (var line : new ReaderLineIterable(reader)) {
			continue;
		}
	}

	/**
	 * Checks that an Exception at hasNext/ next time is thrown on close.
	 */
	@Test
	public void testDelayedException() {
		var iterable = new ReaderLineIterable(new WeirdReader());
		int count = 0;
		for (var line : iterable) {
			count += 1;
		}
		assertEquals(0, count);
		try {
			iterable.close();
			assertTrue(false, "Expected Exception");
		} catch (IOException ex) {
			assertEquals("Weird marker", ex.getMessage());
		}
	}

	@Test
	public void testHasNext() {
		var reader = new StringReader("First\nSecond\nThird");
		var iterable = new ReaderLineIterable(reader);
		var iterator = iterable.iterator();
		assertEquals("First", iterator.next());
		iterator.hasNext();
		iterator.hasNext();
		iterator.hasNext();
		assertEquals("Second", iterator.next());
		assertEquals("Third", iterator.next());
		assertThrows(NoSuchElementException.class, () -> {
			iterator.next();
		});
		iterator.hasNext();
		try {
			iterable.close();
		} catch (IOException ex) {
			assertTrue(false, "Unexpected Exception");
		}
	}

	private class CloseError extends Reader {

		@Override
		public int read(char[] cbuf, int off, int len) throws IOException {
			return -1;
		}

		@Override
		public void close() throws IOException {
			throw new IOException("Close marker");
		}

	}

	private class WeirdReader extends Reader {

		@Override
		public int read(char[] cbuf, int off, int len) throws IOException {
			throw new IOException("Weird marker");
		}

		@Override
		public void close() throws IOException {
		}

	}

}
