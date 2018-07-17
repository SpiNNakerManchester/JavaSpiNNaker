/*
 * Copyright (c) 2018 The University of Manchester
 */
package uk.ac.manchester.spinnaker.machine.datalinks;

import java.net.InetAddress;
import java.util.Objects;

/**
 *
 * @author Christian-B
 */
public final class InetFpgaTuple {

    /** The InetAddress of this tuple which may be null. */
    public final InetAddress address;

    /** The id of fpga in this tuple. */
    public final FpgaId fpga;

    /** The id of link in this tuple. */
    public final int linkId;

    /**
     *
     * @param address The InetAddress of this tuple which may be null.
     * @param fpga The id of fpga in this tuple.
     * @param linkId  The id of link in this tuple.
     */
    public InetFpgaTuple(InetAddress address, FpgaId fpga, int linkId) {
        this.address = address;
        this.fpga = fpga;
        this.linkId = linkId;
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 29 * hash + Objects.hashCode(this.address);
        hash = 29 * hash + this.fpga.id;
        hash = 29 * hash + this.linkId;
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final InetFpgaTuple other = (InetFpgaTuple) obj;
        if (this.fpga != other.fpga) {
            return false;
        }
        if (this.linkId != other.linkId) {
            return false;
        }
        if (!Objects.equals(this.address, other.address)) {
            return false;
        }
        return true;
    }



}
