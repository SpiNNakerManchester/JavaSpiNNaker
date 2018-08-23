/*
 * Copyright (c) 2018 The University of Manchester
 */
package uk.ac.manchester.spinnaker.front_end.interfaces.buffer_management;

import java.io.IOException;
import uk.ac.manchester.spinnaker.connections.EIEIOConnection;
import uk.ac.manchester.spinnaker.connections.UDPConnection;
import uk.ac.manchester.spinnaker.messages.eieio.EIEIOHeader;
import uk.ac.manchester.spinnaker.messages.eieio.EIEIOMessage;
import uk.ac.manchester.spinnaker.transceiver.UDPTransceiver;

/**
 *
 * @author Christian-B
 */
public class EIEIOConnectionFactory2 implements
        //UDPTransceiver.ConnectionFactory<EIEIOConnection>{
        UDPTransceiver.ConnectionFactory<UDPConnection<EIEIOMessage>> {

    @Override
    public Class<UDPConnection<EIEIOMessage>> getClassKey() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public UDPConnection<EIEIOMessage> getInstance(String localAddress) throws IOException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public UDPConnection<EIEIOMessage> getInstance(String localAddress, int localPort) throws IOException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    /*
    @Override
    public Class<EIEIOConnection> getClassKey() {
        return EIEIOConnection.class;
    }

    @Override
    public EIEIOConnection getInstance(String localAddress) throws IOException {
        return new EIEIOConnection(localAddress, null, null, null);
    }

    @Override
    public EIEIOConnection getInstance(String localAddress, int localPort) throws IOException {
        return new EIEIOConnection(localAddress, localPort, null, null);
    }*/



 }
