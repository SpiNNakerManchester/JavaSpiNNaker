package uk.ac.manchester.spinnaker.spalloc.responses;

/**
 * The description of where some resource is on a SpiNNaker system.
 */
public class WhereIs {
	private int[] jobChip;
	private int jobId;
	private int[] chip;
	private int[] logical;
	private String machine;
	private int[] boardChip;
	private int[] physical;

	/**
	 * Get the job_chip.
	 *
	 * @return the job_chip
	 */
	public int[] getJobChip() {
		return jobChip;
	}

	/**
	 * Sets the job chip.
	 *
	 * @param jobChip
	 *            the job chip to set
	 */
	public void setJobChip(int[] jobChip) {
		this.jobChip = jobChip;
	}

	/**
	 * Get the job id.
	 *
	 * @return the job id
	 */
	public int getJobId() {
		return jobId;
	}

	/**
	 * Sets the job id.
	 *
	 * @param jobId
	 *            the job id to set
	 */
	public void setJobId(int jobId) {
		this.jobId = jobId;
	}

	/**
	 * Get the chip.
	 *
	 * @return the chip
	 */
	public int[] getChip() {
		return chip;
	}

	/**
	 * Sets the chip.
	 *
	 * @param chip
	 *            the chip to set
	 */
	public void setChip(int[] chip) {
		this.chip = chip;
	}

	/**
	 * Get the logical.
	 *
	 * @return the logical
	 */
	public int[] getLogical() {
		return logical;
	}

	/**
	 * Sets the logical.
	 *
	 * @param logical
	 *            the logical to set
	 */
	public void setLogical(int[] logical) {
		this.logical = logical;
	}

	/**
	 * Get the machine.
	 *
	 * @return the machine
	 */
	public String getMachine() {
		return machine;
	}

	/**
	 * Sets the machine.
	 *
	 * @param machine
	 *            the machine to set
	 */
	public void setMachine(String machine) {
		this.machine = machine;
	}

	/**
	 * Get the board chip.
	 *
	 * @return the board chip
	 */
	public int[] getBoardChip() {
		return boardChip;
	}

	/**
	 * Sets the board chip.
	 *
	 * @param boardChip
	 *            the board chip to set
	 */
	public void setBoardChip(int[] boardChip) {
		this.boardChip = boardChip;
	}

	/**
	 * Get the physical.
	 *
	 * @return the physical
	 */
	public int[] getPhysical() {
		return physical;
	}

	/**
	 * Sets the physical.
	 *
	 * @param physical
	 *            the physical to set
	 */
	public void setPhysical(int[] physical) {
		this.physical = physical;
	}
}
