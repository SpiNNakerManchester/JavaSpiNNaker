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

import static java.lang.Byte.toUnsignedInt;
import static java.util.stream.IntStream.range;
import static uk.ac.manchester.spinnaker.messages.scp.IPTagFieldDefinitions.BYTE_SHIFT;

import java.net.InetAddress;
import java.nio.ByteBuffer;

import uk.ac.manchester.spinnaker.machine.HasChipLocation;

/** An SCP Request to initialise Big Data. */
public class BigDataInit extends SCPRequest<CheckOKResponse> {

    /**
     * Flag to indicate to use the sender IP address and port.
     */
    private static final int USE_SENDER_FLAG = 0x80000000;

    /**
     * Shift of port in argument 2.
     */
    private static final int PORT_SHIFT = 8;

    /**
     * Initialise Big Data with a specific IP Address and port.
     *
     * @param chip The chip to send the message to.
     * @param core The core to set up as Big Data receiver.
     * @param port The port to send the data back to.
     * @param ipAddress The IP Address to send the data back to.
     */
    public BigDataInit(HasChipLocation chip, int core, int port,
            InetAddress ipAddress) {
        super(chip.getScampCore(), SCPCommand.CMD_BIG_DATA,
                BigDataCommand.BIG_DATA_INIT.value, port << PORT_SHIFT | core,
                argument3(ipAddress.getAddress()));
    }

    /**
     * Initialise Big Data using the sender IP Address and port.
     *
     * @param chip The chip to send the message to.
     * @param core The core to set up as the Big Data receiver.
     */
    public BigDataInit(HasChipLocation chip, int core) {
        super(chip.getScampCore(), SCPCommand.CMD_BIG_DATA,
                BigDataCommand.BIG_DATA_INIT.value, USE_SENDER_FLAG | core, 0);
    }

    private static int argument3(byte[] host) {
        if (host == null) {
            return 0;
        }
        return range(0, host.length)
                .map(i -> toUnsignedInt(host[host.length - 1 - i]))
                .reduce(0, (i, j) -> (i << BYTE_SHIFT) | j);
    }

    @Override
    public CheckOKResponse getSCPResponse(ByteBuffer buffer) throws Exception {
        return new CheckOKResponse("Big Data Initialise",
                SCPCommand.CMD_BIG_DATA, buffer);
    }
}
