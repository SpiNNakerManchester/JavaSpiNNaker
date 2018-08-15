package uk.ac.manchester.spinnaker.utils;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Iterator;

/**
 * A simple iterable wrapper for a reader.
 *
 * @author Donal Fellows, Christian
 */
public class ReaderLineIterable implements Iterable<String>, Closeable {
	private BufferedReader r;

	/**
	 * Create a new <i>one-shot</i> iterable.
	 *
	 * @param inputStream
	 *            The input stream to read from, using the platform default
	 *            encoding.
	 */
	public ReaderLineIterable(InputStream inputStream) {
		this(new InputStreamReader(inputStream));
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
		return new Iterator<String>() {
			private String s;

			@Override
			public boolean hasNext() {
				if (s == null) {
					try {
						s = r.readLine();
					} catch (IOException e) {
						return false;
					}
				}
				return s != null;
			}

			@Override
			public String next() {
				try {
					return s;
				} finally {
					s = null;
				}
			}
		};
	}

	@Override
	public void close() throws IOException {
		r.close();
	}
}
