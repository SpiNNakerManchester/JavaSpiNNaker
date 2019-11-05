/*
 * Copyright (c) 2018 The University of Manchester
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package uk.ac.manchester.spinnaker.messages.scp;

import static uk.ac.manchester.spinnaker.messages.scp.SCPCommand.CMD_BIG_DATA;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;

import uk.ac.manchester.spinnaker.machine.HasChipLocation;
import uk.ac.manchester.spinnaker.messages.model.UnexpectedResponseCodeException;

/** An SCP Request information about Big Data. */
public class BigDataInfo extends SCPRequest<BigDataInfo.Response> {

    /**
    * @param chip
    *            The chip to query for information.
    */
    public BigDataInfo(HasChipLocation chip) {
        super(chip.getScampCore(), CMD_BIG_DATA,
                BigDataCommand.BIG_DATA_INFO.value);
    }

    @Override
    public Response getSCPResponse(ByteBuffer buffer)
            throws UnexpectedResponseCodeException {
        return new BigDataInfo.Response(buffer);
    }

    /** An SCP response to a request for information about Big Data. */
    public static class Response extends CheckOKResponse {
        /** The number of bytes in an IP address. */
        private static final int IP_ADDR_BYTES = 4;
        /** The port where data is sent to. */
        public final int port;
        /** The number of packets sent back to host. */
        public final int nSent;
        /** The number of packets received by the machine. */
        public final int nReceived;
        /** The number of packets discarded as the buffer was in use. */
        public final int nDiscarded;
        /** The IP address where data is sent to. */
        public final InetAddress ipAddress;

        Response(ByteBuffer buffer) throws UnexpectedResponseCodeException {
            super("Get Big Data Info", CMD_BIG_DATA, buffer);

            port = buffer.getInt();
            nSent = buffer.getInt();
            nReceived = buffer.getInt();
            nDiscarded = buffer.getInt();
            byte[] ip = new byte[IP_ADDR_BYTES];
            buffer.get(ip);
            try {
                ipAddress = InetAddress.getByAddress(ip);
            } catch (UnknownHostException e) {
                e.printStackTrace();
                throw new UnexpectedResponseCodeException(
                        "Get Big Data Info", CMD_BIG_DATA.toString(),
                        "Error getting host name from data: " + e.getMessage());
            }
        }

        @Override
        public String toString() {
            return "Big Data: Sending to " + ipAddress + ":" + port + "; nSent="
                    + nSent + "; nReceived=" + nReceived + "; nDiscarded="
                    + nDiscarded;
        }
    }
}
