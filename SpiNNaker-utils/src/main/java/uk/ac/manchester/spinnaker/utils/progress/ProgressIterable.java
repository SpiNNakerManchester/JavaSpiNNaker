/*
 * Copyright (c) 2018 The University of Manchester
 */
package uk.ac.manchester.spinnaker.utils.progress;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;

/**
 *
 * @author Christian-B
 * @param <E>
 */
public class ProgressIterable<E> implements Iterable<E>, Closeable  {

    private final Collection<E> things;
    private final String description;
    private final ArrayList<ProgressIterator<E>> progressIterables;

    public ProgressIterable(Collection<E> things, String description) {
        this.things = things;
        this.description = description;
        progressIterables = new ArrayList<>();
    }

    @Override
    public Iterator<E> iterator() {
        ProgressIterator<E> iterator =
                new ProgressIterator(things, description);
        progressIterables.add(iterator);
        return iterator;
    }

    @Override
    public void close() {
        Iterator<ProgressIterator<E>> iter = progressIterables.iterator();
        while(iter.hasNext()) {
            iter.next().close();
            iter.remove();
        }
    }

    public static void main(String [] args) throws InterruptedException, IOException {
        ProgressBar pb = new ProgressBar(5, "Easiest");
        for (int i = 0; i < 5 ; i++){
            pb.update();
        }
        for (Integer i:  new ProgressIterable<Integer>(Arrays.asList(1,2,3,4,5), "iterable")){

        }
        pb = new ProgressBar(5, "Stopped");
        for (int i = 0; i < 3 ; i++){
            pb.update();
        }
        pb.close();

        try (ProgressIterable<Integer> bar = new ProgressIterable<Integer>(Arrays.asList(1,2,3,4,5), "complex"); ) {
            for (int i:bar) {
                if (i == 3) {
                    break;
                }
            }
        }
    }

}
