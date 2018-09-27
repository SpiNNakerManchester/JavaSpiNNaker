package uk.ac.manchester.spinnaker.spalloc.messages;

/**
 * Request the physical location of a board.
 */
public class GetBoardPositionCommand extends Command<Integer> {
	/**
	 * Create a request.
	 *
	 * @param machine
	 *            The machine to ask about.
	 * @param coords
	 *            The logical coordinates of the board.
	 */
	public GetBoardPositionCommand(String machine, BoardCoordinates coords) {
		super("get_board_position");
		addKwArg("machine_name", machine);
		addKwArg("x", coords.getX());
		addKwArg("y", coords.getY());
		addKwArg("z", coords.getZ());
	}
}
