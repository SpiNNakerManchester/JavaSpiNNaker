package uk.ac.manchester.spinnaker.machine.tags;

import static java.lang.Integer.rotateLeft;

import java.net.InetAddress;

import uk.ac.manchester.spinnaker.machine.CoreLocation;
import uk.ac.manchester.spinnaker.machine.HasCoreLocation;

/**
 * Used to hold data that is contained within a Reverse IP tag. Reverse IP tags
 * allow data to flow at runtime from the outside world into SpiNNaker.
 */
public final class ReverseIPTag extends Tag {
    private static final int DEFAULT_SDP_PORT = 1;
    private final CoreLocation destination;
    private final int sdpPort;

    /**
     * Create a reverse IP tag.
     *
     * @param boardAddress
     *            The IP address of the board on which the tag is allocated.
     * @param tagID
     *            The tag of the SDP packet.
     * @param udpPort
     *            The UDP port on which SpiNNaker will listen for packets.
     * @param destination
     *            The coordinates of the core to send packets to.
     */
    public ReverseIPTag(InetAddress boardAddress, int tagID, int udpPort,
            HasCoreLocation destination) {
        this(boardAddress, tagID, udpPort, destination, DEFAULT_SDP_PORT);
    }

    /**
     * Create a reverse IP tag.
     *
     * @param boardAddress
     *            The IP address of the board on which the tag is allocated.
     * @param tagID
     *            The tag of the SDP packet.
     * @param udpPort
     *            The UDP port on which SpiNNaker will listen for packets.
     * @param destination
     *            The coordinates of the core to send packets to.
     * @param sdpPort
     *            The port number to use for SDP packets that are formed on the
     *            machine.
     */
    public ReverseIPTag(InetAddress boardAddress, int tagID, int udpPort,
            HasCoreLocation destination, int sdpPort) {
        super(boardAddress, tagID, udpPort);
        this.destination = destination.asCoreLocation();
        this.sdpPort = sdpPort;
    }

    /**
     * @return The destination coordinates of a core in the SpiNNaker machine
     *         that packets should be sent to for this reverse IP tag.
     */
    public CoreLocation getDestination() {
        return destination;
    }

    /**
     * @return The SDP port number of the tag that these packets are to be
     *         received on for the processor.
     */
    public int getSdpPort() {
        return sdpPort;
    }

    @Override
    public boolean equals(Object o) {
        if (o != null && o instanceof ReverseIPTag) {
            return equals((ReverseIPTag) o);
        }
        return false;
    }

    /**
     * An optimised test for whether two {@link ReverseIPTag}s are equal.
     *
     * @param otherTag
     *            The other tag
     * @return whether they are equal
     */
    public boolean equals(ReverseIPTag otherTag) {
        if (otherTag == null) {
            return false;
        }
        return partialEquals(otherTag) && sdpPort == otherTag.sdpPort
                && destination.equals(otherTag.destination);
    }

    @Override
    public int hashCode() {
        int h = partialHashCode();
        h ^= rotateLeft(sdpPort, 11);
        h ^= rotateLeft(destination.hashCode(), 19);
        return h;
    }
}
