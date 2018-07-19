package uk.ac.manchester.spinnaker.processes;

import static java.util.stream.IntStream.range;

public class MulticastRoutingEntry {
	// FIXME Class needs finishing off

	private static final int NUM_PROCESSORS = 26;
	private static final int NUM_LINKS = 6;

	private int[] processor_ids;
	private int[] link_ids;
	private int key;
	private int mask;
	private boolean flag;

	private static boolean bitset(int word, int bit) {
		return (word & (1 << bit)) != 0;
	}

	public MulticastRoutingEntry(int key, int mask, int route, boolean flag) {
		this.processor_ids = range(0, NUM_PROCESSORS)
				.filter(pi -> bitset(route, NUM_LINKS + pi)).toArray();
		this.link_ids = range(0, NUM_LINKS).filter(li -> bitset(route, li))
				.toArray();
		this.key = key;
		this.mask = mask;
		this.flag = flag;
	}

	public MulticastRoutingEntry(int key, int mask, int[] processor_ids,
			int[] link_ids, boolean flag) {
		this.key = key;
		this.mask = mask;
		this.processor_ids = processor_ids;
		this.link_ids = link_ids;
		this.flag = flag;
	}

}
