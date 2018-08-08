package uk.ac.manchester.spinnaker.machine;

/** A multicast packet routing table entry. */
public class MulticastRoutingEntry extends RoutingEntry {
    private final int key;
    private final int mask;
    private final boolean defaultable;

    /**
     * Create a multicast routing table entry.
     *
     * @param key
     *            The key of the entry.
     * @param mask
     *            The mask of the entry.
     * @param route
     *            The route descriptor.
     * @param defaultable
     *            Whether this entry is default routable.
     */
    public MulticastRoutingEntry(int key, int mask, int route,
            boolean defaultable) {
        super(route);
        this.key = key;
        this.mask = mask;
        this.defaultable = defaultable;
    }

    /**
     * Create a multicast routing table entry.
     *
     * @param key
     *            The key of the entry.
     * @param mask
     *            The mask of the entry.
     * @param processorIDs
     *            The IDs of the processors that this entry routes to.
     *            The Duplicate IDs are ignored.
     * @param linkIDs
     *            The IDs of the links that this entry routes to.
     *            The Duplicate IDs are ignored.
     * @param defaultable
     *            Whether this entry is default routable.
     */
    public MulticastRoutingEntry(
            int key, int mask, Iterable<Integer> processorIDs,
            Iterable<Direction> linkIDs, boolean defaultable) {
        super(processorIDs, linkIDs);
        this.key = key;
        this.mask = mask;
        this.defaultable = defaultable;
    }

    /** @return the key of this entry */
    public int getKey() {
        return key;
    }

    /** @return the mask of this entry */
    public int getMask() {
        return mask;
    }

    /** @return whether this entry is default routable */
    public boolean isDefaultable() {
        return defaultable;
    }
}
