package uk.ac.manchester.spinnaker.alloc;

import uk.ac.manchester.spinnaker.machine.ChipLocation;
import uk.ac.manchester.spinnaker.spalloc.messages.BoardCoordinates;
import uk.ac.manchester.spinnaker.spalloc.messages.BoardPhysicalCoordinates;

public class BoardLocation {

	public Job job;
	public String machine;
	public ChipLocation chip;
	public BoardCoordinates logical;
	public BoardPhysicalCoordinates physical;

	public ChipLocation getBoardChip() {
		// TODO Auto-generated method stub
		return null;
	}

	public ChipLocation getChipRelativeTo(ChipLocation rootChip) {
		return new ChipLocation(chip.getX() - rootChip.getX(),
				chip.getY() - rootChip.getY());
	}

}
