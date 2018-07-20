package uk.ac.manchester.spinnaker.messages.model;

public class MulticastRoutingEntry extends RoutingEntry {
	// FIXME Class needs finishing off

	private int key;
	private int mask;
	private boolean flag;

	public MulticastRoutingEntry(int key, int mask, int route, boolean flag) {
		super(route);
		this.key = key;
		this.mask = mask;
		this.flag = flag;
	}

	public MulticastRoutingEntry(int key, int mask, int[] processor_ids,
			int[] link_ids, boolean flag) {
		super(processor_ids, link_ids);
		this.key = key;
		this.mask = mask;
		this.flag = flag;
	}

	public int getKey() {
		return key;
	}

	public int getMask() {
		return mask;
	}
}
