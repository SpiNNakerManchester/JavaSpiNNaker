/*
 * Copyright (c) 2018 The University of Manchester
 */
package uk.ac.manchester.spinnaker.front_end.interfaces.buffer_management;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import uk.ac.manchester.spinnaker.connections.EIEIOConnection;
import uk.ac.manchester.spinnaker.connections.UDPConnection;
import uk.ac.manchester.spinnaker.messages.eieio.EIEIOHeader;
import uk.ac.manchester.spinnaker.messages.eieio.EIEIOMessage;
import uk.ac.manchester.spinnaker.transceiver.UDPTransceiver;

/**
 *
 * @author Christian-B
 */
public class EIEIOConnectionFactory implements
        UDPTransceiver.ConnectionFactory {

    @Override
    public Class<EIEIOConnection> getClassKey() {
        return EIEIOConnection.class;
    }

    @Override
    public UDPConnection getInstance(String localAddress) throws IOException {
        return new EIEIOConnection(localAddress, null, null, null);
    }

    @Override
    public UDPConnection getInstance(String localAddress, int localPort) throws IOException {
        return new EIEIOConnection(localAddress, null, null, null);
    }


 }
