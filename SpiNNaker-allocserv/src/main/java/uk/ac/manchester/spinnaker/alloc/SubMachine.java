package uk.ac.manchester.spinnaker.alloc;

import java.util.List;

import uk.ac.manchester.spinnaker.spalloc.messages.BoardCoordinates;
import uk.ac.manchester.spinnaker.spalloc.messages.Connection;

public class SubMachine {
	public Machine machine;
	public int width;
	public int height;
	public List<Connection> connections;//FIXME
	public List<BoardCoordinates> boards;//FIXME

	public PowerState getPower() {
		// TODO Auto-generated method stub
		return null;
	}

	public void setPower(PowerState ps) {

	}
}
