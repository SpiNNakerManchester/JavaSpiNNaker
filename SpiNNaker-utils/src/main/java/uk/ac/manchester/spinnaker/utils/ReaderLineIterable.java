/*
 * Copyright (c) 2018 The University of Manchester
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package uk.ac.manchester.spinnaker.utils;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * A simple <i>one-shot</i> iterable wrapper for a reader.
 * <p>
 * It has been designed for a single use in a for statement. A second use will
 * of the same Object will cause an Exception.
 * <p>
 * Any Exception thrown by the Reader during iteration are intercepted and not
 * throw.
 * <p>
 * The Iterator will automatically close the underlying reader when
 * {@link Iterator#hasNext()} returns false, which includes when it leaves a for
 * loop. Any Exception thrown by the Reader during this close are intercepted
 * and not throw.
 * <p>
 * It is recommended to always specifically call the Iterable's close method
 * after finishing with the Iteration. It makes sure the underlying reader has
 * been closed. It also raises the first Exception that may have been trapped
 * during an early {@link Iterator#next()} or {@link Iterator#hasNext()}
 * including during the reader closed done then. The recommended way to make
 * sure close is called is with a {@code try}-with-resources statement.
 *
 * @author Donal Fellows
 * @author Christian
 */
public class ReaderLineIterable implements MappableIterable<String>, Closeable {
	private final BufferedReader r;

	private boolean closed = false;

	private boolean used = false;

	private IOException caught;

	/**
	 * Create a new <i>one-shot</i> iterable.
	 *
	 * @param inputStream
	 *            The input stream to read from, using UTF-8 as the encoding.
	 */
	public ReaderLineIterable(InputStream inputStream) {
		this(new InputStreamReader(inputStream, UTF_8));
	}

	/**
	 * Create a new <i>one-shot</i> iterable.
	 *
	 * @param reader
	 *            The reader to read from.
	 */
	public ReaderLineIterable(Reader reader) {
		this.r = new BufferedReader(reader);
	}

	/**
	 * @return An iterator over the lines of the underlying reader.
	 */
	@Override
	public Iterator<String> iterator() {
		if (used) {
			throw new IllegalStateException(
					"This Iterator can only be used once.");
		}
		used = true;
		if (closed) {
			throw new IllegalStateException(
					"This Iterator can not be used as it is already closed.");
		}
		return new Iterator<String>() {
			private String s;

			@Override
			public boolean hasNext() {
				if (s == null) {
					try {
						s = r.readLine();
						if (s == null) {
							silentClose();
						}
					} catch (IOException ex) {
						// Ignore an error because this closed the stream
						if (!closed) {
							if (caught == null) {
								caught = ex;
							}
						}
						silentClose();
						return false;
					}
				}
				return s != null;
			}

			@Override
			public String next() {
				if (s == null) {
					boolean boolResult = hasNext();
					if (!boolResult) {
						throw new NoSuchElementException("No lines left.");
					}
				}
				var temp = s;
				s = null;
				return temp;
			}
		};
	}

	private void silentClose() {
		try {
			r.close();
		} catch (IOException ex) {
			if (caught == null) {
				caught = ex;
			}
		} finally {
			closed = true;
		}
	}

	@Override
	public void close() throws IOException {
		silentClose();
		if (caught != null) {
			var temp = caught;
			caught = null;
			throw temp;
		}
	}
}
