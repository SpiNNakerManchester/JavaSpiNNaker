package uk.ac.manchester.spinnaker.spalloc.messages;

import static com.fasterxml.jackson.annotation.JsonFormat.Shape.ARRAY;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

/**
 * The physical coordinates of a board.
 */
@JsonPropertyOrder({
		"cabinet", "frame", "board"
})
@JsonFormat(shape = ARRAY)
public class BoardPhysicalCoordinates {
	private int cabinet, frame, board;

	public int getCabinet() {
		return cabinet;
	}

	public void setCabinet(int cabinet) {
		this.cabinet = cabinet;
	}

	public int getFrame() {
		return frame;
	}

	public void setFrame(int frame) {
		this.frame = frame;
	}

	public int getBoard() {
		return board;
	}

	public void setBoard(int board) {
		this.board = board;
	}
}
