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
public class InetIdTuple {

    /** The InetAddress of this tuple which may be null. */
    public final InetAddress address;

    /** The id of this tuple. */
    public final int id;

    /**
     * The main Constructor which sets all values.
     * @param address The InetAddress of this tuple which may be null.
     * @param id  The id of this tuple.
     */
    public InetIdTuple(InetAddress address, int id) {
        this.address = address;
        this.id = id;
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 29 * hash + Objects.hashCode(this.address);
        hash = 29 * hash + this.id;
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
        final InetIdTuple other = (InetIdTuple) obj;
        if (this.id != other.id) {
            return false;
        }
        if (!Objects.equals(this.address, other.address)) {
            return false;
        }
        return true;
    }

}
