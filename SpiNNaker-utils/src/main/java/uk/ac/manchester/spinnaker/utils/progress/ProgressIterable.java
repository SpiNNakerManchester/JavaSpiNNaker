/*
 * Copyright (c) 2018 The University of Manchester
 */
package uk.ac.manchester.spinnaker.utils.progress;

import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;

/**
 *
 * @author Christian-B
 * @param <E>
 */
public class ProgressIterable<E> extends AbstractProgress implements Iterable<E>{

    private final Collection<E> things;
    private boolean started = false;

    public ProgressIterable(Collection<E> things, String description) {
        super(things.size(), description);
        this.things = things;
    }

    @Override
    public Iterator<E> iterator() {
        return new Iterator<E>() {
            private final Iterator<E> inner = things.iterator();

            @Override
            public boolean hasNext() {
                return inner.hasNext();
            }

            @Override
            public E next() {
                calcUpdate(1);
                return inner.next();
            }
        };
    }

    public static void main(String [] args) throws InterruptedException {
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
