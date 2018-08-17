/*
 * Copyright (c) 2018 The University of Manchester
 */
package uk.ac.manchester.spinnaker.utils.progress;

/**
 *
 * @author Christian-B
 */
public class ProgressBar extends AbstractProgress {


    public ProgressBar(int numberOfThings, String description) {
        super(numberOfThings, description);
    }

    public void update() {
        update(1);
    }

    public void update(int amountToAdd) {
        super.calcUpdate(1);
    }

}
