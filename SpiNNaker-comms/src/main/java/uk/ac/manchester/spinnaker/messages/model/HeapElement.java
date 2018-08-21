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

	private static final int FREE_MASK = 0xFFFF0000;
	private static final int BYTE_MASK = 0x000000FF;
	// WORD := [ BYTE3 | BYTE2 | BYTE1 | BYTE0 ]
	private static final int BYTE1_SHIFT = 8;
	private static final int BLOCK_HEADER_SIZE = 8;

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
		this.isFree = (free & FREE_MASK) != FREE_MASK;
		if (isFree) {
			tag = null;
			appID = null;
		} else {
			tag = free & BYTE_MASK;
			appID = (free >>> BYTE1_SHIFT) & BYTE_MASK;
		}
		size = nextAddress - blockAddress - BLOCK_HEADER_SIZE;
	}
}
