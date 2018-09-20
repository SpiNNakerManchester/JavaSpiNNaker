package uk.ac.manchester.spinnaker.spalloc.messages;

import uk.ac.manchester.spinnaker.machine.ChipLocation;

/**
 * The description of where some resource is on a SpiNNaker system.
 */
public class WhereIs {
	private ChipLocation jobChip;
	private int jobId;
	private ChipLocation chip;
	private BoardCoordinates logical;
	private String machine;
	private ChipLocation boardChip;
	private BoardPhysicalCoordinates physical;

	/**
	 * Get the chip location relative to the job's allocation.
	 *
	 * @return the job-relative chip location
	 */
	public ChipLocation getJobChip() {
		return jobChip;
	}

	/**
	 * Sets the chip location relative to the job's allocation.
	 *
	 * @param jobChip
	 *            the job-relative chip location to set
	 */
	public void setJobChip(Chip jobChip) {
		this.jobChip = jobChip.asChipLocation();
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
	public ChipLocation getChip() {
		return chip;
	}

	/**
	 * Sets the chip.
	 *
	 * @param chip
	 *            the chip to set
	 */
	public void setChip(Chip chip) {
		this.chip = chip.asChipLocation();
	}

	/**
	 * Get the logical board coordinates.
	 *
	 * @return the logical board coordinates
	 */
	public BoardCoordinates getLogical() {
		return logical;
	}

	/**
	 * Sets the logical board coordinates.
	 *
	 * @param logical
	 *            the logical board coordinates to set
	 */
	public void setLogical(BoardCoordinates logical) {
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
	 * Get the chip location relative to the board.
	 *
	 * @return the board chip location
	 */
	public ChipLocation getBoardChip() {
		return boardChip;
	}

	/**
	 * Sets the chip location relative to the board.
	 *
	 * @param boardChip
	 *            the board chip location to set
	 */
	public void setBoardChip(Chip boardChip) {
		this.boardChip = boardChip.asChipLocation();
	}

	/**
	 * Get the physical board coordinates.
	 *
	 * @return the physical board coordinates
	 */
	public BoardPhysicalCoordinates getPhysical() {
		return physical;
	}

	/**
	 * Sets the physical board coordinates.
	 *
	 * @param physical
	 *            the physical board coordinates to set
	 */
	public void setPhysical(BoardPhysicalCoordinates physical) {
		this.physical = physical;
	}
    
    @Override
	public boolean equals(Object o) {
		if (o != null && o instanceof WhereIs) {
			WhereIs other = (WhereIs) o;
			return jobChip.equals(other.jobChip) 
                    && jobId == other.jobId 
                    && chip.equals(other.chip)
                    && logical.equals(other.logical)
                    && machine.equals(other.machine)
                    && boardChip.equals(other.boardChip)
                    && physical.equals(other.physical);
		}
		return false;
	}

}
