package uk.ac.manchester.spinnaker.machine;

public class MulticastRoutingEntry extends RoutingEntry {
	private int key;
	private int mask;
	private boolean defaultable;

	public MulticastRoutingEntry(int key, int mask, int route,
			boolean defaultable) {
		super(route);
		this.key = key;
		this.mask = mask;
		this.defaultable = defaultable;
	}

	public MulticastRoutingEntry(int key, int mask, int[] processor_ids,
			int[] link_ids, boolean defaultable) {
		super(processor_ids, link_ids);
		this.key = key;
		this.mask = mask;
		this.defaultable = defaultable;
	}

	public int getKey() {
		return key;
	}

	public int getMask() {
		return mask;
	}

	public boolean isDefaultable() {
		return defaultable;
	}
}
