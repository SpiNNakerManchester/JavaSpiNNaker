/*
 * Copyright (c) 2018 The University of Manchester
 */
package uk.ac.manchester.spinnaker.utils.progress;

import java.io.Closeable;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

/**
 * An {@link Iterable} wrapper that will start a {@link ProgressBar} for each
 * iterator.
 * <p>
 * As items are obtained by calling {@link ProgressIterator#next()next()} the
 * ProgressBar is updated. When {@link ProgressIterator#hasNext() hasNext()}
 * returns false the ProgressBar is closed resulting in the duration to be
 * written out. If the Iterable is used in a for loop the duration is started at
 * the beginning of the for loop and duration finishes and reports as the for
 * loop ends. (Not at the time the last element is provided.)
 * <p>
 * Calling {@link #iterator()} more than once is supported but could result in
 * mixed output if they are run at the same time.
 *
 * @author Christian-B
 * @param <E>
 *            Type of Element to be iterated over.
 */
public class ProgressIterable<E> implements Iterable<E>, Closeable  {

    private final Collection<E> things;
    private final String description;
    private final ArrayList<ProgressIterator<E>> progressIterables;
    private final PrintStream output;

    /**
     * Creates an Iterable wrapper but not yet a ProgroessBar.
     *
     * @param things
     *      A Collection to supply a size and and Iterable.
     * @param description
     *      A text description to add at the start and
     *      when reporting duration.
     * @param output
     *      The Stream to write output too. For example System.out
     */
    public ProgressIterable(
            Collection<E> things, String description, PrintStream output) {
        this.things = things;
        this.description = description;
        this.output = output;
        progressIterables = new ArrayList<>();
    }

    /**
     * Creates an Iterable wrapper but not yet a ProgroessBar.
     *
     * @param things
     *      A Collection to supply a size and and Iterable.
     * @param description
     *      A text description to add at the start and
     *      when reporting duration.
     */
    public ProgressIterable(Collection<E> things, String description) {
        this(things, description, System.out);
    }

    /**
     * Starts a ProgressBar and returns an iterator over elements of type E.
     *
     * @return an Iterator.
     */
    @Override
    public Iterator<E> iterator() {
        ProgressIterator<E> iterator =
                new ProgressIterator<>(things, description, output);
        progressIterables.add(iterator);
        return iterator;
    }

    /**
	 * Closes all created Iterators and their ProgressBar(s).
	 * <p>
	 * This method allows the Iterable to be used in a try with resource
	 * statement, which guarantees the ProgressBar is closed.
	 * <p>
	 * Note: As <tt>hasNext() == false</tt> automatically calls close there is
	 * no need to call this method unless you break out of the iterator early.
	 * <p>
	 * If the bar is already closed then invoking this method has no effect.
	 */
    @Override
    public void close() {
        Iterator<ProgressIterator<E>> iter = progressIterables.iterator();
        while (iter.hasNext()) {
            iter.next().close();
            iter.remove();
        }
    }

}
