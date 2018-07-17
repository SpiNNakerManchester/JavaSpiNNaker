package uk.ac.manchester.spinnaker.messages.model;

/** An element of one of the heaps on SpiNNaker. */
public class HeapElement {
	/** The address of the block */
	public final int block_address;
	/** A pointer to the next block, or 0 if none. */
	public final int next_address;
	/** The usable size of this block (not including the header). */
	public final int size;
	/** True if the block is free */
	public final boolean is_free;
	/** The tag of the block if allocated, or <tt>null</tt> if not. */
	public final Integer tag;
	/** The app ID of the block if allocated, or <tt>null</tt> if not. */
	public final Integer app_id;

	/**
	 * @param block_address
	 *            The address of this element on the heap
	 * @param next_address
	 *            The address of the next element on the heap
	 * @param free
	 *            The "free" element of the block as read from the heap
	 */
	public HeapElement(int blockAddress, int nextAddress, int free) {
		this.block_address = blockAddress;
		this.next_address = nextAddress;
		this.is_free = (free & 0xFFFF0000) != 0xFFFF0000;
		if (is_free) {
			tag = null;
			app_id = null;
		} else {
			tag = free & 0xFF;
			app_id = (free >> 8) & 0xFF;
		}
		size = next_address - block_address - 8;
	}
}
