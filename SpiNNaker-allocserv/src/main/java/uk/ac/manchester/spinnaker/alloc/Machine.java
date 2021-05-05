package uk.ac.manchester.spinnaker.alloc;

import java.util.List;

public class Machine {
	public int id;
	public String name;
	public List<String> tags;
	public int width;
	public int height;
	// TODO: dead boards, dead links

	public void waitForChange() {
		// TODO Auto-generated method stub

	}

	public BoardLocation getBoardByChip(int x, int y) {
		// TODO Auto-generated method stub
		return null;
	}

	public BoardLocation getBoardByPhysicalCoords(int cabinet, int frame,
			int board) {
		// TODO Auto-generated method stub
		return null;
	}

	public BoardLocation getBoardByLogicalCoords(int x, int y, int z) {
		// TODO Auto-generated method stub
		return null;
	}

}
