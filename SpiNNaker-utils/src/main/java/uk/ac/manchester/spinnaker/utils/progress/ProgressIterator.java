/*
 * Copyright (c) 2018 The University of Manchester
 */
package uk.ac.manchester.spinnaker.utils.progress;

import java.io.Closeable;
import java.io.PrintStream;
import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * An Iterator wrapper with and an attached {@link ProgressBar}.
 * <p>
 * Mainly designed a a Support class for {@link ProgressIterable}.
 * <p>
 * As items are obtained by calling next() the ProgressBar is updated.
 * When hasNext() returns false the ProgressBar is closed resulting in the
 *      duration to be written out.
 * @param <E> Type of Element to be iterated over.
 * @author Christian-B
 */
public class ProgressIterator<E>  implements Iterator<E>, Closeable {

    private final ProgressBar bar;

    private final Iterator<E> inner;

    private boolean first;

    /**
     * Creates a new Progress Iterable and a bar on the output stream.
     *
     * @param outer
     *      A Collection to supply a size and and Iterable.
     * @param description
     *      A text description to add at the start and
     *      when reporting duration.
     * @param output
     *      The Stream to write output too. For example System.out
     */
    public ProgressIterator(
            Collection<E> outer, String description, PrintStream output) {
        bar = new ProgressBar(outer.size(), description, output);
        inner = outer.iterator();
        first = true;
    }

    /**
     * Returns the next element in the iteration, and updates the ProegessBar.
     * <p>
     * When hasNext returns false the ProgessBAr is closed.
     *
     * @return the next element in the iteration
     */
    @Override
    public boolean hasNext() {
        boolean result = inner.hasNext();
        if (!result) {
            close();
        }
        return result;
    }

    /**
     * Returns true if the iteration has more elements.
     * (In other words, returns true if next() would return an element
     *      rather than throwing an exception.)
     * <p>
     * The ProgessBar is also update by one each time.
     *
     * @throws NoSuchElementException - if the iteration has no more elements
     * @throws IllegalStateException is the Iterator and therefor the
     *         ProgressBar have been closed.
     * @return
     */
    @Override
    public E next() throws NoSuchElementException {
        if (first) {
            first = false;
        } else {
            bar.update();
        }
        return inner.next();
    }

    /**
     * Closes the underlying ProgressBar.
     * <p>
     * Note: As hasNext()==false automatically calls close there is no need to
     *      call this method unless you break out of the iterator early.
     * <p>
     * If the bar is already closed then invoking this method has no effect.
     */
    @Override
    public void close() {
        // As we did not update on the first pass we have to update once
        if (!bar.isClosed()){
            bar.update();
        }
        bar.close();
    }

}
