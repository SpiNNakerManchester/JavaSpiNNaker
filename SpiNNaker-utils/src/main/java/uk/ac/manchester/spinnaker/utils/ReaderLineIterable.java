package uk.ac.manchester.spinnaker.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.util.Iterator;
import java.util.NoSuchElementException;

import static org.slf4j.LoggerFactory.getLogger;
import org.slf4j.Logger;

/**
 * A simple One Shot <i>one-shot</i> wrapper for a reader.
 * <p>
 * This class is mainly designed to be used once in a for loop,
 * without having to worry about exceptions ect.
 * <p>
 * When hasNext returns False the stream is closed.
 * So once the for loop end there is no need to call close.
 * <p>
 * <b>WARNING!</b> The hasNext() and next() calls may hide exceptions.
 * the first exception caught and not throw in saved and throws when
 * close is called.
 * So it is recommended to create the iterable in a try call.
  <pre>
 * {@code
 * try (ReaderLineIterable lines = new ReaderLineIterable(...)) {
 *    for (String line : lines) {
 *       ...
 *    }
 * }
 * }
 * </pre>
 *
 * @author Donal Fellows, Christian
 */
public class ReaderLineIterable implements Iterable<String>, AutoCloseable {
    private static final Logger log = getLogger(ReaderLineIterable.class);

    private final BufferedReader r;
    private boolean used = false;
    private boolean closed = false;
    private IOException caught = null;

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
     * @param resource
     *            The URL of an existing resource to wrap in a Reader.
     * @throws IOException
     *            Throws if it is not possible to wrap the URL in a Reader.
     */
	public ReaderLineIterable(URL resource) throws IOException {
		this(new InputStreamReader(resource.openStream()));
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
     * @throws IllegalStateException
     *      If there is an attempt to obtain a second or subsequent iterator.
	 */
	@Override
	public Iterator<String> iterator() {
        if (used) {
            throw new IllegalStateException(
                    "ReaderLineIterable may only be used once");
        }
        used = true;
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
                        // Ignore if Reader if already closed.
                        if (!closed) {
                            log.error("Error iterating over lines", ex);
                            if (caught == null) {
                                caught = ex;
                            }
                            silentClose();
                        }
                    }
                }
				return s != null;
			}

			@Override
			public String next() {
                if (s == null) {
                    if (!hasNext()) {
                        throw new NoSuchElementException(
                                "No lines left in Stream.");
                    }
                }
                String temp = s;
                s = null;
                return temp;
			}
		};
	}

    private void silentClose() {
        closed = true;
        try {
            // Readers can be closed more than once so no reason
            // even if we think it is already closed.
            r.close();
        } catch (IOException ex) {
            log.error("Error closing reader", ex);
            if (caught == null) {
                caught = ex;
            }
        }
    }

    @Override
    public void close() throws IOException {
        silentClose();
        if (caught != null) {
            IOException temp = caught;
            caught = null;
            throw temp;
        }
    }

    /**
     * Show if the Iterable has tried to close the undelying Stream.
     * <p>
     * This method does not detect if close was called outside of the Iterable.
     *
     * @return True is close has been called on the underlying Stream.
     * @deprecated Added purely for testing and can be removed at any time.
     */
    boolean isClosed() {
        return closed;
    }
}
