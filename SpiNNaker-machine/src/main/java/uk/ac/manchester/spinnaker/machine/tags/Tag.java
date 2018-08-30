package uk.ac.manchester.spinnaker.machine.tags;

import java.net.InetAddress;

/** Common properties of SpiNNaker IP tags and reverse IP tags. */
public abstract class Tag {
    /** The board address associated with this tagID. */
    private final InetAddress boardAddress;
    /** The tagID ID associated with this tagID. */
    private final int tagID;
    /** The port number associated with this tagID. */
    private Integer port;

    /**
     * Create a tag.
     *
     * @param boardAddress
     *            The address of the board where the tag is.
     * @param tagID
     *            The ID of the tag (0-7?)
     * @param port
     *            The port of the tag.
     */
    Tag(InetAddress boardAddress, int tagID, Integer port) {
        this.boardAddress = boardAddress;
        this.tagID = tagID;
        this.port = port;
    }

    /** @return The board address of the tagID. */
    public InetAddress getBoardAddress() {
        return boardAddress;
    }

    /** @return The tagID ID of the tagID. */
    public int getTag() {
        return tagID;
    }

    /**
     * @return The port of the tagID, or <tt>null</tt> if there isn't one yet.
     */
    public Integer getPort() {
        return port;
    }

    /**
     * Set the port; will fail if the port is already set.
     *
     * @param port
     *            the port to set
     */
    public void setPort(int port) {
        if (this.port != null && this.port != port) {
            throw new IllegalStateException(
                    "port cannot be changed to " + port
                    + " once set to " + this.port);
        }
        this.port = port;
    }

    // Subclasses *must* create an equality test and hash code
    /** {@inheritDoc} */
    @Override
    public abstract boolean equals(Object other);

    /** {@inheritDoc} */
    @Override
    public abstract int hashCode();

    /**
     * Partial equality test between two tags. Only compares on fields defined
     * in the {@link Tag} class. Used to make implementing a full equality test
     * simpler.
     *
     * @param otherTag
     *            The other tag to compare to.
     * @return Whether the two tags are partially equal.
     */
    protected final boolean partialEquals(Tag otherTag) {
        return tagID == otherTag.tagID
                && boardAddress.equals(otherTag.boardAddress)
                && (port == null ? otherTag.port == null
                        : (otherTag.port != null
                                && port.equals(otherTag.port)));
    }

    private static final int MAGIC1 = 43;
    private static final int MAGIC2 = 17;

    /**
     * Partial hash code of a tag. Used to make implementing a full hash
     * simpler.
     *
     * @return The hash of this class's fields.
     */
    protected final int partialHashCode() {
        int h = (tagID * MAGIC1) ^ boardAddress.hashCode();
        if (port != null) {
            h ^= port * MAGIC2;
        }
        return h;
    }
}
