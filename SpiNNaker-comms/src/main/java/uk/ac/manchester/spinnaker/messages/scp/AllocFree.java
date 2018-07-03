package uk.ac.manchester.spinnaker.messages.scp;

/** The SCP Allocation and Free codes */
public enum AllocFree {
	/** Allocate SDRAM */
	ALLOC_SDRAM(0),
	/** Free SDRAM using a Pointer */
	FREE_SDRAM_BY_POINTER(1),
	/** Free SDRAM using an APP ID */
	FREE_SDRAM_BY_APP_ID(2),
	/** Allocate Routing Entries */
	ALLOC_ROUTING(3),
	/** Free Routing Entries by Pointer */
	FREE_ROUTING_BY_POINTER(4),
	/** Free Routing Entries by APP ID */
	FREE_ROUTING_BY_APP_ID(5);
	public final byte value;

	private AllocFree(int value) {
		this.value = (byte) value;
	}
}
