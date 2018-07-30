package uk.ac.manchester.spinnaker.messages.model;

/** An element of one of the heaps on SpiNNaker. */
public class HeapElement {
	/** The address of the block. */
	public final int blockAddress;
	/** A pointer to the next block, or 0 if none. */
	public final int nextAddress;
	/** The usable size of this block (not including the header). */
	public final int size;
	/** True if the block is free. */
	public final boolean isFree;
	/** The tag of the block if allocated, or <tt>null</tt> if not. */
	public final Integer tag;
	/**
	 * The application ID of the block if allocated, or <tt>null</tt> if not.
	 */
	public final Integer appID;

	/**
	 * @param blockAddress
	 *            The address of this element on the heap
	 * @param nextAddress
	 *            The address of the next element on the heap
	 * @param free
	 *            The "free" element of the block as read from the heap
	 */
	public HeapElement(int blockAddress, int nextAddress, int free) {
		this.blockAddress = blockAddress;
		this.nextAddress = nextAddress;
		this.isFree = (free & 0xFFFF0000) != 0xFFFF0000;
		if (isFree) {
			tag = null;
			appID = null;
		} else {
			tag = free & 0xFF;
			appID = (free >> 8) & 0xFF;
		}
		size = nextAddress - blockAddress - 8;
	}
}
