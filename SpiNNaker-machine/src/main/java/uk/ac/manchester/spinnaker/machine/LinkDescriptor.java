package uk.ac.manchester.spinnaker.machine;

public final class LinkDescriptor implements HasChipLocation {
    private final int x, y, linkID;

    /**
     * Create a link descriptor.
     *
     * @param x
     *            The X location of the source of the link.
     * @param y
     *            The Y location of the source of the link.
     * @param linkID
     *            The ID of the link.
     */
    public LinkDescriptor(int x, int y, int linkID) {
        this.x = x;
        this.y = y;
        this.linkID = linkID;
    }

    /**
     * Create a link descriptor.
     *
     * @param chip
     *            The coordinates of the source of the link.
     * @param linkID
     *            The ID of the link.
     */
    public LinkDescriptor(HasChipLocation chip, int linkID) {
        this.x = chip.getX();
        this.y = chip.getY();
        this.linkID = linkID;
    }

    /**
     * @return The X coordinate of the originating chip of the link.
     */
    @Override
    public int getX() {
        return x;
    }

    /**
     * @return The Y coordinate of the originating chip of the link.
     */
    @Override
    public int getY() {
        return y;
    }

    /**
     * @return The link ID of the link, as seen by the originating chip.
     */
    public int getLinkID() {
        return linkID;
    }

    @Override
    public int hashCode() {
        return (x << 16) ^ (y << 8) ^ linkID;
    }

    @Override
    public boolean equals(Object o) {
        return (o != null) && (o instanceof LinkDescriptor)
                && equals((LinkDescriptor) o);
    }

    /**
     * Optimised equality test.
     *
     * @param linkDescriptor
     *            the other link descriptor.
     * @return Whether they are equal.
     */
    public boolean equals(LinkDescriptor linkDescriptor) {
        return x == linkDescriptor.x && y == linkDescriptor.y
                && linkID == linkDescriptor.linkID;
    }
}
