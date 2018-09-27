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

	/**
	 * Create with default coordinates.
	 */
	public BoardPhysicalCoordinates() {
	}

	/**
	 * Create with given coordinates.
	 *
	 * @param cabinet
	 *            the cabinet ID
	 * @param frame
	 *            the frame ID within the cabinet
	 * @param board
	 *            the board ID within the frame
	 */
	public BoardPhysicalCoordinates(int cabinet, int frame, int board) {
		this.cabinet = cabinet;
		this.frame = frame;
		this.board = board;
	}

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

	@Override
	public boolean equals(Object o) {
		if (o != null && o instanceof BoardPhysicalCoordinates) {
			BoardPhysicalCoordinates other = (BoardPhysicalCoordinates) o;
			return cabinet == other.cabinet && frame == other.frame
					&& board == other.board;
		}
		return false;
	}

	@Override
	public int hashCode() {
		return 9 * (cabinet * 1234567 + frame * 56789 + board);
	}

	@Override
	public String toString() {
		return "Board{" + cabinet + "," + frame + "," + board + "}";
	}
}
