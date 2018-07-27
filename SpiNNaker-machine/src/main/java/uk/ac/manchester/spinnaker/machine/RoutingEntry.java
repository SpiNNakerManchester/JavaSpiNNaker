package uk.ac.manchester.spinnaker.machine;

import static java.util.Objects.requireNonNull;
import static java.util.stream.IntStream.range;

/** A basic SpiNNaker routing entry. */
public class RoutingEntry {
    private static final int NUM_PROCESSORS = 26;
    private static final int NUM_LINKS = 6;

    private int[] processorIDs;
    private int[] linkIDs;

    private static boolean bitset(int word, int bit) {
        return (word & (1 << bit)) != 0;
    }

    /**
     * Create a routing entry from its encoded form.
     *
     * @param route
     *            the encoded route
     */
    public RoutingEntry(int route) {
        processorIDs = range(0, NUM_PROCESSORS)
                .filter(pi -> bitset(route, NUM_LINKS + pi)).toArray();
        linkIDs = range(0, NUM_LINKS).filter(li -> bitset(route, li)).toArray();
    }

    /**
     * Create a routing entry from its expanded description.
     *
     * @param processorIDs
     *            The IDs of the processors that this entry routes to.
     * @param linkIDs
     *            The IDs of the links that this entry routes to.
     */
    public RoutingEntry(int[] processorIDs, int[] linkIDs) {
        this.processorIDs = processorIDs.clone();
        this.linkIDs = linkIDs.clone();
    }

    /**
     * @return The word-encoded form of the routing entry.
     */
    public int encode() {
        int route = 0;
        for (int processorID : processorIDs) {
            if (processorID >= NUM_PROCESSORS || processorID < 0) {
                throw new IllegalArgumentException(
                        "Processor IDs must be between 0 and 25");
            }
            route |= 1 << (6 + processorID);
        }
        for (int linkID : linkIDs) {
            if (linkID >= NUM_LINKS || linkID < 0) {
                throw new IllegalArgumentException(
                        "Link IDs must be between 0 and 5");
            }
            route |= 1 << linkID;
        }
        return route;
    }

    public int[] getProcessorIDs() {
        return processorIDs;
    }

    public int getProcessorIDs(int index) {
        return processorIDs[index];
    }

    public void setProcessorIDs(int[] newValue) {
        processorIDs = requireNonNull(newValue);
    }

    public void setProcessorIDs(int index, int newValue) {
        processorIDs[index] = newValue;
    }

    public int[] getLinkIDs() {
        return linkIDs;
    }

    public int getLinkIDs(int index) {
        return linkIDs[index];
    }

    public void setLinkIDs(int[] newValue) {
        linkIDs = requireNonNull(newValue);
    }

    public void setLinkIDs(int index, int newValue) {
        linkIDs[index] = newValue;
    }
}
