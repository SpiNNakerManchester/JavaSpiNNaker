package uk.ac.manchester.spinnaker.machine;

import static java.util.Objects.requireNonNull;
import static java.util.stream.IntStream.range;

public class RoutingEntry {
    private static final int NUM_PROCESSORS = 26;
    private static final int NUM_LINKS = 6;

    private int[] processor_ids;
    private int[] link_ids;

    private static boolean bitset(int word, int bit) {
        return (word & (1 << bit)) != 0;
    }

    public RoutingEntry(int route) {
        processor_ids = range(0, NUM_PROCESSORS)
                .filter(pi -> bitset(route, NUM_LINKS + pi)).toArray();
        link_ids = range(0, NUM_LINKS).filter(li -> bitset(route, li))
                .toArray();
    }

    public RoutingEntry(int[] processor_ids, int[] link_ids) {
        this.processor_ids = processor_ids.clone();
        this.link_ids = link_ids.clone();
    }

    public int encode() {
        int route_entry = 0;
        for (int processor_id : processor_ids) {
            if (processor_id >= NUM_PROCESSORS || processor_id < 0) {
                throw new IllegalArgumentException(
                        "Processor IDs must be between 0 and 25");
            }
            route_entry |= (1 << (6 + processor_id));
        }
        for (int link_id : link_ids) {
            if (link_id >= NUM_LINKS || link_id < 0) {
                throw new IllegalArgumentException(
                        "Link IDs must be between 0 and 5");
            }
            route_entry |= (1 << link_id);
        }
        return route_entry;
    }

    public int[] getProcessorIDs() {
        return processor_ids;
    }

    public int getProcessorIDs(int index) {
        return processor_ids[index];
    }

    public void setProcessorIDs(int[] newValue) {
        processor_ids = requireNonNull(newValue);
    }

    public void setProcessorIDs(int index, int newValue) {
        processor_ids[index] = newValue;
    }

    public int[] getLinkIDs() {
        return link_ids;
    }

    public int getLinkIDs(int index) {
        return link_ids[index];
    }

    public void setLinkIDs(int[] newValue) {
        link_ids = requireNonNull(newValue);
    }

    public void setLinkIDs(int index, int newValue) {
        link_ids[index] = newValue;
    }
}
