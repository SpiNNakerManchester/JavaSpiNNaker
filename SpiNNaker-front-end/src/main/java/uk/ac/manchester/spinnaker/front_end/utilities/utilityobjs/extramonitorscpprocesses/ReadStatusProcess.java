/*
 * Copyright (c) 2018 The University of Manchester
 */
package uk.ac.manchester.spinnaker.front_end.utilities.utilityobjs.extramonitorscpprocesses;

import uk.ac.manchester.spinnaker.connections.SCPConnection;
import uk.ac.manchester.spinnaker.connections.selectors.ConnectionSelector;
import uk.ac.manchester.spinnaker.machine.CoreLocation;
import uk.ac.manchester.spinnaker.transceiver.RetryTracker;
import uk.ac.manchester.spinnaker.transceiver.processes.MultiConnectionProcess;

/**
 *
 * @author Christian-B
 */
public class ReadStatusProcess extends MultiConnectionProcess {

    public ReadStatusProcess(ConnectionSelector<SCPConnection> selector, RetryTracker retryTracker) {
       super (selector, retryTracker);
    }

    public Object getReinjectionStatus(CoreLocation asCoreLocation) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

}
