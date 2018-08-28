/*
 * Copyright (c) 2018 The University of Manchester
 */
package uk.ac.manchester.spinnaker.front_end.interfaces.buffer_management;

import java.io.IOException;
import java.net.InetAddress;
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
public class EIEIOConnectionFactory1 implements
        //UDPTransceiver.ConnectionFactory<EIEIOConnection>{
        UDPTransceiver.ConnectionFactory<UDPConnection<EIEIOMessage<? extends EIEIOHeader>>> {

    @Override
    public Class<UDPConnection<EIEIOMessage<? extends EIEIOHeader>>> getClassKey() {
        try {
            EIEIOConnection stub = new EIEIOConnection(null, null, null, null);
            UDPConnection<EIEIOMessage<? extends EIEIOHeader>> stub2 = (UDPConnection<EIEIOMessage<? extends EIEIOHeader>>)stub;
            return (Class<UDPConnection<EIEIOMessage<? extends EIEIOHeader>>>)stub2.getClass();
        } catch (IOException ex) {
            throw new Error(ex);
        }
    }

    @Override
    public UDPConnection<EIEIOMessage<? extends EIEIOHeader>> getInstance(InetAddress localAddress) throws IOException {
        return new EIEIOConnection(localAddress, null, null, null);
    }

    @Override
    public UDPConnection<EIEIOMessage<? extends EIEIOHeader>> getInstance(InetAddress localAddress, int localPort) throws IOException {
        return new EIEIOConnection(localAddress, null, null, null);
    }


 }
